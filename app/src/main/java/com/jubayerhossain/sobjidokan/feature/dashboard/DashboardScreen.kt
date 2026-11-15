@file:Suppress("ktlint:standard:function-naming")

package com.jubayerhossain.sobjidokan.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jubayerhossain.sobjidokan.data.local.entity.SaleEntity
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableEntity
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator
import com.jubayerhossain.sobjidokan.feature.sales.SaleDetailBottomSheet
import com.jubayerhossain.sobjidokan.ui.ShopUiState
import com.jubayerhossain.sobjidokan.ui.components.BadgeTone
import com.jubayerhossain.sobjidokan.ui.components.MetricSummaryCard
import com.jubayerhossain.sobjidokan.ui.components.SectionHeader
import com.jubayerhossain.sobjidokan.ui.components.StatusBadge
import com.jubayerhossain.sobjidokan.ui.components.StockHealthBar
import com.jubayerhossain.sobjidokan.ui.theme.BazarDimens
import com.jubayerhossain.sobjidokan.ui.theme.LocalBazarColors

private const val DASHBOARD_LOW_STOCK_LIMIT = 5

@Composable
fun DashboardScreen(
  uiState: ShopUiState,
  onOpenSettings: () -> Unit,
  onQuickSale: () -> Unit,
  onQuickPurchase: () -> Unit,
  onQuickWastage: () -> Unit,
  onQuickExpense: () -> Unit,
  onOpenStock: () -> Unit,
  onOpenReports: () -> Unit = {},
  onOpenSalesHistory: () -> Unit = {},
  onOpenExpenses: () -> Unit = {},
  onOpenWastage: () -> Unit = {},
  onOpenParties: (Int) -> Unit = {},
  onCancelSale: (Long) -> Unit = {},
) {
  val bn = uiState.preferences.useBengaliDigits
  val summary = uiState.todaySummary
  val bazarColors = LocalBazarColors.current
  val isProfitPositive = summary.netProfit >= 0.0

  var inspectingSale by remember { mutableStateOf<SaleEntity?>(null) }

  val totalCustomerDue =
    remember(uiState.customerBalances) { uiState.customerBalances.sumOf { it.balanceDue } }
  val customerDueCount =
    remember(uiState.customerBalances) {
      uiState.customerBalances.count { it.balanceDue > 0.005 }
    }
  val totalSupplierDue =
    remember(uiState.supplierBalances) { uiState.supplierBalances.sumOf { it.balanceDue } }
  val supplierDueCount =
    remember(uiState.supplierBalances) {
      uiState.supplierBalances.count { it.balanceDue > 0.005 }
    }
  val recentSales = remember(uiState.sales) { uiState.sales.filter { !it.isCancelled }.take(3) }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isWide = maxWidth >= 720.dp

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
      LazyColumn(
        modifier =
          Modifier
            .fillMaxSize()
            .widthIn(max = BazarDimens.MaxContentWidthExpanded)
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(BazarDimens.ScreenHorizontal),
        verticalArrangement = Arrangement.spacedBy(BazarDimens.SpaceMd),
      ) {
        // Header
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = uiState.preferences.shopName.ifBlank { "সবজি খাতা" },
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.height(BazarDimens.SpaceXxs))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BazarDimens.SpaceSm),
              ) {
                Text(
                  text = BusinessCalculator.formatDate(System.currentTimeMillis(), bn),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusBadge(
                  text = "অফলাইন সক্রিয়",
                  tone = BadgeTone.SUCCESS,
                  icon = Icons.Filled.OfflinePin,
                )
              }
            }
            IconButton(
              onClick = onOpenSettings,
              modifier =
                Modifier.minimumInteractiveComponentSize().testTag("dashboard_settings_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "সেটিংস ও ব্যাকআপ",
                tint = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }

        // Hero Net Profit Card (tappable drill-down to P&L Report)
        item {
          val containerColor =
            if (isProfitPositive) {
              bazarColors.successContainer
            } else {
              MaterialTheme.colorScheme.errorContainer
            }
          val contentAccent =
            if (isProfitPositive) bazarColors.success else MaterialTheme.colorScheme.error

          val cardBorderColor = contentAccent.copy(alpha = 0.25f)
          Card(
            onClick = onOpenReports,
            modifier =
              Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .testTag("hero_net_profit_card"),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(BazarDimens.CardHairlineBorder, cardBorderColor),
          ) {
            Column(modifier = Modifier.padding(BazarDimens.HeroCardPadding)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = if (isProfitPositive) "আজকের প্রকৃত নিট লাভ" else "আজকের নিট ক্ষতি",
                  style = MaterialTheme.typography.titleMedium,
                  color = contentAccent,
                )
                Icon(
                  imageVector =
                    if (isProfitPositive) {
                      Icons.AutoMirrored.Filled.TrendingUp
                    } else {
                      Icons.AutoMirrored.Filled.TrendingDown
                    },
                  contentDescription = null,
                  tint = contentAccent,
                )
              }
              Spacer(modifier = Modifier.height(BazarDimens.SpaceSm))
              Text(
                text = BusinessCalculator.formatTaka(summary.netProfit, bn, showSign = true),
                style = MaterialTheme.typography.displaySmall,
                color = contentAccent,
              )
              Spacer(modifier = Modifier.height(BazarDimens.SpaceSm))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                val expenseTotal =
                  BusinessCalculator.formatTaka(
                    summary.operatingExpenses + summary.wastageLoss,
                    bn,
                  )
                Text(
                  text =
                    "বিক্রি ${BusinessCalculator.formatTaka(summary.netSales, bn)} • " +
                      "কেনা দাম ${BusinessCalculator.formatTaka(summary.cogs, bn)} • " +
                      "খরচ ও পচা $expenseTotal",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.weight(1f),
                )
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                  contentDescription = "রিপোর্ট দেখুন",
                  tint = contentAccent,
                  modifier = Modifier.size(BazarDimens.IconMd),
                )
              }
            }
          }
        }

        // Metric Summary Grid (2x2 on phone, 4-across on tablet)
        if (isWide) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              MetricSummaryCard(
                title =
                  "আজকের বিক্রি (${BusinessCalculator.formatNumber(summary.activeSalesCount, bn)})",
                valueText = BusinessCalculator.formatTaka(summary.netSales, bn),
                subtitle = "মোট লাভ: ${BusinessCalculator.formatTaka(summary.grossProfit, bn)}",
                icon = Icons.Filled.PointOfSale,
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = onOpenSalesHistory,
                modifier = Modifier.weight(1f).testTag("metric_sales_card"),
              )
              MetricSummaryCard(
                title = "আজকের নগদ প্রবাহ",
                valueText =
                  BusinessCalculator.formatTaka(summary.netCashFlow, bn, showSign = true),
                subtitle = "এল: ${BusinessCalculator.formatTaka(summary.cashInflow, bn)}",
                icon = Icons.Filled.AccountBalanceWallet,
                accentColor = MaterialTheme.colorScheme.tertiary,
                onClick = onOpenReports,
                modifier = Modifier.weight(1f).testTag("metric_cash_card"),
              )
              MetricSummaryCard(
                title = "দোকানের খরচ",
                valueText = BusinessCalculator.formatTaka(summary.operatingExpenses, bn),
                subtitle = "পরিবহন ও পরিচালনা",
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                accentColor = MaterialTheme.colorScheme.secondary,
                onClick = onOpenExpenses,
                modifier = Modifier.weight(1f).testTag("metric_expense_card"),
              )
              MetricSummaryCard(
                title = "পচা/নষ্ট ক্ষতি",
                valueText = BusinessCalculator.formatTaka(summary.wastageLoss, bn),
                subtitle = "স্টক থেকে বাদ",
                icon = Icons.Filled.DeleteSweep,
                accentColor = MaterialTheme.colorScheme.error,
                onClick = onOpenWastage,
                modifier = Modifier.weight(1f).testTag("metric_wastage_card"),
              )
            }
          }
        } else {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              MetricSummaryCard(
                title =
                  "আজকের বিক্রি (${BusinessCalculator.formatNumber(summary.activeSalesCount, bn)})",
                valueText = BusinessCalculator.formatTaka(summary.netSales, bn),
                subtitle = "মোট লাভ: ${BusinessCalculator.formatTaka(summary.grossProfit, bn)}",
                icon = Icons.Filled.PointOfSale,
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = onOpenSalesHistory,
                modifier = Modifier.weight(1f).testTag("metric_sales_card"),
              )
              MetricSummaryCard(
                title = "আজকের নগদ প্রবাহ",
                valueText =
                  BusinessCalculator.formatTaka(summary.netCashFlow, bn, showSign = true),
                subtitle = "এল: ${BusinessCalculator.formatTaka(summary.cashInflow, bn)}",
                icon = Icons.Filled.AccountBalanceWallet,
                accentColor = MaterialTheme.colorScheme.tertiary,
                onClick = onOpenReports,
                modifier = Modifier.weight(1f).testTag("metric_cash_card"),
              )
            }
          }
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              MetricSummaryCard(
                title = "দোকানের খরচ",
                valueText = BusinessCalculator.formatTaka(summary.operatingExpenses, bn),
                subtitle = "পরিবহন ও পরিচালনা",
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                accentColor = MaterialTheme.colorScheme.secondary,
                onClick = onOpenExpenses,
                modifier = Modifier.weight(1f).testTag("metric_expense_card"),
              )
              MetricSummaryCard(
                title = "পচা/নষ্ট ক্ষতি",
                valueText = BusinessCalculator.formatTaka(summary.wastageLoss, bn),
                subtitle = "স্টক থেকে বাদ",
                icon = Icons.Filled.DeleteSweep,
                accentColor = MaterialTheme.colorScheme.error,
                onClick = onOpenWastage,
                modifier = Modifier.weight(1f).testTag("metric_wastage_card"),
              )
            }
          }
        }

        // Dues & Payables Summary Strip (when credit ledger enabled)
        if (uiState.preferences.showCreditLedger) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              DueStripCard(
                title = "ক্রেতা পাওনা",
                amountText = BusinessCalculator.formatTaka(totalCustomerDue, bn),
                countText =
                  if (customerDueCount == 0) {
                    "বাকি নেই"
                  } else {
                    "${BusinessCalculator.formatNumber(customerDueCount, bn)} জনের বাকি"
                  },
                accent =
                  if (totalCustomerDue > BusinessCalculator.AMOUNT_EPSILON) {
                    bazarColors.warning
                  } else {
                    bazarColors.success
                  },
                icon = Icons.Filled.People,
                onClick = { onOpenParties(0) },
                modifier = Modifier.weight(1f).testTag("dashboard_customer_due_card"),
              )
              DueStripCard(
                title = "পাইকার দেনা",
                amountText = BusinessCalculator.formatTaka(totalSupplierDue, bn),
                countText =
                  if (supplierDueCount == 0) {
                    "দেনা নেই"
                  } else {
                    "${BusinessCalculator.formatNumber(supplierDueCount, bn)} জন পাইকার"
                  },
                accent =
                  if (totalSupplierDue > BusinessCalculator.AMOUNT_EPSILON) {
                    MaterialTheme.colorScheme.error
                  } else {
                    bazarColors.success
                  },
                icon = Icons.Filled.Storefront,
                onClick = { onOpenParties(1) },
                modifier = Modifier.weight(1f).testTag("dashboard_supplier_due_card"),
              )
            }
          }
        }

        // Backup reminder banner if >= 5 transactions and no backup in the last 7 days
        val totalTxCount = uiState.sales.size + uiState.purchases.size
        val lastBackup = uiState.preferences.lastBackupTimestamp
        val backupOverdue =
          totalTxCount >= 5 &&
            (
              lastBackup == 0L ||
                (System.currentTimeMillis() - lastBackup) > 7L * 24 * 60 * 60 * 1000L
            )
        if (backupOverdue) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = bazarColors.warningContainer),
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier.fillMaxWidth().testTag("dashboard_backup_reminder_card"),
            ) {
              Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  modifier = Modifier.weight(1f).padding(end = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = null,
                    tint = bazarColors.warning,
                    modifier = Modifier.size(20.dp),
                  )
                  Text(
                    text =
                      if (lastBackup == 0L) {
                        "নিরাপত্তার জন্য একটি JSON ব্যাকআপ সেভ করে রাখুন"
                      } else {
                        "৭ দিনের বেশি সময় ব্যাকআপ নেওয়া হয়নি"
                      },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
                OutlinedButton(
                  onClick = onOpenSettings,
                  modifier =
                    Modifier
                      .minimumInteractiveComponentSize()
                      .testTag("dashboard_backup_now_button"),
                ) {
                  Text("ব্যাকআপ নিন")
                }
              }
            }
          }
        }

        // Quick Actions
        item {
          SectionHeader(
            title = "দ্রুত কাজ করুন",
            modifier = Modifier.padding(top = BazarDimens.SpaceXs),
          )
          Spacer(modifier = Modifier.height(BazarDimens.SpaceSm))
          if (isWide) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              QuickActionTile(
                label = "নতুন বিক্রি",
                icon = Icons.Filled.PointOfSale,
                isPrimary = true,
                onClick = onQuickSale,
                modifier = Modifier.weight(1f).testTag("quick_action_sale"),
              )
              QuickActionTile(
                label = "নতুন কেনা",
                icon = Icons.Filled.AddShoppingCart,
                isPrimary = false,
                onClick = onQuickPurchase,
                modifier = Modifier.weight(1f).testTag("quick_action_purchase"),
              )
              QuickActionTile(
                label = "পচা বাদ দিন",
                icon = Icons.Filled.DeleteSweep,
                isPrimary = false,
                onClick = onQuickWastage,
                modifier = Modifier.weight(1f).testTag("quick_action_wastage"),
              )
              QuickActionTile(
                label = "নতুন খরচ",
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                isPrimary = false,
                onClick = onQuickExpense,
                modifier = Modifier.weight(1f).testTag("quick_action_expense"),
              )
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(BazarDimens.SpaceSm)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                QuickActionTile(
                  label = "নতুন বিক্রি",
                  icon = Icons.Filled.PointOfSale,
                  isPrimary = true,
                  onClick = onQuickSale,
                  modifier = Modifier.weight(1f).testTag("quick_action_sale"),
                )
                QuickActionTile(
                  label = "নতুন কেনা",
                  icon = Icons.Filled.AddShoppingCart,
                  isPrimary = false,
                  onClick = onQuickPurchase,
                  modifier = Modifier.weight(1f).testTag("quick_action_purchase"),
                )
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                QuickActionTile(
                  label = "পচা বাদ দিন",
                  icon = Icons.Filled.DeleteSweep,
                  isPrimary = false,
                  onClick = onQuickWastage,
                  modifier = Modifier.weight(1f).testTag("quick_action_wastage"),
                )
                QuickActionTile(
                  label = "নতুন খরচ",
                  icon = Icons.AutoMirrored.Filled.ReceiptLong,
                  isPrimary = false,
                  onClick = onQuickExpense,
                  modifier = Modifier.weight(1f).testTag("quick_action_expense"),
                )
              }
            }
          }
        }

        // Recent Sales Section
        if (recentSales.isNotEmpty()) {
          item {
            SectionHeader(
              title = "সাম্প্রতিক বিক্রি",
              actionLabel = "সব দেখুন",
              onAction = onOpenSalesHistory,
              actionTestTag = "dashboard_view_all_sales",
              modifier = Modifier.padding(top = BazarDimens.SpaceXs),
            )
          }
          items(recentSales, key = { "recent_${it.id}" }) { sale ->
            val items = uiState.saleItemsBySaleId[sale.id].orEmpty()
            val itemsPreview =
              items.take(3).joinToString(", ") {
                val qty = BusinessCalculator.formatQuantity(it.quantityUnits, it.unitType, bn)
                "${it.vegetableName} ($qty)"
              }
            Card(
              onClick = { inspectingSale = sale },
              colors =
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border =
                BorderStroke(
                  BazarDimens.CardHairlineBorder,
                  MaterialTheme.colorScheme.outlineVariant,
                ),
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier.fillMaxWidth().testTag("dashboard_recent_sale_${sale.id}"),
            ) {
              Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                  Text(
                    text =
                      "#${BusinessCalculator.formatNumber(sale.id, bn)} • ${sale.customerName}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text =
                      itemsPreview.ifBlank {
                        BusinessCalculator.formatDateTime(sale.timestamp, bn)
                      },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = BusinessCalculator.formatTaka(sale.totalAmount, bn),
                    style =
                      MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                  )
                  StatusBadge(
                    text =
                      if (sale.dueAmount > BusinessCalculator.AMOUNT_EPSILON) {
                        "বাকি ${BusinessCalculator.formatTaka(sale.dueAmount, bn)}"
                      } else {
                        sale.paymentMethod.labelBn
                      },
                    tone =
                      if (sale.dueAmount > BusinessCalculator.AMOUNT_EPSILON) {
                        BadgeTone.WARNING
                      } else {
                        BadgeTone.SUCCESS
                      },
                  )
                }
              }
            }
          }
        }

        // Stock alerts section
        item {
          SectionHeader(
            title = "স্টক সতর্কতা",
            modifier = Modifier.padding(top = BazarDimens.SpaceXs),
            trailingContent =
              if (uiState.lowStockVegetables.isNotEmpty()) {
                {
                  val lowCount =
                    BusinessCalculator.formatNumber(uiState.lowStockVegetables.size, bn)
                  StatusBadge(
                    text = "${lowCount}টি কম আছে",
                    tone = BadgeTone.WARNING,
                    icon = Icons.Filled.Warning,
                  )
                }
              } else {
                null
              },
          )
        }

        if (uiState.activeVegetables.isEmpty()) {
          item {
            Card(
              onClick = onOpenStock,
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(BazarDimens.SpaceLg)) {
                Text("এখনো কোনো সবজি যোগ করা হয়নি", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(BazarDimens.SpaceXs))
                Text(
                  "স্টক ও কেনা ট্যাব থেকে সবজি যোগ করে বা নতুন কেনা দিয়ে শুরু করুন।",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else if (uiState.lowStockVegetables.isEmpty()) {
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
              Row(
                modifier = Modifier.padding(BazarDimens.CardPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.CheckCircle,
                  contentDescription = null,
                  tint = bazarColors.success,
                )
                Text(
                  text = "সব সবজির স্টক পর্যাপ্ত আছে",
                  style = MaterialTheme.typography.bodyMedium,
                )
              }
            }
          }
        } else {
          val visibleLowStock = uiState.lowStockVegetables.take(DASHBOARD_LOW_STOCK_LIMIT)
          items(visibleLowStock, key = { it.id }) { veg ->
            LowStockAlertCard(veg = veg, bn = bn, onClick = onOpenStock)
          }
        }
      }
    }
  }

  inspectingSale?.let { sale ->
    SaleDetailBottomSheet(
      sale = sale,
      items = uiState.saleItemsBySaleId[sale.id].orEmpty(),
      bn = bn,
      onDismiss = { inspectingSale = null },
      onCancelSale = {
        onCancelSale(sale.id)
        inspectingSale = null
      },
    )
  }
}

@Composable
private fun DueStripCard(
  title: String,
  amountText: String,
  countText: String,
  accent: Color,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border =
      BorderStroke(BazarDimens.CardHairlineBorder, MaterialTheme.colorScheme.outlineVariant),
    shape = MaterialTheme.shapes.medium,
    modifier = modifier.semantics(mergeDescendants = true) {},
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = amountText,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = accent,
      )
      Text(
        text = countText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun QuickActionTile(
  label: String,
  icon: ImageVector,
  isPrimary: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (isPrimary) {
    Button(
      onClick = onClick,
      modifier = modifier.height(BazarDimens.PrimaryButtonHeight),
      shape = RoundedCornerShape(14.dp),
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(BazarDimens.IconMd))
      Spacer(modifier = Modifier.width(8.dp))
      Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  } else {
    FilledTonalButton(
      onClick = onClick,
      modifier = modifier.height(BazarDimens.PrimaryButtonHeight),
      shape = RoundedCornerShape(14.dp),
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(BazarDimens.IconMd))
      Spacer(modifier = Modifier.width(8.dp))
      Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

@Composable
private fun LowStockAlertCard(
  veg: VegetableEntity,
  bn: Boolean,
  onClick: () -> Unit,
) {
  val bazarColors = LocalBazarColors.current
  val isDeficit = veg.stockUnits < 0L
  Card(
    onClick = onClick,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border =
      BorderStroke(
        1.dp,
        if (isDeficit) MaterialTheme.colorScheme.error else bazarColors.warning,
      ),
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(veg.name, style = MaterialTheme.typography.titleSmall)
          val stockText = BusinessCalculator.formatQuantity(veg.stockUnits, veg.unitType, bn)
          Text(
            text = "বর্তমান স্টক: $stockText",
            style = MaterialTheme.typography.bodySmall,
            color = if (isDeficit) MaterialTheme.colorScheme.error else bazarColors.warning,
          )
        }
        StatusBadge(
          text = if (isDeficit) "ঘাটতি মেলান" else "স্টক কম",
          tone = if (isDeficit) BadgeTone.ERROR else BadgeTone.WARNING,
          icon = Icons.Filled.Warning,
        )
      }
      StockHealthBar(currentUnits = veg.stockUnits, thresholdUnits = veg.lowStockThresholdUnits)
    }
  }
}
