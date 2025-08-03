package dev.arkbuilders.drop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import dagger.hilt.android.AndroidEntryPoint
import dev.arkbuilders.drop.app.data.HistoryRepository
import dev.arkbuilders.drop.app.navigation.DropDestination
import dev.arkbuilders.drop.app.ui.history.History
import dev.arkbuilders.drop.app.ui.home.Home
import dev.arkbuilders.drop.app.ui.profile.EditProfile
import dev.arkbuilders.drop.app.ui.receive.Receive
import dev.arkbuilders.drop.app.ui.send.Send
import dev.arkbuilders.drop.app.ui.theme.DropTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var transferManager: TransferManager

    @Inject
    lateinit var profileManager: ProfileManager

    @Inject
    lateinit var historyRepository: HistoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DropTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                ) { innerPadding ->
                    DropNavigation(
                        modifier = Modifier
                            .padding(innerPadding),
                        transferManager = transferManager,
                        profileManager = profileManager,
                        historyRepository = historyRepository
                    )
                }
            }
        }
    }
}

@Composable
fun DropNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    transferManager: TransferManager,
    profileManager: ProfileManager,
    historyRepository: HistoryRepository
) {
    NavHost(
        navController = navController,
        startDestination = DropDestination.Home.route,
        modifier = modifier
    ) {
        composable(DropDestination.Home.route) {
            Home(
                navController = navController,
                profileManager = profileManager
            )
        }
        composable(DropDestination.Send.route) {
            Send(
                navController = navController,
                transferManager = transferManager
            )
        }
        composable(
            DropDestination.Receive.route,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = DropDestination.Receive.DEEP_LINK_PATTERN
                }
            )
        ) {
            Receive(
                navController = navController,
                transferManager = transferManager
            )
        }
        composable(DropDestination.History.route) {
            History(
                navController = navController,
                historyRepository = historyRepository
            )
        }
        composable(DropDestination.EditProfile.route) {
            EditProfile(
                navController = navController,
                profileManager = profileManager
            )
        }
    }
}
