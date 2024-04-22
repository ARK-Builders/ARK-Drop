package com.arkbuilders.arkdrop.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.arkbuilders.arkdrop.Greeting
import com.arkbuilders.arkdrop.presentation.feature.filestransfers.FilesTransferScreen
import com.arkbuilders.arkdrop.presentation.feature.settings.SettingsScreen
import com.arkbuilders.arkdrop.presentation.feature.transferconfirmation.TransferConfirmation
import com.arkbuilders.arkdrop.presentation.feature.transferprogress.TransferProgressScreen

fun NavGraphBuilder.navRegistration(navController: NavController) {
    composable(TransfersDestination.route) {
        FilesTransferScreen(navController = navController)
    }
    composable(HistoryDestination.route) {
        Greeting(name = "HistoryDestination")
    }
    composable(SettingsDestination.route) {
        SettingsScreen()
    }
    composable(TransferConfirmationDestination.route) {
        TransferConfirmation(navController = navController)
    }
    composable(TransferProgressDestination.route) {
        TransferProgressScreen(navController = navController)
    }
}
