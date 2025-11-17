package dev.arkbuilders.drop.app.presentation.profile

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import dev.arkbuilders.components.about.presentation.ArkAbout
import dev.arkbuilders.drop.app.R
import dev.arkbuilders.drop.app.domain.model.BuildConfigFields
import dev.arkbuilders.drop.app.presentation.components.DropTopBarBack
import org.koin.compose.koinInject

@Composable
fun AboutScreen(navController: NavController) {
    val fields = koinInject<BuildConfigFields>()

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
            versionName = fields.versionName,
            privacyPolicyUrl = "",
        )
    }
}
