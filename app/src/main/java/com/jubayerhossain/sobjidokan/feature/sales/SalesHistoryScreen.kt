@file:Suppress("ktlint:standard:function-naming")

package com.jubayerhossain.sobjidokan.feature.sales

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseItemEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleItemEntity
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator
import com.jubayerhossain.sobjidokan.ui.ReportPeriod
import com.jubayerhossain.sobjidokan.ui.ShopUiState
import com.jubayerhossain.sobjidokan.ui.components.BadgeTone
import com.jubayerhossain.sobjidokan.ui.components.ConfirmationDialog
import com.jubayerhossain.sobjidokan.ui.components.EmptyStateView
import com.jubayerhossain.sobjidokan.ui.components.PnlKeyValueRow
import com.jubayerhossain.sobjidokan.ui.components.SearchFieldBar
import com.jubayerhossain.sobjidokan.ui.components.StatusBadge
import com.jubayerhossain.sobjidokan.ui.theme.BazarDimens

private const val WEEK_LOOKBACK_DAYS = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun salesHistoryScreen(
  uiState: ShopUiState,
  onBack: () -> Unit,
  onCancelSale: (Long) -> Unit,
) {
  val bn = uiState.preferences.useBengaliDigits
  var selectedPeriod by remember { mutableStateOf(ReportPeriod.TODAY) }
  var statusFilter by remember { mutableStateOf(SalesStatusFilter.ALL) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedSale by remember { mutableStateOf<SaleEntity?>(null) }

  val cutoff =
    when (selectedPeriod) {
      ReportPeriod.TODAY -> BusinessCalculator.startOfDay()
      ReportPeriod.WEEK -> BusinessCalculator.startOfDaysAgo(WEEK_LOOKBACK_DAYS)
      ReportPeriod.MONTH -> BusinessCalculator.startOfMonth()
      else -> 0L
    }

  val filteredSales =
    remember(uiState.sales, cutoff, statusFilter, searchQuery, uiState.saleItemsBySaleId) {
      uiState.sales.filter { sale ->
        val matchesDate = sale.timestamp >= cutoff
        val matchesStatus =
          when (statusFilter) {
            SalesStatusFilter.ALL -> {
              true
            }

            SalesStatusFilter.PAID -> {
              !sale.isCancelled && sale.dueAmount <= BusinessCalculator.AMOUNT_EPSILON
            }

            SalesStatusFilter.DUE -> {
              !sale.isCancelled && sale.dueAmount > BusinessCalculator.AMOUNT_EPSILON
            }

            SalesStatusFilter.CANCELLED -> {
              sale.isCancelled
            }
          }
        val items = uiState.saleItemsBySaleId[sale.id].orEmpty()
        val matchesQuery =
          searchQuery.isBlank() ||
            sale.customerName.contains(searchQuery.trim(), ignoreCase = true) ||
            sale.id.toString().contains(searchQuery.trim()) ||
            items.any { it.vegetableName.contains(searchQuery.trim(), ignoreCase = true) }
        matchesDate && matchesStatus && matchesQuery
      }
    }

  val activeFilteredSales = remember(filteredSales) { filteredSales.filter { !it.isCancelled } }
  val filteredNetSales =
    remember(activeFilteredSales) { activeFilteredSales.sumOf { it.totalAmount } }
  val filteredTotalDue =
    remember(activeFilteredSales) { activeFilteredSales.sumOf { it.dueAmount } }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = { Text("বিক্রির ইতিহাস") },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("sales_history_back"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
          }
        },
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(modifier = Modifier.fillMaxSize().widthIn(max = BazarDimens.MaxContentWidthExpanded)) {
        SearchFieldBar(
          query = searchQuery,
          onQueryChange = { searchQuery = it },
          placeholder = "ক্রেতা, বিল নম্বর বা সবজির নাম...",
          modifier = Modifier.padding(horizontal = BazarDimens.ScreenHorizontal),
          testTag = "sales_history_search",
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
          contentPadding = PaddingValues(horizontal = BazarDimens.ScreenHorizontal),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(
            listOf(ReportPeriod.TODAY, ReportPeriod.WEEK, ReportPeriod.MONTH, ReportPeriod.ALL),
          ) { period ->
            FilterChip(
              selected = selectedPeriod == period,
              onClick = { selectedPeriod = period },
              label = { Text(period.labelBn) },
              modifier = Modifier.minimumInteractiveComponentSize(),
            )
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
          contentPadding = PaddingValues(horizontal = BazarDimens.ScreenHorizontal),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(SalesStatusFilter.entries) { filter ->
            FilterChip(
              selected = statusFilter == filter,
              onClick = { statusFilter = filter },
              label = { Text(filter.labelBn) },
              modifier =
                Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("sales_filter_${filter.name}"),
            )
          }
        }

        if (filteredSales.isNotEmpty()) {
          Card(
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
              ),
            shape = MaterialTheme.shapes.medium,
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(horizontal = BazarDimens.ScreenHorizontal, vertical = 8.dp)
                .testTag("sales_summary_banner"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                val billCountText = BusinessCalculator.formatNumber(activeFilteredSales.size, bn)
                Text(
                  text = "মোট ${billCountText}টি সক্রিয় বিল",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = BusinessCalculator.formatTaka(filteredNetSales, bn),
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              if (filteredTotalDue > BusinessCalculator.AMOUNT_EPSILON) {
                StatusBadge(
                  text = "মোট বাকি ${BusinessCalculator.formatTaka(filteredTotalDue, bn)}",
                  tone = BadgeTone.WARNING,
                )
              }
            }
          }
        }

        if (filteredSales.isEmpty()) {
          EmptyStateView(
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            title = "কোনো বিক্রির রেকর্ড নেই",
            tip = "নির্বাচিত সময় বা ফিল্টারে এখনো কোনো বিক্রি পাওয়া যায়নি।",
            actionLabel =
              if (searchQuery.isNotBlank() || statusFilter != SalesStatusFilter.ALL) {
                "ফিল্টার মুছুন"
              } else {
                null
              },
            onAction =
              if (searchQuery.isNotBlank() || statusFilter != SalesStatusFilter.ALL) {
                {
                  searchQuery = ""
                  statusFilter = SalesStatusFilter.ALL
                }
              } else {
                null
              },
            modifier = Modifier.weight(1f),
          )
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(BazarDimens.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(filteredSales, key = { it.id }) { sale ->
              val items = uiState.saleItemsBySaleId[sale.id].orEmpty()
              SaleHistoryCard(
                sale = sale,
                items = items,
                bn = bn,
                onClick = { selectedSale = sale },
              )
            }
          }
        }
      }
    }
  }

  selectedSale?.let { sale ->
    SaleDetailBottomSheet(
      sale = sale,
      items = uiState.saleItemsBySaleId[sale.id].orEmpty(),
      bn = bn,
      onDismiss = { selectedSale = null },
      onCancelSale = {
        onCancelSale(sale.id)
        selectedSale = null
      },
    )
  }
}

@Composable
private fun SaleHistoryCard(
  sale: SaleEntity,
  items: List<SaleItemEntity>,
  bn: Boolean,
  onClick: () -> Unit,
) {
  val summaryText =
    items.joinToString(", ") {
      val qty = BusinessCalculator.formatQuantity(it.quantityUnits, it.unitType, bn)
      "${it.vegetableName} ($qty)"
    }
  val tone =
    when {
      sale.isCancelled -> BadgeTone.ERROR
      sale.dueAmount > BusinessCalculator.AMOUNT_EPSILON -> BadgeTone.WARNING
      else -> BadgeTone.SUCCESS
    }
  val badgeLabel =
    when {
      sale.isCancelled -> {
        "বাতিল"
      }

      sale.dueAmount > BusinessCalculator.AMOUNT_EPSILON -> {
        "বাকি ${BusinessCalculator.formatTaka(sale.dueAmount, bn)}"
      }

      else -> {
        sale.paymentMethod.labelBn
      }
    }

  Card(
    onClick = onClick,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border =
      BorderStroke(BazarDimens.CardHairlineBorder, MaterialTheme.colorScheme.outlineVariant),
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth().testTag("sale_card_${sale.id}"),
  ) {
    Column(modifier = Modifier.padding(BazarDimens.CardPadding)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "#${BusinessCalculator.formatNumber(sale.id, bn)} • ${sale.customerName}",
          style = MaterialTheme.typography.titleSmall,
        )
        StatusBadge(text = badgeLabel, tone = tone)
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = summaryText.ifBlank { "সবজির বিবরণ" },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          text = BusinessCalculator.formatDateTime(sale.timestamp, bn),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = BusinessCalculator.formatTaka(sale.totalAmount, bn),
          style =
            MaterialTheme.typography.titleMedium.copy(
              textDecoration =
                if (sale.isCancelled) TextDecoration.LineThrough else TextDecoration.None,
            ),
          color =
            if (sale.isCancelled) {
              MaterialTheme.colorScheme.error
            } else {
              MaterialTheme.colorScheme.primary
            },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailBottomSheet(
  sale: SaleEntity,
  items: List<SaleItemEntity>,
  bn: Boolean,
  onDismiss: () -> Unit,
  onCancelSale: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var showConfirm by remember { mutableStateOf(false) }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = BazarDimens.SheetHorizontal, vertical = 10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "বিল #${BusinessCalculator.formatNumber(sale.id, bn)}",
          style = MaterialTheme.typography.headlineSmall,
        )
        if (sale.isCancelled) StatusBadge(text = "বাতিলকৃত", tone = BadgeTone.ERROR)
      }
      Text(
        text = "${sale.customerName} • ${BusinessCalculator.formatDateTime(sale.timestamp, bn)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (sale.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "নোট: ${sale.notes}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

      items.forEach { item ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(item.vegetableName, style = MaterialTheme.typography.titleSmall)
            Text(
              text =
                "${BusinessCalculator.formatQuantity(item.quantityUnits, item.unitType, bn)} × " +
                  BusinessCalculator.formatTaka(item.sellingPricePerUnit, bn),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = BusinessCalculator.formatTaka(item.lineTotal, bn),
            style = MaterialTheme.typography.titleSmall,
          )
        }
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
      if (sale.discount > BusinessCalculator.AMOUNT_EPSILON) {
        PnlKeyValueRow(
          label = "ছাড়",
          value = "-${BusinessCalculator.formatTaka(sale.discount, bn)}",
        )
        Spacer(modifier = Modifier.height(4.dp))
      }
      PnlKeyValueRow(
        label = "চূড়ান্ত বিল",
        value = BusinessCalculator.formatTaka(sale.totalAmount, bn),
        bold = true,
        valueColor = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(4.dp))
      PnlKeyValueRow(
        label = "আদায় (${sale.paymentMethod.labelBn})",
        value = BusinessCalculator.formatTaka(sale.paidAmount, bn),
      )
      if (sale.dueAmount > BusinessCalculator.AMOUNT_EPSILON) {
        Spacer(modifier = Modifier.height(4.dp))
        PnlKeyValueRow(
          label = "বাকি",
          value = BusinessCalculator.formatTaka(sale.dueAmount, bn),
          bold = true,
          labelColor = MaterialTheme.colorScheme.error,
          valueColor = MaterialTheme.colorScheme.error,
        )
      }

      if (!sale.isCancelled) {
        Spacer(modifier = Modifier.height(18.dp))
        Button(
          onClick = { showConfirm = true },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("cancel_sale_button"),
        ) {
          Icon(Icons.Filled.Cancel, contentDescription = null)
          Spacer(modifier = Modifier.padding(4.dp))
          Text("বিক্রি বাতিল / ফেরত নিন")
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }

  if (showConfirm) {
    ConfirmationDialog(
      title = "বিক্রি বাতিল করবেন?",
      message =
        "এই বিক্রিটি বাতিল করলে সবজি আবার স্টকে ফিরে আসবে এবং ক্যাশ/বকেয়ার হিসাব সমন্বয় হবে।",
      confirmLabel = "হ্যাঁ, বাতিল করুন",
      isDestructive = true,
      onConfirm = {
        showConfirm = false
        onCancelSale()
      },
      onDismiss = { showConfirm = false },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailBottomSheet(
  purchase: PurchaseEntity,
  items: List<PurchaseItemEntity>,
  bn: Boolean,
  onDismiss: () -> Unit,
  onCancelPurchase: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var showConfirm by remember { mutableStateOf(false) }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = BazarDimens.SheetHorizontal, vertical = 10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "ক্রয় #${BusinessCalculator.formatNumber(purchase.id, bn)}",
          style = MaterialTheme.typography.headlineSmall,
        )
        if (purchase.isCancelled) StatusBadge(text = "বাতিলকৃত", tone = BadgeTone.ERROR)
      }
      Text(
        text =
          "${purchase.supplierName} • ${BusinessCalculator.formatDateTime(purchase.timestamp, bn)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (purchase.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "নোট: ${purchase.notes}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

      items.forEach { item ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(item.vegetableName, style = MaterialTheme.typography.titleSmall)
            Text(
              text =
                "${BusinessCalculator.formatQuantity(item.quantityUnits, item.unitType, bn)} × " +
                  BusinessCalculator.formatTaka(item.unitCost, bn),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = BusinessCalculator.formatTaka(item.lineTotal, bn),
            style = MaterialTheme.typography.titleSmall,
          )
        }
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
      PnlKeyValueRow(
        label = "পণ্যের মোট দাম",
        value = BusinessCalculator.formatTaka(purchase.itemsTotal, bn),
        bold = true,
      )
      if (purchase.extraCost > BusinessCalculator.AMOUNT_EPSILON) {
        Spacer(modifier = Modifier.height(4.dp))
        PnlKeyValueRow(
          label = "ভ্যান/লেবার খরচ",
          value = BusinessCalculator.formatTaka(purchase.extraCost, bn),
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      PnlKeyValueRow(
        label = "পরিশোধিত",
        value = BusinessCalculator.formatTaka(purchase.paidAmount, bn),
      )
      if (purchase.dueAmount > BusinessCalculator.AMOUNT_EPSILON) {
        Spacer(modifier = Modifier.height(4.dp))
        PnlKeyValueRow(
          label = "পাইকার বাকি",
          value = BusinessCalculator.formatTaka(purchase.dueAmount, bn),
          bold = true,
          labelColor = MaterialTheme.colorScheme.error,
          valueColor = MaterialTheme.colorScheme.error,
        )
      }

      if (!purchase.isCancelled) {
        Spacer(modifier = Modifier.height(18.dp))
        Button(
          onClick = { showConfirm = true },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("cancel_purchase_button"),
        ) {
          Text("ক্রয় বাতিল করুন")
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }

  if (showConfirm) {
    ConfirmationDialog(
      title = "ক্রয় বাতিল করবেন?",
      message = "এই ক্রয়টি বাতিল করলে যোগ হওয়া স্টক ও দেনার হিসাব ফেরত যাবে। আপনি কি নিশ্চিত?",
      confirmLabel = "হ্যাঁ, বাতিল করুন",
      isDestructive = true,
      onConfirm = {
        showConfirm = false
        onCancelPurchase()
      },
      onDismiss = { showConfirm = false },
    )
  }
}
