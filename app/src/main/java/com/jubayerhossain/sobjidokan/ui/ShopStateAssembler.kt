package com.jubayerhossain.sobjidokan.ui

import com.jubayerhossain.sobjidokan.data.local.dao.ShopDbSnapshot
import com.jubayerhossain.sobjidokan.data.local.entity.DuePaymentEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyType
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleEntity
import com.jubayerhossain.sobjidokan.data.prefs.ShopPreferences
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator

internal data class ShopSessionState(
  val posCart: List<PosCartItem>,
  val purchaseCart: List<PurchaseDraftItem>,
  val preselectedCustomerId: Long?,
  val isUnlocked: Boolean,
  val isSaving: Boolean,
  val memLockoutUntilMs: Long,
)

internal object ShopStateAssembler {
  fun buildUiState(
    prefs: ShopPreferences,
    snapshot: ShopDbSnapshot,
    session: ShopSessionState,
  ): ShopUiState {
    val allVegs = snapshot.vegetables
    val sales = snapshot.sales
    val purchases = snapshot.purchases
    val expenses = snapshot.expenses
    val wastages = snapshot.wastages
    val parties = snapshot.parties
    val duePayments = snapshot.duePayments

    val activeVegs = allVegs.filter { !it.isArchived }
    val lowStock = activeVegs.filter { it.stockUnits <= it.lowStockThresholdUnits }
    val saleItemsMap = snapshot.saleItems.groupBy { it.saleId }
    val purchaseItemsMap = snapshot.purchaseItems.groupBy { it.purchaseId }

    val todayStart = BusinessCalculator.startOfDay()
    val todaySummary =
      BusinessCalculator.calculateFinancialSummary(
        sales = sales.filter { it.timestamp >= todayStart },
        saleItemsBySaleId = saleItemsMap,
        purchases = purchases.filter { it.timestamp >= todayStart },
        expenses = expenses.filter { it.timestamp >= todayStart },
        wastages = wastages.filter { it.timestamp >= todayStart },
        duePayments = duePayments.filter { it.timestamp >= todayStart },
        vegetables = activeVegs,
      )

    val customerBalances = computeCustomerBalances(parties, sales, duePayments)
    val supplierBalances = computeSupplierBalances(parties, purchases, duePayments)

    return ShopUiState(
      isLoaded = true,
      preferences = prefs,
      isUnlocked = session.isUnlocked || prefs.pinCode.isBlank(),
      activeVegetables = activeVegs,
      allVegetables = allVegs,
      lowStockVegetables = lowStock,
      sales = sales,
      saleItemsBySaleId = saleItemsMap,
      purchases = purchases,
      purchaseItemsByPurchaseId = purchaseItemsMap,
      wastages = wastages,
      expenses = expenses,
      duePayments = duePayments,
      customerBalances = customerBalances,
      supplierBalances = supplierBalances,
      movements = snapshot.movements,
      todaySummary = todaySummary,
      posCart = session.posCart,
      purchaseCart = session.purchaseCart,
      preselectedPosCustomerId = session.preselectedCustomerId,
      isSaving = session.isSaving,
      pinLockoutUntilMs = maxOf(prefs.pinLockoutUntilMs, session.memLockoutUntilMs),
    )
  }

  private fun computeCustomerBalances(
    parties: List<PartyEntity>,
    sales: List<SaleEntity>,
    duePayments: List<DuePaymentEntity>,
  ): List<PartyBalanceSummary> {
    val activeSalesByCustomerId =
      sales.filter { !it.isCancelled && it.customerId != null }.groupBy { it.customerId!! }
    val activeCustomerPayments =
      duePayments
        .filter { !it.isCancelled && it.partyType == PartyType.CUSTOMER }
        .groupBy { it.partyId }
    return parties
      .filter { it.partyType == PartyType.CUSTOMER }
      .map { party ->
        val creditSales = activeSalesByCustomerId[party.id].orEmpty()
        val totalDueFromSales = creditSales.sumOf { it.dueAmount }
        val totalCollected = activeCustomerPayments[party.id].orEmpty().sumOf { it.amount }
        PartyBalanceSummary(
          party = party,
          totalBilled = BusinessCalculator.round2(creditSales.sumOf { it.totalAmount }),
          totalPaid =
            BusinessCalculator.round2(creditSales.sumOf { it.paidAmount } + totalCollected),
          balanceDue =
            BusinessCalculator.round2(
              (totalDueFromSales - totalCollected).coerceAtLeast(0.0),
            ),
        )
      }
  }

  private fun computeSupplierBalances(
    parties: List<PartyEntity>,
    purchases: List<PurchaseEntity>,
    duePayments: List<DuePaymentEntity>,
  ): List<PartyBalanceSummary> {
    val activePurchasesBySupplierId =
      purchases.filter { !it.isCancelled && it.supplierId != null }.groupBy { it.supplierId!! }
    val activeSupplierPayments =
      duePayments
        .filter { !it.isCancelled && it.partyType == PartyType.SUPPLIER }
        .groupBy { it.partyId }
    return parties
      .filter { it.partyType == PartyType.SUPPLIER }
      .map { party ->
        val supplierPurchases = activePurchasesBySupplierId[party.id].orEmpty()
        val totalDueFromPurchases = supplierPurchases.sumOf { it.dueAmount }
        val totalPaidLater = activeSupplierPayments[party.id].orEmpty().sumOf { it.amount }
        PartyBalanceSummary(
          party = party,
          totalBilled = BusinessCalculator.round2(supplierPurchases.sumOf { it.itemsTotal }),
          totalPaid =
            BusinessCalculator.round2(
              supplierPurchases.sumOf { it.paidAmount } + totalPaidLater,
            ),
          balanceDue =
            BusinessCalculator.round2(
              (totalDueFromPurchases - totalPaidLater).coerceAtLeast(0.0),
            ),
        )
      }
  }
}
