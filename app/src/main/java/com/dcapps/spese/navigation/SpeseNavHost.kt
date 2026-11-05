package com.dcapps.spese.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R
import kotlinx.coroutines.launch
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dcapps.spese.feature.checklist.ChecklistDetailScreen
import com.dcapps.spese.feature.checklist.ChecklistScreen
import com.dcapps.spese.feature.expenses.ExpensesDetailScreen
import com.dcapps.spese.feature.expenses.ExpensesListScreen
import com.dcapps.spese.feature.about.AboutScreen
import com.dcapps.spese.feature.admin.AdminScreen
import com.dcapps.spese.feature.prices.PriceDetailScreen
import com.dcapps.spese.feature.prices.PriceListScreen
import com.dcapps.spese.feature.profile.ProfileScreen

object Routes {
    const val EXPENSES_LIST = "expenses"
    const val EXPENSES_DETAIL = "expenses/{listId}"
    const val CHECKLIST = "checklist"
    const val CHECKLIST_DETAIL = "checklist/{listId}"
    const val PRICES = "prices"
    const val PRICE_DETAIL = "prices/{productId}"
    const val PROFILE = "profile"
    const val ADMIN = "admin"
    const val ABOUT = "about"
    fun expensesDetail(listId: Long) = "expenses/$listId"
    fun checklistDetail(listId: Long) = "checklist/$listId"
    fun priceDetail(productId: Long) = "prices/$productId"
}

private data class TopLevel(val route: String, val label: String, val icon: ImageVector)

private val topLevelDestinations = listOf(
    TopLevel(Routes.EXPENSES_LIST, "Spese", Icons.AutoMirrored.Filled.ListAlt),
    TopLevel(Routes.CHECKLIST, "Checklist", Icons.Default.ShoppingCart),
    TopLevel(Routes.PRICES, "Prezzi", Icons.AutoMirrored.Filled.TrendingUp),
    TopLevel(Routes.PROFILE, "Profilo", Icons.Default.Person),
)

@Composable
fun SpeseNavHost(
    authViewModel: com.dcapps.spese.feature.auth.AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    updateViewModel: com.dcapps.spese.feature.update.UpdateViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // La bottom bar è visibile solo nelle destinazioni top-level (non nel dettaglio).
    val showBottomBar = topLevelDestinations.any { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val user by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isAdmin = user?.role == "admin" || user?.role == "superadmin"
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }
    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    // Feedback del controllo aggiornamenti manuale (es. "Nessun aggiornamento disponibile").
    com.dcapps.spese.ui.components.MessageSnackbarEffect(
        updateState.checkMessage, snackbarHostState, updateViewModel::clearCheckMessage,
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    stringResource(R.string.drawer_menu),
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 28.dp, top = 24.dp, bottom = 12.dp),
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    label = {
                        Text(
                            if (updateState.checking) stringResource(R.string.profile_checking)
                            else stringResource(R.string.profile_check_updates),
                        )
                    },
                    selected = false,
                    onClick = { closeDrawer(); updateViewModel.checkForUpdateManual() },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
                if (isAdmin) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                        label = { Text(stringResource(R.string.profile_admin)) },
                        selected = currentDestination?.route == Routes.ADMIN,
                        onClick = { closeDrawer(); navController.navigate(Routes.ADMIN) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text(stringResource(R.string.profile_about)) },
                    selected = currentDestination?.route == Routes.ABOUT,
                    onClick = { closeDrawer(); navController.navigate(Routes.ABOUT) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        },
    ) {
    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                // Rotta "crea" per il + centrale in base alla tab attiva (null su Profilo).
                val createRoute = createRouteFor(currentDestination)
                val navigate: (String) -> Unit = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                CenterFabBottomBar(
                    destinations = topLevelDestinations,
                    currentDestination = currentDestination,
                    onNavigate = navigate,
                    createRoute = createRoute,
                    onCreate = { createRoute?.let { CreateActionBus.request(it) } },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.EXPENSES_LIST,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                androidx.compose.animation.slideInHorizontally(initialOffsetX = { it / 4 }) +
                    androidx.compose.animation.fadeIn()
            },
            exitTransition = { androidx.compose.animation.fadeOut() },
            popEnterTransition = { androidx.compose.animation.fadeIn() },
            popExitTransition = {
                androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it / 4 }) +
                    androidx.compose.animation.fadeOut()
            },
        ) {
            composable(Routes.EXPENSES_LIST) {
                ExpensesListScreen(
                    onOpenList = { list -> navController.navigate(Routes.expensesDetail(list.id)) },
                    onOpenDrawer = openDrawer,
                )
            }
            composable(
                route = Routes.EXPENSES_DETAIL,
                arguments = listOf(navArgument("listId") { type = NavType.LongType }),
            ) {
                ExpensesDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CHECKLIST) {
                ChecklistScreen(
                    onOpenList = { list -> navController.navigate(Routes.checklistDetail(list.id)) },
                    onOpenDrawer = openDrawer,
                )
            }
            composable(
                route = Routes.CHECKLIST_DETAIL,
                arguments = listOf(navArgument("listId") { type = NavType.LongType }),
            ) {
                ChecklistDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PRICES) {
                PriceListScreen(
                    onOpenProduct = { product -> navController.navigate(Routes.priceDetail(product.id)) },
                    onOpenDrawer = openDrawer,
                )
            }
            composable(
                route = Routes.PRICE_DETAIL,
                arguments = listOf(navArgument("productId") { type = NavType.LongType }),
            ) {
                PriceDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(onOpenDrawer = openDrawer)
            }
            composable(Routes.ADMIN) {
                AdminScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
    } // ModalNavigationDrawer

    // Popup di aggiornamento se il controllo manuale trova una versione nuova.
    val info = updateState.info
    if (updateState.showDialog && info != null) {
        com.dcapps.spese.feature.update.UpdateDialog(
            state = updateState,
            info = info,
            canInstall = updateViewModel.canInstall(),
            onUpdate = updateViewModel::onUpdateClick,
            onDismiss = updateViewModel::dismiss,
        )
    }
}

/** Rotta di creazione associata alla tab attiva, o null se la tab non ha un'azione "+". */
private fun createRouteFor(dest: androidx.navigation.NavDestination?): String? = when {
    dest == null -> null
    dest.hierarchy.any { it.route == Routes.EXPENSES_LIST } -> Routes.EXPENSES_LIST
    dest.hierarchy.any { it.route == Routes.CHECKLIST } -> Routes.CHECKLIST
    dest.hierarchy.any { it.route == Routes.PRICES } -> Routes.PRICES
    else -> null // Profilo: nessuna azione di creazione
}

/**
 * Bottom bar con 4 voci di navigazione (2 + 2) e un pulsante "+" centrale rialzato (come nei mock).
 * Il "+" appare solo quando la tab attiva ha un'azione di creazione ([createRoute] != null).
 */
@Composable
private fun CenterFabBottomBar(
    destinations: List<TopLevel>,
    currentDestination: androidx.navigation.NavDestination?,
    onNavigate: (String) -> Unit,
    createRoute: String?,
    onCreate: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        NavigationBar {
            destinations.forEachIndexed { index, dest ->
                // Inserisce uno slot vuoto al centro per far spazio al "+".
                if (index == destinations.size / 2) {
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        enabled = false,
                        icon = {},
                    )
                }
                val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(dest.route) },
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = { Text(dest.label) },
                )
            }
        }
        if (createRoute != null) {
            FloatingActionButton(
                onClick = onCreate,
                shape = CircleShape,
                modifier = Modifier.padding(top = 2.dp).size(56.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
            }
        }
    }
}
