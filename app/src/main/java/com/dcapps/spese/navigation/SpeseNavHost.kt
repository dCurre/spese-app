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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R
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
import com.dcapps.spese.feature.profile.ProfileEditScreen
import com.dcapps.spese.feature.profile.ProfileSettingsScreen
import com.dcapps.spese.feature.profile.ProfileStatsScreen

object Routes {
    const val EXPENSES_LIST = "expenses"
    const val EXPENSES_DETAIL = "expenses/{listId}"
    const val CHECKLIST = "checklist"
    const val CHECKLIST_DETAIL = "checklist/{listId}"
    const val PRICES = "prices"
    const val PRICE_DETAIL = "prices/{productId}"
    const val PROFILE = "profile"
    const val PROFILE_EDIT = "profile/edit"
    const val PROFILE_SETTINGS = "profile/settings"
    const val PROFILE_STATS = "profile/stats"
    const val ADMIN = "admin"
    const val ABOUT = "about"
    fun expensesDetail(listId: Long) = "expenses/$listId"
    fun checklistDetail(listId: Long) = "checklist/$listId"
    fun priceDetail(productId: Long) = "prices/$productId"
}

private data class TopLevel(
    val route: String,
    @androidx.annotation.StringRes val labelRes: Int,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevel(Routes.EXPENSES_LIST, R.string.nav_expenses, Icons.AutoMirrored.Filled.ListAlt),
    TopLevel(Routes.CHECKLIST, R.string.nav_checklist, Icons.Default.ShoppingCart),
    TopLevel(Routes.PRICES, R.string.nav_prices, Icons.AutoMirrored.Filled.TrendingUp),
    TopLevel(Routes.PROFILE, R.string.nav_profile, Icons.Default.Person),
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

    val user by authViewModel.currentUser.collectAsStateWithLifecycle()
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    // Feedback del controllo aggiornamenti manuale (es. "Nessun aggiornamento disponibile").
    com.dcapps.spese.ui.components.MessageSnackbarEffect(
        updateState.checkMessage, snackbarHostState, updateViewModel::clearCheckMessage,
    )

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
                    profileImageUrl = user?.profileImage,
                )
            }
        },
    ) { innerPadding ->
        // Applica solo il padding inferiore (per la bottom bar): l'inset superiore
        // (status bar) lo gestiscono le TopAppBar delle singole schermate, altrimenti
        // verrebbe applicato due volte lasciando spazio vuoto sopra il titolo.
        NavHost(
            navController = navController,
            startDestination = Routes.EXPENSES_LIST,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
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
                )
            }
            composable(
                route = Routes.PRICE_DETAIL,
                arguments = listOf(navArgument("productId") { type = NavType.LongType }),
            ) {
                PriceDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onOpenProfileDetail = { navController.navigate(Routes.PROFILE_EDIT) },
                    onOpenSettings = { navController.navigate(Routes.PROFILE_SETTINGS) },
                    onOpenStats = { navController.navigate(Routes.PROFILE_STATS) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onOpenAdmin = { navController.navigate(Routes.ADMIN) },
                    onCheckUpdates = { updateViewModel.checkForUpdateManual() },
                    updateChecking = updateState.checking,
                )
            }
            composable(Routes.PROFILE_EDIT) {
                ProfileEditScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PROFILE_SETTINGS) {
                ProfileSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PROFILE_STATS) {
                ProfileStatsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ADMIN) {
                AdminScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }

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
    profileImageUrl: String?,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        NavigationBar {
            destinations.forEachIndexed { index, dest ->
                // Slot vuoto centrale solo quando il "+" è presente, così su Profilo (niente "+")
                // le voci riempiono la barra senza lasciare un buco.
                if (createRoute != null && index == destinations.size / 2) {
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        enabled = false,
                        icon = {},
                    )
                }
                val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                val label = stringResource(dest.labelRes)
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(dest.route) },
                    icon = {
                        if (dest.route == Routes.PROFILE && !profileImageUrl.isNullOrBlank()) {
                            coil.compose.AsyncImage(
                                model = profileImageUrl,
                                contentDescription = label,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape),
                            )
                        } else {
                            Icon(dest.icon, contentDescription = label)
                        }
                    },
                    label = { Text(label) },
                )
            }
        }
        if (createRoute != null) {
            FloatingActionButton(
                onClick = onCreate,
                shape = CircleShape,
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(top = 2.dp).size(56.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
            }
        }
    }
}
