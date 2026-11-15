package com.jubayerhossain.sobjidokan.data.repository

import com.jubayerhossain.sobjidokan.data.local.AppDatabase
import com.jubayerhossain.sobjidokan.data.local.dao.ShopDao
import com.jubayerhossain.sobjidokan.data.local.dao.ShopDbSnapshot
import com.jubayerhossain.sobjidokan.data.local.entity.DuePaymentEntity
import com.jubayerhossain.sobjidokan.data.local.entity.ExpenseCategory
import com.jubayerhossain.sobjidokan.data.local.entity.ExpenseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.InventoryMovementEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyType
import com.jubayerhossain.sobjidokan.data.local.entity.PaymentMethod
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseItemEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleItemEntity
import com.jubayerhossain.sobjidokan.data.local.entity.UnitType
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableCategory
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableEntity
import com.jubayerhossain.sobjidokan.data.local.entity.WastageEntity
import com.jubayerhossain.sobjidokan.data.local.entity.WastageReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest

class ShopRepository(
  private val dao: ShopDao,
  private val db: AppDatabase? = null,
) {
  val allVegetables: Flow<List<VegetableEntity>> = dao.observeAllVegetables()
  val activeVegetables: Flow<List<VegetableEntity>> = dao.observeActiveVegetables()
  val allParties: Flow<List<PartyEntity>> = dao.observeAllParties()
  val allPurchases: Flow<List<PurchaseEntity>> = dao.observeAllPurchases()
  val allPurchaseItems: Flow<List<PurchaseItemEntity>> = dao.observeAllPurchaseItems()
  val allSales: Flow<List<SaleEntity>> = dao.observeAllSales()
  val allSaleItems: Flow<List<SaleItemEntity>> = dao.observeAllSaleItems()
  val allWastages: Flow<List<WastageEntity>> = dao.observeAllWastages()
  val allExpenses: Flow<List<ExpenseEntity>> = dao.observeAllExpenses()
  val allDuePayments: Flow<List<DuePaymentEntity>> = dao.observeAllDuePayments()
  val recentMovements: Flow<List<InventoryMovementEntity>> = dao.observeRecentMovements()

  @OptIn(ExperimentalCoroutinesApi::class)
  val atomicSnapshot: Flow<ShopDbSnapshot>? =
    db
      ?.invalidationTracker
      ?.createFlow(
        "vegetables",
        "parties",
        "purchases",
        "purchase_items",
        "sales",
        "sale_items",
        "wastages",
        "expenses",
        "due_payments",
        "inventory_movements",
        emitInitialState = true,
      )?.conflate()
      ?.mapLatest {
        if (!db.isOpen) throw CancellationException("Database closed")
        try {
          dao.loadAtomicSnapshot()
        } catch (e: IllegalStateException) {
          if (!db.isOpen) throw CancellationException("Database closed", e)
          throw e
        }
      }?.flowOn(Dispatchers.IO)

  suspend fun seedDefaultVegetablesIfEmpty() {
    if (dao.getAllVegetablesSnapshot().isNotEmpty()) return
    val defaults =
      listOf(
        defaultVeg("আলু", VegetableCategory.ROOT, UnitType.KG, 30.0, 40.0, 5000L),
        defaultVeg("পেঁয়াজ", VegetableCategory.ROOT, UnitType.KG, 65.0, 80.0, 3000L),
        defaultVeg("টমেটো", VegetableCategory.FRUIT_VEG, UnitType.KG, 45.0, 60.0, 2000L),
        defaultVeg(
          "কাঁচা মরিচ",
          VegetableCategory.SPICE_OTHER,
          UnitType.KG,
          90.0,
          120.0,
          1000L,
        ),
        defaultVeg("বেগুন", VegetableCategory.FRUIT_VEG, UnitType.KG, 38.0, 50.0, 2000L),
        defaultVeg("পটল", VegetableCategory.FRUIT_VEG, UnitType.KG, 40.0, 55.0, 2000L),
        defaultVeg("ঢেঁড়স", VegetableCategory.FRUIT_VEG, UnitType.KG, 42.0, 55.0, 2000L),
        defaultVeg("শসা", VegetableCategory.FRUIT_VEG, UnitType.KG, 35.0, 50.0, 2000L),
        defaultVeg("মিষ্টি কুমড়া", VegetableCategory.FRUIT_VEG, UnitType.KG, 25.0, 35.0, 3000L),
        defaultVeg("লাউ", VegetableCategory.FRUIT_VEG, UnitType.PIECE, 35.0, 50.0, 3L),
        defaultVeg("লাল শাক", VegetableCategory.LEAFY, UnitType.BUNCH, 12.0, 20.0, 5L),
        defaultVeg("পালং শাক", VegetableCategory.LEAFY, UnitType.BUNCH, 15.0, 25.0, 5L),
        defaultVeg("ধনেপাতা", VegetableCategory.LEAFY, UnitType.BUNCH, 10.0, 20.0, 5L),
        defaultVeg("রসুন", VegetableCategory.SPICE_OTHER, UnitType.KG, 160.0, 200.0, 1000L),
        defaultVeg("আদা", VegetableCategory.SPICE_OTHER, UnitType.KG, 180.0, 220.0, 1000L),
      )
    dao.insertVegetables(defaults)
  }

  private fun defaultVeg(
    name: String,
    category: VegetableCategory,
    unitType: UnitType,
    avgCost: Double,
    sellingPrice: Double,
    lowStockThreshold: Long,
  ) = VegetableEntity(
    name = name,
    category = category,
    unitType = unitType,
    stockUnits = 0L,
    avgCostPerUnit = avgCost,
    sellingPricePerUnit = sellingPrice,
    lowStockThresholdUnits = lowStockThreshold,
  )

  suspend fun saveVegetable(
    existingId: Long?,
    name: String,
    category: VegetableCategory,
    unitType: UnitType,
    avgCost: Double,
    sellingPrice: Double,
    lowStockThreshold: Long,
    initialStock: Long,
  ): Result<Unit> =
    runCatching {
      val cleanName = name.trim()
      require(cleanName.isNotEmpty()) { "অনুগ্রহ করে সবজির নাম লিখুন।" }
      require(sellingPrice.isFinite() && sellingPrice > 0.0) {
        "বিক্রয়দর অবশ্যই শূন্যের বেশি হতে হবে।"
      }
      require(avgCost.isFinite() && avgCost >= 0.0) { "সঠিক ক্রয়দর লিখুন।" }
      require(lowStockThreshold >= 0L && initialStock >= 0L) { "সঠিক স্টক মান লিখুন।" }
      val existingWithName = dao.findVegetableByName(cleanName)
      if (existingWithName != null && existingWithName.id != existingId) {
        error("এই নামের সবজি ইতোমধ্যেই তালিকায় আছে।")
      }
      if (existingId == null) {
        dao.insertVegetable(
          VegetableEntity(
            name = cleanName,
            category = category,
            unitType = unitType,
            stockUnits = initialStock,
            avgCostPerUnit = avgCost,
            sellingPricePerUnit = sellingPrice,
            lowStockThresholdUnits = lowStockThreshold,
          ),
        )
      } else {
        val current = dao.getVegetableById(existingId) ?: error("সবজি খুঁজে পাওয়া যায়নি")
        dao.updateVegetable(
          current.copy(
            name = cleanName,
            category = category,
            unitType = unitType,
            avgCostPerUnit = if (avgCost > 0.0) avgCost else current.avgCostPerUnit,
            sellingPricePerUnit = sellingPrice,
            lowStockThresholdUnits = lowStockThreshold,
          ),
        )
      }
    }

  suspend fun setVegetableArchived(
    veg: VegetableEntity,
    archived: Boolean,
  ) {
    dao.updateVegetable(veg.copy(isArchived = archived))
  }

  suspend fun bulkUpdateSellingPrices(updates: Map<Long, Double>): Int = dao.bulkUpdateSellingPricesAtomic(updates)

  suspend fun completeSale(
    customerId: Long?,
    customerName: String,
    subtotal: Double,
    discount: Double,
    totalAmount: Double,
    paidAmount: Double,
    dueAmount: Double,
    paymentMethod: PaymentMethod,
    notes: String,
    items: List<SaleItemEntity>,
  ): Long {
    require(items.isNotEmpty()) { "বিক্রির তালিকায় অন্তত একটি পণ্য থাকতে হবে।" }
    require(
      subtotal.isFinite() &&
        discount.isFinite() &&
        totalAmount.isFinite() &&
        paidAmount.isFinite() &&
        dueAmount.isFinite(),
    ) {
      "অবৈধ হিসাব মান পাওয়া গেছে।"
    }
    require(
      subtotal >= 0.0 &&
        discount >= 0.0 &&
        totalAmount >= 0.0 &&
        paidAmount >= 0.0 &&
        dueAmount >= 0.0,
    ) {
      "বিক্রির হিসাব ঋণাত্মক হতে পারবে না।"
    }
    return dao.completeSaleAtomic(
      sale =
        SaleEntity(
          customerId = customerId,
          customerName = customerName.ifBlank { "সাধারণ ক্রেতা" },
          subtotal = subtotal,
          discount = discount,
          totalAmount = totalAmount,
          paidAmount = paidAmount,
          dueAmount = dueAmount,
          paymentMethod = paymentMethod,
          notes = notes.trim(),
        ),
      items = items,
    )
  }

  suspend fun cancelSale(saleId: Long) {
    dao.cancelSaleAtomic(saleId)
  }

  suspend fun completePurchase(
    supplierId: Long?,
    supplierName: String,
    itemsTotal: Double,
    extraCost: Double,
    paidAmount: Double,
    dueAmount: Double,
    notes: String,
    timestamp: Long,
    items: List<PurchaseItemEntity>,
  ): Long {
    require(items.isNotEmpty()) { "ক্রয়ের তালিকায় অন্তত একটি পণ্য থাকতে হবে।" }
    require(
      itemsTotal.isFinite() &&
        extraCost.isFinite() &&
        paidAmount.isFinite() &&
        dueAmount.isFinite(),
    ) {
      "অবৈধ ক্রয় হিসাব পাওয়া গেছে।"
    }
    require(itemsTotal >= 0.0 && extraCost >= 0.0 && paidAmount >= 0.0 && dueAmount >= 0.0) {
      "ক্রয়ের হিসাব ঋণাত্মক হতে পারবে না।"
    }
    return dao.completePurchaseAtomic(
      purchase =
        PurchaseEntity(
          supplierId = supplierId,
          supplierName = supplierName.ifBlank { "নগদ বাজার" },
          itemsTotal = itemsTotal,
          extraCost = extraCost,
          paidAmount = paidAmount,
          dueAmount = dueAmount,
          notes = notes.trim(),
          timestamp = timestamp,
        ),
      items = items,
    )
  }

  suspend fun cancelPurchase(purchaseId: Long) {
    dao.cancelPurchaseAtomic(purchaseId)
  }

  suspend fun recordWastage(
    vegetableId: Long,
    quantityUnits: Long,
    reason: WastageReason,
    notes: String,
  ) {
    require(quantityUnits > 0L) { "অপচয়ের পরিমাণ শূন্যের বেশি হতে হবে।" }
    dao.recordWastageAtomic(
      vegetableId = vegetableId,
      quantityUnits = quantityUnits,
      reason = reason,
      notes = notes.trim(),
      timestamp = System.currentTimeMillis(),
    )
  }

  suspend fun cancelWastage(wastageId: Long) {
    dao.cancelWastageAtomic(wastageId)
  }

  suspend fun adjustStock(
    vegetableId: Long,
    actualStockUnits: Long,
    reason: String,
  ) {
    require(actualStockUnits >= 0L) { "বাস্তব স্টক ঋণাত্মক হতে পারে না।" }
    dao.adjustStockAtomic(vegetableId, actualStockUnits, reason.trim())
  }

  suspend fun saveExpense(
    existingId: Long?,
    category: ExpenseCategory,
    amount: Double,
    paymentMethod: PaymentMethod,
    note: String,
    timestamp: Long,
  ) {
    require(amount.isFinite() && amount > 0.0) { "সঠিক খরচের পরিমাণ লিখুন।" }
    dao.insertOrUpdateExpense(
      ExpenseEntity(
        id = existingId ?: 0L,
        category = category,
        amount = amount,
        paymentMethod = paymentMethod,
        note = note.trim(),
        timestamp = timestamp,
      ),
    )
  }

  suspend fun cancelExpense(expenseId: Long) {
    dao.markExpenseCancelled(expenseId)
  }

  suspend fun addParty(
    name: String,
    phone: String,
    partyType: PartyType,
    notes: String,
  ): Long {
    val cleanName = name.trim()
    require(cleanName.isNotEmpty()) { "অনুগ্রহ করে নাম লিখুন।" }
    return dao.insertParty(
      PartyEntity(
        name = cleanName,
        phone = phone.trim(),
        partyType = partyType,
        notes = notes.trim(),
      ),
    )
  }

  suspend fun recordDuePayment(
    party: PartyEntity,
    amount: Double,
    paymentMethod: PaymentMethod,
    note: String,
  ) {
    require(amount.isFinite() && amount > 0.0) { "সঠিক টাকার পরিমাণ লিখুন।" }
    dao.insertDuePayment(
      DuePaymentEntity(
        partyId = party.id,
        partyName = party.name,
        partyType = party.partyType,
        amount = amount,
        paymentMethod = paymentMethod,
        note = note.trim(),
      ),
    )
  }

  suspend fun cancelDuePayment(paymentId: Long) {
    dao.markDuePaymentCancelled(paymentId)
  }
}
