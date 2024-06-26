package dev.arkbuilders.arkdrop.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.arkbuilders.arkdrop.Greeting
import dev.arkbuilders.arkdrop.presentation.feature.filestransfers.FilesTransferScreen
import dev.arkbuilders.arkdrop.presentation.feature.settings.SettingsScreen
import dev.arkbuilders.arkdrop.presentation.feature.transferconfirmation.TransferConfirmationScreen
import dev.arkbuilders.arkdrop.presentation.feature.transferprogress.TransferProgressScreen

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
        TransferConfirmationScreen(navController = navController)
    }
    composable(TransferProgressDestination.route) {
        TransferProgressScreen(navController = navController)
    }
}
