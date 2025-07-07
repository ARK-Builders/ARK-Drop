package dev.arkbuilders.drop.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import dev.arkbuilders.drop.app.FileChunk
import dev.arkbuilders.drop.app.FileManager
import dev.arkbuilders.drop.app.ui.home.Home
import dev.arkbuilders.drop.app.ui.profile.EditProfile
import dev.arkbuilders.drop.app.ui.receive.Receive
import dev.arkbuilders.drop.app.ui.send.Send
//import dev.arkbuilders.drop.app.ui.history.History
import dev.arkbuilders.drop.app.ui.settings.Settings

@Composable
fun DropNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    fileManager: FileManager
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = DropDestination.Home.route
    ) {
        composable(DropDestination.Home.route) {
            Home(navController = navController)
        }
        
        composable(DropDestination.Send.route) {
            Send(navController = navController)
        }
        
        composable(DropDestination.History.route) {
//            History(navController = navController)
        }
        
        composable(DropDestination.Settings.route) {
            Settings(navController = navController)
        }

        composable(DropDestination.Settings.route) {
            Settings(navController = navController)
        }

        composable(DropDestination.EditProfile.route) {
            EditProfile(navController = navController)
        }

        composable(
            route = DropDestination.Receive.route,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = DropDestination.Receive.deepLinkPattern
                }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            val ticket = args?.getString("ticket")
            val confirmations = args?.getString("confirmations")
                ?.split(",")
                ?.mapNotNull { it.toUByteOrNull() }
                ?: emptyList()
            
            Receive(
                ticket = ticket,
                confirmations = confirmations,
                onBack = { navController.popBackStack() },
                onReceive = { chunks ->
                    fileManager.saveReceivedChunks(chunks.map { it -> FileChunk(it.name, it.data) })
                },
                onScanQRCode = { deepLink ->
                    navController.navigate(deepLink)
                }
            )
        }
    }
}