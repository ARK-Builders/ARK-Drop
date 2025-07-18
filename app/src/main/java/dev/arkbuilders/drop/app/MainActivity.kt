package dev.arkbuilders.drop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.arkbuilders.drop.app.navigation.DropNavHost
import dev.arkbuilders.drop.app.ui.theme.DropTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var fileManager: FileManager

    @Inject
    lateinit var profileManager: ProfileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            DropTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DropNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        fileManager = fileManager,
                        profileManager = profileManager
                    )
                }
            }
        }
    }
}