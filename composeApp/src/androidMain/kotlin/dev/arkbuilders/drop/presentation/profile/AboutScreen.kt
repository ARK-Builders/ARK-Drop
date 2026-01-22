package dev.arkbuilders.drop.presentation.profile

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import dev.arkbuilders.components.about.presentation.ArkAbout
import dev.arkbuilders.drop.R
import dev.arkbuilders.drop.presentation.components.DropTopBarBack

@Composable
fun AboutScreen(navController: NavController) {
    Scaffold(
        topBar = {
            DropTopBarBack(
                title = "About",
                onBackClick = { navController.popBackStack() },
            )
        },
    ) {
        ArkAbout(
            modifier = Modifier.padding(it),
            appName = stringResource(id = R.string.app_name),
            appLogoResId = R.drawable.ic_logo,
            versionName = "1.0",
            privacyPolicyUrl = "",
        )
    }
}
