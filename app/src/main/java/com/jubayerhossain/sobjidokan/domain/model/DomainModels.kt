package com.jubayerhossain.sobjidokan.domain.model

import com.jubayerhossain.sobjidokan.data.local.entity.PartyType
import com.jubayerhossain.sobjidokan.data.local.entity.PaymentMethod
import com.jubayerhossain.sobjidokan.data.local.entity.UnitType
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableCategory

data class VegetableSummary(
  val id: Long,
  val name: String,
  val category: VegetableCategory,
  val unitType: UnitType,
  val stockUnits: Long,
  val avgCostPerUnit: Double,
  val sellingPricePerUnit: Double,
  val lowStockThresholdUnits: Long,
  val isArchived: Boolean,
  val stockValue: Double,
  val isLowStock: Boolean,
) {
  val displayName: String
    get() = name

  val categoryLabel: String
    get() = category.labelBn

  val unitLabel: String
    get() = unitType.labelBn
}

data class PartyBalance(
  val id: Long,
  val name: String,
  val partyType: PartyType,
  val phone: String,
  val totalBilled: Double,
  val totalPaid: Double,
  val balanceDue: Double,
) {
  val isCustomer: Boolean
    get() = partyType == PartyType.CUSTOMER

  val isSupplier: Boolean
    get() = partyType == PartyType.SUPPLIER

  val hasOutstandingBalance: Boolean
    @Suppress("MagicNumber")
    get() = balanceDue > 0.01
}

data class SaleSummary(
  val id: Long,
  val customerId: Long?,
  val customerName: String,
  val subtotal: Double,
  val discount: Double,
  val totalAmount: Double,
  val paidAmount: Double,
  val dueAmount: Double,
  val paymentMethod: PaymentMethod,
  val timestamp: Long,
  val isCancelled: Boolean,
) {
  val isPaidInFull: Boolean
    @Suppress("MagicNumber")
    get() = dueAmount <= 0.01

  val hasPendingDue: Boolean
    @Suppress("MagicNumber")
    get() = dueAmount > 0.01
}

data class PurchaseSummary(
  val id: Long,
  val supplierId: Long?,
  val supplierName: String,
  val itemsTotal: Double,
  val extraCost: Double,
  val paidAmount: Double,
  val dueAmount: Double,
  val timestamp: Long,
  val isCancelled: Boolean,
) {
  val totalCost: Double
    get() = itemsTotal + extraCost

  val isPaidInFull: Boolean
    @Suppress("MagicNumber")
    get() = dueAmount <= 0.01
}

data class FinancialSnapshot(
  val netSales: Double,
  val cogs: Double,
  val grossProfit: Double,
  val wastageLoss: Double,
  val operatingExpenses: Double,
  val netProfit: Double,
  val cashInflow: Double,
  val cashOutflow: Double,
  val netCashFlow: Double,
  val inventoryValue: Double,
  val activeSalesCount: Int,
  val activePurchasesCount: Int,
) {
  val profitMargin: Double
    get() = if (netSales > 0.0) (netProfit / netSales) * 100.0 else 0.0
}

data class LowStockItem(
  val id: Long,
  val name: String,
  val currentStock: Long,
  val threshold: Long,
  val unitType: com.jubayerhossain.sobjidokan.data.local.entity.UnitType,
) {
  val shortage: Long
    get() = threshold - currentStock
}

data class BackupInfo(
  val fileName: String,
  val fileSize: Long,
  val createdAt: Long,
  val isEncrypted: Boolean = false,
)
