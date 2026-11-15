package com.jubayerhossain.sobjidokan.domain.calculator

import com.jubayerhossain.sobjidokan.data.local.entity.DuePaymentEntity
import com.jubayerhossain.sobjidokan.data.local.entity.ExpenseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyType
import com.jubayerhossain.sobjidokan.data.local.entity.PaymentMethod
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleItemEntity
import com.jubayerhossain.sobjidokan.data.local.entity.UnitType
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableEntity
import com.jubayerhossain.sobjidokan.data.local.entity.WastageEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToLong

data class FinancialSummary(
  val netSales: Double = 0.0,
  val cogs: Double = 0.0,
  val grossProfit: Double = 0.0,
  val wastageLoss: Double = 0.0,
  val operatingExpenses: Double = 0.0,
  val purchaseExtraExpenses: Double = 0.0,
  val netProfit: Double = 0.0,
  val cashInflow: Double = 0.0,
  val cashOutflow: Double = 0.0,
  val netCashFlow: Double = 0.0,
  val inventoryValue: Double = 0.0,
  val activeSalesCount: Int = 0,
  val activePurchasesCount: Int = 0,
  val cashSalesAndCollections: Double = 0.0,
  val mobileSalesAndCollections: Double = 0.0,
  val cashOutflowOnly: Double = 0.0,
  val expectedCashInDrawer: Double = 0.0,
)

object BusinessCalculator {
  const val AMOUNT_EPSILON = 0.005
  const val DUE_TOLERANCE = 0.01
  const val PIN_LENGTH = 4
  const val PIN_POLL_INTERVAL_MS = 500L
  const val MS_PER_SECOND = 1000L
  const val ROUNDING_CEIL_OFFSET_MS = 999L
  const val GRAMS_PER_KG = 1000L
  const val GRAMS_PER_KG_DOUBLE = 1000.0
  const val MAX_CUSTOM_WEIGHT_GRAMS = 100_000_000.0

  private const val INTERMEDIATE_SCALE = 6

  private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
  private val bdTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")
  private val usSymbols: DecimalFormatSymbols = DecimalFormatSymbols(Locale.US)
  private val intFormatLocal = ThreadLocal.withInitial { DecimalFormat("#,##0", usSymbols) }
  private val decFormatLocal = ThreadLocal.withInitial { DecimalFormat("#,##0.##", usSymbols) }
  private val dateFormatLocal =
    ThreadLocal.withInitial {
      SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("bn-BD")).apply {
        timeZone = bdTimeZone
      }
    }
  private val dateTimeFormatLocal =
    ThreadLocal.withInitial {
      SimpleDateFormat("dd MMM, hh:mm a", Locale.forLanguageTag("bn-BD")).apply {
        timeZone = bdTimeZone
      }
    }

  /** BR-001 & BR-003: Line total calculated via BigDecimal HALF_UP to avoid floating-point drift */
  fun calculateLineTotal(
    quantityUnits: Long,
    unitType: UnitType,
    pricePerDisplayUnit: Double,
  ): Double {
    if (quantityUnits <= 0L || !pricePerDisplayUnit.isFinite() || pricePerDisplayUnit <= 0.0) {
      return 0.0
    }
    val qty = BigDecimal.valueOf(quantityUnits)
    val factor = BigDecimal.valueOf(unitType.baseFactor)
    val rate = BigDecimal.valueOf(pricePerDisplayUnit)
    return qty.multiply(rate).divide(factor, 2, RoundingMode.HALF_UP).toDouble()
  }

  /** BR-002: Weighted Average Cost update when purchasing stock */
  fun calculateWeightedAverageCost(
    currentStockUnits: Long,
    currentAvgCost: Double,
    addedUnits: Long,
    addedLineTotal: Double,
    unitType: UnitType,
  ): Double {
    val safeCurrentAvg =
      if (currentAvgCost.isFinite() && currentAvgCost >= 0.0) currentAvgCost else 0.0
    if (addedUnits <= 0L || !addedLineTotal.isFinite() || addedLineTotal < 0.0) {
      return safeCurrentAvg
    }
    val factor = BigDecimal.valueOf(unitType.baseFactor)
    val addedQtyBd = BigDecimal.valueOf(addedUnits)
    val addedTotalBd = BigDecimal.valueOf(addedLineTotal)

    if (currentStockUnits <= 0L) {
      return if (addedLineTotal > 0.0) {
        addedTotalBd.multiply(factor).divide(addedQtyBd, 2, RoundingMode.HALF_UP).toDouble()
      } else {
        round2(safeCurrentAvg)
      }
    }

    val existingValue =
      BigDecimal
        .valueOf(currentStockUnits)
        .multiply(BigDecimal.valueOf(safeCurrentAvg))
        .divide(factor, INTERMEDIATE_SCALE, RoundingMode.HALF_UP)
    val totalQtyDisplay =
      BigDecimal
        .valueOf(currentStockUnits + addedUnits)
        .divide(factor, INTERMEDIATE_SCALE, RoundingMode.HALF_UP)
    return existingValue
      .add(addedTotalBd)
      .divide(totalQtyDisplay, 2, RoundingMode.HALF_UP)
      .toDouble()
  }

  /** BR-006: Wastage monetary loss = quantity * current weighted average cost */
  fun calculateWastageLoss(
    quantityUnits: Long,
    avgCostPerUnit: Double,
    unitType: UnitType,
  ): Double {
    if (quantityUnits <= 0L || !avgCostPerUnit.isFinite() || avgCostPerUnit <= 0.0) return 0.0
    return BigDecimal
      .valueOf(quantityUnits)
      .multiply(BigDecimal.valueOf(avgCostPerUnit))
      .divide(BigDecimal.valueOf(unitType.baseFactor), 2, RoundingMode.HALF_UP)
      .toDouble()
  }

  /** BR-008, BR-011, BR-012: Full Net Profit, Cash Flow & Inventory Valuation */
  fun calculateFinancialSummary(
    sales: List<SaleEntity>,
    saleItemsBySaleId: Map<Long, List<SaleItemEntity>>,
    purchases: List<PurchaseEntity>,
    expenses: List<ExpenseEntity>,
    wastages: List<WastageEntity>,
    duePayments: List<DuePaymentEntity>,
    vegetables: List<VegetableEntity>,
  ): FinancialSummary {
    val activeSales = sales.filter { !it.isCancelled }
    val activePurchases = purchases.filter { !it.isCancelled }
    val activeExpenses = expenses.filter { !it.isCancelled }
    val activeWastages = wastages.filter { !it.isCancelled }
    val activeDuePayments = duePayments.filter { !it.isCancelled }

    val profit =
      computeProfitMetrics(
        activeSales = activeSales,
        saleItemsBySaleId = saleItemsBySaleId,
        activePurchases = activePurchases,
        activeExpenses = activeExpenses,
        activeWastages = activeWastages,
      )
    val cashFlow =
      computeCashFlowMetrics(
        activeSales = activeSales,
        activePurchases = activePurchases,
        activeExpenses = activeExpenses,
        activeDuePayments = activeDuePayments,
        operatingExpenses = profit.operatingExpenses,
      )
    val inventoryValue = computeInventoryValuation(vegetables)

    return FinancialSummary(
      netSales = round2(profit.netSales),
      cogs = round2(profit.cogs),
      grossProfit = round2(profit.grossProfit),
      wastageLoss = round2(profit.wastageLoss),
      operatingExpenses = round2(profit.totalExpenses),
      purchaseExtraExpenses = round2(profit.purchaseExtraExpenses),
      netProfit = round2(profit.netProfit),
      cashInflow = round2(cashFlow.cashInflow),
      cashOutflow = round2(cashFlow.cashOutflow),
      netCashFlow = round2(cashFlow.netCashFlow),
      inventoryValue = round2(inventoryValue),
      activeSalesCount = activeSales.size,
      activePurchasesCount = activePurchases.size,
      cashSalesAndCollections = round2(cashFlow.physicalCashIn),
      mobileSalesAndCollections = round2(cashFlow.mobileInflow),
      cashOutflowOnly = round2(cashFlow.physicalCashOut),
      expectedCashInDrawer = round2(cashFlow.expectedDrawerCash),
    )
  }

  private data class ProfitMetrics(
    val netSales: BigDecimal,
    val cogs: BigDecimal,
    val grossProfit: BigDecimal,
    val wastageLoss: BigDecimal,
    val operatingExpenses: BigDecimal,
    val purchaseExtraExpenses: BigDecimal,
    val totalExpenses: BigDecimal,
    val netProfit: BigDecimal,
  )

  private data class CashFlowMetrics(
    val cashInflow: BigDecimal,
    val cashOutflow: BigDecimal,
    val netCashFlow: BigDecimal,
    val mobileInflow: BigDecimal,
    val physicalCashIn: BigDecimal,
    val physicalCashOut: BigDecimal,
    val expectedDrawerCash: BigDecimal,
  )

  private fun computeProfitMetrics(
    activeSales: List<SaleEntity>,
    saleItemsBySaleId: Map<Long, List<SaleItemEntity>>,
    activePurchases: List<PurchaseEntity>,
    activeExpenses: List<ExpenseEntity>,
    activeWastages: List<WastageEntity>,
  ): ProfitMetrics {
    val netSales =
      activeSales.fold(BigDecimal.ZERO) { acc, sale -> acc.add(safeBd(sale.totalAmount)) }
    val cogs =
      activeSales.fold(BigDecimal.ZERO) { acc, sale ->
        val saleCost =
          saleItemsBySaleId[sale.id].orEmpty().fold(BigDecimal.ZERO) { itemAcc, item ->
            val cost =
              BigDecimal
                .valueOf(item.quantityUnits)
                .multiply(safeBd(item.costSnapshotPerUnit))
                .divide(
                  BigDecimal.valueOf(item.unitType.baseFactor),
                  INTERMEDIATE_SCALE,
                  RoundingMode.HALF_UP,
                )
            itemAcc.add(cost)
          }
        acc.add(saleCost)
      }
    val grossProfit = netSales.subtract(cogs)
    val wastageLoss =
      activeWastages.fold(BigDecimal.ZERO) { acc, wastage -> acc.add(safeBd(wastage.lossAmount)) }
    val operatingExpenses =
      activeExpenses.fold(BigDecimal.ZERO) { acc, expense -> acc.add(safeBd(expense.amount)) }
    val purchaseExtraExpenses =
      activePurchases.fold(BigDecimal.ZERO) { acc, purchase ->
        acc.add(safeBd(purchase.extraCost))
      }
    val totalExpenses = operatingExpenses.add(purchaseExtraExpenses)
    val netProfit = grossProfit.subtract(wastageLoss).subtract(totalExpenses)
    return ProfitMetrics(
      netSales = netSales,
      cogs = cogs,
      grossProfit = grossProfit,
      wastageLoss = wastageLoss,
      operatingExpenses = operatingExpenses,
      purchaseExtraExpenses = purchaseExtraExpenses,
      totalExpenses = totalExpenses,
      netProfit = netProfit,
    )
  }

  private fun computeCashFlowMetrics(
    activeSales: List<SaleEntity>,
    activePurchases: List<PurchaseEntity>,
    activeExpenses: List<ExpenseEntity>,
    activeDuePayments: List<DuePaymentEntity>,
    operatingExpenses: BigDecimal,
  ): CashFlowMetrics {
    val customerCollections =
      activeDuePayments
        .filter { it.partyType == PartyType.CUSTOMER }
        .fold(BigDecimal.ZERO) { acc, payment -> acc.add(safeBd(payment.amount)) }
    val supplierPayments =
      activeDuePayments
        .filter { it.partyType == PartyType.SUPPLIER }
        .fold(BigDecimal.ZERO) { acc, payment -> acc.add(safeBd(payment.amount)) }

    val cashInflow =
      activeSales
        .fold(BigDecimal.ZERO) { acc, sale -> acc.add(safeBd(sale.paidAmount)) }
        .add(customerCollections)
    val cashOutflow =
      activePurchases
        .fold(BigDecimal.ZERO) { acc, purchase ->
          acc.add(safeBd(purchase.paidAmount)).add(safeBd(purchase.extraCost))
        }.add(operatingExpenses)
        .add(supplierPayments)
    val netCashFlow = cashInflow.subtract(cashOutflow)

    val mobileSalesIn =
      activeSales
        .filter { it.paymentMethod == PaymentMethod.MOBILE }
        .fold(BigDecimal.ZERO) { acc, sale -> acc.add(safeBd(sale.paidAmount)) }
    val mobileCollections =
      activeDuePayments
        .filter {
          it.partyType == PartyType.CUSTOMER && it.paymentMethod == PaymentMethod.MOBILE
        }.fold(BigDecimal.ZERO) { acc, payment -> acc.add(safeBd(payment.amount)) }
    val mobileInflow = mobileSalesIn.add(mobileCollections)
    val physicalCashIn = cashInflow.subtract(mobileInflow)

    val mobileExpenses =
      activeExpenses
        .filter { it.paymentMethod == PaymentMethod.MOBILE }
        .fold(BigDecimal.ZERO) { acc, expense -> acc.add(safeBd(expense.amount)) }
    val mobileSupplierPayments =
      activeDuePayments
        .filter {
          it.partyType == PartyType.SUPPLIER && it.paymentMethod == PaymentMethod.MOBILE
        }.fold(BigDecimal.ZERO) { acc, payment -> acc.add(safeBd(payment.amount)) }
    val physicalCashOut = cashOutflow.subtract(mobileExpenses).subtract(mobileSupplierPayments)
    val expectedDrawerCash = physicalCashIn.subtract(physicalCashOut)

    return CashFlowMetrics(
      cashInflow = cashInflow,
      cashOutflow = cashOutflow,
      netCashFlow = netCashFlow,
      mobileInflow = mobileInflow,
      physicalCashIn = physicalCashIn,
      physicalCashOut = physicalCashOut,
      expectedDrawerCash = expectedDrawerCash,
    )
  }

  private fun computeInventoryValuation(vegetables: List<VegetableEntity>): BigDecimal =
    vegetables
      .filter { !it.isArchived && it.stockUnits > 0L }
      .fold(BigDecimal.ZERO) { acc, veg ->
        val value =
          BigDecimal
            .valueOf(veg.stockUnits)
            .multiply(safeBd(veg.avgCostPerUnit))
            .divide(
              BigDecimal.valueOf(veg.unitType.baseFactor),
              INTERMEDIATE_SCALE,
              RoundingMode.HALF_UP,
            )
        acc.add(value)
      }

  private fun safeBd(value: Double): BigDecimal = if (value.isFinite()) BigDecimal.valueOf(value) else BigDecimal.ZERO

  fun round2(value: Double): Double =
    if (value.isFinite()) {
      BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
    } else {
      0.0
    }

  private fun round2(value: BigDecimal): Double = value.setScale(2, RoundingMode.HALF_UP).toDouble()

  fun toDisplayDigits(
    input: String,
    useBengaliDigits: Boolean,
  ): String {
    if (!useBengaliDigits) return input
    val sb = StringBuilder(input.length)
    for (ch in input) {
      if (ch in '0'..'9') {
        sb.append(bengaliDigits[ch - '0'])
      } else {
        sb.append(ch)
      }
    }
    return sb.toString()
  }

  fun formatNumber(
    value: Number,
    useBengaliDigits: Boolean,
  ): String {
    val raw =
      when (value) {
        is Long,
        is Int,
        -> {
          intFormatLocal.get()!!.format(value.toLong())
        }

        else -> {
          val d = value.toDouble()
          if (!d.isFinite()) {
            "0"
          } else if (abs(d - d.roundToLong()) < AMOUNT_EPSILON) {
            intFormatLocal.get()!!.format(d.roundToLong())
          } else {
            decFormatLocal.get()!!.format(d)
          }
        }
      }
    return toDisplayDigits(raw, useBengaliDigits)
  }

  fun formatEditableNumber(value: Double): String {
    if (!value.isFinite() || value <= 0.0) return ""
    val bd = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
    return bd.toPlainString()
  }

  fun formatTaka(
    amount: Double,
    useBengaliDigits: Boolean,
    showSign: Boolean = false,
  ): String {
    val safeAmount = if (amount.isFinite()) amount else 0.0
    val sign =
      when {
        !showSign -> if (safeAmount < -AMOUNT_EPSILON) "-" else ""
        safeAmount > AMOUNT_EPSILON -> "+"
        safeAmount < -AMOUNT_EPSILON -> "-"
        else -> ""
      }
    val absStr = formatNumber(abs(safeAmount), useBengaliDigits)
    return "$sign৳ $absStr"
  }

  fun formatQuantity(
    units: Long,
    unitType: UnitType,
    useBengaliDigits: Boolean,
  ): String {
    val isNegative = units < 0L
    val absUnits = abs(units)
    val prefix = if (isNegative) "-" else ""
    val text =
      when (unitType) {
        UnitType.KG -> {
          val kg = absUnits / GRAMS_PER_KG
          val grams = absUnits % GRAMS_PER_KG
          when {
            kg > 0L && grams > 0L -> "$kg কেজি $grams গ্রাম"
            kg > 0L -> "$kg কেজি"
            else -> "$grams গ্রাম"
          }
        }

        UnitType.PIECE -> {
          "$absUnits পিস"
        }

        UnitType.BUNCH -> {
          "$absUnits আঁটি"
        }
      }
    return toDisplayDigits(prefix + text, useBengaliDigits)
  }

  fun formatDate(
    timestamp: Long,
    useBengaliDigits: Boolean,
  ): String {
    val sdf = dateFormatLocal.get()!!
    return toDisplayDigits(sdf.format(Date(timestamp)), useBengaliDigits)
  }

  fun formatDateTime(
    timestamp: Long,
    useBengaliDigits: Boolean,
  ): String {
    val sdf = dateTimeFormatLocal.get()!!
    return toDisplayDigits(sdf.format(Date(timestamp)), useBengaliDigits)
  }

  fun startOfDay(timestamp: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance(bdTimeZone)
    cal.timeInMillis = timestamp
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
  }

  fun startOfDaysAgo(daysAgo: Int): Long {
    val cal = Calendar.getInstance(bdTimeZone)
    cal.timeInMillis = startOfDay()
    cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return cal.timeInMillis
  }

  fun startOfMonth(): Long {
    val cal = Calendar.getInstance(bdTimeZone)
    cal.timeInMillis = startOfDay()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    return cal.timeInMillis
  }

  fun utcDateMillisToDhakaStartOfDay(utcMillis: Long): Long {
    val utcCal =
      Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    val bdCal =
      Calendar.getInstance(bdTimeZone).apply {
        clear()
        set(
          utcCal.get(Calendar.YEAR),
          utcCal.get(Calendar.MONTH),
          utcCal.get(Calendar.DAY_OF_MONTH),
          0,
          0,
          0,
        )
        set(Calendar.MILLISECOND, 0)
      }
    return bdCal.timeInMillis
  }

  fun utcDateMillisToDhakaEndOfDay(utcMillis: Long): Long {
    val utcCal =
      Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    val bdCal =
      Calendar.getInstance(bdTimeZone).apply {
        clear()
        set(
          utcCal.get(Calendar.YEAR),
          utcCal.get(Calendar.MONTH),
          utcCal.get(Calendar.DAY_OF_MONTH),
          23,
          59,
          59,
        )
        set(Calendar.MILLISECOND, 999)
      }
    return bdCal.timeInMillis
  }
}
