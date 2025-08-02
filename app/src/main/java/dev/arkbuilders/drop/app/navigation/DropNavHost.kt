package dev.arkbuilders.drop.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import dev.arkbuilders.drop.app.ProfileManager
import dev.arkbuilders.drop.app.TransferManager
import dev.arkbuilders.drop.app.ui.history.History
import dev.arkbuilders.drop.app.ui.home.Home
import dev.arkbuilders.drop.app.ui.profile.EditProfile
import dev.arkbuilders.drop.app.ui.receive.Receive
import dev.arkbuilders.drop.app.ui.send.Send
import dev.arkbuilders.drop.app.ui.settings.Settings

@Composable
fun DropNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    profileManager: ProfileManager,
    transferManager: TransferManager,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = DropDestination.Home.route
    ) {
        composable(DropDestination.Home.route) {
            Home(navController = navController, profileManager = profileManager)
        }

        composable(DropDestination.Send.route) {
            Send(navController = navController, transferManager = transferManager)
        }

        composable(DropDestination.History.route) {
            History(navController = navController)
        }

        composable(DropDestination.Settings.route) {
            Settings(navController = navController, profileManager = profileManager)
        }

        composable(DropDestination.EditProfile.route) {
            EditProfile(navController = navController, profileManager = profileManager)
        }

        composable(
            route = DropDestination.Receive.route,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = DropDestination.Receive.deepLinkPattern
                }
            )
        ) { backStackEntry ->
            Receive(
                navController = navController,
                transferManager = transferManager,
            )
        }
    }
}
