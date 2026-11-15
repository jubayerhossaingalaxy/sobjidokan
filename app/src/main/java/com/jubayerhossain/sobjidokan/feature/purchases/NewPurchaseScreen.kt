
@file:Suppress("ktlint:standard:function-naming")

package com.jubayerhossain.sobjidokan.feature.purchases

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jubayerhossain.sobjidokan.data.local.entity.PartyEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyType
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableEntity
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator
import com.jubayerhossain.sobjidokan.feature.sales.QuantitySelectorBottomSheet
import com.jubayerhossain.sobjidokan.ui.PosCartItem
import com.jubayerhossain.sobjidokan.ui.ShopUiState
import com.jubayerhossain.sobjidokan.ui.components.BadgeTone
import com.jubayerhossain.sobjidokan.ui.components.ConfirmationDialog
import com.jubayerhossain.sobjidokan.ui.components.PartyPickerSheet
import com.jubayerhossain.sobjidokan.ui.components.PnlKeyValueRow
import com.jubayerhossain.sobjidokan.ui.components.QuickAddPartyDialog
import com.jubayerhossain.sobjidokan.ui.components.SearchFieldBar
import com.jubayerhossain.sobjidokan.ui.components.StatusBadge
import com.jubayerhossain.sobjidokan.ui.theme.BazarDimens
import kotlinx.coroutines.launch

private const val VEG_SEARCH_VISIBILITY_MIN_COUNT = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPurchaseScreen(
  uiState: ShopUiState,
  onAddDraftItem: (VegetableEntity, Long, Double) -> Unit,
  onRemoveDraftItem: (Long) -> Unit,
  onClearDraft: () -> Unit,
  onCreateSupplier: suspend (String, String) -> PartyEntity,
  onSavePurchase: (
    supplierId: Long?,
    supplierName: String,
    extraCost: Double,
    paidAmount: Double,
    notes: String,
    timestamp: Long,
    onSuccess: () -> Unit,
  ) -> Unit,
  onBack: () -> Unit,
) {
  val bn = uiState.preferences.useBengaliDigits
  val scope = rememberCoroutineScope()
  var selectingVeg by remember { mutableStateOf<VegetableEntity?>(null) }
  var selectedSupplier by remember { mutableStateOf<PartyEntity?>(null) }
  var showSupplierPicker by remember { mutableStateOf(false) }
  var showAddSupplier by remember { mutableStateOf(false) }
  var vegSearchQuery by remember { mutableStateOf("") }
  var extraCostText by remember { mutableStateOf("") }

  val itemsTotal = BusinessCalculator.round2(uiState.purchaseCart.sumOf { it.lineTotal })
  var paidManuallyEdited by remember { mutableStateOf(false) }
  var paidText by remember {
    mutableStateOf(
      if (itemsTotal <= 0.0) "" else BusinessCalculator.formatEditableNumber(itemsTotal),
    )
  }

  LaunchedEffect(itemsTotal) {
    if (!paidManuallyEdited) {
      paidText = if (itemsTotal <= 0.0) "" else BusinessCalculator.formatEditableNumber(itemsTotal)
    }
  }

  var notes by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }
  var showLeaveConfirm by remember { mutableStateOf(false) }

  val suppliers = remember(uiState.supplierBalances) { uiState.supplierBalances.map { it.party } }
  val supplierDueMap =
    remember(uiState.supplierBalances) {
      uiState.supplierBalances.associate { it.party.id to it.balanceDue }
    }
  val quickSuppliers =
    remember(suppliers, selectedSupplier) {
      val top = suppliers.take(4).toMutableList()
      if (selectedSupplier != null && top.none { it.id == selectedSupplier!!.id }) {
        top.add(0, selectedSupplier!!)
      }
      top.toList()
    }

  val filteredVegs =
    remember(uiState.activeVegetables, vegSearchQuery) {
      val q = vegSearchQuery.trim()
      if (q.isEmpty()) {
        uiState.activeVegetables
      } else {
        uiState.activeVegetables.filter { it.name.contains(q, ignoreCase = true) }
      }
    }
  val draftMap =
    remember(uiState.purchaseCart) { uiState.purchaseCart.associateBy { it.vegetable.id } }

  val parsedExtraCost = if (extraCostText.isBlank()) 0.0 else extraCostText.toDoubleOrNull()
  val extraCost = parsedExtraCost?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
  val grandPurchaseTotal = BusinessCalculator.round2(itemsTotal + extraCost)
  val parsedPaid =
    if (!uiState.preferences.showCreditLedger) {
      itemsTotal
    } else if (paidText.isBlank()) {
      0.0
    } else {
      paidText.toDoubleOrNull()
    }
  val paidAmount = parsedPaid?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
  val dueAmount = BusinessCalculator.round2((itemsTotal - paidAmount).coerceAtLeast(0.0))

  fun handleBackRequest() {
    if (uiState.purchaseCart.isNotEmpty()) showLeaveConfirm = true else onBack()
  }

  BackHandler { handleBackRequest() }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = { Text("নতুন কেনা যোগ করুন") },
        navigationIcon = {
          IconButton(
            onClick = ::handleBackRequest,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("new_purchase_back"),
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
      LazyColumn(
        modifier =
          Modifier
            .fillMaxSize()
            .widthIn(max = BazarDimens.MaxContentWidthExpanded)
            .imePadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(BazarDimens.ScreenHorizontal),
        verticalArrangement = Arrangement.spacedBy(BazarDimens.SpaceMd),
      ) {
        if (uiState.preferences.showCreditLedger) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("পাইকার / আড়তদার (ঐচ্ছিক)", style = MaterialTheme.typography.titleSmall)
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (suppliers.isNotEmpty()) {
                  TextButton(
                    onClick = { showSupplierPicker = true },
                    modifier =
                      Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("purchase_open_supplier_picker"),
                  ) {
                    Icon(
                      Icons.Filled.Search,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("সব পাইকার")
                  }
                }
                TextButton(
                  onClick = { showAddSupplier = true },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(
                    Icons.Filled.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("নতুন পাইকার")
                }
              }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              item {
                FilterChip(
                  selected = selectedSupplier == null,
                  onClick = {
                    selectedSupplier = null
                    errorText = null
                  },
                  label = { Text("নগদ বাজার") },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                )
              }
              items(quickSuppliers, key = { it.id }) { sup ->
                FilterChip(
                  selected = selectedSupplier?.id == sup.id,
                  onClick = {
                    selectedSupplier = sup
                    errorText = null
                  },
                  label = { Text(sup.name) },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                )
              }
            }
          }
        }

        item {
          Text("সবজি বাছাই করে কেনা যোগ করুন", style = MaterialTheme.typography.titleSmall)
          Spacer(modifier = Modifier.height(6.dp))
          if (uiState.activeVegetables.size > VEG_SEARCH_VISIBILITY_MIN_COUNT) {
            SearchFieldBar(
              query = vegSearchQuery,
              onQueryChange = { vegSearchQuery = it },
              placeholder = "সবজি খুঁজুন...",
              testTag = "purchase_veg_search",
            )
            Spacer(modifier = Modifier.height(8.dp))
          }
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredVegs, key = { it.id }) { veg ->
              val isAdded = draftMap.containsKey(veg.id)
              OutlinedButton(
                onClick = { selectingVeg = veg },
                border =
                  if (isAdded) {
                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                  } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                  },
                modifier =
                  Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("purchase_pick_veg_${veg.id}"),
              ) {
                Icon(
                  imageVector = if (isAdded) Icons.Filled.Check else Icons.Filled.Add,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(veg.name)
              }
            }
          }
        }

        if (uiState.purchaseCart.isNotEmpty()) {
          items(uiState.purchaseCart, key = { it.vegetable.id }) { item ->
            val cardTag = "purchase_draft_item_${item.vegetable.id}"
            Card(
              onClick = { selectingVeg = item.vegetable },
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag(cardTag),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(item.vegetable.name, style = MaterialTheme.typography.titleSmall)
                  val qtyText =
                    BusinessCalculator.formatQuantity(
                      item.quantityUnits,
                      item.vegetable.unitType,
                      bn,
                    )
                  val rateText = BusinessCalculator.formatTaka(item.unitCost, bn)
                  Text(
                    text = "$qtyText × $rateText • ট্যাপ করে বদলান",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = BusinessCalculator.formatTaka(item.lineTotal, bn),
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.secondary,
                )
                IconButton(
                  onClick = { onRemoveDraftItem(item.vegetable.id) },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(
                    Icons.Filled.Delete,
                    contentDescription = "বাদ দিন",
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }
        }

        item {
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
          OutlinedTextField(
            value = extraCostText,
            onValueChange = {
              extraCostText = it
              errorText = null
            },
            label = { Text("ভ্যান / লেবার খরচ (৳, ঐচ্ছিক)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("purchase_extra_cost_input"),
          )
          Spacer(modifier = Modifier.height(10.dp))

          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border =
              BorderStroke(
                BazarDimens.CardHairlineBorder,
                MaterialTheme.colorScheme.outlineVariant,
              ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              PnlKeyValueRow(
                label = "পণ্যের মোট বিল",
                value = BusinessCalculator.formatTaka(itemsTotal, bn),
                bold = true,
                valueColor = MaterialTheme.colorScheme.primary,
              )
              if (extraCost > BusinessCalculator.AMOUNT_EPSILON) {
                PnlKeyValueRow(
                  label = "ভ্যান/লেবার খরচ",
                  value = BusinessCalculator.formatTaka(extraCost, bn),
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                PnlKeyValueRow(
                  label = "সর্বমোট ক্রয় ব্যয়",
                  value = BusinessCalculator.formatTaka(grandPurchaseTotal, bn),
                  bold = true,
                )
              }
            }
          }

          if (uiState.preferences.showCreditLedger) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
              value = paidText,
              onValueChange = {
                paidText = it
                paidManuallyEdited = true
                errorText = null
              },
              label = { Text("পাইকারকে নগদ পরিশোধ (৳)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("purchase_paid_input"),
            )
            if (itemsTotal > 0.0) {
              Spacer(modifier = Modifier.height(6.dp))
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                  val isFullPaid =
                    kotlin.math.abs(paidAmount - itemsTotal) < BusinessCalculator.DUE_TOLERANCE
                  FilterChip(
                    selected = isFullPaid,
                    onClick = {
                      paidText = BusinessCalculator.formatEditableNumber(itemsTotal)
                      paidManuallyEdited = false
                      errorText = null
                    },
                    label = { Text("পুরো পরিশোধ") },
                    modifier =
                      Modifier
                        .minimumInteractiveComponentSize()
                        .testTag(
                          "purchase_full_paid_chip",
                        ),
                  )
                }
                item {
                  FilterChip(
                    selected =
                      paidAmount < BusinessCalculator.AMOUNT_EPSILON && paidText.isNotEmpty(),
                    onClick = {
                      paidText = "0"
                      paidManuallyEdited = true
                      errorText = null
                    },
                    label = { Text("পুরো বাকি") },
                    modifier =
                      Modifier
                        .minimumInteractiveComponentSize()
                        .testTag(
                          "purchase_zero_paid_chip",
                        ),
                  )
                }
              }
            }
            if (dueAmount > BusinessCalculator.AMOUNT_EPSILON) {
              Spacer(modifier = Modifier.height(6.dp))
              StatusBadge(
                text =
                  "পাইকারের কাছে বাকি থাকবে: ${BusinessCalculator.formatTaka(dueAmount, bn)}",
                tone = BadgeTone.WARNING,
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("নোট (ঐচ্ছিক)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          if (errorText != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              errorText!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
            )
          }

          Spacer(modifier = Modifier.height(16.dp))
          Button(
            onClick = {
              if (uiState.purchaseCart.isEmpty()) {
                errorText = "অনুগ্রহ করে অন্তত একটি সবজি যোগ করুন।"
              } else if (
                parsedExtraCost == null || !parsedExtraCost.isFinite() || parsedExtraCost < 0.0
              ) {
                errorText = "সঠিক ভ্যান বা লেবার খরচ লিখুন।"
              } else if (parsedPaid == null || !parsedPaid.isFinite() || parsedPaid < 0.0) {
                errorText = "সঠিক পরিশোধিত টাকার পরিমাণ লিখুন।"
              } else if (parsedPaid > itemsTotal + 0.005) {
                errorText = "পরিশোধিত টাকা মোট বিলের চেয়ে বেশি হতে পারে না।"
              } else if (dueAmount > 0.005 && selectedSupplier == null) {
                errorText = "বাকি রাখার জন্য অনুগ্রহ করে পাইকার নির্বাচন করুন।"
              } else {
                onSavePurchase(
                  selectedSupplier?.id,
                  selectedSupplier?.name ?: "নগদ বাজার",
                  extraCost,
                  parsedPaid,
                  notes,
                  System.currentTimeMillis(),
                  onBack,
                )
              }
            },
            enabled = !uiState.isSaving && uiState.purchaseCart.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("save_purchase_button"),
          ) {
            Text(if (uiState.isSaving) "সংরক্ষণ হচ্ছে..." else "ক্রয় সংরক্ষণ করুন")
          }
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }

  selectingVeg?.let { veg ->
    val existing = uiState.purchaseCart.firstOrNull { it.vegetable.id == veg.id }
    QuantitySelectorBottomSheet(
      veg = veg,
      existingCartItem =
        existing?.let {
          PosCartItem(it.vegetable, it.quantityUnits, it.unitCost, it.lineTotal)
        },
      bn = bn,
      titlePrefix = "ক্রয়ের পরিমাণ",
      defaultPrice =
        if (veg.avgCostPerUnit > 0.0) veg.avgCostPerUnit else veg.sellingPricePerUnit,
      confirmLabel = "তালিকায় যোগ করুন",
      allowZeroStockCheck = false,
      onDismiss = { selectingVeg = null },
      onRemove =
        if (existing != null) {
          {
            onRemoveDraftItem(veg.id)
            selectingVeg = null
          }
        } else {
          null
        },
      onConfirm = { qtyUnits, cost ->
        onAddDraftItem(veg, qtyUnits, cost)
        selectingVeg = null
      },
    )
  }

  if (showSupplierPicker) {
    PartyPickerSheet(
      title = "পাইকার নির্বাচন করুন",
      defaultWalkInLabel = "নগদ বাজার (কোনো পাইকার নয়)",
      parties = suppliers,
      selectedPartyId = selectedSupplier?.id,
      partyType = PartyType.SUPPLIER,
      bn = bn,
      balanceByPartyId = supplierDueMap,
      onSelectWalkIn = {
        selectedSupplier = null
        errorText = null
      },
      onSelectParty = {
        selectedSupplier = it
        errorText = null
      },
      onAddNewClick = { showAddSupplier = true },
      onDismiss = { showSupplierPicker = false },
    )
  }

  if (showAddSupplier) {
    QuickAddPartyDialog(
      partyType = PartyType.SUPPLIER,
      onDismiss = { showAddSupplier = false },
      onSave = { name, phone ->
        scope.launch {
          runCatching { onCreateSupplier(name, phone) }
            .onSuccess { created ->
              selectedSupplier = created
              showAddSupplier = false
              showSupplierPicker = false
              errorText = null
            }.onFailure { err ->
              showAddSupplier = false
              errorText = err.message ?: "পাইকার যোগ করা যায়নি।"
            }
        }
      },
    )
  }

  if (showLeaveConfirm) {
    ConfirmationDialog(
      title = "ক্রয় বাতিল করে বের হবেন?",
      message = "অসম্পূর্ণ ক্রয়ের তথ্য এখনো সেভ করা হয়নি। বের হয়ে গেলে তালিকা মুছে যাবে।",
      confirmLabel = "বের হয়ে যান",
      isDestructive = true,
      onConfirm = {
        showLeaveConfirm = false
        onClearDraft()
        onBack()
      },
      onDismiss = { showLeaveConfirm = false },
    )
  }
}
