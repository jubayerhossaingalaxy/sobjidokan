package com.jubayerhossain.sobjidokan.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jubayerhossain.sobjidokan.data.backup.BackupManager
import com.jubayerhossain.sobjidokan.data.backup.SobjiKhataBackupPayload
import com.jubayerhossain.sobjidokan.data.local.AppDatabase
import com.jubayerhossain.sobjidokan.data.local.entity.ExpenseCategory
import com.jubayerhossain.sobjidokan.data.local.entity.PartyEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyType
import com.jubayerhossain.sobjidokan.data.local.entity.PaymentMethod
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseItemEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleItemEntity
import com.jubayerhossain.sobjidokan.data.local.entity.UnitType
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableCategory
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableEntity
import com.jubayerhossain.sobjidokan.data.local.entity.WastageReason
import com.jubayerhossain.sobjidokan.data.prefs.AppThemeMode
import com.jubayerhossain.sobjidokan.data.prefs.ShopPreferencesRepository
import com.jubayerhossain.sobjidokan.data.repository.ShopRepository
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ShopViewModel(
  application: Application,
) : AndroidViewModel(application) {
  private val db = AppDatabase.getInstance(application)
  private val repo = ShopRepository(db.shopDao(), db)
  private val prefsRepo = ShopPreferencesRepository(application)
  private val backupManager = BackupManager(application, db.shopDao(), prefsRepo)

  private val _posCart = MutableStateFlow<List<PosCartItem>>(emptyList())
  val posCart: StateFlow<List<PosCartItem>> = _posCart.asStateFlow()
  private val _purchaseCart = MutableStateFlow<List<PurchaseDraftItem>>(emptyList())
  val purchaseCart: StateFlow<List<PurchaseDraftItem>> = _purchaseCart.asStateFlow()
  private val _preselectedPosCustomerId = MutableStateFlow<Long?>(null)
  val preselectedPosCustomerId: StateFlow<Long?> = _preselectedPosCustomerId.asStateFlow()
  private val pinLock = PinLockController(prefsRepo, viewModelScope)
  private val _activeSaveCount = MutableStateFlow(0)
  val activeSaveCount: StateFlow<Int> = _activeSaveCount.asStateFlow()
  private val writeMutex = Mutex()

  private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val snackbarEvents = _snackbarEvents.asSharedFlow()

  private val backupCoordinator =
    ShopBackupCoordinator(
      backupManager = backupManager,
      snackbarEvents = _snackbarEvents,
      launchSerialized = ::launchSerializedSave,
      scope = viewModelScope,
      onRestored = {
        _posCart.value = emptyList()
        _purchaseCart.value = emptyList()
      },
    )

  val pendingRestorePayload: StateFlow<SobjiKhataBackupPayload?> =
    backupCoordinator.pendingRestorePayload

  private fun launchSerializedSave(block: suspend () -> Unit) {
    _activeSaveCount.update { it + 1 }
    viewModelScope.launch {
      try {
        writeMutex.withLock { block() }
      } finally {
        _activeSaveCount.update { (it - 1).coerceAtLeast(0) }
      }
    }
  }

  private val sessionFlow =
    combine(
      combine(_posCart, _purchaseCart, _preselectedPosCustomerId) {
        posCart,
        purchaseCart,
        custId,
        ->
        Triple(posCart, purchaseCart, custId)
      },
      combine(pinLock.isUnlocked, _activeSaveCount, pinLock.lockoutUntilMs) {
        unlocked,
        saveCount,
        lockoutMs,
        ->
        Triple(unlocked, saveCount > 0, lockoutMs)
      },
    ) { (posCart, purchaseCart, custId), (unlocked, saving, lockoutMs) ->
      ShopSessionState(
        posCart = posCart,
        purchaseCart = purchaseCart,
        preselectedCustomerId = custId,
        isUnlocked = unlocked,
        isSaving = saving,
        memLockoutUntilMs = lockoutMs,
      )
    }

  val uiState: StateFlow<ShopUiState> =
    combine(prefsRepo.preferencesFlow, requireNotNull(repo.atomicSnapshot), sessionFlow) {
      prefs,
      snapshot,
      session,
      ->
      ShopStateAssembler.buildUiState(prefs, snapshot, session)
    }.flowOn(Dispatchers.Default)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_STOP_TIMEOUT_MS),
        initialValue = ShopUiState(),
      )

  private val preferencesActions =
    ShopPreferencesActions(
      prefsRepo = prefsRepo,
      repo = repo,
      pinLock = pinLock,
      uiStateProvider = { uiState.value },
      snackbarEvents = _snackbarEvents,
      scope = viewModelScope,
      launchSerializedSave = ::launchSerializedSave,
    )

  private val inventoryActions =
    ShopInventoryActions(
      repo = repo,
      snackbarEvents = _snackbarEvents,
      uiStateProvider = { uiState.value },
      launchSerializedSave = ::launchSerializedSave,
    )

  private val ledgerActions =
    ShopLedgerActions(
      repo = repo,
      snackbarEvents = _snackbarEvents,
      uiStateProvider = { uiState.value },
      scope = viewModelScope,
      launchSerializedSave = ::launchSerializedSave,
    )

  fun onAppMovedToBackground() = preferencesActions.onAppMovedToBackground()

  fun onAppReturnedToForeground() = preferencesActions.onAppReturnedToForeground()

  fun unlockWithPin(pin: String): Boolean = preferencesActions.unlockWithPin(pin)

  fun completeInitialSetup(
    shopName: String,
    ownerName: String,
    useBengaliDigits: Boolean,
    seedDefaults: Boolean,
  ) = preferencesActions.completeInitialSetup(shopName, ownerName, useBengaliDigits, seedDefaults)

  fun updateShopProfile(
    shopName: String,
    ownerName: String,
  ) = preferencesActions.updateShopProfile(shopName, ownerName)

  fun setUseBengaliDigits(enabled: Boolean) = preferencesActions.setUseBengaliDigits(enabled)

  fun setShowCreditLedger(enabled: Boolean) = preferencesActions.setShowCreditLedger(enabled)

  fun setThemeMode(mode: AppThemeMode) = preferencesActions.setThemeMode(mode)

  fun preselectPosCustomer(customerId: Long?) {
    _preselectedPosCustomerId.value = customerId
  }

  fun setPinCode(pin: String) = preferencesActions.setPinCode(pin)

  // POS Cart actions
  fun addToPosCart(
    veg: VegetableEntity,
    quantityUnits: Long,
    customRate: Double,
  ) {
    if (!customRate.isFinite() || customRate <= 0.0 || quantityUnits <= 0L) return
    val lineTotal = BusinessCalculator.calculateLineTotal(quantityUnits, veg.unitType, customRate)
    if (!lineTotal.isFinite() || lineTotal <= 0.0) return
    val current = _posCart.value.toMutableList()
    val idx = current.indexOfFirst { it.vegetable.id == veg.id }
    val item = PosCartItem(veg, quantityUnits, customRate, lineTotal)
    if (idx >= 0) current[idx] = item else current.add(item)
    _posCart.value = current
  }

  fun removeFromPosCart(vegId: Long) {
    _posCart.value = _posCart.value.filterNot { it.vegetable.id == vegId }
  }

  fun clearPosCart() {
    _posCart.value = emptyList()
  }

  fun completePosSale(
    customerId: Long?,
    customerName: String,
    discount: Double,
    paidAmount: Double,
    paymentMethod: PaymentMethod,
    notes: String = "",
    onSuccess: () -> Unit = {},
  ) {
    if (!discount.isFinite() || !paidAmount.isFinite()) {
      _snackbarEvents.tryEmit("সঠিক টাকার পরিমাণ লিখুন।")
      return
    }
    if (_posCart.value.isEmpty()) return

    launchSerializedSave {
      val cart = _posCart.value
      if (cart.isEmpty()) return@launchSerializedSave
      val subtotal = BusinessCalculator.round2(cart.sumOf { it.lineTotal })
      val cleanDiscount = BusinessCalculator.round2(discount.coerceIn(0.0, subtotal))
      val totalAmount = BusinessCalculator.round2((subtotal - cleanDiscount).coerceAtLeast(0.0))
      val cleanPaid = BusinessCalculator.round2(paidAmount.coerceIn(0.0, totalAmount))
      val due = BusinessCalculator.round2((totalAmount - cleanPaid).coerceAtLeast(0.0))
      try {
        val saleItems =
          cart.map {
            SaleItemEntity(
              saleId = 0L,
              vegetableId = it.vegetable.id,
              vegetableName = it.vegetable.name,
              unitType = it.vegetable.unitType,
              quantityUnits = it.quantityUnits,
              sellingPricePerUnit = it.sellingPricePerUnit,
              costSnapshotPerUnit = it.vegetable.avgCostPerUnit,
              lineTotal = it.lineTotal,
            )
          }
        repo.completeSale(
          customerId = customerId,
          customerName = customerName.ifBlank { "সাধারণ ক্রেতা" },
          subtotal = subtotal,
          discount = cleanDiscount,
          totalAmount = totalAmount,
          paidAmount = cleanPaid,
          dueAmount = due,
          paymentMethod = paymentMethod,
          notes = notes,
          items = saleItems,
        )
        _posCart.value = emptyList()
        _preselectedPosCustomerId.value = null
        val formattedTotal =
          BusinessCalculator.formatTaka(totalAmount, uiState.value.preferences.useBengaliDigits)
        _snackbarEvents.tryEmit("বিক্রি সংরক্ষিত হয়েছে • $formattedTotal")
        onSuccess()
      } catch (_: Exception) {
        _snackbarEvents.tryEmit("তথ্য সংরক্ষণ করা যায়নি। আবার চেষ্টা করুন।")
      }
    }
  }

  fun cancelSale(saleId: Long) {
    launchSerializedSave {
      try {
        repo.cancelSale(saleId)
        _snackbarEvents.tryEmit("বিক্রি বাতিল হয়েছে ও সবজি স্টকে ফিরেছে")
      } catch (_: Exception) {
        _snackbarEvents.tryEmit("বিক্রি বাতিল করা যায়নি। আবার চেষ্টা করুন।")
      }
    }
  }

  // Purchase Draft Actions
  fun addPurchaseDraftItem(
    veg: VegetableEntity,
    quantityUnits: Long,
    unitCost: Double,
  ) {
    if (!unitCost.isFinite() || unitCost <= 0.0 || quantityUnits <= 0L) return
    val lineTotal = BusinessCalculator.calculateLineTotal(quantityUnits, veg.unitType, unitCost)
    if (!lineTotal.isFinite() || lineTotal <= 0.0) return
    val list = _purchaseCart.value.toMutableList()
    val idx = list.indexOfFirst { it.vegetable.id == veg.id }
    val item = PurchaseDraftItem(veg, quantityUnits, unitCost, lineTotal)
    if (idx >= 0) list[idx] = item else list.add(item)
    _purchaseCart.value = list
  }

  fun removePurchaseDraftItem(vegId: Long) {
    _purchaseCart.value = _purchaseCart.value.filterNot { it.vegetable.id == vegId }
  }

  fun clearPurchaseDraft() {
    _purchaseCart.value = emptyList()
  }

  fun completePurchase(
    supplierId: Long?,
    supplierName: String,
    extraCost: Double,
    paidAmount: Double,
    notes: String,
    timestamp: Long,
    onSuccess: () -> Unit,
  ) {
    if (!extraCost.isFinite() || !paidAmount.isFinite()) {
      _snackbarEvents.tryEmit("সঠিক টাকার পরিমাণ লিখুন।")
      return
    }
    if (_purchaseCart.value.isEmpty()) return

    launchSerializedSave {
      val draft = _purchaseCart.value
      if (draft.isEmpty()) return@launchSerializedSave
      val itemsTotal = BusinessCalculator.round2(draft.sumOf { it.lineTotal })
      val cleanExtra = BusinessCalculator.round2(extraCost.coerceAtLeast(0.0))
      val cleanPaid = BusinessCalculator.round2(paidAmount.coerceIn(0.0, itemsTotal))
      val due = BusinessCalculator.round2((itemsTotal - cleanPaid).coerceAtLeast(0.0))
      try {
        val entities =
          draft.map {
            PurchaseItemEntity(
              purchaseId = 0L,
              vegetableId = it.vegetable.id,
              vegetableName = it.vegetable.name,
              unitType = it.vegetable.unitType,
              quantityUnits = it.quantityUnits,
              unitCost = it.unitCost,
              lineTotal = it.lineTotal,
            )
          }
        repo.completePurchase(
          supplierId = supplierId,
          supplierName = supplierName.ifBlank { "নগদ বাজার" },
          itemsTotal = itemsTotal,
          extraCost = cleanExtra,
          paidAmount = cleanPaid,
          dueAmount = due,
          notes = notes,
          timestamp = timestamp,
          items = entities,
        )
        _purchaseCart.value = emptyList()
        _snackbarEvents.tryEmit("নতুন ক্রয় ও স্টক আপডেট হয়েছে")
        onSuccess()
      } catch (_: Exception) {
        _snackbarEvents.tryEmit("ক্রয় সংরক্ষণ করা যায়নি। আবার চেষ্টা করুন।")
      }
    }
  }

  fun cancelPurchase(purchaseId: Long) {
    launchSerializedSave {
      try {
        repo.cancelPurchase(purchaseId)
        _snackbarEvents.tryEmit("ক্রয় বাতিল ও স্টক সমন্বয় হয়েছে")
      } catch (_: Exception) {
        _snackbarEvents.tryEmit("ক্রয় বাতিল করা যায়নি। আবার চেষ্টা করুন।")
      }
    }
  }

  // Product Management
  fun saveVegetable(
    existingId: Long?,
    name: String,
    category: VegetableCategory,
    unitType: UnitType,
    avgCost: Double,
    sellingPrice: Double,
    lowStockThreshold: Long,
    initialStock: Long,
    onResult: (Result<Unit>) -> Unit,
  ) = inventoryActions.saveVegetable(
    existingId = existingId,
    name = name,
    category = category,
    unitType = unitType,
    avgCost = avgCost,
    sellingPrice = sellingPrice,
    lowStockThreshold = lowStockThreshold,
    initialStock = initialStock,
    onResult = onResult,
  )

  fun toggleArchiveVegetable(veg: VegetableEntity) = inventoryActions.toggleArchiveVegetable(veg)

  fun bulkUpdateVegetableSellingPrices(
    updates: Map<Long, Double>,
    onSuccess: () -> Unit = {},
  ) = inventoryActions.bulkUpdateVegetableSellingPrices(updates, onSuccess)

  fun adjustStock(
    vegId: Long,
    actualStockUnits: Long,
    reason: String,
    onSuccess: () -> Unit = {},
  ) = inventoryActions.adjustStock(vegId, actualStockUnits, reason, onSuccess)

  fun recordWastage(
    vegId: Long,
    quantityUnits: Long,
    reason: WastageReason,
    notes: String,
    onSuccess: () -> Unit = {},
  ) = inventoryActions.recordWastage(vegId, quantityUnits, reason, notes, onSuccess)

  fun cancelWastage(wastageId: Long) = inventoryActions.cancelWastage(wastageId)

  fun saveExpense(
    existingId: Long?,
    category: ExpenseCategory,
    amount: Double,
    paymentMethod: PaymentMethod,
    note: String,
    timestamp: Long,
    onSuccess: () -> Unit = {},
  ) = ledgerActions.saveExpense(existingId, category, amount, paymentMethod, note, timestamp, onSuccess)

  fun cancelExpense(expenseId: Long) = ledgerActions.cancelExpense(expenseId)

  suspend fun createPartyImmediate(
    name: String,
    phone: String,
    partyType: PartyType,
    notes: String = "",
  ): PartyEntity = ledgerActions.createPartyImmediate(name, phone, partyType, notes)

  fun addParty(
    name: String,
    phone: String,
    partyType: PartyType,
    notes: String = "",
  ) = ledgerActions.addParty(name, phone, partyType, notes)

  fun recordDuePayment(
    party: PartyEntity,
    amount: Double,
    paymentMethod: PaymentMethod,
    note: String,
    onSuccess: () -> Unit = {},
  ) = ledgerActions.recordDuePayment(party, amount, paymentMethod, note, onSuccess)

  fun cancelDuePayment(paymentId: Long) = ledgerActions.cancelDuePayment(paymentId)

  fun exportBackup(uri: Uri) = backupCoordinator.exportBackup(uri)

  fun prepareRestoreFromUri(uri: Uri) = backupCoordinator.prepareRestoreFromUri(uri)

  fun dismissPendingRestore() = backupCoordinator.dismissPendingRestore()

  fun confirmRestore() = backupCoordinator.confirmRestore()

  companion object {
    const val MAX_PIN_ATTEMPTS = 5
    const val PIN_LOCKOUT_DURATION_MS = 30_000L
    const val PIN_RELOCK_GRACE_MS = 120_000L
    private const val STATE_STOP_TIMEOUT_MS = 5_000L
  }
}
