package com.arkbuilders.arkdrop.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.arkbuilders.arkdrop.Greeting
import com.arkbuilders.arkdrop.presentation.filestransfers.FilesTransferScreen
import com.arkbuilders.arkdrop.presentation.transferconfirmation.TransferConfirmation

fun NavGraphBuilder.navRegistration(navController: NavController) {
    composable(TransfersDestination.route) {
        FilesTransferScreen(navController = navController)
    }
    composable(HistoryDestination.route) {
        Greeting(name = "HistoryDestination")
    }
    composable(SettingsDestination.route) {
        Greeting(name = "SettingsDestination")
    }
    composable(TransferConfirmationDestination.route) {
        TransferConfirmation(navController = navController)
    }
}
