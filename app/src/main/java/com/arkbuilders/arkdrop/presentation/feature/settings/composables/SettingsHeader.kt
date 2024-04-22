package com.arkbuilders.arkdrop.presentation.feature.settings.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkbuilders.arkdrop.R
import com.arkbuilders.arkdrop.ui.theme.Background
import com.arkbuilders.arkdrop.ui.theme.BlueDark600

@Composable
fun SettingsHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                BlueDark600
            )
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Spacer(modifier = modifier.height(24.dp))
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Background.copy(alpha = 0.2f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.avatar_mock),
                contentDescription = null,
                modifier = modifier
                    .size(64.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = modifier.width(12.dp))
            Text(
                text = "Gillbert",
                modifier = modifier.weight(1.0f),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
            Icon(
                modifier = modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .padding(4.dp),
                imageVector = Icons.Filled.Edit,
                contentDescription = null
            )
        }
    }
}

@Preview
@Composable
fun PreviewSettingsHeader() {
    SettingsHeader()
}