package com.dcapps.spese.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dcapps.spese.feature.expenses.ExpensesDetailScreen
import com.dcapps.spese.feature.expenses.ExpensesListScreen

object Routes {
    const val EXPENSES_LIST = "expenses"
    const val EXPENSES_DETAIL = "expenses/{listId}"
    fun expensesDetail(listId: Long) = "expenses/$listId"
}

@Composable
fun SpeseNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.EXPENSES_LIST) {
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
    }
}
