package com.arkbuilders.arkdrop.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.arkbuilders.arkdrop.Greeting

fun NavGraphBuilder.navRegistration(navController: NavController) {
    composable(TransfersDestination.route) {
        Greeting(name = "Timer dest")

    }
    composable(SettingsDestination.route) {
        Greeting(name = "Timer report 2")
    }
}
