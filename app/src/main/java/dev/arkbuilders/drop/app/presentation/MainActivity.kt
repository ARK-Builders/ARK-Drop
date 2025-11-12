package dev.arkbuilders.drop.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import dagger.hilt.android.AndroidEntryPoint
import dev.arkbuilders.drop.app.data.repository.TransferManager
import dev.arkbuilders.drop.app.domain.repository.ProfileRepo
import dev.arkbuilders.drop.app.domain.repository.TransferHistoryItemRepository
import dev.arkbuilders.drop.app.presentation.history.History
import dev.arkbuilders.drop.app.presentation.home.Home
import dev.arkbuilders.drop.app.presentation.navigation.DropDestination
import dev.arkbuilders.drop.app.presentation.profile.EditProfileEnhanced
import dev.arkbuilders.drop.app.presentation.receive.Receive
import dev.arkbuilders.drop.app.presentation.send.Send
import dev.arkbuilders.drop.app.presentation.theme.DropTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var transferManager: TransferManager

    @Inject
    lateinit var profileRepo: ProfileRepo

    @Inject
    lateinit var transferHistoryItemRepository: TransferHistoryItemRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DropTheme {
                Scaffold(
                    modifier =
                        Modifier
                            .fillMaxSize(),
                ) { innerPadding ->
                    DropNavigation(
                        modifier =
                            Modifier
                                .padding(innerPadding),
                        transferManager = transferManager,
                        profileRepo = profileRepo,
                        transferHistoryItemRepository = transferHistoryItemRepository,
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
    profileRepo: ProfileRepo,
    transferHistoryItemRepository: TransferHistoryItemRepository,
) {
    NavHost(
        navController = navController,
        startDestination = DropDestination.Home.route,
        modifier = modifier,
    ) {
        composable(DropDestination.Home.route) {
            Home(
                navController = navController,
                profileRepo = profileRepo,
                transferHistoryItemRepository = transferHistoryItemRepository,
            )
        }
        composable(DropDestination.Send.route) {
            Send(
                navController = navController,
                transferManager = transferManager,
            )
        }
        composable(
            DropDestination.Receive.route,
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern = DropDestination.Receive.DEEP_LINK_PATTERN
                    },
                ),
        ) {
            Receive(
                navController = navController,
            )
        }
        composable(DropDestination.History.route) {
            History(
                navController = navController,
                transferHistoryItemRepository = transferHistoryItemRepository,
            )
        }
        composable(DropDestination.EditProfile.route) {
            EditProfileEnhanced(
                navController = navController,
            )
        }
    }
}
