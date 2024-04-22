package com.arkbuilders.arkdrop.presentation.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkbuilders.arkdrop.presentation.feature.settings.composables.SettingsHeader
import com.arkbuilders.arkdrop.ui.theme.Background
import com.arkbuilders.arkdrop.ui.theme.LightBlack

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsHeader()
        settingsItemList.map { item ->
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 8.dp
                    )
                    .clickable {

                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = LightBlack,
                )
                Spacer(modifier = modifier.width(ButtonDefaults.IconSpacing))
                Text(
                    text = item.text,
                    color = LightBlack,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private data class SettingsItem(
    val icon: ImageVector,
    val text: String
)

private val settingsItemList = listOf(
    SettingsItem(
        icon = Icons.AutoMirrored.Outlined.TextSnippet,
        text = "Terms of service"
    ),
    SettingsItem(
        icon = Icons.Outlined.PrivacyTip,
        text = "Terms of service"
    ),
    SettingsItem(
        icon = Icons.Outlined.Star,
        text = "Rate Us"
    ),
    SettingsItem(
        icon = Icons.Outlined.Quiz,
        text = "Feedback"
    )
)

@Preview
@Composable
fun PreviewSettingsScreen() {
    SettingsScreen()
}