package com.arkbuilders.arkdrop.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.ui.graphics.vector.ImageVector


interface Destination {
    val route: String
    val title: String
}

interface BottomTabDestination : Destination {
    val activeIcon: ImageVector
    val inActiveIcon: ImageVector
}

object TransfersDestination : BottomTabDestination {
    // Put information needed to navigate here
    override val route = "transfer"
    override val activeIcon = Icons.Filled.SwapVert
    override val inActiveIcon: ImageVector = Icons.Outlined.SwapVert
    override val title = "Transfer"
}

object HistoryDestination : BottomTabDestination {
    override val route = "history"
    override val activeIcon = Icons.Filled.History
    override val inActiveIcon: ImageVector = Icons.Outlined.History
    override val title = "History"
}

object SettingsDestination : BottomTabDestination {
    override val route = "settings"
    override val activeIcon = Icons.Filled.Person
    override val inActiveIcon: ImageVector = Icons.Outlined.Person
    override val title = "Settings"
}

object TransferConfirmationDestination : Destination {
    override val route: String = "transfer_confirmation"
    override val title: String = "Transfer confirmation"
}

object TransferProgressDestination : Destination {
    override val route: String = "transfer_progress_destination"
    override val title: String = "Transfer progress destination"
}


val bottomTabRowScreens = listOf(TransfersDestination, HistoryDestination, SettingsDestination)