package com.arkbuilders.arkdrop.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.compose.rememberNavController
import com.hieuwu.gofocus.presentation.navigation.navigateSingleTopTo

@Composable
fun BottomTab(currentDestination: NavDestination?, navController: NavController) {
    NavigationBar {
        bottomTabRowScreens.forEach { screen ->
            val isSelected = currentDestination?.route == screen.route
            val tabTintColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else
                    MaterialTheme.colorScheme.onBackground
            )
            val bottomTabIcon = if (isSelected) screen.activeIcon else screen.inActiveIcon
            NavigationBarItem(
                selected = isSelected,
                onClick = { navController.navigateSingleTopTo(screen.route) },
                label = {
                    Text(
                        text = screen.title,
                        color = tabTintColor
                    )
                },
                icon = {
                    Icon(
                        imageVector = bottomTabIcon,
                        contentDescription = null,
                        tint = tabTintColor
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