package com.arkbuilders.arkdrop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.arkbuilders.arkdrop.navigation.BottomTab
import com.arkbuilders.arkdrop.navigation.TransfersDestination
import com.arkbuilders.arkdrop.ui.theme.ARKDropTheme
import com.arkbuilders.arkdrop.navigation.navRegistration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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