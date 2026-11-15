package com.jubayerhossain.sobjidokan.ui

import com.jubayerhossain.sobjidokan.data.local.entity.DuePaymentEntity
import com.jubayerhossain.sobjidokan.data.local.entity.ExpenseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.InventoryMovementEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseItemEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleItemEntity
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableEntity
import com.jubayerhossain.sobjidokan.data.local.entity.WastageEntity
import com.jubayerhossain.sobjidokan.data.prefs.ShopPreferences
import com.jubayerhossain.sobjidokan.domain.calculator.FinancialSummary

data class PosCartItem(
  val vegetable: VegetableEntity,
  val quantityUnits: Long,
  val sellingPricePerUnit: Double,
  val lineTotal: Double,
)

data class PurchaseDraftItem(
  val vegetable: VegetableEntity,
  val quantityUnits: Long,
  val unitCost: Double,
  val lineTotal: Double,
)

data class PartyBalanceSummary(
  val party: PartyEntity,
  val totalBilled: Double,
  val totalPaid: Double,
  val balanceDue: Double,
)

enum class ReportPeriod(
  val labelBn: String,
) {
  TODAY("আজ"),
  WEEK("৭ দিন"),
  MONTH("এই মাস"),
  ALL("সব সময়"),
  CUSTOM("তারিখ"),
}

data class ShopUiState(
  val isLoaded: Boolean = false,
  val preferences: ShopPreferences = ShopPreferences(),
  val isUnlocked: Boolean = false,
  val activeVegetables: List<VegetableEntity> = emptyList(),
  val allVegetables: List<VegetableEntity> = emptyList(),
  val lowStockVegetables: List<VegetableEntity> = emptyList(),
  val sales: List<SaleEntity> = emptyList(),
  val saleItemsBySaleId: Map<Long, List<SaleItemEntity>> = emptyMap(),
  val purchases: List<PurchaseEntity> = emptyList(),
  val purchaseItemsByPurchaseId: Map<Long, List<PurchaseItemEntity>> = emptyMap(),
  val wastages: List<WastageEntity> = emptyList(),
  val expenses: List<ExpenseEntity> = emptyList(),
  val duePayments: List<DuePaymentEntity> = emptyList(),
  val customerBalances: List<PartyBalanceSummary> = emptyList(),
  val supplierBalances: List<PartyBalanceSummary> = emptyList(),
  val movements: List<InventoryMovementEntity> = emptyList(),
  val todaySummary: FinancialSummary = FinancialSummary(),
  val posCart: List<PosCartItem> = emptyList(),
  val purchaseCart: List<PurchaseDraftItem> = emptyList(),
  val preselectedPosCustomerId: Long? = null,
  val isSaving: Boolean = false,
  val pinLockoutUntilMs: Long = 0L,
)
