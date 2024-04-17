package com.arkbuilders.arkdrop.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.compose.rememberNavController
import com.arkbuilders.arkdrop.ui.theme.BlueDark600
import com.hieuwu.gofocus.presentation.navigation.navigateSingleTopTo

@Composable
fun BottomTab(currentDestination: NavDestination?, navController: NavController) {
    NavigationBar(
        containerColor = Color.White
    ) {
        bottomTabRowScreens.forEach { screen ->
            val isSelected = currentDestination?.route == screen.route
            val tabTintColor by animateColorAsState(
                targetValue = if (isSelected) BlueDark600 else
                    Color.LightGray
            )
            val bottomTabIcon = if (isSelected) screen.activeIcon else screen.inActiveIcon
            NavigationBarItem(
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = tabTintColor,
                    selectedTextColor = tabTintColor,
                ),
                selected = isSelected,
                onClick = { navController.navigateSingleTopTo(screen.route) },
                label = {
                    Text(
                        text = screen.title,
                    )
                },
                icon = {
                    Icon(
                        imageVector = bottomTabIcon,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

@Preview
@Composable
fun BottomTabPreview() {
    BottomTab(currentDestination = null, navController = rememberNavController())
}