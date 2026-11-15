@file:Suppress("ktlint:standard:function-naming")

package com.jubayerhossain.sobjidokan.feature.inventory

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.jubayerhossain.sobjidokan.data.local.entity.InventoryMovementEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PurchaseEntity
import com.jubayerhossain.sobjidokan.data.local.entity.UnitType
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableCategory
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableEntity
import com.jubayerhossain.sobjidokan.data.local.entity.WastageEntity
import com.jubayerhossain.sobjidokan.data.local.entity.WastageReason
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator
import com.jubayerhossain.sobjidokan.feature.sales.PurchaseDetailBottomSheet
import com.jubayerhossain.sobjidokan.ui.ShopUiState
import com.jubayerhossain.sobjidokan.ui.components.BadgeTone
import com.jubayerhossain.sobjidokan.ui.components.ConfirmationDialog
import com.jubayerhossain.sobjidokan.ui.components.EmptyStateView
import com.jubayerhossain.sobjidokan.ui.components.SearchFieldBar
import com.jubayerhossain.sobjidokan.ui.components.StatusBadge
import com.jubayerhossain.sobjidokan.ui.components.StockHealthBar
import com.jubayerhossain.sobjidokan.ui.theme.BazarDimens
import com.jubayerhossain.sobjidokan.ui.theme.LocalBazarColors
import kotlin.math.abs
import kotlin.math.roundToLong

private const val MAX_RECENT_MOVEMENTS = 50
private const val BULK_PRICE_FIELD_WEIGHT = 0.9f

@Composable
fun StockAndPurchaseScreen(
  uiState: ShopUiState,
  initialTab: Int = 0,
  onOpenNewPurchase: () -> Unit,
  onSaveVegetable: (
    existingId: Long?,
    name: String,
    category: VegetableCategory,
    unitType: UnitType,
    avgCost: Double,
    sellingPrice: Double,
    lowStockThreshold: Long,
    initialStock: Long,
    onResult: (Result<Unit>) -> Unit,
  ) -> Unit,
  onToggleArchive: (VegetableEntity) -> Unit,
  onAdjustStock: (Long, Long, String) -> Unit,
  onRecordWastage: (Long, Long, WastageReason, String) -> Unit,
  onCancelWastage: (Long) -> Unit = {},
  onCancelPurchase: (Long) -> Unit,
  onAdjustStockAsync: ((Long, Long, String, () -> Unit) -> Unit)? = null,
  onRecordWastageAsync: ((Long, Long, WastageReason, String, () -> Unit) -> Unit)? = null,
  onBulkUpdatePrices: ((Map<Long, Double>, () -> Unit) -> Unit)? = null,
) {
  val bn = uiState.preferences.useBengaliDigits
  var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
  var stockFilter by remember { mutableStateOf(StockFilter.ALL) }
  var selectedCategory by remember { mutableStateOf<VegetableCategory?>(null) }
  var searchQuery by remember { mutableStateOf("") }

  var showAddEditVegSheet by remember { mutableStateOf(false) }
  var showBulkPriceSheet by remember { mutableStateOf(false) }
  var editingVeg by remember { mutableStateOf<VegetableEntity?>(null) }
  var adjustingVeg by remember { mutableStateOf<VegetableEntity?>(null) }
  var wastageVeg by remember { mutableStateOf<VegetableEntity?>(null) }
  var showGeneralWastageSheet by remember { mutableStateOf(false) }
  var cancellingWastage by remember { mutableStateOf<WastageEntity?>(null) }
  var archivingConfirmVeg by remember { mutableStateOf<VegetableEntity?>(null) }
  var selectedPurchase by remember { mutableStateOf<PurchaseEntity?>(null) }

  val vegById = remember(uiState.allVegetables) { uiState.allVegetables.associateBy { it.id } }

  val filteredVegetables =
    remember(uiState.allVegetables, stockFilter, selectedCategory, searchQuery) {
      uiState.allVegetables.filter { veg ->
        val matchesFilter =
          when (stockFilter) {
            StockFilter.ALL -> !veg.isArchived
            StockFilter.LOW -> !veg.isArchived && veg.stockUnits <= veg.lowStockThresholdUnits
            StockFilter.DEFICIT -> !veg.isArchived && veg.stockUnits < 0L
            StockFilter.ARCHIVED -> veg.isArchived
          }
        val matchesCategory = selectedCategory == null || veg.category == selectedCategory
        val matchesSearch =
          searchQuery.isBlank() || veg.name.contains(searchQuery.trim(), ignoreCase = true)
        matchesFilter && matchesCategory && matchesSearch
      }
    }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = {
          when (selectedTab) {
            0 -> {
              editingVeg = null
              showAddEditVegSheet = true
            }

            1 -> {
              onOpenNewPurchase()
            }

            else -> {
              showGeneralWastageSheet = true
            }
          }
        },
        icon = {
          Icon(
            when (selectedTab) {
              0 -> Icons.Filled.Add
              1 -> Icons.Filled.AddShoppingCart
              else -> Icons.Filled.DeleteSweep
            },
            contentDescription = null,
          )
        },
        text = {
          Text(
            when (selectedTab) {
              0 -> "নতুন সবজি"
              1 -> "নতুন কেনা"
              else -> "পচা বাদ দিন"
            },
          )
        },
        modifier =
          Modifier.testTag(
            when (selectedTab) {
              0 -> "fab_add_veg"
              1 -> "fab_new_purchase"
              else -> "fab_add_wastage"
            },
          ),
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(modifier = Modifier.fillMaxSize().widthIn(max = BazarDimens.MaxContentWidthExpanded)) {
        Text(
          text = "স্টক ও কেনাকাটা",
          style = MaterialTheme.typography.headlineSmall,
          modifier =
            Modifier.padding(horizontal = BazarDimens.ScreenHorizontal, vertical = 12.dp),
        )

        TabRow(selectedTabIndex = selectedTab) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("বর্তমান স্টক") },
            modifier = Modifier.testTag("tab_stock"),
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("কেনাকাটা") },
            modifier = Modifier.testTag("tab_purchases"),
          )
          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            text = { Text("নষ্ট ও লগ") },
            modifier = Modifier.testTag("tab_wastage_log"),
          )
        }

        if (selectedTab == 0) {
          Column(modifier = Modifier.fillMaxSize()) {
            Card(
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
              shape = MaterialTheme.shapes.medium,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .padding(horizontal = BazarDimens.ScreenHorizontal, vertical = 10.dp),
            ) {
              Column(modifier = Modifier.fillMaxWidth().padding(BazarDimens.CardPadding)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column {
                    Text("মোট স্টক মূল্য", style = MaterialTheme.typography.titleSmall)
                    val activeCount =
                      BusinessCalculator.formatNumber(uiState.activeVegetables.size, bn)
                    Text(
                      text = "${activeCount}টি সক্রিয় সবজি",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    text = BusinessCalculator.formatTaka(uiState.todaySummary.inventoryValue, bn),
                    style =
                      MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                if (onBulkUpdatePrices != null && uiState.activeVegetables.isNotEmpty()) {
                  Spacer(modifier = Modifier.height(8.dp))
                  OutlinedButton(
                    onClick = { showBulkPriceSheet = true },
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("bulk_price_update_button"),
                  ) {
                    Icon(
                      Icons.Filled.EditNote,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সকালের দর একসাথে আপডেট করুন")
                  }
                }
              }
            }

            SearchFieldBar(
              query = searchQuery,
              onQueryChange = { searchQuery = it },
              placeholder = "সবজি খুঁজুন...",
              modifier = Modifier.padding(horizontal = BazarDimens.ScreenHorizontal),
              testTag = "stock_search_field",
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
              contentPadding = PaddingValues(horizontal = BazarDimens.ScreenHorizontal),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              items(StockFilter.entries) { filter ->
                FilterChip(
                  selected = stockFilter == filter,
                  onClick = { stockFilter = filter },
                  label = { Text(filter.labelBn) },
                  modifier =
                    Modifier
                      .minimumInteractiveComponentSize()
                      .testTag("stock_filter_${filter.name}"),
                )
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
              contentPadding = PaddingValues(horizontal = BazarDimens.ScreenHorizontal),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              item {
                FilterChip(
                  selected = selectedCategory == null,
                  onClick = { selectedCategory = null },
                  label = { Text("সব শ্রেণি") },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                )
              }
              items(VegetableCategory.entries) { cat ->
                FilterChip(
                  selected = selectedCategory == cat,
                  onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                  label = { Text(cat.labelBn) },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                )
              }
            }

            if (filteredVegetables.isEmpty()) {
              EmptyStateView(
                icon = Icons.Filled.Inventory2,
                title = "তালিকায় কোনো সবজি নেই",
                tip =
                  if (
                    searchQuery.isNotBlank() ||
                    stockFilter != StockFilter.ALL ||
                    selectedCategory != null
                  ) {
                    "ফিল্টার বদলে দেখুন অথবা নতুন সবজি যোগ করুন।"
                  } else {
                    "নতুন সবজি যোগ করতে নিচের '+ নতুন সবজি' বাটনে চাপুন।"
                  },
                actionLabel =
                  if (
                    searchQuery.isNotBlank() ||
                    stockFilter != StockFilter.ALL ||
                    selectedCategory != null
                  ) {
                    "ফিল্টার মুছুন"
                  } else {
                    null
                  },
                onAction =
                  if (
                    searchQuery.isNotBlank() ||
                    stockFilter != StockFilter.ALL ||
                    selectedCategory != null
                  ) {
                    {
                      searchQuery = ""
                      stockFilter = StockFilter.ALL
                      selectedCategory = null
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
                items(filteredVegetables, key = { it.id }) { veg ->
                  StockVegetableItemCard(
                    veg = veg,
                    bn = bn,
                    onEdit = {
                      editingVeg = veg
                      showAddEditVegSheet = true
                    },
                    onAdjust = { adjustingVeg = veg },
                    onWastage = { wastageVeg = veg },
                    onToggleArchive = {
                      if (!veg.isArchived) archivingConfirmVeg = veg else onToggleArchive(veg)
                    },
                  )
                }
              }
            }
          }
        } else if (selectedTab == 1) {
          if (uiState.purchases.isEmpty()) {
            EmptyStateView(
              icon = Icons.Filled.AddShoppingCart,
              title = "এখনো কোনো কেনাকাটা নেই",
              tip = "সকালের বাজার বা পাইকারি ক্রয় যোগ করতে '+ নতুন কেনা' বাটনে চাপুন।",
              actionLabel = "নতুন কেনা যোগ করুন",
              onAction = onOpenNewPurchase,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding =
                PaddingValues(
                  start = BazarDimens.ScreenHorizontal,
                  end = BazarDimens.ScreenHorizontal,
                  top = 12.dp,
                  bottom = BazarDimens.FabBottomClearance,
                ),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(uiState.purchases, key = { it.id }) { purchase ->
                val items = uiState.purchaseItemsByPurchaseId[purchase.id].orEmpty()
                PurchaseHistoryCard(
                  purchase = purchase,
                  summaryItems = items.joinToString(", ") { it.vegetableName },
                  bn = bn,
                  onClick = { selectedPurchase = purchase },
                )
              }
            }
          }
        } else {
          if (uiState.wastages.isEmpty() && uiState.movements.isEmpty()) {
            EmptyStateView(
              icon = Icons.Filled.DeleteSweep,
              title = "এখনো কোনো অপচয় বা স্টক লগ নেই",
              tip = "পচা সবজি বাদ দিলে বা কেনাবেচা করলে এখানে বিস্তারিত ইতিহাস দেখাবে।",
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding =
                PaddingValues(
                  start = BazarDimens.ScreenHorizontal,
                  end = BazarDimens.ScreenHorizontal,
                  top = 12.dp,
                  bottom = BazarDimens.FabBottomClearance,
                ),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              if (uiState.wastages.isNotEmpty()) {
                item {
                  Text("পচা / নষ্ট অপচয়ের ইতিহাস", style = MaterialTheme.typography.titleMedium)
                }
                items(uiState.wastages, key = { "w_${it.id}" }) { wastage ->
                  WastageHistoryCard(
                    wastage = wastage,
                    bn = bn,
                    onCancelClick = { cancellingWastage = wastage },
                  )
                }
              }
              if (uiState.movements.isNotEmpty()) {
                item {
                  Spacer(modifier = Modifier.height(6.dp))
                  Text("সাম্প্রতিক স্টক পরিবর্তন লগ", style = MaterialTheme.typography.titleMedium)
                }
                items(
                  uiState.movements.take(MAX_RECENT_MOVEMENTS),
                  key = { "m_${it.id}" },
                ) { movement ->
                  val matchedUnit = vegById[movement.vegetableId]?.unitType
                  StockMovementCard(movement = movement, unitType = matchedUnit, bn = bn)
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddEditVegSheet) {
    AddEditVegetableSheet(
      existing = editingVeg,
      isSaving = uiState.isSaving,
      onDismiss = { showAddEditVegSheet = false },
      onSave = { name, cat, unit, cost, sell, low, initStock, setErr ->
        onSaveVegetable(editingVeg?.id, name, cat, unit, cost, sell, low, initStock) { res ->
          res
            .onSuccess { showAddEditVegSheet = false }
            .onFailure { err -> setErr(err.message ?: "সংরক্ষণ ব্যর্থ হয়েছে") }
        }
      },
    )
  }

  adjustingVeg?.let { veg ->
    StockAdjustmentSheet(
      veg = veg,
      bn = bn,
      isSaving = uiState.isSaving,
      onDismiss = { adjustingVeg = null },
      onConfirm = { newStock, reason ->
        if (onAdjustStockAsync != null) {
          onAdjustStockAsync(veg.id, newStock, reason) { adjustingVeg = null }
        } else {
          onAdjustStock(veg.id, newStock, reason)
          adjustingVeg = null
        }
      },
    )
  }

  wastageVeg?.let { veg ->
    AddWastageSheet(
      vegetables = uiState.activeVegetables,
      preselectedVeg = veg,
      bn = bn,
      isSaving = uiState.isSaving,
      onDismiss = { wastageVeg = null },
      onConfirm = { targetVegId, qty, reason, note ->
        if (onRecordWastageAsync != null) {
          onRecordWastageAsync(targetVegId, qty, reason, note) { wastageVeg = null }
        } else {
          onRecordWastage(targetVegId, qty, reason, note)
          wastageVeg = null
        }
      },
    )
  }

  if (showGeneralWastageSheet) {
    AddWastageSheet(
      vegetables = uiState.activeVegetables,
      preselectedVeg = null,
      bn = bn,
      isSaving = uiState.isSaving,
      onDismiss = { showGeneralWastageSheet = false },
      onConfirm = { targetVegId, qty, reason, note ->
        if (onRecordWastageAsync != null) {
          onRecordWastageAsync(targetVegId, qty, reason, note) { showGeneralWastageSheet = false }
        } else {
          onRecordWastage(targetVegId, qty, reason, note)
          showGeneralWastageSheet = false
        }
      },
    )
  }

  cancellingWastage?.let { w ->
    ConfirmationDialog(
      title = "অপচয় বাতিল করবেন?",
      message =
        "${w.vegetableName}-এর এই অপচয় এন্ট্রিটি বাতিল করলে পরিমাণ পুনরায় স্টকে ফিরে যাবে।",
      confirmLabel = "হ্যাঁ, বাতিল করুন",
      isDestructive = true,
      onConfirm = {
        onCancelWastage(w.id)
        cancellingWastage = null
      },
      onDismiss = { cancellingWastage = null },
    )
  }

  archivingConfirmVeg?.let { v ->
    ConfirmationDialog(
      title = "সবজি আর্কাইভ করবেন?",
      message =
        "\"${v.name}\" আর্কাইভ করলে বিক্রির তালিকায় আর দেখাবে না। " +
          "পরে 'আর্কাইভ' ফিল্টার থেকে ফিরিয়ে আনা যাবে।",
      confirmLabel = "হ্যাঁ, আর্কাইভ করুন",
      isDestructive = false,
      onConfirm = {
        onToggleArchive(v)
        archivingConfirmVeg = null
      },
      onDismiss = { archivingConfirmVeg = null },
    )
  }

  selectedPurchase?.let { p ->
    PurchaseDetailBottomSheet(
      purchase = p,
      items = uiState.purchaseItemsByPurchaseId[p.id].orEmpty(),
      bn = bn,
      onDismiss = { selectedPurchase = null },
      onCancelPurchase = {
        onCancelPurchase(p.id)
        selectedPurchase = null
      },
    )
  }

  if (showBulkPriceSheet && onBulkUpdatePrices != null) {
    BulkPriceUpdateSheet(
      vegetables = uiState.activeVegetables,
      bn = bn,
      isSaving = uiState.isSaving,
      onDismiss = { showBulkPriceSheet = false },
      onConfirm = { updates -> onBulkUpdatePrices(updates) { showBulkPriceSheet = false } },
    )
  }
}

@Composable
private fun WastageHistoryCard(
  wastage: WastageEntity,
  bn: Boolean,
  onCancelClick: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border =
      BorderStroke(BazarDimens.CardHairlineBorder, MaterialTheme.colorScheme.outlineVariant),
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth().testTag("wastage_card_${wastage.id}"),
  ) {
    Row(
      modifier = Modifier.padding(BazarDimens.CardPadding),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(wastage.vegetableName, style = MaterialTheme.typography.titleSmall)
          StatusBadge(
            text = if (wastage.isCancelled) "বাতিল" else wastage.reason.labelBn,
            tone = if (wastage.isCancelled) BadgeTone.NEUTRAL else BadgeTone.ERROR,
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text =
            "${BusinessCalculator.formatQuantity(wastage.quantityUnits, wastage.unitType, bn)} • " +
              "ক্ষতি ${BusinessCalculator.formatTaka(wastage.lossAmount, bn)}",
          style = MaterialTheme.typography.bodySmall,
          color =
            if (wastage.isCancelled) {
              MaterialTheme.colorScheme.onSurfaceVariant
            } else {
              MaterialTheme.colorScheme.error
            },
        )
        Text(
          text = BusinessCalculator.formatDateTime(wastage.timestamp, bn),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (!wastage.isCancelled) {
        TextButton(
          onClick = onCancelClick,
          modifier =
            Modifier.minimumInteractiveComponentSize().testTag("cancel_wastage_${wastage.id}"),
        ) {
          Icon(
            Icons.Filled.Cancel,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
          )
          Text(" বাতিল", color = MaterialTheme.colorScheme.error)
        }
      }
    }
  }
}

@Composable
private fun StockMovementCard(
  movement: InventoryMovementEntity,
  unitType: UnitType?,
  bn: Boolean,
) {
  val sign = if (movement.deltaUnits > 0L) "+" else "-"
  val color =
    if (movement.deltaUnits >= 0L) {
      MaterialTheme.colorScheme.primary
    } else {
      MaterialTheme.colorScheme.error
    }
  val deltaText =
    if (unitType != null) {
      "$sign${BusinessCalculator.formatQuantity(abs(movement.deltaUnits), unitType, bn)}"
    } else {
      "$sign${BusinessCalculator.formatNumber(abs(movement.deltaUnits), bn)}"
    }
  val stockRangeText =
    if (unitType != null) {
      "আগে ${BusinessCalculator.formatQuantity(movement.previousStock, unitType, bn)} → " +
        "এখন ${BusinessCalculator.formatQuantity(movement.newStock, unitType, bn)}"
    } else {
      null
    }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth().testTag("movement_card_${movement.id}"),
  ) {
    Row(
      modifier = Modifier.padding(12.dp).fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(
          "${movement.vegetableName} • ${movement.movementType.labelBn}",
          style = MaterialTheme.typography.titleSmall,
        )
        if (stockRangeText != null) {
          Text(
            text = stockRangeText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (movement.reason.isNotBlank()) {
          Text(
            text = movement.reason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = BusinessCalculator.formatDateTime(movement.timestamp, bn),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = deltaText,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = color,
      )
    }
  }
}

@Composable
private fun StockVegetableItemCard(
  veg: VegetableEntity,
  bn: Boolean,
  onEdit: () -> Unit,
  onAdjust: () -> Unit,
  onWastage: () -> Unit,
  onToggleArchive: () -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }
  val bazarColors = LocalBazarColors.current
  val isDeficit = veg.stockUnits < 0L
  val isLow = !veg.isArchived && veg.stockUnits <= veg.lowStockThresholdUnits

  Card(
    onClick = onEdit,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border =
      BorderStroke(BazarDimens.CardHairlineBorder, MaterialTheme.colorScheme.outlineVariant),
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth().testTag("stock_veg_card_${veg.id}"),
  ) {
    Column(modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(veg.name, style = MaterialTheme.typography.titleMedium)
            if (veg.isArchived) {
              StatusBadge(text = "আর্কাইভ", tone = BadgeTone.NEUTRAL)
            } else if (isDeficit) {
              StatusBadge(text = "ঘাটতি", tone = BadgeTone.ERROR, icon = Icons.Filled.Warning)
            } else if (isLow) {
              StatusBadge(text = "কম", tone = BadgeTone.WARNING, icon = Icons.Filled.Warning)
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "মজুত: ${BusinessCalculator.formatQuantity(veg.stockUnits, veg.unitType, bn)}",
            style = MaterialTheme.typography.titleSmall,
            color =
              when {
                isDeficit -> MaterialTheme.colorScheme.error
                isLow -> bazarColors.warning
                else -> MaterialTheme.colorScheme.primary
              },
          )
          val avgCostStr = BusinessCalculator.formatTaka(veg.avgCostPerUnit, bn)
          val sellPriceStr = BusinessCalculator.formatTaka(veg.sellingPricePerUnit, bn)
          Text(
            text = "গড় কেনা $avgCostStr • বেচা $sellPriceStr/${veg.unitType.labelBn}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Box {
          IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("stock_menu_${veg.id}"),
          ) {
            Icon(Icons.Filled.MoreVert, contentDescription = "${veg.name} বিকল্প")
          }
          DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
              text = { Text("তথ্য ও দর বদলান") },
              onClick = {
                menuExpanded = false
                onEdit()
              },
            )
            DropdownMenuItem(
              text = { Text("বাস্তব স্টক মিলান") },
              onClick = {
                menuExpanded = false
                onAdjust()
              },
            )
            DropdownMenuItem(
              text = { Text("পচা / নষ্ট বাদ দিন") },
              onClick = {
                menuExpanded = false
                onWastage()
              },
            )
            DropdownMenuItem(
              text = { Text(if (veg.isArchived) "পুনরায় চালু করুন" else "আর্কাইভ করুন") },
              onClick = {
                menuExpanded = false
                onToggleArchive()
              },
            )
          }
        }
      }
      if (!veg.isArchived) {
        Spacer(modifier = Modifier.height(8.dp))
        StockHealthBar(
          currentUnits = veg.stockUnits,
          thresholdUnits = veg.lowStockThresholdUnits,
          modifier = Modifier.padding(end = 10.dp),
        )
      }
    }
  }
}

@Composable
private fun PurchaseHistoryCard(
  purchase: PurchaseEntity,
  summaryItems: String,
  bn: Boolean,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border =
      BorderStroke(BazarDimens.CardHairlineBorder, MaterialTheme.colorScheme.outlineVariant),
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth().testTag("purchase_card_${purchase.id}"),
  ) {
    Column(modifier = Modifier.padding(BazarDimens.CardPadding)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          text =
            "#${BusinessCalculator.formatNumber(purchase.id, bn)} • ${purchase.supplierName}",
          style = MaterialTheme.typography.titleSmall,
        )
        when {
          purchase.isCancelled -> {
            StatusBadge("বাতিল", BadgeTone.ERROR)
          }

          purchase.dueAmount > BusinessCalculator.AMOUNT_EPSILON -> {
            StatusBadge(
              "বাকি ${BusinessCalculator.formatTaka(purchase.dueAmount, bn)}",
              BadgeTone.WARNING,
            )
          }

          else -> {
            StatusBadge("পরিশোধিত", BadgeTone.SUCCESS)
          }
        }
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = summaryItems.ifBlank { "সবজি ক্রয়" },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          text = BusinessCalculator.formatDateTime(purchase.timestamp, bn),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = BusinessCalculator.formatTaka(purchase.itemsTotal, bn),
          style =
            MaterialTheme.typography.titleMedium.copy(
              textDecoration =
                if (purchase.isCancelled) {
                  TextDecoration.LineThrough
                } else {
                  TextDecoration.None
                },
            ),
          color = MaterialTheme.colorScheme.secondary,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVegetableSheet(
  existing: VegetableEntity?,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onSave: (
    name: String,
    category: VegetableCategory,
    unitType: UnitType,
    avgCost: Double,
    sellingPrice: Double,
    lowStockThreshold: Long,
    initialStock: Long,
    onError: (String) -> Unit,
  ) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var name by remember { mutableStateOf(existing?.name ?: "") }
  var category by remember { mutableStateOf(existing?.category ?: VegetableCategory.FRUIT_VEG) }
  var unitType by remember { mutableStateOf(existing?.unitType ?: UnitType.KG) }
  var costText by remember {
    mutableStateOf(
      existing?.avgCostPerUnit?.let { BusinessCalculator.formatEditableNumber(it) } ?: "",
    )
  }
  var sellText by remember {
    mutableStateOf(
      existing?.sellingPricePerUnit?.let { BusinessCalculator.formatEditableNumber(it) } ?: "",
    )
  }
  var lowThresholdText by remember {
    mutableStateOf(
      if (existing != null) {
        BusinessCalculator.formatEditableNumber(
          existing.lowStockThresholdUnits.toDouble() / existing.unitType.baseFactor.toDouble(),
        )
      } else {
        if (unitType == UnitType.KG) "2" else "3"
      },
    )
  }
  var initStockText by remember { mutableStateOf("") }
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
        text = if (existing == null) "নতুন সবজি যোগ করুন" else "সবজি সম্পাদনা",
        style = MaterialTheme.typography.headlineSmall,
      )
      Spacer(modifier = Modifier.height(12.dp))
      OutlinedTextField(
        value = name,
        onValueChange = {
          name = it
          errorText = null
        },
        label = { Text("সবজির নাম *") },
        placeholder = { Text("যেমন: করলা, কচু, পালং শাক") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("veg_name_input"),
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text("শ্রেণি", style = MaterialTheme.typography.labelLarge)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(VegetableCategory.entries) { cat ->
          FilterChip(
            selected = category == cat,
            onClick = { category = cat },
            label = { Text(cat.labelBn) },
            modifier = Modifier.minimumInteractiveComponentSize(),
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text("পরিমাপের একক", style = MaterialTheme.typography.labelLarge)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        UnitType.entries.forEach { u ->
          FilterChip(
            selected = unitType == u,
            onClick = {
              if (existing == null) {
                unitType = u
                lowThresholdText = if (u == UnitType.KG) "2" else "3"
              }
            },
            label = { Text(u.labelBn) },
            enabled = existing == null || unitType == u,
            modifier = Modifier.minimumInteractiveComponentSize(),
          )
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = sellText,
          onValueChange = {
            sellText = it
            errorText = null
          },
          label = { Text("বিক্রয়দর (৳/${unitType.labelBn}) *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("veg_sell_price_input"),
        )
        OutlinedTextField(
          value = costText,
          onValueChange = {
            costText = it
            errorText = null
          },
          label = { Text("ক্রয়দর (৳)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("veg_cost_price_input"),
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = lowThresholdText,
        onValueChange = {
          lowThresholdText = it
          errorText = null
        },
        label = { Text("কম স্টক সতর্কতা সীমা (${unitType.labelBn})") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("veg_low_threshold_input"),
      )
      if (existing == null) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = initStockText,
          onValueChange = {
            initStockText = it
            errorText = null
          },
          label = { Text("শুরুর স্টক (${unitType.labelBn}, ঐচ্ছিক)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("veg_initial_stock_input"),
        )
      }

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
          val sell = sellText.toDoubleOrNull()
          val parsedCost = if (costText.isBlank()) 0.0 else costText.toDoubleOrNull()
          val parsedInit = if (initStockText.isBlank()) 0.0 else initStockText.toDoubleOrNull()
          val parsedLow =
            if (lowThresholdText.isBlank()) {
              if (unitType == UnitType.KG) 2.0 else 3.0
            } else {
              lowThresholdText.toDoubleOrNull()
            }
          if (name.trim().isEmpty()) {
            errorText = "অনুগ্রহ করে সবজির নাম লিখুন।"
          } else if (sell == null || !sell.isFinite() || sell <= 0.0) {
            errorText = "বিক্রয়দর অবশ্যই শূন্যের বেশি হতে হবে।"
          } else if (parsedCost == null || !parsedCost.isFinite() || parsedCost < 0.0) {
            errorText = "সঠিক ক্রয়দর লিখুন।"
          } else if (parsedLow == null || !parsedLow.isFinite() || parsedLow < 0.0) {
            errorText = "সঠিক কম স্টক সীমা লিখুন।"
          } else if (parsedInit == null || !parsedInit.isFinite() || parsedInit < 0.0) {
            errorText = "সঠিক শুরুর স্টকের পরিমাণ লিখুন।"
          } else {
            val initUnits =
              (parsedInit.coerceAtMost(1_000_000_000.0) * unitType.baseFactor)
                .roundToLong()
                .coerceAtLeast(0L)
            val lowUnits =
              (parsedLow.coerceAtMost(1_000_000_000.0) * unitType.baseFactor)
                .roundToLong()
                .coerceAtLeast(0L)
            onSave(name, category, unitType, parsedCost, sell, lowUnits, initUnits) { err ->
              errorText = err
            }
          }
        },
        enabled = !isSaving,
        modifier =
          Modifier.fillMaxWidth().height(BazarDimens.PrimaryButtonHeight).run {
            testTag("save_veg_button")
          },
      ) {
        Text("সংরক্ষণ করুন")
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentSheet(
  veg: VegetableEntity,
  bn: Boolean,
  isSaving: Boolean = false,
  onDismiss: () -> Unit,
  onConfirm: (actualStockUnits: Long, reason: String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val bazarColors = LocalBazarColors.current
  var kgOrCount by remember {
    mutableStateOf(
      if (veg.unitType == UnitType.KG) {
        (veg.stockUnits.coerceAtLeast(0L) / 1000L).toString()
      } else {
        veg.stockUnits.coerceAtLeast(0L).toString()
      },
    )
  }
  var grams by remember {
    mutableStateOf(
      if (veg.unitType == UnitType.KG) {
        (veg.stockUnits.coerceAtLeast(0L) % 1000L).let { if (it == 0L) "" else it.toString() }
      } else {
        ""
      },
    )
  }
  var reason by remember { mutableStateOf("দোকানের বাস্তব ওজন মিলানো") }

  val targetUnits =
    if (veg.unitType == UnitType.KG) {
      ((kgOrCount.toLongOrNull() ?: 0L) * 1000L) + (grams.toLongOrNull() ?: 0L)
    } else {
      (kgOrCount.toLongOrNull() ?: 0L)
    }
  val delta = targetUnits - veg.stockUnits

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
      Text("বাস্তব স্টক মিলান • ${veg.name}", style = MaterialTheme.typography.headlineSmall)
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text =
          "অ্যাপে আছে: ${BusinessCalculator.formatQuantity(veg.stockUnits, veg.unitType, bn)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(12.dp))
      if (veg.unitType == UnitType.KG) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = kgOrCount,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) kgOrCount = it },
            label = { Text("বাস্তব কেজি") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = grams,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) grams = it },
            label = { Text("গ্রাম") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
      } else {
        OutlinedTextField(
          value = kgOrCount,
          onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) kgOrCount = it },
          label = { Text("বাস্তব পরিমাণ (${veg.unitType.labelBn})") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      StatusBadge(
        text =
          when {
            delta > 0L -> {
              "পার্থক্য: +${BusinessCalculator.formatQuantity(delta, veg.unitType, bn)} বাড়বে"
            }

            delta < 0L -> {
              "পার্থক্য: -${BusinessCalculator.formatQuantity(abs(delta), veg.unitType, bn)} কমবে"
            }

            else -> {
              "কোনো পরিবর্তন নেই"
            }
          },
        tone =
          when {
            delta > 0L -> BadgeTone.SUCCESS
            delta < 0L -> BadgeTone.WARNING
            else -> BadgeTone.NEUTRAL
          },
      )
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = reason,
        onValueChange = { reason = it },
        label = { Text("কারণ") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = { onConfirm(targetUnits, reason) },
        enabled = !isSaving,
        modifier =
          Modifier
            .fillMaxWidth()
            .height(BazarDimens.PrimaryButtonHeight)
            .testTag("confirm_adjust_stock_button"),
      ) {
        Text(if (isSaving) "আপডেট হচ্ছে..." else "স্টক আপডেট করুন")
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWastageSheet(
  vegetables: List<VegetableEntity>,
  preselectedVeg: VegetableEntity?,
  bn: Boolean,
  isSaving: Boolean = false,
  onDismiss: () -> Unit,
  onConfirm: (vegetableId: Long, quantityUnits: Long, reason: WastageReason, notes: String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedVeg by remember { mutableStateOf(preselectedVeg ?: vegetables.firstOrNull()) }
  var kgOrCount by remember { mutableStateOf("") }
  var grams by remember { mutableStateOf(if (selectedVeg?.unitType == UnitType.KG) "500" else "") }
  var reason by remember { mutableStateOf(WastageReason.ROTTEN) }
  var notes by remember { mutableStateOf("") }

  LaunchedEffect(selectedVeg?.id) {
    val v = selectedVeg
    if (v?.unitType == UnitType.KG) {
      kgOrCount = ""
      grams = "500"
    } else {
      kgOrCount = "1"
      grams = ""
    }
  }

  val veg = selectedVeg
  val qtyUnits =
    if (veg == null) {
      0L
    } else if (veg.unitType == UnitType.KG) {
      ((kgOrCount.toLongOrNull() ?: 0L) * 1000L) + (grams.toLongOrNull() ?: 0L)
    } else {
      (kgOrCount.toLongOrNull() ?: 0L)
    }
  val lossPreview =
    if (veg == null) {
      0.0
    } else {
      BusinessCalculator.calculateWastageLoss(qtyUnits, veg.avgCostPerUnit, veg.unitType)
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
      Text("পচা / নষ্ট সবজি বাদ দিন", style = MaterialTheme.typography.headlineSmall)
      Spacer(modifier = Modifier.height(10.dp))

      if (veg == null) {
        Text("প্রথমে অন্তত একটি সবজি যোগ করুন।", color = MaterialTheme.colorScheme.error)
      } else {
        if (preselectedVeg == null) {
          Text("সবজি বাছাই করুন", style = MaterialTheme.typography.labelLarge)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vegetables, key = { it.id }) { item ->
              FilterChip(
                selected = item.id == veg.id,
                onClick = { selectedVeg = item },
                label = { Text(item.name) },
                modifier = Modifier.minimumInteractiveComponentSize(),
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
        } else {
          Text("সবজি: ${veg.name}", style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
          text =
            "বর্তমান স্টক: ${BusinessCalculator.formatQuantity(veg.stockUnits, veg.unitType, bn)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (veg.unitType == UnitType.KG) {
          val presets =
            listOf("২৫০ গ্রাম" to 250L, "৫০০ গ্রাম" to 500L, "১ কেজি" to 1000L, "২ কেজি" to 2000L)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { (label, g) ->
              FilterChip(
                selected = qtyUnits == g,
                onClick = {
                  kgOrCount = (g / 1000L).let { if (it == 0L) "" else it.toString() }
                  grams = (g % 1000L).let { if (it == 0L) "" else it.toString() }
                },
                label = { Text(label) },
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("wastage_preset_$g"),
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = kgOrCount,
              onValueChange = {
                if (it.length <= 6 && it.all { c -> c.isDigit() }) kgOrCount = it
              },
              label = { Text("কেজি") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = grams,
              onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) grams = it },
              label = { Text("গ্রাম") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
        } else {
          OutlinedTextField(
            value = kgOrCount,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) kgOrCount = it },
            label = { Text("নষ্ট পরিমাণ (${veg.unitType.labelBn})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }

        if (qtyUnits > 0L) {
          Spacer(modifier = Modifier.height(6.dp))
          val remainingText =
            BusinessCalculator.formatQuantity(veg.stockUnits - qtyUnits, veg.unitType, bn)
          Text(
            text = "বাদ দেওয়ার পর থাকবে: $remainingText",
            style = MaterialTheme.typography.bodySmall,
            color =
              if (veg.stockUnits - qtyUnits < 0L) {
                MaterialTheme.colorScheme.error
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
          )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("কারণ", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(WastageReason.entries) { r ->
            FilterChip(
              selected = reason == r,
              onClick = { reason = r },
              label = { Text(r.labelBn) },
              modifier = Modifier.minimumInteractiveComponentSize(),
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("নোট (ঐচ্ছিক)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "আনুমানিক আর্থিক ক্ষতি: ${BusinessCalculator.formatTaka(lossPreview, bn)}",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
          onClick = { onConfirm(veg.id, qtyUnits, reason, notes) },
          enabled = !isSaving && qtyUnits > 0L,
          modifier =
            Modifier
              .fillMaxWidth()
              .height(BazarDimens.PrimaryButtonHeight)
              .testTag("confirm_wastage_button"),
        ) {
          Text(if (isSaving) "সংরক্ষণ হচ্ছে..." else "অপচয় বাদ দিন")
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkPriceUpdateSheet(
  vegetables: List<VegetableEntity>,
  bn: Boolean,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (Map<Long, Double>) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val priceTexts =
    remember(vegetables) {
      mutableStateMapOf<Long, String>().apply {
        vegetables.forEach { veg ->
          put(veg.id, BusinessCalculator.formatEditableNumber(veg.sellingPricePerUnit))
        }
      }
    }
  var errorText by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .imePadding()
          .navigationBarsPadding()
          .padding(horizontal = BazarDimens.SheetHorizontal, vertical = BazarDimens.SpaceSm),
    ) {
      Text("সকালের দর আপডেট", style = MaterialTheme.typography.headlineSmall)
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "প্রতিটি সবজির আজকের বিক্রয়দর একসাথে পরিবর্তন করে সেভ করুন",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(10.dp))

      LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(vegetables, key = { it.id }) { veg ->
          val currentText = priceTexts[veg.id] ?: ""
          val parsed = currentText.toDoubleOrNull()
          val isInvalid = parsed == null || !parsed.isFinite() || parsed <= 0.0
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(veg.name, style = MaterialTheme.typography.titleSmall)
              val avgCostStr = BusinessCalculator.formatTaka(veg.avgCostPerUnit, bn)
              Text(
                text = "গড় কেনা $avgCostStr/${veg.unitType.labelBn}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            OutlinedTextField(
              value = currentText,
              onValueChange = {
                priceTexts[veg.id] = it
                errorText = null
              },
              label = { Text("৳/${veg.unitType.labelBn}") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              isError = isInvalid,
              singleLine = true,
              modifier =
                Modifier.weight(BULK_PRICE_FIELD_WEIGHT).testTag("bulk_price_input_${veg.id}"),
            )
          }
        }
      }

      if (errorText != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = errorText!!,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
      Button(
        onClick = {
          val updates = linkedMapOf<Long, Double>()
          for (veg in vegetables) {
            val parsed = (priceTexts[veg.id] ?: "").toDoubleOrNull()
            if (parsed == null || !parsed.isFinite() || parsed <= 0.0) {
              errorText = "${veg.name}-এর বিক্রয়দর শূন্যের বেশি হতে হবে।"
              return@Button
            }
            updates[veg.id] = parsed
          }
          onConfirm(updates)
        },
        enabled = !isSaving && vegetables.isNotEmpty(),
        modifier =
          Modifier
            .fillMaxWidth()
            .height(BazarDimens.PrimaryButtonHeight)
            .testTag("bulk_price_save_button"),
      ) {
        Text(if (isSaving) "আপডেট হচ্ছে..." else "সব দর সংরক্ষণ করুন")
      }
      Spacer(modifier = Modifier.height(18.dp))
    }
  }
}
