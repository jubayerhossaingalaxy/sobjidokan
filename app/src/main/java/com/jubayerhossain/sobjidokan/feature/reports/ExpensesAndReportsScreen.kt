
@file:Suppress("ktlint:standard:function-naming")

package com.jubayerhossain.sobjidokan.feature.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jubayerhossain.sobjidokan.data.local.entity.ExpenseCategory
import com.jubayerhossain.sobjidokan.data.local.entity.ExpenseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyType
import com.jubayerhossain.sobjidokan.data.local.entity.PaymentMethod
import com.jubayerhossain.sobjidokan.data.local.entity.UnitType
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator
import com.jubayerhossain.sobjidokan.feature.parties.PartiesLedgerContent
import com.jubayerhossain.sobjidokan.ui.ReportPeriod
import com.jubayerhossain.sobjidokan.ui.ShopUiState
import com.jubayerhossain.sobjidokan.ui.components.BadgeTone
import com.jubayerhossain.sobjidokan.ui.components.ConfirmationDialog
import com.jubayerhossain.sobjidokan.ui.components.EmptyStateView
import com.jubayerhossain.sobjidokan.ui.components.PnlKeyValueRow
import com.jubayerhossain.sobjidokan.ui.components.QuickAddPartyDialog
import com.jubayerhossain.sobjidokan.ui.components.SearchFieldBar
import com.jubayerhossain.sobjidokan.ui.components.StatusBadge
import com.jubayerhossain.sobjidokan.ui.theme.BazarDimens
import com.jubayerhossain.sobjidokan.ui.theme.LocalBazarColors

private const val WEEK_LOOKBACK_DAYS = 6

private data class TopVegMetric(
  val name: String,
  val unitType: UnitType,
  val totalQuantityUnits: Long,
  val totalRevenue: Double,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesAndReportsScreen(
  uiState: ShopUiState,
  initialTab: Int = 1,
  showCreditLedgerTab: Boolean = true,
  onSaveExpense: (Long?, ExpenseCategory, Double, PaymentMethod, String, Long) -> Unit,
  onCancelExpense: (Long) -> Unit,
  onAddParty: (String, String, PartyType, String) -> Unit,
  onRecordDuePayment: (PartyEntity, Double, PaymentMethod, String) -> Unit,
  onSaveExpenseAsync: ((Long?, ExpenseCategory, Double, PaymentMethod, String, Long, () -> Unit) -> Unit)? =
    null,
  onRecordDuePaymentAsync: ((PartyEntity, Double, PaymentMethod, String, () -> Unit) -> Unit)? =
    null,
  onCancelSale: (Long) -> Unit = {},
  onCancelPurchase: (Long) -> Unit = {},
  onCancelDuePayment: (Long) -> Unit = {},
) {
  val bn = uiState.preferences.useBengaliDigits
  val showCredit = showCreditLedgerTab && uiState.preferences.showCreditLedger
  var selectedTab by
    remember(initialTab, showCredit) {
      mutableIntStateOf(
        if (!showCredit && initialTab > 1) {
          1
        } else {
          initialTab.coerceIn(0, if (showCredit) 2 else 1)
        },
      )
    }

  var showExpenseSheet by remember { mutableStateOf(false) }
  var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
  var deletingConfirmExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
  var expensePeriod by remember { mutableStateOf(ReportPeriod.ALL) }
  var expenseCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }
  var expenseSearchQuery by remember { mutableStateOf("") }

  var showAddPartyDialog by remember { mutableStateOf<PartyType?>(null) }
  var partySubTab by remember { mutableIntStateOf(0) } // 0 = Customer, 1 = Supplier

  var reportPeriod by remember { mutableStateOf(ReportPeriod.TODAY) }
  var showCustomRangePicker by remember { mutableStateOf(false) }
  var customStart by remember { mutableStateOf(BusinessCalculator.startOfDay()) }
  var customEnd by remember { mutableStateOf(BusinessCalculator.startOfDay() + 86_399_999L) }

  val (startMs, endMs) =
    when (reportPeriod) {
      ReportPeriod.TODAY -> BusinessCalculator.startOfDay() to Long.MAX_VALUE
      ReportPeriod.WEEK -> BusinessCalculator.startOfDaysAgo(WEEK_LOOKBACK_DAYS) to Long.MAX_VALUE
      ReportPeriod.MONTH -> BusinessCalculator.startOfMonth() to Long.MAX_VALUE
      ReportPeriod.ALL -> 0L to Long.MAX_VALUE
      ReportPeriod.CUSTOM -> customStart to customEnd
    }

  val periodSummary =
    remember(uiState, startMs, endMs) {
      BusinessCalculator.calculateFinancialSummary(
        sales = uiState.sales.filter { it.timestamp in startMs..endMs },
        saleItemsBySaleId = uiState.saleItemsBySaleId,
        purchases = uiState.purchases.filter { it.timestamp in startMs..endMs },
        expenses = uiState.expenses.filter { it.timestamp in startMs..endMs },
        wastages = uiState.wastages.filter { it.timestamp in startMs..endMs },
        duePayments = uiState.duePayments.filter { it.timestamp in startMs..endMs },
        vegetables = uiState.activeVegetables,
      )
    }

  val expenseBreakdown =
    remember(uiState.expenses, startMs, endMs) {
      uiState.expenses
        .filter { !it.isCancelled && it.timestamp in startMs..endMs }
        .groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
        .entries
        .sortedByDescending { it.value }
    }

  val topSellingVegs =
    remember(uiState.sales, uiState.saleItemsBySaleId, startMs, endMs) {
      val activeSaleIds =
        uiState.sales
          .asSequence()
          .filter { !it.isCancelled && it.timestamp in startMs..endMs }
          .map { it.id }
          .toHashSet()
      uiState.saleItemsBySaleId
        .filterKeys { it in activeSaleIds }
        .values
        .flatten()
        .groupBy { it.vegetableName to it.unitType }
        .map { (key, items) ->
          TopVegMetric(
            name = key.first,
            unitType = key.second,
            totalQuantityUnits = items.sumOf { it.quantityUnits },
            totalRevenue = items.sumOf { it.lineTotal },
          )
        }.sortedByDescending { it.totalRevenue }
        .take(5)
    }

  val expCutoff =
    when (expensePeriod) {
      ReportPeriod.TODAY -> BusinessCalculator.startOfDay()
      ReportPeriod.WEEK -> BusinessCalculator.startOfDaysAgo(WEEK_LOOKBACK_DAYS)
      ReportPeriod.MONTH -> BusinessCalculator.startOfMonth()
      else -> 0L
    }

  val filteredExpenses =
    remember(uiState.expenses, expCutoff, expenseCategoryFilter, expenseSearchQuery) {
      val q = expenseSearchQuery.trim()
      uiState.expenses.filter { exp ->
        !exp.isCancelled &&
          exp.timestamp >= expCutoff &&
          (expenseCategoryFilter == null || exp.category == expenseCategoryFilter) &&
          (
            q.isEmpty() ||
              exp.category.labelBn.contains(q, ignoreCase = true) ||
              exp.note.contains(q, ignoreCase = true)
          )
      }
    }
  val filteredExpenseTotal = remember(filteredExpenses) { filteredExpenses.sumOf { it.amount } }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    floatingActionButton = {
      if (selectedTab == 0) {
        ExtendedFloatingActionButton(
          onClick = {
            editingExpense = null
            showExpenseSheet = true
          },
          icon = { Icon(Icons.Filled.Add, contentDescription = null) },
          text = { Text("নতুন খরচ") },
          modifier = Modifier.testTag("fab_add_expense"),
        )
      } else if (selectedTab == 2 && showCredit) {
        ExtendedFloatingActionButton(
          onClick = {
            showAddPartyDialog =
              if (partySubTab == 0) PartyType.CUSTOMER else PartyType.SUPPLIER
          },
          icon = { Icon(Icons.Filled.Add, contentDescription = null) },
          text = { Text(if (partySubTab == 0) "নতুন ক্রেতা" else "নতুন পাইকার") },
          modifier = Modifier.testTag("fab_add_party"),
        )
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(modifier = Modifier.fillMaxSize().widthIn(max = BazarDimens.MaxContentWidthExpanded)) {
        Text(
          text = if (showCredit) "খরচ, রিপোর্ট ও বাকি" else "খরচ ও রিপোর্ট",
          style = MaterialTheme.typography.headlineSmall,
          modifier =
            Modifier.padding(horizontal = BazarDimens.ScreenHorizontal, vertical = 12.dp),
        )

        TabRow(selectedTabIndex = selectedTab) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("খরচ") },
            modifier = Modifier.testTag("tab_expenses"),
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("রিপোর্ট") },
            modifier = Modifier.testTag("tab_reports"),
          )
          if (showCredit) {
            Tab(
              selected = selectedTab == 2,
              onClick = { selectedTab = 2 },
              text = { Text("বাকি খাতা") },
              modifier = Modifier.testTag("tab_credit_ledger"),
            )
          }
        }

        when (selectedTab) {
          0 -> {
            Column(modifier = Modifier.fillMaxSize()) {
              Spacer(modifier = Modifier.height(10.dp))
              SearchFieldBar(
                query = expenseSearchQuery,
                onQueryChange = { expenseSearchQuery = it },
                placeholder = "খরচের বিবরণ বা খাত খুঁজুন...",
                modifier = Modifier.padding(horizontal = BazarDimens.ScreenHorizontal),
                testTag = "expense_search_field",
              )
              Spacer(modifier = Modifier.height(8.dp))
              LazyRow(
                contentPadding = PaddingValues(horizontal = BazarDimens.ScreenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                items(
                  listOf(
                    ReportPeriod.ALL,
                    ReportPeriod.TODAY,
                    ReportPeriod.WEEK,
                    ReportPeriod.MONTH,
                  ),
                ) { p ->
                  FilterChip(
                    selected = expensePeriod == p,
                    onClick = { expensePeriod = p },
                    label = { Text(p.labelBn) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  )
                }
              }
              LazyRow(
                contentPadding = PaddingValues(horizontal = BazarDimens.ScreenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                item {
                  FilterChip(
                    selected = expenseCategoryFilter == null,
                    onClick = { expenseCategoryFilter = null },
                    label = { Text("সব খাত") },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  )
                }
                items(ExpenseCategory.entries) { cat ->
                  FilterChip(
                    selected = expenseCategoryFilter == cat,
                    onClick = {
                      expenseCategoryFilter = if (expenseCategoryFilter == cat) null else cat
                    },
                    label = { Text(cat.labelBn) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  )
                }
              }

              if (filteredExpenses.isNotEmpty()) {
                Card(
                  colors =
                    CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                  shape = MaterialTheme.shapes.medium,
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .padding(horizontal = BazarDimens.ScreenHorizontal, vertical = 6.dp)
                      .testTag("expenses_total_strip"),
                ) {
                  Row(
                    modifier =
                      Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text =
                        "মোট খরচ (${BusinessCalculator.formatNumber(
                          filteredExpenses.size,
                          bn,
                        )}টি)",
                      style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                      text = BusinessCalculator.formatTaka(filteredExpenseTotal, bn),
                      style =
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.secondary,
                    )
                  }
                }
              }

              if (filteredExpenses.isEmpty()) {
                EmptyStateView(
                  icon = Icons.AutoMirrored.Filled.ReceiptLong,
                  title = "কোনো খরচ পাওয়া যায়নি",
                  tip =
                    if (uiState.expenses.none { !it.isCancelled }) {
                      "দোকান ভাড়া, পলিথিন বা ভ্যান ভাড়া লিখতে '+ নতুন খরচ' চাপুন।"
                    } else {
                      "অন্য সময় বা খাত নির্বাচন করে দেখুন।"
                    },
                  actionLabel =
                    if (
                      expenseCategoryFilter != null ||
                      expensePeriod != ReportPeriod.ALL ||
                      expenseSearchQuery.isNotBlank()
                    ) {
                      "ফিল্টার মুছুন"
                    } else {
                      null
                    },
                  onAction =
                    if (
                      expenseCategoryFilter != null ||
                      expensePeriod != ReportPeriod.ALL ||
                      expenseSearchQuery.isNotBlank()
                    ) {
                      {
                        expenseCategoryFilter = null
                        expensePeriod = ReportPeriod.ALL
                        expenseSearchQuery = ""
                      }
                    } else {
                      null
                    },
                  modifier = Modifier.weight(1f),
                )
              } else {
                LazyColumn(
                  modifier = Modifier.weight(1f),
                  contentPadding =
                    PaddingValues(
                      start = BazarDimens.ScreenHorizontal,
                      end = BazarDimens.ScreenHorizontal,
                      top = 8.dp,
                      bottom = BazarDimens.FabBottomClearance,
                    ),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  items(filteredExpenses, key = { it.id }) { exp ->
                    Card(
                      onClick = {
                        editingExpense = exp
                        showExpenseSheet = true
                      },
                      colors =
                        CardDefaults.cardColors(
                          containerColor = MaterialTheme.colorScheme.surface,
                        ),
                      border =
                        BorderStroke(
                          BazarDimens.CardHairlineBorder,
                          MaterialTheme.colorScheme.outlineVariant,
                        ),
                      shape = MaterialTheme.shapes.medium,
                      modifier = Modifier.fillMaxWidth().testTag("expense_card_${exp.id}"),
                    ) {
                      Row(
                        modifier = Modifier.padding(BazarDimens.CardPadding),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                          ) {
                            Text(
                              exp.category.labelBn,
                              style = MaterialTheme.typography.titleSmall,
                            )
                            StatusBadge(
                              text = exp.paymentMethod.labelBn,
                              tone = BadgeTone.NEUTRAL,
                            )
                          }
                          if (exp.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(exp.note, style = MaterialTheme.typography.bodySmall)
                          }
                          Text(
                            text = BusinessCalculator.formatDateTime(exp.timestamp, bn),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                        }
                        Text(
                          text = BusinessCalculator.formatTaka(exp.amount, bn),
                          style =
                            MaterialTheme.typography.titleMedium.copy(
                              fontWeight = FontWeight.Bold,
                            ),
                          color = MaterialTheme.colorScheme.secondary,
                        )
                        IconButton(
                          onClick = { deletingConfirmExpense = exp },
                          modifier =
                            Modifier
                              .minimumInteractiveComponentSize()
                              .testTag("expense_delete_${exp.id}"),
                        ) {
                          Icon(
                            Icons.Filled.Delete,
                            contentDescription = "বাতিল",
                            tint = MaterialTheme.colorScheme.error,
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          1 -> {
            val bazarColors = LocalBazarColors.current
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(BazarDimens.ScreenHorizontal),
              verticalArrangement = Arrangement.spacedBy(BazarDimens.SpaceMd),
            ) {
              item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ReportPeriod.entries) { period ->
                      FilterChip(
                        selected = reportPeriod == period,
                        onClick = {
                          if (period == ReportPeriod.CUSTOM) {
                            showCustomRangePicker = true
                          } else {
                            reportPeriod = period
                          }
                        },
                        label = { Text(period.labelBn) },
                        modifier =
                          Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("period_chip_${period.name}"),
                      )
                    }
                  }
                  if (reportPeriod == ReportPeriod.CUSTOM) {
                    Text(
                      text =
                        "নির্বাচিত তারিখ: ${BusinessCalculator.formatDate(
                          customStart,
                          bn,
                        )} – " +
                          BusinessCalculator.formatDate(customEnd, bn),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
              }

              // P&L Waterfall Card
              item {
                Card(
                  colors =
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  border =
                    BorderStroke(
                      BazarDimens.CardHairlineBorder,
                      MaterialTheme.colorScheme.outlineVariant,
                    ),
                  shape = MaterialTheme.shapes.large,
                  modifier = Modifier.fillMaxWidth().testTag("pnl_waterfall_card"),
                ) {
                  Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text("লাভ-ক্ষতির হিসাব", style = MaterialTheme.typography.titleLarge)
                    HorizontalDivider()
                    PnlKeyValueRow(
                      label = "মোট বিক্রি",
                      value = BusinessCalculator.formatTaka(periodSummary.netSales, bn),
                    )
                    PnlKeyValueRow(
                      label = "(-) বিক্রিত পণ্যের কেনা দাম",
                      value = BusinessCalculator.formatTaka(periodSummary.cogs, bn),
                    )
                    HorizontalDivider()
                    PnlKeyValueRow(
                      label = "(=) মোট লাভ",
                      value = BusinessCalculator.formatTaka(periodSummary.grossProfit, bn),
                      bold = true,
                    )
                    PnlKeyValueRow(
                      label = "(-) পচা/নষ্ট অপচয় ক্ষতি",
                      value = BusinessCalculator.formatTaka(periodSummary.wastageLoss, bn),
                      labelColor = MaterialTheme.colorScheme.error,
                      valueColor = MaterialTheme.colorScheme.error,
                    )
                    PnlKeyValueRow(
                      label = "(-) দোকান ও পরিবহন খরচ",
                      value = BusinessCalculator.formatTaka(periodSummary.operatingExpenses, bn),
                      labelColor = MaterialTheme.colorScheme.secondary,
                      valueColor = MaterialTheme.colorScheme.secondary,
                    )
                    HorizontalDivider()
                    val netColor =
                      if (periodSummary.netProfit >= 0) {
                        bazarColors.success
                      } else {
                        MaterialTheme.colorScheme.error
                      }
                    PnlKeyValueRow(
                      label = "(=) প্রকৃত নিট লাভ",
                      value =
                        BusinessCalculator.formatTaka(
                          periodSummary.netProfit,
                          bn,
                          showSign = true,
                        ),
                      bold = true,
                      labelColor = netColor,
                      valueColor = netColor,
                    )
                  }
                }
              }

              item {
                CashFlowAndReconciliationCards(
                  periodSummary = periodSummary,
                  useBengaliDigits = bn,
                )
              }

              // Expense Category Breakdown Card
              if (expenseBreakdown.isNotEmpty()) {
                item {
                  val totalCatExp = expenseBreakdown.sumOf { it.value }.coerceAtLeast(1.0)
                  Card(
                    colors =
                      CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                      ),
                    border =
                      BorderStroke(
                        BazarDimens.CardHairlineBorder,
                        MaterialTheme.colorScheme.outlineVariant,
                      ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().testTag("expense_breakdown_card"),
                  ) {
                    Column(
                      modifier = Modifier.padding(16.dp),
                      verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Text("খরচের খাতভিত্তিক হিসাব", style = MaterialTheme.typography.titleMedium)
                      expenseBreakdown.forEach { (cat, amount) ->
                        val ratio = (amount / totalCatExp).toFloat().coerceIn(0.04f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                          PnlKeyValueRow(
                            label = cat.labelBn,
                            value = BusinessCalculator.formatTaka(amount, bn),
                          )
                          Box(
                            modifier =
                              Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                          ) {
                            Box(
                              modifier =
                                Modifier
                                  .fillMaxWidth(ratio)
                                  .fillMaxHeight()
                                  .clip(RoundedCornerShape(4.dp))
                                  .background(MaterialTheme.colorScheme.secondary),
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }

              // Top Selling Vegetables Card
              if (topSellingVegs.isNotEmpty()) {
                item {
                  Card(
                    colors =
                      CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                      ),
                    border =
                      BorderStroke(
                        BazarDimens.CardHairlineBorder,
                        MaterialTheme.colorScheme.outlineVariant,
                      ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().testTag("top_selling_vegs_card"),
                  ) {
                    Column(
                      modifier = Modifier.padding(16.dp),
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Text("বেশি বিক্রি হওয়া সবজি", style = MaterialTheme.typography.titleMedium)
                      topSellingVegs.forEach { item ->
                        val qtyText =
                          BusinessCalculator.formatQuantity(
                            item.totalQuantityUnits,
                            item.unitType,
                            bn,
                          )
                        PnlKeyValueRow(
                          label = "${item.name} ($qtyText)",
                          value = BusinessCalculator.formatTaka(item.totalRevenue, bn),
                          valueColor = MaterialTheme.colorScheme.primary,
                        )
                      }
                    }
                  }
                }
              }

              // Stock & Activity Card
              item {
                Card(
                  colors =
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  border =
                    BorderStroke(
                      BazarDimens.CardHairlineBorder,
                      MaterialTheme.colorScheme.outlineVariant,
                    ),
                  shape = MaterialTheme.shapes.medium,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text("দোকানের অবস্থা", style = MaterialTheme.typography.titleMedium)
                    PnlKeyValueRow(
                      label = "সক্রিয় বিক্রয় বিল",
                      value =
                        "${BusinessCalculator.formatNumber(
                          periodSummary.activeSalesCount,
                          bn,
                        )} টি",
                    )
                    val purchasesCountText =
                      BusinessCalculator.formatNumber(periodSummary.activePurchasesCount, bn)
                    PnlKeyValueRow(label = "সক্রিয় ক্রয় এন্ট্রি", value = "$purchasesCountText টি")
                    PnlKeyValueRow(
                      label = "বর্তমান অবশিষ্ট স্টক মূল্য",
                      value = BusinessCalculator.formatTaka(periodSummary.inventoryValue, bn),
                      bold = true,
                    )
                  }
                }
              }
            }
          }

          2 -> {
            PartiesLedgerContent(
              uiState = uiState,
              partySubTab = partySubTab,
              onSubTabChange = { partySubTab = it },
              onRecordDuePayment = { party, amt, method, note, onDone ->
                if (onRecordDuePaymentAsync != null) {
                  onRecordDuePaymentAsync(party, amt, method, note, onDone)
                } else {
                  onRecordDuePayment(party, amt, method, note)
                  onDone()
                }
              },
              onCancelSale = onCancelSale,
              onCancelPurchase = onCancelPurchase,
              onAddPartyClick = { showAddPartyDialog = it },
              onCancelDuePayment = onCancelDuePayment,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }

  if (showExpenseSheet) {
    AddEditExpenseSheet(
      existing = editingExpense,
      isSaving = uiState.isSaving,
      onDismiss = { showExpenseSheet = false },
      onSave = { cat, amt, method, note ->
        val ts = editingExpense?.timestamp ?: System.currentTimeMillis()
        if (onSaveExpenseAsync != null) {
          onSaveExpenseAsync(editingExpense?.id, cat, amt, method, note, ts) {
            showExpenseSheet = false
          }
        } else {
          onSaveExpense(editingExpense?.id, cat, amt, method, note, ts)
          showExpenseSheet = false
        }
      },
    )
  }

  deletingConfirmExpense?.let { exp ->
    val formattedExpAmount = BusinessCalculator.formatTaka(exp.amount, bn)
    ConfirmationDialog(
      title = "খরচ বাতিল করবেন?",
      message = "${exp.category.labelBn} বাবদ $formattedExpAmount খরচটি বাতিল করতে চান?",
      confirmLabel = "হ্যাঁ, বাতিল করুন",
      isDestructive = true,
      onConfirm = {
        onCancelExpense(exp.id)
        deletingConfirmExpense = null
      },
      onDismiss = { deletingConfirmExpense = null },
    )
  }

  showAddPartyDialog?.let { pType ->
    QuickAddPartyDialog(
      partyType = pType,
      onDismiss = { showAddPartyDialog = null },
      onSave = { name, phone ->
        onAddParty(name, phone, pType, "")
        showAddPartyDialog = null
      },
    )
  }

  if (showCustomRangePicker) {
    val pickerState = rememberDateRangePickerState()
    var dateError by remember { mutableStateOf<String?>(null) }
    DatePickerDialog(
      onDismissRequest = { showCustomRangePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            val s = pickerState.selectedStartDateMillis
            val e = pickerState.selectedEndDateMillis
            val todayEnd = BusinessCalculator.startOfDay() + 86_399_999L
            if (s == null || e == null) {
              dateError = "শুরু এবং শেষ তারিখ উভয়ই নির্বাচন করুন।"
            } else {
              val startBd = BusinessCalculator.utcDateMillisToDhakaStartOfDay(s)
              val endBd = BusinessCalculator.utcDateMillisToDhakaEndOfDay(e)
              when {
                startBd > endBd -> {
                  dateError = "শুরুর তারিখ শেষ তারিখের পরে হতে পারবে না।"
                }

                startBd > todayEnd -> {
                  dateError = "ভবিষ্যতের তারিখ দিয়ে রিপোর্ট দেখা যাবে না।"
                }

                else -> {
                  customStart = startBd
                  customEnd = endBd
                  reportPeriod = ReportPeriod.CUSTOM
                  showCustomRangePicker = false
                }
              }
            }
          },
        ) {
          Text("ঠিক আছে")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCustomRangePicker = false }) { Text("বাতিল") }
      },
    ) {
      Column {
        DateRangePicker(state = pickerState, modifier = Modifier.height(400.dp))
        if (dateError != null) {
          Text(
            text = dateError!!,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseSheet(
  existing: ExpenseEntity?,
  isSaving: Boolean = false,
  onDismiss: () -> Unit,
  onSave: (ExpenseCategory, Double, PaymentMethod, String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var category by remember { mutableStateOf(existing?.category ?: ExpenseCategory.TRANSPORT) }
  var paymentMethod by remember {
    mutableStateOf(
      existing?.paymentMethod?.takeIf { it != PaymentMethod.CREDIT } ?: PaymentMethod.CASH,
    )
  }
  var amountText by remember {
    mutableStateOf(existing?.amount?.let { BusinessCalculator.formatEditableNumber(it) } ?: "")
  }
  var note by remember { mutableStateOf(existing?.note ?: "") }
  var errorText by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .imePadding()
          .navigationBarsPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = BazarDimens.SheetHorizontal, vertical = BazarDimens.SpaceSm),
    ) {
      Text(
        text = if (existing == null) "নতুন দোকান খরচ" else "খরচ সম্পাদনা",
        style = MaterialTheme.typography.headlineSmall,
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text("খরচের খাত", style = MaterialTheme.typography.labelLarge)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ExpenseCategory.entries) { cat ->
          FilterChip(
            selected = category == cat,
            onClick = { category = cat },
            label = { Text(cat.labelBn) },
            modifier = Modifier.minimumInteractiveComponentSize(),
          )
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = amountText,
        onValueChange = {
          amountText = it
          errorText = null
        },
        label = { Text("টাকার পরিমাণ (৳) *") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        isError = errorText != null,
        supportingText = { if (errorText != null) Text(errorText!!) },
        modifier = Modifier.fillMaxWidth().testTag("expense_amount_input"),
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text("পেমেন্ট মাধ্যম", style = MaterialTheme.typography.labelLarge)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(PaymentMethod.CASH, PaymentMethod.MOBILE).forEach { m ->
          FilterChip(
            selected = paymentMethod == m,
            onClick = { paymentMethod = m },
            label = { Text(m.labelBn) },
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("expense_method_${m.name}"),
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("বিবরণ (ঐচ্ছিক)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = {
          val amt = amountText.toDoubleOrNull()
          if (amt == null || !amt.isFinite() || amt <= 0.0) {
            errorText = "সঠিক টাকার পরিমাণ লিখুন (শূন্যের বেশি)।"
          } else {
            onSave(category, amt, paymentMethod, note)
          }
        },
        enabled = !isSaving,
        modifier =
          Modifier
            .fillMaxWidth()
            .height(BazarDimens.PrimaryButtonHeight)
            .testTag("save_expense_button"),
      ) {
        Text(if (isSaving) "সংরক্ষণ হচ্ছে..." else "খরচ সেভ করুন")
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
