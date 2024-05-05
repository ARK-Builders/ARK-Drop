package dev.arkbuilders.arkdrop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.arkbuilders.arkdrop.presentation.navigation.BottomTab
import dev.arkbuilders.arkdrop.presentation.navigation.TransfersDestination
import dev.arkbuilders.arkdrop.presentation.navigation.navRegistration
import dev.arkbuilders.arkdrop.presentation.permission.PermissionManager
import dev.arkbuilders.arkdrop.ui.theme.ARKDropTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register ActivityResult handler
        val requestPermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                // Handle permission requests results
                // See the permission example in the Android platform samples: https://github.com/android/platform-samples
            }

        PermissionManager.initialize(requestPermissions)
        PermissionManager.requestPermission(baseContext)
        setContent {
            ARKDropTheme {
                // A surface container using the 'background' color from the theme
                val navController = rememberNavController()
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentDestination = currentBackStack?.destination
                val scope = rememberCoroutineScope()

                Scaffold(
                    bottomBar = {
                        BottomTab(
                            navController = navController,
                            currentDestination = currentDestination
                        )
                    }) { innerPadding ->
                    NavHost(
                        navController,
                        startDestination = TransfersDestination.route,
                        Modifier.padding(innerPadding)
                    ) {
                        navRegistration(navController)
                    }
                }

            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ARKDropTheme {
        Greeting("Android")
    }
}