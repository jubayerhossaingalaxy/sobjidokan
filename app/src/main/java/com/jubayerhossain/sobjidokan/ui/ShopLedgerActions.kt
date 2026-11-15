package com.jubayerhossain.sobjidokan.ui

import com.jubayerhossain.sobjidokan.data.local.entity.ExpenseCategory
import com.jubayerhossain.sobjidokan.data.local.entity.PartyEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyType
import com.jubayerhossain.sobjidokan.data.local.entity.PaymentMethod
import com.jubayerhossain.sobjidokan.data.repository.ShopRepository
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/** Owns expense and party-ledger operations. */
internal class ShopLedgerActions(
  private val repo: ShopRepository,
  private val snackbarEvents: MutableSharedFlow<String>,
  private val uiStateProvider: () -> ShopUiState,
  private val scope: CoroutineScope,
  private val launchSerializedSave: (suspend () -> Unit) -> Unit,
) {
  fun saveExpense(
    existingId: Long?,
    category: ExpenseCategory,
    amount: Double,
    paymentMethod: PaymentMethod,
    note: String,
    timestamp: Long,
    onSuccess: () -> Unit = {},
  ) {
    if (!amount.isFinite() || amount <= 0.0) {
      snackbarEvents.tryEmit("সঠিক টাকার পরিমাণ লিখুন (শূন্যের বেশি)।")
      return
    }
    launchSerializedSave {
      try {
        repo.saveExpense(
          existingId,
          category,
          BusinessCalculator.round2(amount),
          paymentMethod,
          note,
          timestamp,
        )
        snackbarEvents.tryEmit("খরচ সংরক্ষিত হয়েছে")
        onSuccess()
      } catch (_: Exception) {
        snackbarEvents.tryEmit("খরচ সংরক্ষণ করা যায়নি।")
      }
    }
  }

  fun cancelExpense(expenseId: Long) {
    launchSerializedSave {
      try {
        repo.cancelExpense(expenseId)
        snackbarEvents.tryEmit("খরচ বাতিল করা হয়েছে")
      } catch (_: Exception) {
        snackbarEvents.tryEmit("খরচ বাতিল করা যায়নি।")
      }
    }
  }

  suspend fun createPartyImmediate(
    name: String,
    phone: String,
    partyType: PartyType,
    notes: String = "",
  ): PartyEntity {
    val cleanName = name.trim()
    val id = repo.addParty(cleanName, phone, partyType, notes)
    return PartyEntity(
      id = id,
      name = cleanName,
      phone = phone.trim(),
      partyType = partyType,
      notes = notes.trim(),
    )
  }

  fun addParty(
    name: String,
    phone: String,
    partyType: PartyType,
    notes: String = "",
  ) {
    if (name.isBlank()) {
      snackbarEvents.tryEmit("অনুগ্রহ করে নাম লিখুন।")
      return
    }
    scope.launch {
      try {
        repo.addParty(name, phone, partyType, notes)
        snackbarEvents.tryEmit("${partyType.labelBn} যোগ করা হয়েছে")
      } catch (_: Exception) {
        snackbarEvents.tryEmit("তথ্য সংরক্ষণ করা যায়নি।")
      }
    }
  }

  fun recordDuePayment(
    party: PartyEntity,
    amount: Double,
    paymentMethod: PaymentMethod,
    note: String,
    onSuccess: () -> Unit = {},
  ) {
    if (!amount.isFinite() || amount <= 0.0) {
      snackbarEvents.tryEmit("সঠিক টাকার পরিমাণ লিখুন (শূন্যের বেশি)।")
      return
    }
    val roundedAmount = BusinessCalculator.round2(amount)
    val currentBalances =
      if (party.partyType == PartyType.CUSTOMER) {
        uiStateProvider().customerBalances
      } else {
        uiStateProvider().supplierBalances
      }
    val maxDue =
      currentBalances.firstOrNull { it.party.id == party.id }?.balanceDue ?: Double.MAX_VALUE
    if (roundedAmount > maxDue + BusinessCalculator.DUE_TOLERANCE) {
      snackbarEvents.tryEmit("পরিশোধিত টাকা মোট পাওনা/বিলের চেয়ে বেশি হতে পারে না।")
      return
    }
    launchSerializedSave {
      try {
        repo.recordDuePayment(party, roundedAmount, paymentMethod, note)
        snackbarEvents.tryEmit(
          if (party.partyType == PartyType.CUSTOMER) {
            "বাকি টাকা আদায় হয়েছে"
          } else {
            "পাইকারের দেনা পরিশোধ হয়েছে"
          },
        )
        onSuccess()
      } catch (_: Exception) {
        snackbarEvents.tryEmit("বকেয়া হিসাব সংরক্ষণ করা যায়নি।")
      }
    }
  }

  fun cancelDuePayment(paymentId: Long) {
    launchSerializedSave {
      try {
        repo.cancelDuePayment(paymentId)
        snackbarEvents.tryEmit("বকেয়া লেনদেন বাতিল হয়েছে")
      } catch (_: Exception) {
        snackbarEvents.tryEmit("বকেয়া লেনদেন বাতিল করা যায়নি।")
      }
    }
  }
}
