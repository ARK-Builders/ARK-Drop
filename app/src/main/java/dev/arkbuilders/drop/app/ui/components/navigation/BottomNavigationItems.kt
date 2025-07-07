package dev.arkbuilders.drop.app.ui.components.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import compose.icons.SimpleIcons
import compose.icons.TablerIcons
import compose.icons.tablericons.History
import compose.icons.tablericons.Home
import compose.icons.tablericons.Send
import compose.icons.tablericons.Share
import dev.arkbuilders.drop.app.navigation.DropDestination

object BottomNavigationItems {
    fun getItems(currentRoute: String?): List<BottomNavigationItem> {
        return listOf(
            BottomNavigationItem(
                route = DropDestination.Home.route,
                icon = TablerIcons.Home,
                label = "Home",
                isSelected = currentRoute == DropDestination.Home.route
            ),
            BottomNavigationItem(
                route = DropDestination.History.route,
                icon = TablerIcons.History,
                label = "History",
                isSelected = currentRoute == DropDestination.History.route
            ),
            BottomNavigationItem(
                route = DropDestination.Settings.route,
                icon = Icons.Default.Person,
                label = "Settings",
                isSelected = currentRoute == DropDestination.Settings.route
            )
        )
    }
}