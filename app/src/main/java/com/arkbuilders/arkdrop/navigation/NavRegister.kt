package com.arkbuilders.arkdrop.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.arkbuilders.arkdrop.Greeting

fun NavGraphBuilder.navRegistration(navController: NavController) {
    composable(TransfersDestination.route) {
        Greeting(name = "TransfersDestination")

    }
    composable(HistoryDestination.route) {
        Greeting(name = "HistoryDestination")
    }
    composable(SettingsDestination.route) {
        Greeting(name = "SettingsDestination")
    }
}
