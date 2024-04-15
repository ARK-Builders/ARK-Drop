package com.arkbuilders.arkdrop.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.ui.graphics.vector.ImageVector


interface Destination {
    val route: String
    val title: String
}

interface BottomTabDestination : Destination {
    val activeIcon: ImageVector
    val inActiveIcon: ImageVector
}

object TimerDestination : BottomTabDestination {
    // Put information needed to navigate here
    override val route = "timer"
    override val activeIcon = Icons.Filled.CheckCircle
    override val inActiveIcon: ImageVector = Icons.Outlined.Check
    override val title = "Timer"
}

object TimerReportDestination : BottomTabDestination {
    override val route = "timer_report"
    override val activeIcon = Icons.Filled.CheckCircle
    override val inActiveIcon: ImageVector = Icons.Outlined.Check
    override val title = "Reports"
}

object TimerReportDestination2 : BottomTabDestination {
    override val route = "timer_report"
    override val activeIcon = Icons.Filled.CheckCircle
    override val inActiveIcon: ImageVector = Icons.Outlined.Check
    override val title = "Reports"
}

val bottomTabRowScreens = listOf(TimerDestination, TimerReportDestination2)
