
@file:Suppress("ktlint:standard:function-naming")

package com.jubayerhossain.sobjidokan.feature.sales

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jubayerhossain.sobjidokan.data.local.entity.PartyEntity
import com.jubayerhossain.sobjidokan.data.local.entity.PartyType
import com.jubayerhossain.sobjidokan.data.local.entity.PaymentMethod
import com.jubayerhossain.sobjidokan.data.local.entity.UnitType
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableCategory
import com.jubayerhossain.sobjidokan.data.local.entity.VegetableEntity
import com.jubayerhossain.sobjidokan.domain.calculator.BusinessCalculator
import com.jubayerhossain.sobjidokan.ui.PosCartItem
import com.jubayerhossain.sobjidokan.ui.ShopUiState
import com.jubayerhossain.sobjidokan.ui.components.BadgeTone
import com.jubayerhossain.sobjidokan.ui.components.ConfirmationDialog
import com.jubayerhossain.sobjidokan.ui.components.EmptyStateView
import com.jubayerhossain.sobjidokan.ui.components.PartyPickerSheet
import com.jubayerhossain.sobjidokan.ui.components.QuickAddPartyDialog
import com.jubayerhossain.sobjidokan.ui.components.SearchFieldBar
import com.jubayerhossain.sobjidokan.ui.components.StatusBadge
import com.jubayerhossain.sobjidokan.ui.theme.BazarDimens
import com.jubayerhossain.sobjidokan.ui.theme.LocalBazarColors
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPosScreen(
  uiState: ShopUiState,
  onOpenHistory: () -> Unit,
  onOpenStock: () -> Unit,
  onAddToCart: (VegetableEntity, Long, Double) -> Unit,
  onRemoveFromCart: (Long) -> Unit,
  onClearCart: () -> Unit,
  onCompleteSale: (
    customerId: Long?,
    customerName: String,
    discount: Double,
    paidAmount: Double,
    paymentMethod: PaymentMethod,
    notes: String,
    onDone: () -> Unit,
  ) -> Unit,
  onCreateCustomer: suspend (String, String) -> PartyEntity,
  onClearPreselectedCustomer: () -> Unit = {},
) {
  val bn = uiState.preferences.useBengaliDigits
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf<VegetableCategory?>(null) }
  var selectedVegForSheet by remember { mutableStateOf<VegetableEntity?>(null) }
  var showCheckoutSheet by remember { mutableStateOf(false) }
  var showLowStockConfirmForCash by remember { mutableStateOf(false) }

  val filteredVegs =
    remember(uiState.activeVegetables, searchQuery, selectedCategory) {
      uiState.activeVegetables.filter { veg ->
        val matchesCategory = selectedCategory == null || veg.category == selectedCategory
        val matchesSearch =
          searchQuery.isBlank() || veg.name.contains(searchQuery.trim(), ignoreCase = true)
        matchesCategory && matchesSearch
      }
    }

  val preselectedCustomer =
    remember(uiState.preselectedPosCustomerId, uiState.customerBalances) {
      uiState.preselectedPosCustomerId?.let { id ->
        uiState.customerBalances.firstOrNull { it.party.id == id }?.party
      }
    }

  val cartMap = remember(uiState.posCart) { uiState.posCart.associateBy { it.vegetable.id } }
  val cartSubtotal = uiState.posCart.sumOf { it.lineTotal }
  val hasInsufficientStockInCart =
    uiState.posCart.any { it.quantityUnits > it.vegetable.stockUnits }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isExpanded = maxWidth >= 840.dp
    val gridColumns =
      when {
        maxWidth >= 900.dp -> 3
        maxWidth >= 600.dp -> 3
        else -> 2
      }

    Row(modifier = Modifier.fillMaxSize()) {
      Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
        // Header
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = BazarDimens.ScreenHorizontal, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("নতুন বিক্রি", style = MaterialTheme.typography.headlineSmall)
            if (preselectedCustomer != null) {
              Spacer(modifier = Modifier.height(2.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                StatusBadge(
                  text = "ক্রেতা: ${preselectedCustomer.name}",
                  tone = BadgeTone.INFO,
                  icon = Icons.Filled.Person,
                )
                TextButton(
                  onClick = onClearPreselectedCustomer,
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Text("পরিবর্তন")
                }
              }
            }
          }
          OutlinedButton(
            onClick = onOpenHistory,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("pos_history_button"),
          ) {
            Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ইতিহাস")
          }
        }

        SearchFieldBar(
          query = searchQuery,
          onQueryChange = { searchQuery = it },
          placeholder = "সবজি খুঁজুন (যেমন: আলু, টমেটো)...",
          modifier = Modifier.padding(horizontal = BazarDimens.ScreenHorizontal),
          testTag = "pos_search_field",
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
          contentPadding = PaddingValues(horizontal = BazarDimens.ScreenHorizontal),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          item {
            FilterChip(
              selected = selectedCategory == null,
              onClick = { selectedCategory = null },
              label = { Text("সব") },
              modifier = Modifier.minimumInteractiveComponentSize(),
            )
          }
          items(VegetableCategory.entries) { category ->
            FilterChip(
              selected = selectedCategory == category,
              onClick = {
                selectedCategory = if (selectedCategory == category) null else category
              },
              label = { Text(category.labelBn) },
              modifier = Modifier.minimumInteractiveComponentSize(),
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (filteredVegs.isEmpty()) {
          EmptyStateView(
            icon = Icons.Filled.Storefront,
            title =
              if (uiState.activeVegetables.isEmpty()) {
                "এখনো কোনো সবজি নেই"
              } else {
                "সবজি খুঁজে পাওয়া যায়নি"
              },
            tip =
              if (uiState.activeVegetables.isEmpty()) {
                "স্টক ও কেনা ট্যাব থেকে সবজি যোগ করুন।"
              } else {
                "অন্য নামে খুঁজে দেখুন অথবা ফিল্টার পরিষ্কার করুন।"
              },
            actionLabel =
              if (uiState.activeVegetables.isEmpty()) "স্টকে যান" else "ফিল্টার মুছুন",
            onAction = {
              if (uiState.activeVegetables.isEmpty()) {
                onOpenStock()
              } else {
                searchQuery = ""
                selectedCategory = null
              }
            },
            modifier = Modifier.weight(1f),
          )
        } else {
          LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            modifier = Modifier.weight(1f),
            contentPadding =
              PaddingValues(
                start = BazarDimens.ScreenHorizontal,
                end = BazarDimens.ScreenHorizontal,
                top = 4.dp,
                bottom = if (uiState.posCart.isNotEmpty() && !isExpanded) 96.dp else 16.dp,
              ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(filteredVegs, key = { it.id }) { veg ->
              PosVegetableCard(
                veg = veg,
                cartItem = cartMap[veg.id],
                bn = bn,
                onClick = { selectedVegForSheet = veg },
              )
            }
          }
        }

        // Compact / Medium Sticky Bottom Cart Bar
        if (!isExpanded) {
          AnimatedVisibility(
            visible = uiState.posCart.isNotEmpty(),
            enter = slideInVertically(animationSpec = spring()) { it },
            exit = slideOutVertically { it },
          ) {
            Surface(
              color = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
              tonalElevation = 4.dp,
              modifier = Modifier.fillMaxWidth().testTag("pos_sticky_cart_bar"),
            ) {
              Row(
                modifier =
                  Modifier.padding(horizontal = BazarDimens.ScreenHorizontal, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                  Text(
                    text =
                      "${BusinessCalculator.formatNumber(uiState.posCart.size, bn)}টি পণ্য • " +
                        BusinessCalculator.formatTaka(cartSubtotal, bn),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                  )
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    if (hasInsufficientStockInCart) {
                      Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                      )
                    }
                    Text(
                      text =
                        if (hasInsufficientStockInCart) {
                          "স্টক সতর্কতা আছে"
                        } else {
                          preselectedCustomer?.name?.let { "ক্রেতা: $it" }
                            ?: "নগদ বা বাকি বিল দেখুন"
                        },
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  OutlinedButton(
                    onClick = { showCheckoutSheet = true },
                    colors =
                      ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                      ),
                    border =
                      BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                      ),
                    modifier =
                      Modifier
                        .minimumInteractiveComponentSize()
                        .testTag(
                          "pos_cart_details_button",
                        ),
                  ) {
                    Text("বিস্তারিত")
                  }
                  Button(
                    onClick = {
                      if (hasInsufficientStockInCart) {
                        showLowStockConfirmForCash = true
                      } else {
                        onCompleteSale(
                          preselectedCustomer?.id,
                          preselectedCustomer?.name ?: "সাধারণ ক্রেতা",
                          0.0,
                          cartSubtotal,
                          PaymentMethod.CASH,
                          "",
                        ) {}
                      }
                    },
                    enabled = !uiState.isSaving,
                    colors =
                      ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary,
                      ),
                    modifier =
                      Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pos_quick_cash_save_button"),
                  ) {
                    Icon(
                      Icons.Filled.Check,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("নগদ সেভ")
                  }
                }
              }
            }
          }
        }
      }

      // Expanded Tablet Two-Pane Cart Panel
      if (isExpanded) {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
          modifier = Modifier.width(BazarDimens.SideCartPaneWidth).fillMaxHeight(),
        ) {
          Column(modifier = Modifier.fillMaxSize().padding(BazarDimens.ScreenHorizontal)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("ক্রেতার বিল", style = MaterialTheme.typography.titleLarge)
              if (uiState.posCart.isNotEmpty()) {
                TextButton(onClick = onClearCart) {
                  Text("সব মুছুন", color = MaterialTheme.colorScheme.error)
                }
              }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (uiState.posCart.isEmpty()) {
              EmptyStateView(
                icon = Icons.Filled.ShoppingCart,
                title = "বিল খালি আছে",
                tip = "বাম পাশ থেকে সবজি ট্যাপ করে ওজন যোগ করুন।",
                modifier = Modifier.weight(1f),
              )
            } else {
              LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                items(uiState.posCart, key = { it.vegetable.id }) { item ->
                  CartLineRow(
                    item = item,
                    bn = bn,
                    onEdit = { selectedVegForSheet = item.vegetable },
                    onRemove = { onRemoveFromCart(item.vegetable.id) },
                  )
                }
              }
              HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
              Text(
                text = "মোট বিল: ${BusinessCalculator.formatTaka(cartSubtotal, bn)}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.height(10.dp))
              Button(
                onClick = { showCheckoutSheet = true },
                modifier = Modifier.fillMaxWidth().height(BazarDimens.PrimaryButtonHeight),
              ) {
                Text("চেকআউট ও পেমেন্ট")
              }
            }
          }
        }
      }
    }
  }

  // SHT-001: Quantity & Price Sheet
  selectedVegForSheet?.let { veg ->
    val existing = cartMap[veg.id]
    QuantitySelectorBottomSheet(
      veg = veg,
      existingCartItem = existing,
      bn = bn,
      onDismiss = { selectedVegForSheet = null },
      onRemove =
        if (existing != null) {
          {
            onRemoveFromCart(veg.id)
            selectedVegForSheet = null
          }
        } else {
          null
        },
      onConfirm = { qtyUnits, rate ->
        onAddToCart(veg, qtyUnits, rate)
        selectedVegForSheet = null
      },
    )
  }

  // SHT-002: Cart & Checkout Sheet
  if (showCheckoutSheet && uiState.posCart.isNotEmpty()) {
    CartCheckoutBottomSheet(
      uiState = uiState,
      onDismiss = { showCheckoutSheet = false },
      onEditItem = { veg ->
        showCheckoutSheet = false
        selectedVegForSheet = veg
      },
      onRemoveItem = onRemoveFromCart,
      onClearCart = {
        onClearCart()
        showCheckoutSheet = false
      },
      onCreateCustomer = onCreateCustomer,
      onSubmitSale = { custId, custName, discount, paid, method, note ->
        onCompleteSale(custId, custName, discount, paid, method, note) {
          showCheckoutSheet = false
        }
      },
    )
  }

  // VAL-006 Low Stock Confirmation for Quick Cash Save
  if (showLowStockConfirmForCash) {
    ConfirmationDialog(
      title = "স্টক সতর্কতা",
      message =
        "কার্টের এক বা একাধিক সবজির পর্যাপ্ত স্টক অ্যাপে নেই। তবুও কি নগদ বিক্রি সম্পন্ন করবেন?",
      confirmLabel = "হ্যাঁ, বিক্রি করুন",
      onConfirm = {
        showLowStockConfirmForCash = false
        onCompleteSale(
          preselectedCustomer?.id,
          preselectedCustomer?.name ?: "সাধারণ ক্রেতা",
          0.0,
          cartSubtotal,
          PaymentMethod.CASH,
          "",
        ) {}
      },
      onDismiss = { showLowStockConfirmForCash = false },
    )
  }
}

@Composable
private fun PosVegetableCard(
  veg: VegetableEntity,
  cartItem: PosCartItem?,
  bn: Boolean,
  onClick: () -> Unit,
) {
  val bazarColors = LocalBazarColors.current
  val inCart = cartItem != null
  val isOutOrLow = veg.stockUnits <= veg.lowStockThresholdUnits

  Card(
    onClick = onClick,
    shape = MaterialTheme.shapes.medium,
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (inCart) {
            MaterialTheme.colorScheme.primaryContainer
          } else {
            MaterialTheme.colorScheme.surface
          },
      ),
    border =
      BorderStroke(
        width =
          if (inCart) BazarDimens.SelectedCardBorder else BazarDimens.CardHairlineBorder,
        color =
          if (inCart) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.outlineVariant
          },
      ),
    modifier = Modifier.fillMaxWidth().testTag("pos_veg_card_${veg.id}"),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Text(
          text = veg.name,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        if (inCart) {
          StatusBadge(
            text = BusinessCalculator.formatQuantity(cartItem.quantityUnits, veg.unitType, bn),
            tone = BadgeTone.SUCCESS,
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text =
          "${BusinessCalculator.formatTaka(veg.sellingPricePerUnit, bn)} / ${veg.unitType.labelBn}",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(4.dp))
      val stockText = BusinessCalculator.formatQuantity(veg.stockUnits, veg.unitType, bn)
      val stockColor =
        when {
          veg.stockUnits <= 0L -> MaterialTheme.colorScheme.error
          isOutOrLow -> bazarColors.warning
          else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (veg.stockUnits <= 0L) {
          Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = "স্টক শূন্য",
            tint = stockColor,
            modifier = Modifier.size(14.dp),
          )
        }
        Text(
          text = "স্টক: $stockText",
          style = MaterialTheme.typography.bodySmall,
          color = stockColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuantitySelectorBottomSheet(
  veg: VegetableEntity,
  existingCartItem: PosCartItem?,
  bn: Boolean,
  titlePrefix: String = "বিক্রির পরিমাণ",
  defaultPrice: Double = veg.sellingPricePerUnit,
  confirmLabel: String = "কার্টে যোগ করুন",
  allowZeroStockCheck: Boolean = true,
  onDismiss: () -> Unit,
  onRemove: (() -> Unit)? = null,
  onConfirm: (quantityUnits: Long, unitPrice: Double) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var kgText by remember {
    mutableStateOf(
      if (veg.unitType == UnitType.KG) {
        val initialGrams = existingCartItem?.quantityUnits ?: 1000L
        (initialGrams / 1000L).toString()
      } else {
        (existingCartItem?.quantityUnits ?: 1L).toString()
      },
    )
  }
  var gramText by remember {
    mutableStateOf(
      if (veg.unitType == UnitType.KG) {
        val initialGrams = existingCartItem?.quantityUnits ?: 1000L
        (initialGrams % 1000L).let { if (it == 0L) "" else it.toString() }
      } else {
        ""
      },
    )
  }
  var rateText by remember {
    mutableStateOf(
      BusinessCalculator.formatEditableNumber(
        existingCartItem?.sellingPricePerUnit ?: defaultPrice,
      ),
    )
  }

  var isTakaMode by remember { mutableStateOf(false) }
  var takaInputText by remember { mutableStateOf("") }

  val rate = rateText.toDoubleOrNull() ?: 0.0
  val manualQtyUnits =
    if (veg.unitType == UnitType.KG) {
      val k = kgText.toLongOrNull() ?: 0L
      val g = gramText.toLongOrNull() ?: 0L
      (k * 1000L) + g
    } else {
      (kgText.toLongOrNull() ?: 0L)
    }
  val takaAmount = takaInputText.toDoubleOrNull() ?: 0.0
  val calculatedGramsFromTaka = calculateGramsFromTaka(veg.unitType, isTakaMode, takaAmount, rate)

  val qtyUnits =
    if (veg.unitType == UnitType.KG && isTakaMode) calculatedGramsFromTaka else manualQtyUnits

  val lineTotal = BusinessCalculator.calculateLineTotal(qtyUnits, veg.unitType, rate)
  val isLowStock = allowZeroStockCheck && qtyUnits > veg.stockUnits
  val isQuantityInvalid = qtyUnits <= 0L
  val isRateInvalid = !rate.isFinite() || rate <= 0.0

  fun syncWeightFields(grams: Long) {
    val safe = grams.coerceAtLeast(0L)
    kgText = (safe / BusinessCalculator.GRAMS_PER_KG).toString()
    gramText = (safe % BusinessCalculator.GRAMS_PER_KG).let { if (it == 0L) "" else it.toString() }
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("$titlePrefix • ${veg.name}", style = MaterialTheme.typography.headlineSmall)
          Spacer(modifier = Modifier.height(4.dp))
          val currentStockText = BusinessCalculator.formatQuantity(veg.stockUnits, veg.unitType, bn)
          Text(
            text = "বর্তমান স্টক: $currentStockText",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (existingCartItem != null && onRemove != null) {
          TextButton(
            onClick = onRemove,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("qty_remove_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Delete,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("কার্ট থেকে সরান", color = MaterialTheme.colorScheme.error)
          }
        }
      }
      if (existingCartItem != null) {
        Spacer(modifier = Modifier.height(6.dp))
        val inCartQty =
          BusinessCalculator.formatQuantity(existingCartItem.quantityUnits, veg.unitType, bn)
        StatusBadge(
          text = "কার্টে আগে থেকেই আছে: $inCartQty",
          tone = BadgeTone.INFO,
          modifier = Modifier.testTag("pos_in_cart_badge"),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))

      if (veg.unitType == UnitType.KG) {
        if (allowZeroStockCheck) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = !isTakaMode,
              onClick = {
                if (isTakaMode && calculatedGramsFromTaka > 0L) {
                  syncWeightFields(calculatedGramsFromTaka)
                }
                isTakaMode = false
              },
              label = { Text("ওজন দিয়ে") },
              modifier =
                Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("pos_mode_weight_chip"),
            )
            FilterChip(
              selected = isTakaMode,
              onClick = {
                isTakaMode = true
                if (takaInputText.isBlank() && lineTotal > 0.0) {
                  takaInputText = BusinessCalculator.formatEditableNumber(lineTotal)
                }
              },
              label = { Text("টাকা দিয়ে") },
              modifier =
                Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("pos_mode_taka_chip"),
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        if (isTakaMode && allowZeroStockCheck) {
          Text("কত টাকার নিতে চান?", style = MaterialTheme.typography.labelLarge)
          Spacer(modifier = Modifier.height(8.dp))
          val takaPresets = listOf(10, 20, 30, 50, 100)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(takaPresets) { preset ->
              FilterChip(
                selected = takaInputText == preset.toString(),
                onClick = { takaInputText = preset.toString() },
                label = { Text("৳ ${BusinessCalculator.formatNumber(preset, bn)}") },
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("taka_chip_$preset"),
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = takaInputText,
            onValueChange = { takaInputText = it },
            label = { Text("টাকার পরিমাণ (৳)") },
            placeholder = { Text("যেমন: 30") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = isQuantityInvalid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("pos_taka_input"),
          )
          if (calculatedGramsFromTaka > 0L) {
            Spacer(modifier = Modifier.height(8.dp))
            val calculatedWeightText =
              BusinessCalculator.formatQuantity(calculatedGramsFromTaka, UnitType.KG, bn)
            StatusBadge(
              text = "হিসাবকৃত ওজন: $calculatedWeightText",
              tone = BadgeTone.SUCCESS,
              modifier = Modifier.testTag("pos_taka_weight_preview"),
            )
          }
        } else {
          Text("দ্রুত ওজন বাছাই", style = MaterialTheme.typography.labelLarge)
          Spacer(modifier = Modifier.height(8.dp))
          val presets =
            listOf(
              "১০০ গ্রাম" to 100L,
              "২৫০ গ্রাম" to 250L,
              "৫০০ গ্রাম" to 500L,
              "১ কেজি" to 1000L,
              "২ কেজি" to 2000L,
              "৫ কেজি" to 5000L,
            )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { (label, grams) ->
              FilterChip(
                selected = qtyUnits == grams,
                onClick = { syncWeightFields(grams) },
                label = { Text(label) },
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("weight_chip_$grams"),
              )
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = kgText,
              onValueChange = {
                if (it.length <= 6 && it.all { ch -> ch.isDigit() }) kgText = it
              },
              label = { Text("কেজি") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              isError = isQuantityInvalid,
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("qty_kg_input"),
            )
            OutlinedTextField(
              value = gramText,
              onValueChange = {
                if (it.length <= 4 && it.all { ch -> ch.isDigit() }) gramText = it
              },
              label = { Text("গ্রাম") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              isError = isQuantityInvalid,
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("qty_gram_input"),
            )
          }
        }
      } else {
        Text("পরিমাণ (${veg.unitType.labelBn})", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedButton(
            onClick = {
              val cur = (kgText.toLongOrNull() ?: 1L).coerceAtLeast(1L)
              if (cur > 1L) kgText = (cur - 1L).toString()
            },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(Icons.Filled.Remove, contentDescription = "কমান")
          }
          OutlinedTextField(
            value = kgText,
            onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) kgText = it },
            label = { Text(veg.unitType.labelBn) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = isQuantityInvalid,
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("qty_count_input"),
          )
          OutlinedButton(
            onClick = {
              val cur = kgText.toLongOrNull() ?: 0L
              kgText = (cur + 1L).toString()
            },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(Icons.Filled.Add, contentDescription = "বাড়ান")
          }
        }
      }

      if (isQuantityInvalid) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "পরিমাণ শূন্যের বেশি হতে হবে।",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      OutlinedTextField(
        value = rateText,
        onValueChange = { rateText = it },
        label = { Text("দর প্রতি ${veg.unitType.labelBn} (৳)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = isRateInvalid,
        supportingText = { if (isRateInvalid) Text("দর শূন্যের বেশি লিখুন।") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("qty_rate_input"),
      )

      if (isLowStock) {
        Spacer(modifier = Modifier.height(10.dp))
        StatusBadge(
          text = "পর্যাপ্ত স্টক নেই (চাইলে তবুও যোগ করতে পারবেন)",
          tone = BadgeTone.WARNING,
          icon = Icons.Filled.Warning,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text("মোট দাম", style = MaterialTheme.typography.bodySmall)
          Text(
            text = BusinessCalculator.formatTaka(lineTotal, bn),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Button(
          onClick = { onConfirm(qtyUnits, rate) },
          enabled = !isQuantityInvalid && !isRateInvalid,
          modifier =
            Modifier.height(BazarDimens.PrimaryButtonHeight).testTag("qty_confirm_button"),
        ) {
          Text(confirmLabel)
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartCheckoutBottomSheet(
  uiState: ShopUiState,
  onDismiss: () -> Unit,
  onEditItem: (VegetableEntity) -> Unit,
  onRemoveItem: (Long) -> Unit,
  onClearCart: () -> Unit,
  onCreateCustomer: suspend (String, String) -> PartyEntity,
  onSubmitSale: (Long?, String, Double, Double, PaymentMethod, String) -> Unit,
) {
  val bn = uiState.preferences.useBengaliDigits
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()

  val subtotal = BusinessCalculator.round2(uiState.posCart.sumOf { it.lineTotal })
  var discountText by remember { mutableStateOf("") }
  val parsedDiscount = if (discountText.isBlank()) 0.0 else discountText.toDoubleOrNull()
  val discount = parsedDiscount?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
  val netTotal = BusinessCalculator.round2((subtotal - discount).coerceAtLeast(0.0))

  var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
  var paidManuallyEdited by remember { mutableStateOf(false) }
  var paidText by remember {
    mutableStateOf(BusinessCalculator.formatEditableNumber(netTotal).ifEmpty { "0" })
  }

  // Keep auto-syncing paidText ONLY if shopkeeper has not manually typed an amount
  LaunchedEffect(netTotal, paymentMethod) {
    if (!paidManuallyEdited) {
      paidText =
        if (paymentMethod == PaymentMethod.CREDIT) {
          "0"
        } else {
          BusinessCalculator.formatEditableNumber(netTotal).ifEmpty { "0" }
        }
    }
  }

  val customers = remember(uiState.customerBalances) { uiState.customerBalances.map { it.party } }
  val balanceById =
    remember(uiState.customerBalances) {
      uiState.customerBalances.associate { it.party.id to it.balanceDue }
    }
  var selectedCustomer by
    remember(uiState.preselectedPosCustomerId) {
      mutableStateOf(
        uiState.preselectedPosCustomerId?.let { id -> customers.firstOrNull { it.id == id } },
      )
    }
  var notesText by remember { mutableStateOf("") }
  var showCustomerPicker by remember { mutableStateOf(false) }
  var showAddCustomerDialog by remember { mutableStateOf(false) }
  var showLowStockConfirm by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }

  val parsedPaid =
    if (!uiState.preferences.showCreditLedger) {
      netTotal
    } else if (paidText.isBlank()) {
      0.0
    } else {
      paidText.toDoubleOrNull()
    }
  val paidAmount = parsedPaid?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
  val dueAmount = BusinessCalculator.round2((netTotal - paidAmount).coerceAtLeast(0.0))
  val hasLowStock = uiState.posCart.any { it.quantityUnits > it.vegetable.stockUnits }

  fun attemptSubmit() {
    if (parsedDiscount == null || !parsedDiscount.isFinite() || parsedDiscount < 0.0) {
      validationError = "সঠিক ছাড়ের পরিমাণ লিখুন।"
      return
    }
    if (discount > subtotal) {
      validationError = "ছাড়ের পরিমাণ মোট বিলের চেয়ে বেশি হতে পারবে না।"
      return
    }
    if (parsedPaid == null || !parsedPaid.isFinite() || parsedPaid < 0.0) {
      validationError = "সঠিক পরিশোধিত টাকার পরিমাণ লিখুন।"
      return
    }
    if (paidAmount > netTotal + BusinessCalculator.AMOUNT_EPSILON) {
      validationError = "পরিশোধিত টাকা মোট বিলের চেয়ে বেশি হতে পারে না।"
      return
    }
    if (dueAmount > BusinessCalculator.AMOUNT_EPSILON && selectedCustomer == null) {
      validationError = "বাকি রাখার জন্য অনুগ্রহ করে ক্রেতার নাম নির্বাচন করুন।"
      return
    }
    if (hasLowStock) {
      showLowStockConfirm = true
      return
    }
    onSubmitSale(
      selectedCustomer?.id,
      selectedCustomer?.name ?: "সাধারণ ক্রেতা",
      discount,
      paidAmount,
      paymentMethod,
      notesText.trim(),
    )
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "ক্রেতার বিল (${BusinessCalculator.formatNumber(uiState.posCart.size, bn)}টি)",
          style = MaterialTheme.typography.headlineSmall,
        )
        TextButton(onClick = onClearCart) {
          Text("সব মুছুন", color = MaterialTheme.colorScheme.error)
        }
      }
      Spacer(modifier = Modifier.height(8.dp))

      uiState.posCart.forEach { item ->
        CartLineRow(
          item = item,
          bn = bn,
          onEdit = { onEditItem(item.vegetable) },
          onRemove = { onRemoveItem(item.vegetable.id) },
        )
        Spacer(modifier = Modifier.height(6.dp))
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = discountText,
          onValueChange = {
            discountText = it
            validationError = null
          },
          label = { Text("ছাড় (৳)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("checkout_discount_input"),
        )
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
          Text(
            "মোট: ${BusinessCalculator.formatTaka(subtotal, bn)}",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            text = "নিট বিল: ${BusinessCalculator.formatTaka(netTotal, bn)}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Text("পেমেন্ট পদ্ধতি", style = MaterialTheme.typography.labelLarge)
      Spacer(modifier = Modifier.height(6.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val methods =
          if (uiState.preferences.showCreditLedger) {
            PaymentMethod.entries
          } else {
            listOf(PaymentMethod.CASH, PaymentMethod.MOBILE)
          }
        methods.forEach { method ->
          FilterChip(
            selected = paymentMethod == method,
            onClick = {
              paymentMethod = method
              paidManuallyEdited = false
              validationError = null
            },
            label = { Text(method.labelBn) },
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("pay_method_${method.name}"),
          )
        }
      }

      if (uiState.preferences.showCreditLedger) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = paidText,
          onValueChange = {
            paidText = it
            paidManuallyEdited = true
            validationError = null
          },
          label = { Text("আদায়কৃত টাকা (৳)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("checkout_paid_input"),
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            FilterChip(
              selected =
                kotlin.math.abs(paidAmount - netTotal) < BusinessCalculator.DUE_TOLERANCE,
              onClick = {
                paidText = BusinessCalculator.formatEditableNumber(netTotal).ifEmpty { "0" }
                paidManuallyEdited = false
                validationError = null
              },
              label = { Text("পুরো বিল") },
              modifier =
                Modifier.minimumInteractiveComponentSize().testTag("checkout_full_paid_chip"),
            )
          }
          if (netTotal > 1.0) {
            item {
              val half = BusinessCalculator.round2(netTotal / 2.0)
              FilterChip(
                selected = kotlin.math.abs(paidAmount - half) < BusinessCalculator.DUE_TOLERANCE,
                onClick = {
                  paidText = BusinessCalculator.formatEditableNumber(half)
                  paidManuallyEdited = true
                  validationError = null
                },
                label = { Text("অর্ধেক") },
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("checkout_half_paid_chip"),
              )
            }
          }
          item {
            FilterChip(
              selected = paidAmount < BusinessCalculator.AMOUNT_EPSILON,
              onClick = {
                paidText = "0"
                paidManuallyEdited = true
                paymentMethod = PaymentMethod.CREDIT
                validationError = null
              },
              label = { Text("সম্পূর্ণ বাকি") },
              modifier =
                Modifier.minimumInteractiveComponentSize().testTag("checkout_zero_paid_chip"),
            )
          }
        }

        if (dueAmount > BusinessCalculator.AMOUNT_EPSILON) {
          Spacer(modifier = Modifier.height(6.dp))
          StatusBadge(
            text = "বাকি থাকবে: ${BusinessCalculator.formatTaka(dueAmount, bn)}",
            tone = BadgeTone.WARNING,
            icon = Icons.Filled.Warning,
          )
        }
      }

      // Always allow selecting/attaching a customer (for repeat cash buyers or credit due)
      Spacer(modifier = Modifier.height(12.dp))
      Card(
        onClick = { showCustomerPicker = true },
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().testTag("checkout_customer_selector"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Filled.Person,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Column {
              Text(
                text = "ক্রেতা",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = selectedCustomer?.name ?: "সাধারণ ক্রেতা (নগদ)",
                style = MaterialTheme.typography.titleSmall,
              )
            }
          }
          Text(
            text = if (selectedCustomer == null) "বাছাই করুন" else "পরিবর্তন",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = notesText,
        onValueChange = { notesText = it },
        label = { Text("নোট (ঐচ্ছিক)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("checkout_note_input"),
      )

      if (validationError != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = validationError!!,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = ::attemptSubmit,
        enabled = !uiState.isSaving && netTotal > 0.0,
        modifier =
          Modifier
            .fillMaxWidth()
            .height(BazarDimens.PrimaryButtonHeight)
            .testTag("checkout_complete_button"),
      ) {
        Text(if (uiState.isSaving) "সংরক্ষণ হচ্ছে..." else "বিক্রি সম্পন্ন করুন")
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }

  if (showCustomerPicker) {
    PartyPickerSheet(
      title = "ক্রেতা নির্বাচন করুন",
      defaultWalkInLabel = "সাধারণ ক্রেতা (নগদ)",
      parties = customers,
      selectedPartyId = selectedCustomer?.id,
      partyType = PartyType.CUSTOMER,
      bn = bn,
      balanceByPartyId = balanceById,
      onSelectWalkIn = {
        selectedCustomer = null
        validationError = null
      },
      onSelectParty = {
        selectedCustomer = it
        validationError = null
      },
      onAddNewClick = { showAddCustomerDialog = true },
      onDismiss = { showCustomerPicker = false },
    )
  }

  if (showLowStockConfirm) {
    ConfirmationDialog(
      title = "স্টক সতর্কতা",
      message = "কার্টে থাকা কিছু সবজির পর্যাপ্ত স্টক নেই। তবুও কি বিক্রি সম্পন্ন করবেন?",
      confirmLabel = "হ্যাঁ, বিক্রি করুন",
      onConfirm = {
        showLowStockConfirm = false
        onSubmitSale(
          selectedCustomer?.id,
          selectedCustomer?.name ?: "সাধারণ ক্রেতা",
          discount,
          paidAmount,
          paymentMethod,
          notesText.trim(),
        )
      },
      onDismiss = { showLowStockConfirm = false },
    )
  }

  if (showAddCustomerDialog) {
    QuickAddPartyDialog(
      partyType = PartyType.CUSTOMER,
      onDismiss = { showAddCustomerDialog = false },
      onSave = { name, phone ->
        scope.launch {
          runCatching { onCreateCustomer(name, phone) }
            .onSuccess { created ->
              selectedCustomer = created
              showAddCustomerDialog = false
              showCustomerPicker = false
              validationError = null
            }.onFailure { err ->
              showAddCustomerDialog = false
              validationError = err.message ?: "ক্রেতা যোগ করা যায়নি।"
            }
        }
      },
    )
  }
}

@Composable
private fun CartLineRow(
  item: PosCartItem,
  bn: Boolean,
  onEdit: () -> Unit,
  onRemove: () -> Unit,
) {
  Card(
    onClick = onEdit,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(item.vegetable.name, style = MaterialTheme.typography.titleSmall)
        val qtyText =
          BusinessCalculator.formatQuantity(item.quantityUnits, item.vegetable.unitType, bn)
        Text(
          text = "$qtyText × ${BusinessCalculator.formatTaka(item.sellingPricePerUnit, bn)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = BusinessCalculator.formatTaka(item.lineTotal, bn),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
      )
      IconButton(onClick = onRemove, modifier = Modifier.minimumInteractiveComponentSize()) {
        Icon(
          imageVector = Icons.Filled.Delete,
          contentDescription = "বাদ দিন",
          tint = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

private fun calculateGramsFromTaka(
  unitType: UnitType,
  isTakaMode: Boolean,
  takaAmount: Double,
  rate: Double,
): Long {
  val canCalculate = unitType == UnitType.KG && isTakaMode && takaAmount > 0.0 && rate > 0.0
  if (!canCalculate) return 0L
  return ((takaAmount / rate) * BusinessCalculator.GRAMS_PER_KG_DOUBLE)
    .coerceAtMost(BusinessCalculator.MAX_CUSTOM_WEIGHT_GRAMS)
    .roundToLong()
    .coerceAtLeast(1L)
}
