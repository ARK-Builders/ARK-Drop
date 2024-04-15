package com.arkbuilders.arkdrop.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.arkbuilders.arkdrop.Greeting

fun NavGraphBuilder.navRegistration(navController: NavController) {
    composable(TimerDestination.route) {
        Greeting(name = "Timer dest")

    }
    composable(TimerReportDestination2.route) {
        Greeting(name = "Timer report 2")
    }
}
