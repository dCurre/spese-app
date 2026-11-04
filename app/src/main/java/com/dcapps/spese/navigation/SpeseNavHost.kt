package com.dcapps.spese.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
fun SpeseNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // La bottom bar è visibile solo nelle destinazioni top-level (non nel dettaglio).
    val showBottomBar = topLevelDestinations.any { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
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
                    onOpenAdmin = { navController.navigate(Routes.ADMIN) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                )
            }
            composable(Routes.ADMIN) {
                AdminScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
