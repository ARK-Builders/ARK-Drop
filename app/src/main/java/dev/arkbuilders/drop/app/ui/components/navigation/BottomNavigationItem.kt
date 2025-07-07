package dev.arkbuilders.drop.app.ui.components.navigation

import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavigationItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
    val isSelected: Boolean = false
)