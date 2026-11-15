@file:Suppress("ktlint:standard:max-line-length")

package com.jubayerhossain.sobjidokan.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jubayerhossain.sobjidokan.data.local.entity.PartyType
import com.jubayerhossain.sobjidokan.feature.dashboard.DashboardScreen
import com.jubayerhossain.sobjidokan.feature.inventory.AddWastageSheet
import com.jubayerhossain.sobjidokan.feature.inventory.StockAndPurchaseScreen
import com.jubayerhossain.sobjidokan.feature.parties.PartiesLedgerScreen
import com.jubayerhossain.sobjidokan.feature.purchases.NewPurchaseScreen
import com.jubayerhossain.sobjidokan.feature.reports.AddEditExpenseSheet
import com.jubayerhossain.sobjidokan.feature.reports.ExpensesAndReportsScreen
import com.jubayerhossain.sobjidokan.feature.sales.QuickPosScreen
import com.jubayerhossain.sobjidokan.feature.sales.salesHistoryScreen
import com.jubayerhossain.sobjidokan.feature.settings.settingsScreen
import com.jubayerhossain.sobjidokan.ui.ShopUiState
import com.jubayerhossain.sobjidokan.ui.ShopViewModel
import kotlinx.serialization.Serializable

@Serializable object RouteDashboard

@Serializable object RoutePos

@Serializable
data class RouteParties(
  val initialSubTab: Int = 0,
)

@Serializable
data class RouteStock(
  val initialTab: Int = 0,
)

@Serializable
data class RouteReports(
  val initialTab: Int = 1,
)

@Serializable object RouteSalesHistory

@Serializable object RouteNewPurchase

@Serializable object RouteSettings

private data class TopDestination(
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val route: Any,
  val testTag: String,
)

@Composable
fun sobjiKhataAppShell(
  uiState: ShopUiState,
  viewModel: ShopViewModel,
  pendingRestore: com.jubayerhossain.sobjidokan.data.backup.SobjiKhataBackupPayload?,
) {
  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDest = backStackEntry?.destination

  var showDashboardWastageSheet by rememberSaveable { mutableStateOf(false) }
  var showDashboardExpenseSheet by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.snackbarEvents.collect { msg -> snackbarHostState.showSnackbar(msg) }
  }

  val showCredit = uiState.preferences.showCreditLedger
  val topDestinations =
    remember(showCredit) {
      buildList {
        add(
          TopDestination(
            label = "হিসাব",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            route = RouteDashboard,
            testTag = "nav_dashboard",
          ),
        )
        add(
          TopDestination(
            label = "বিক্রি",
            selectedIcon = Icons.Filled.PointOfSale,
            unselectedIcon = Icons.Outlined.PointOfSale,
            route = RoutePos,
            testTag = "nav_pos",
          ),
        )
        if (showCredit) {
          add(
            TopDestination(
              label = "বাকি খাতা",
              selectedIcon = Icons.Filled.People,
              unselectedIcon = Icons.Outlined.People,
              route = RouteParties(0),
              testTag = "nav_parties",
            ),
          )
        }
        add(
          TopDestination(
            label = "স্টক ও কেনা",
            selectedIcon = Icons.Filled.Inventory2,
            unselectedIcon = Icons.Outlined.Inventory2,
            route = RouteStock(0),
            testTag = "nav_stock",
          ),
        )
        add(
          TopDestination(
            label = "রিপোর্ট",
            selectedIcon = Icons.Filled.Assessment,
            unselectedIcon = Icons.Outlined.Assessment,
            route = RouteReports(1),
            testTag = "nav_reports",
          ),
        )
      }
    }

  val isTopLevel =
    currentDest?.hasRoute<RouteDashboard>() == true ||
      currentDest?.hasRoute<RoutePos>() == true ||
      currentDest?.hasRoute<RouteParties>() == true ||
      currentDest?.hasRoute<RouteStock>() == true ||
      currentDest?.hasRoute<RouteReports>() == true

  fun navigateTopTab(route: Any) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  fun navigateTabDirect(route: Any) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = false }
      launchSingleTop = true
      restoreState = false
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (isTopLevel && !useRail) {
          NavigationBar {
            topDestinations.forEach { item ->
              val selected =
                when (item.route) {
                  is RouteDashboard -> currentDest.hasRoute<RouteDashboard>() == true
                  is RoutePos -> currentDest.hasRoute<RoutePos>() == true
                  is RouteParties -> currentDest.hasRoute<RouteParties>() == true
                  is RouteStock -> currentDest.hasRoute<RouteStock>() == true
                  is RouteReports -> currentDest.hasRoute<RouteReports>() == true
                  else -> false
                }
              NavigationBarItem(
                selected = selected,
                onClick = { navigateTopTab(item.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                  )
                },
                label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (isTopLevel && useRail) {
          NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            topDestinations.forEach { item ->
              val selected =
                when (item.route) {
                  is RouteDashboard -> currentDest.hasRoute<RouteDashboard>() == true
                  is RoutePos -> currentDest.hasRoute<RoutePos>() == true
                  is RouteParties -> currentDest.hasRoute<RouteParties>() == true
                  is RouteStock -> currentDest.hasRoute<RouteStock>() == true
                  is RouteReports -> currentDest.hasRoute<RouteReports>() == true
                  else -> false
                }
              NavigationRailItem(
                selected = selected,
                onClick = { navigateTopTab(item.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                  )
                },
                label = { Text(item.label, maxLines = 1) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          NavHost(navController = navController, startDestination = RouteDashboard) {
            composable<RouteDashboard> {
              DashboardScreen(
                uiState = uiState,
                onOpenSettings = { navController.navigate(RouteSettings) },
                onQuickSale = { navigateTopTab(RoutePos) },
                onQuickPurchase = { navController.navigate(RouteNewPurchase) },
                onQuickWastage = { showDashboardWastageSheet = true },
                onQuickExpense = { showDashboardExpenseSheet = true },
                onOpenStock = { navigateTabDirect(RouteStock(0)) },
                onOpenReports = { navigateTabDirect(RouteReports(1)) },
                onOpenSalesHistory = { navController.navigate(RouteSalesHistory) },
                onOpenExpenses = { navigateTabDirect(RouteReports(0)) },
                onOpenWastage = { navigateTabDirect(RouteStock(2)) },
                onOpenParties = { subTab -> navigateTabDirect(RouteParties(subTab)) },
                onCancelSale = viewModel::cancelSale,
              )
            }
            composable<RoutePos> {
              QuickPosScreen(
                uiState = uiState,
                onOpenHistory = { navController.navigate(RouteSalesHistory) },
                onOpenStock = { navigateTabDirect(RouteStock(0)) },
                onAddToCart = viewModel::addToPosCart,
                onRemoveFromCart = viewModel::removeFromPosCart,
                onClearCart = viewModel::clearPosCart,
                onCompleteSale = viewModel::completePosSale,
                onCreateCustomer = { name, phone ->
                  viewModel.createPartyImmediate(name, phone, PartyType.CUSTOMER)
                },
                onClearPreselectedCustomer = { viewModel.preselectPosCustomer(null) },
              )
            }
            composable<RouteParties> { entry ->
              val route = entry.toRoute<RouteParties>()
              PartiesLedgerScreen(
                uiState = uiState,
                initialSubTab = route.initialSubTab,
                onAddParty = viewModel::addParty,
                onRecordDuePayment = viewModel::recordDuePayment,
                onCancelSale = viewModel::cancelSale,
                onCancelPurchase = viewModel::cancelPurchase,
                onStartSaleForCustomer = { customerId ->
                  viewModel.preselectPosCustomer(customerId)
                  navigateTopTab(RoutePos)
                },
                onCancelDuePayment = viewModel::cancelDuePayment,
              )
            }
            composable<RouteStock> { entry ->
              val route = entry.toRoute<RouteStock>()
              StockAndPurchaseScreen(
                uiState = uiState,
                initialTab = route.initialTab,
                onOpenNewPurchase = { navController.navigate(RouteNewPurchase) },
                onSaveVegetable = viewModel::saveVegetable,
                onToggleArchive = viewModel::toggleArchiveVegetable,
                onAdjustStock = viewModel::adjustStock,
                onRecordWastage = viewModel::recordWastage,
                onCancelWastage = viewModel::cancelWastage,
                onCancelPurchase = viewModel::cancelPurchase,
                onAdjustStockAsync = viewModel::adjustStock,
                onRecordWastageAsync = viewModel::recordWastage,
                onBulkUpdatePrices = viewModel::bulkUpdateVegetableSellingPrices,
              )
            }
            composable<RouteReports> { entry ->
              val route = entry.toRoute<RouteReports>()
              ExpensesAndReportsScreen(
                uiState = uiState,
                initialTab = route.initialTab,
                showCreditLedgerTab = false,
                onSaveExpense = viewModel::saveExpense,
                onCancelExpense = viewModel::cancelExpense,
                onAddParty = viewModel::addParty,
                onRecordDuePayment = viewModel::recordDuePayment,
                onSaveExpenseAsync = viewModel::saveExpense,
                onRecordDuePaymentAsync = viewModel::recordDuePayment,
                onCancelSale = viewModel::cancelSale,
                onCancelPurchase = viewModel::cancelPurchase,
                onCancelDuePayment = viewModel::cancelDuePayment,
              )
            }
            composable<RouteSalesHistory> {
              salesHistoryScreen(
                uiState = uiState,
                onBack = { navController.navigateUp() },
                onCancelSale = viewModel::cancelSale,
              )
            }
            composable<RouteNewPurchase> {
              NewPurchaseScreen(
                uiState = uiState,
                onAddDraftItem = viewModel::addPurchaseDraftItem,
                onRemoveDraftItem = viewModel::removePurchaseDraftItem,
                onClearDraft = viewModel::clearPurchaseDraft,
                onCreateSupplier = { name, phone ->
                  viewModel.createPartyImmediate(name, phone, PartyType.SUPPLIER)
                },
                onSavePurchase = viewModel::completePurchase,
                onBack = { navController.navigateUp() },
              )
            }
            composable<RouteSettings> {
              settingsScreen(
                uiState = uiState,
                pendingRestore = pendingRestore,
                onBack = { navController.navigateUp() },
                onUpdateShopProfile = viewModel::updateShopProfile,
                onToggleBengaliDigits = viewModel::setUseBengaliDigits,
                onToggleCreditLedger = viewModel::setShowCreditLedger,
                onSetPinCode = viewModel::setPinCode,
                onExportBackupUri = viewModel::exportBackup,
                onSelectRestoreUri = viewModel::prepareRestoreFromUri,
                onConfirmRestore = viewModel::confirmRestore,
                onDismissRestore = viewModel::dismissPendingRestore,
                onSetThemeMode = viewModel::setThemeMode,
              )
            }
          }
        }
      }
    }
  }

  if (showDashboardWastageSheet) {
    AddWastageSheet(
      vegetables = uiState.activeVegetables,
      preselectedVeg = null,
      bn = uiState.preferences.useBengaliDigits,
      isSaving = uiState.isSaving,
      onDismiss = { showDashboardWastageSheet = false },
      onConfirm = { vegId, qty, reason, note ->
        viewModel.recordWastage(vegId, qty, reason, note) { showDashboardWastageSheet = false }
      },
    )
  }

  if (showDashboardExpenseSheet) {
    AddEditExpenseSheet(
      existing = null,
      isSaving = uiState.isSaving,
      onDismiss = { showDashboardExpenseSheet = false },
      onSave = { cat, amt, method, note ->
        viewModel.saveExpense(null, cat, amt, method, note, System.currentTimeMillis()) {
          showDashboardExpenseSheet = false
        }
      },
    )
  }
}
