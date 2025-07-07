package dev.arkbuilders.drop.app.ui.components.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun DropBottomNavigation(
    navController: NavController,
    currentRoute: String? = null
) {
    val items = BottomNavigationItems.getItems(currentRoute)
    
    NavigationBar(
        containerColor = Color.White,
        contentColor = Color.Gray
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (item.isSelected) Color(0xFF4285F4) else Color.Gray
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = if (item.isSelected) Color(0xFF4285F4) else Color.Gray,
                        fontSize = 12.sp
                    )
                },
                selected = item.isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF4285F4),
                    selectedTextColor = Color(0xFF4285F4),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}