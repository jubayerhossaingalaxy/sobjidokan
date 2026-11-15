@file:Suppress("ktlint:standard:function-naming")

package com.jubayerhossain.sobjidokan.feature.parties

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jubayerhossain.sobjidokan.data.local.entity.PartyEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyType
import com.jubayerhossain.sobjidokan.data.local.entity.PaymentMethod
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.SaleEntity
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator
import com.jubayerhossain.sobjidokan.feature.sales.PurchaseDetailBottomSheet
import com.jubayerhossain.sobjidokan.feature.sales.SaleDetailBottomSheet
import com.jubayerhossain.sobjidokan.ui.PartyBalanceSummary
import com.jubayerhossain.sobjidokan.ui.ShopUiState
import com.jubayerhossain.sobjidokan.ui.components.BadgeTone
import com.jubayerhossain.sobjidokan.ui.components.EmptyStateView
import com.jubayerhossain.sobjidokan.ui.components.PartyDetailSheet
import com.jubayerhossain.sobjidokan.ui.components.QuickAddPartyDialog
import com.jubayerhossain.sobjidokan.ui.components.SearchFieldBar
import com.jubayerhossain.sobjidokan.ui.components.StatusBadge
import com.jubayerhossain.sobjidokan.ui.theme.BazarDimens
import com.jubayerhossain.sobjidokan.ui.theme.LocalBazarColors

@Composable
fun PartiesLedgerScreen(
  uiState: ShopUiState,
  initialSubTab: Int = 0,
  onAddParty: (String, String, PartyType, String) -> Unit,
  onRecordDuePayment: (PartyEntity, Double, PaymentMethod, String, onSuccess: () -> Unit) -> Unit,
  onCancelSale: (Long) -> Unit = {},
  onCancelPurchase: (Long) -> Unit = {},
  onStartSaleForCustomer: (Long) -> Unit = {},
  onCancelDuePayment: (Long) -> Unit = {},
) {
  var partySubTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
  var showAddPartyDialog by remember { mutableStateOf<PartyType?>(null) }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = {
          showAddPartyDialog = if (partySubTab == 0) PartyType.CUSTOMER else PartyType.SUPPLIER
        },
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text(if (partySubTab == 0) "নতুন ক্রেতা" else "নতুন পাইকার") },
        modifier = Modifier.testTag("fab_add_party"),
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(modifier = Modifier.fillMaxSize().widthIn(max = BazarDimens.MaxContentWidthExpanded)) {
        Text(
          text = "বাকি খাতা",
          style = MaterialTheme.typography.headlineSmall,
          modifier =
            Modifier.padding(horizontal = BazarDimens.ScreenHorizontal, vertical = 12.dp),
        )
        PartiesLedgerContent(
          uiState = uiState,
          partySubTab = partySubTab,
          onSubTabChange = { partySubTab = it },
          onRecordDuePayment = onRecordDuePayment,
          onCancelSale = onCancelSale,
          onCancelPurchase = onCancelPurchase,
          onStartSaleForCustomer = onStartSaleForCustomer,
          onCancelDuePayment = onCancelDuePayment,
          onAddPartyClick = { showAddPartyDialog = it },
          modifier = Modifier.weight(1f),
        )
      }
    }
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
}

@Composable
fun PartiesLedgerContent(
  uiState: ShopUiState,
  partySubTab: Int,
  onSubTabChange: (Int) -> Unit,
  onRecordDuePayment: (PartyEntity, Double, PaymentMethod, String, onSuccess: () -> Unit) -> Unit,
  onCancelSale: (Long) -> Unit = {},
  onCancelPurchase: (Long) -> Unit = {},
  onStartSaleForCustomer: ((Long) -> Unit)? = null,
  onAddPartyClick: ((PartyType) -> Unit)? = null,
  onCancelDuePayment: ((Long) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val bn = uiState.preferences.useBengaliDigits
  val bazarColors = LocalBazarColors.current
  var searchQuery by remember { mutableStateOf("") }
  var dueFilter by remember { mutableStateOf(PartyDueFilter.ALL) }
  var collectingParty by remember { mutableStateOf<PartyBalanceSummary?>(null) }
  var inspectingPartyId by remember { mutableStateOf<Long?>(null) }
  var inspectingSale by remember { mutableStateOf<SaleEntity?>(null) }
  var inspectingPurchase by remember { mutableStateOf<PurchaseEntity?>(null) }

  val totalCustomerDue =
    remember(uiState.customerBalances) { uiState.customerBalances.sumOf { it.balanceDue } }
  val totalSupplierDue =
    remember(uiState.supplierBalances) { uiState.supplierBalances.sumOf { it.balanceDue } }

  val baseList = if (partySubTab == 0) uiState.customerBalances else uiState.supplierBalances
  val filteredList =
    remember(baseList, searchQuery, dueFilter) {
      val q = searchQuery.trim()
      baseList.filter { item ->
        val matchesQuery =
          q.isEmpty() ||
            item.party.name.contains(q, ignoreCase = true) ||
            item.party.phone.contains(q, ignoreCase = true)
        val matchesDue =
          when (dueFilter) {
            PartyDueFilter.ALL -> true
            PartyDueFilter.HAS_DUE -> item.balanceDue > BusinessCalculator.AMOUNT_EPSILON
            PartyDueFilter.CLEARED -> item.balanceDue <= BusinessCalculator.AMOUNT_EPSILON
          }
        matchesQuery && matchesDue
      }
    }

  val inspectingPartySummary =
    remember(inspectingPartyId, uiState.customerBalances, uiState.supplierBalances) {
      inspectingPartyId?.let { id ->
        (uiState.customerBalances + uiState.supplierBalances).firstOrNull { it.party.id == id }
      }
    }

  Column(modifier = modifier.fillMaxSize()) {
    // Customer / Supplier Balance Overview Strip
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = BazarDimens.ScreenHorizontal),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Card(
        onClick = { onSubTabChange(0) },
        colors =
          CardDefaults.cardColors(
            containerColor =
              if (partySubTab == 0) {
                MaterialTheme.colorScheme.primaryContainer
              } else {
                MaterialTheme.colorScheme.surface
              },
          ),
        border =
          BorderStroke(
            if (partySubTab == 0) 1.5.dp else BazarDimens.CardHairlineBorder,
            if (partySubTab == 0) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.outlineVariant
            },
          ),
        shape = MaterialTheme.shapes.medium,
        modifier =
          Modifier
            .weight(1f)
            .semantics(mergeDescendants = true) {}
            .testTag("parties_subtab_customers"),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              Icons.Filled.People,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.primary,
            )
            Text("ক্রেতা পাওনা", style = MaterialTheme.typography.labelMedium)
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = BusinessCalculator.formatTaka(totalCustomerDue, bn),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color =
              if (totalCustomerDue > BusinessCalculator.AMOUNT_EPSILON) {
                bazarColors.warning
              } else {
                bazarColors.success
              },
          )
        }
      }
      Card(
        onClick = { onSubTabChange(1) },
        colors =
          CardDefaults.cardColors(
            containerColor =
              if (partySubTab == 1) {
                MaterialTheme.colorScheme.primaryContainer
              } else {
                MaterialTheme.colorScheme.surface
              },
          ),
        border =
          BorderStroke(
            if (partySubTab == 1) 1.5.dp else BazarDimens.CardHairlineBorder,
            if (partySubTab == 1) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.outlineVariant
            },
          ),
        shape = MaterialTheme.shapes.medium,
        modifier =
          Modifier
            .weight(1f)
            .semantics(mergeDescendants = true) {}
            .testTag("parties_subtab_suppliers"),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              Icons.Filled.Storefront,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.secondary,
            )
            Text("পাইকার দেনা", style = MaterialTheme.typography.labelMedium)
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = BusinessCalculator.formatTaka(totalSupplierDue, bn),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color =
              if (totalSupplierDue > BusinessCalculator.AMOUNT_EPSILON) {
                MaterialTheme.colorScheme.error
              } else {
                bazarColors.success
              },
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SearchFieldBar(
      query = searchQuery,
      onQueryChange = { searchQuery = it },
      placeholder = "নাম বা মোবাইল নম্বর দিয়ে খুঁজুন...",
      modifier = Modifier.padding(horizontal = BazarDimens.ScreenHorizontal),
      testTag = "parties_search_field",
    )
    Spacer(modifier = Modifier.height(6.dp))
    LazyRow(
      contentPadding = PaddingValues(horizontal = BazarDimens.ScreenHorizontal),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(PartyDueFilter.entries) { filter ->
        FilterChip(
          selected = dueFilter == filter,
          onClick = { dueFilter = filter },
          label = { Text(filter.labelBn) },
          modifier =
            Modifier.minimumInteractiveComponentSize().testTag("party_filter_${filter.name}"),
        )
      }
    }
    Spacer(modifier = Modifier.height(4.dp))

    if (filteredList.isEmpty()) {
      EmptyStateView(
        icon = if (partySubTab == 0) Icons.Filled.People else Icons.Filled.Storefront,
        title = if (baseList.isEmpty()) "এখনো কোনো হিসাব নেই" else "খোঁজা অনুযায়ী কোনো হিসাব নেই",
        tip =
          if (baseList.isEmpty()) {
            "বাকি ক্রেতা বা পাইকার যোগ করতে নিচের '+' বাটনে চাপুন।"
          } else {
            "অন্য নাম বা ফিল্টার দিয়ে দেখুন।"
          },
        actionLabel =
          if (baseList.isEmpty() && onAddPartyClick != null) {
            if (partySubTab == 0) "নতুন ক্রেতা যোগ করুন" else "নতুন পাইকার যোগ করুন"
          } else if (searchQuery.isNotBlank() || dueFilter != PartyDueFilter.ALL) {
            "ফিল্টার মুছুন"
          } else {
            null
          },
        onAction = {
          if (baseList.isEmpty() && onAddPartyClick != null) {
            onAddPartyClick(if (partySubTab == 0) PartyType.CUSTOMER else PartyType.SUPPLIER)
          } else {
            searchQuery = ""
            dueFilter = PartyDueFilter.ALL
          }
        },
        modifier = Modifier.weight(1f),
      )
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f).testTag("parties_list"),
        contentPadding =
          PaddingValues(
            start = BazarDimens.ScreenHorizontal,
            end = BazarDimens.ScreenHorizontal,
            top = 6.dp,
            bottom = BazarDimens.FabBottomClearance,
          ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(filteredList, key = { it.party.id }) { item ->
          PartyBalanceCard(
            summary = item,
            bn = bn,
            onCardClick = { inspectingPartyId = item.party.id },
            onPayClick = { collectingParty = item },
          )
        }
      }
    }
  }

  inspectingPartySummary?.let { summary ->
    val isCustomer = summary.party.partyType == PartyType.CUSTOMER
    val partySales =
      remember(uiState.sales, summary.party.id) {
        if (isCustomer) {
          uiState.sales.filter { it.customerId == summary.party.id }
        } else {
          emptyList()
        }
      }
    val partyPurchases =
      remember(uiState.purchases, summary.party.id) {
        if (!isCustomer) {
          uiState.purchases.filter { it.supplierId == summary.party.id }
        } else {
          emptyList()
        }
      }
    val partyPayments =
      remember(uiState.duePayments, summary.party.id) {
        uiState.duePayments.filter { it.partyId == summary.party.id }
      }

    PartyDetailSheet(
      summary = summary,
      sales = partySales,
      purchases = partyPurchases,
      payments = partyPayments,
      bn = bn,
      shopName = uiState.preferences.shopName,
      onDismiss = { inspectingPartyId = null },
      onPayClick = { collectingParty = summary },
      onStartSaleForCustomer =
        if (isCustomer && onStartSaleForCustomer != null) {
          {
            inspectingPartyId = null
            onStartSaleForCustomer(summary.party.id)
          }
        } else {
          null
        },
      onOpenSaleDetail = { inspectingSale = it },
      onOpenPurchaseDetail = { inspectingPurchase = it },
      onCancelDuePayment = onCancelDuePayment,
    )
  }

  collectingParty?.let { summary ->
    DuePaymentSheet(
      summary = summary,
      bn = bn,
      isSaving = uiState.isSaving,
      onDismiss = { collectingParty = null },
      onConfirm = { amt, method, note ->
        onRecordDuePayment(summary.party, amt, method, note) { collectingParty = null }
      },
    )
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

  inspectingPurchase?.let { purchase ->
    PurchaseDetailBottomSheet(
      purchase = purchase,
      items = uiState.purchaseItemsByPurchaseId[purchase.id].orEmpty(),
      bn = bn,
      onDismiss = { inspectingPurchase = null },
      onCancelPurchase = {
        onCancelPurchase(purchase.id)
        inspectingPurchase = null
      },
    )
  }
}

@Composable
fun PartyBalanceCard(
  summary: PartyBalanceSummary,
  bn: Boolean,
  onCardClick: () -> Unit = {},
  onPayClick: () -> Unit,
) {
  val isCustomer = summary.party.partyType == PartyType.CUSTOMER
  Card(
    onClick = onCardClick,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border =
      BorderStroke(BazarDimens.CardHairlineBorder, MaterialTheme.colorScheme.outlineVariant),
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth().testTag("party_card_${summary.party.id}"),
  ) {
    Row(
      modifier = Modifier.padding(BazarDimens.CardPadding),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(
          text = summary.party.name,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (summary.party.phone.isNotBlank()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              Icons.Filled.Phone,
              contentDescription = null,
              modifier = Modifier.size(12.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = summary.party.phone,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        val billedLabel = if (isCustomer) "বিল" else "সরবরাহ"
        val paidLabel = if (isCustomer) "জমা" else "শোধ"
        val billedFormatted = BusinessCalculator.formatTaka(summary.totalBilled, bn)
        val paidFormatted = BusinessCalculator.formatTaka(summary.totalPaid, bn)
        Text(
          text = "$billedLabel $billedFormatted • $paidLabel $paidFormatted",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        val hasDue = summary.balanceDue > BusinessCalculator.AMOUNT_EPSILON
        val duePrefix = if (isCustomer) "পাওনা" else "দেনা"
        StatusBadge(
          text =
            if (!hasDue) {
              "বকেয়া নেই"
            } else {
              "$duePrefix ${BusinessCalculator.formatTaka(summary.balanceDue, bn)}"
            },
          tone = if (!hasDue) BadgeTone.SUCCESS else BadgeTone.WARNING,
        )
      }
      if (summary.balanceDue > BusinessCalculator.AMOUNT_EPSILON) {
        OutlinedButton(
          onClick = onPayClick,
          modifier =
            Modifier
              .minimumInteractiveComponentSize()
              .testTag("party_pay_button_${summary.party.id}"),
        ) {
          Text(if (isCustomer) "টাকা আদায়" else "টাকা দিন")
        }
      } else {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = "বিস্তারিত",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuePaymentSheet(
  summary: PartyBalanceSummary,
  bn: Boolean,
  isSaving: Boolean = false,
  onDismiss: () -> Unit,
  onConfirm: (Double, PaymentMethod, String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var amountText by
    remember(summary.party.id, summary.balanceDue) {
      mutableStateOf(BusinessCalculator.formatEditableNumber(summary.balanceDue))
    }
  var method by
    remember(summary.party.id, summary.balanceDue) { mutableStateOf(PaymentMethod.CASH) }
  var note by remember(summary.party.id, summary.balanceDue) { mutableStateOf("") }
  var errorText by remember(summary.party.id, summary.balanceDue) { mutableStateOf<String?>(null) }

  val isCustomer = summary.party.partyType == PartyType.CUSTOMER
  val parsedAmount = amountText.toDoubleOrNull()
  val remainingDue =
    if (parsedAmount != null && parsedAmount.isFinite() && parsedAmount > 0.0) {
      BusinessCalculator.round2((summary.balanceDue - parsedAmount).coerceAtLeast(0.0))
    } else {
      summary.balanceDue
    }

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
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.AccountBalanceWallet,
          contentDescription = null,
          tint =
            if (isCustomer) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.secondary
            },
        )
        Text(
          text =
            if (isCustomer) {
              "বাকি আদায় • ${summary.party.name}"
            } else {
              "দেনা পরিশোধ • ${summary.party.name}"
            },
          style = MaterialTheme.typography.headlineSmall,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      StatusBadge(
        text =
          if (isCustomer) {
            "ক্রেতা দোকানে টাকা জমা দিচ্ছেন"
          } else {
            "দোকান থেকে পাইকারকে পরিশোধ হচ্ছে"
          },
        tone = if (isCustomer) BadgeTone.INFO else BadgeTone.NEUTRAL,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "মোট বকেয়া: ${BusinessCalculator.formatTaka(summary.balanceDue, bn)}",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.error,
      )
      Spacer(modifier = Modifier.height(10.dp))

      // Quick amount presets
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected =
              parsedAmount != null && kotlin.math.abs(parsedAmount - summary.balanceDue) < 0.01,
            onClick = {
              amountText = BusinessCalculator.formatEditableNumber(summary.balanceDue)
              errorText = null
            },
            label = { Text("পূর্ণ (${BusinessCalculator.formatTaka(summary.balanceDue, bn)})") },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("due_full_amount_chip"),
          )
        }
        if (summary.balanceDue > 2.0) {
          val half = BusinessCalculator.round2(summary.balanceDue / 2.0)
          item {
            FilterChip(
              selected = parsedAmount != null && kotlin.math.abs(parsedAmount - half) < 0.01,
              onClick = {
                amountText = BusinessCalculator.formatEditableNumber(half)
                errorText = null
              },
              label = { Text("অর্ধেক (${BusinessCalculator.formatTaka(half, bn)})") },
              modifier =
                Modifier.minimumInteractiveComponentSize().testTag("due_half_amount_chip"),
            )
          }
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
        supportingText = {
          if (errorText != null) {
            Text(errorText!!)
          } else {
            Text("এর পর বাকি থাকবে: ${BusinessCalculator.formatTaka(remainingDue, bn)}")
          }
        },
        modifier = Modifier.fillMaxWidth().testTag("due_amount_input"),
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(PaymentMethod.CASH, PaymentMethod.MOBILE).forEach { m ->
          FilterChip(
            selected = method == m,
            onClick = { method = m },
            label = { Text(m.labelBn) },
            modifier = Modifier.minimumInteractiveComponentSize(),
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("নোট (ঐচ্ছিক)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("due_note_input"),
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = {
          val amt = amountText.toDoubleOrNull()
          if (amt == null || !amt.isFinite() || amt <= 0.0) {
            errorText = "সঠিক টাকার পরিমাণ লিখুন।"
          } else if (amt > summary.balanceDue + 0.005) {
            errorText = "পরিশোধিত টাকা মোট পাওনা/দেনার চেয়ে বেশি হতে পারে না।"
          } else {
            onConfirm(amt, method, note)
          }
        },
        enabled = !isSaving,
        modifier =
          Modifier
            .fillMaxWidth()
            .height(BazarDimens.PrimaryButtonHeight)
            .testTag("due_confirm_button"),
      ) {
        Text(
          if (isSaving) {
            "সংরক্ষণ হচ্ছে..."
          } else if (isCustomer) {
            "টাকা জমা নিন"
          } else {
            "পরিশোধ সম্পন্ন করুন"
          },
        )
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
