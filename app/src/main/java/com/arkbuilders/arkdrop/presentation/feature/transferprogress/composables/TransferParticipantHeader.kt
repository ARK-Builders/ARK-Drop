package com.arkbuilders.arkdrop.presentation.feature.transferprogress.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkbuilders.arkdrop.R
import com.arkbuilders.arkdrop.ui.theme.BlueDark600

@Composable
fun TransferParticipantHeader(modifier: Modifier = Modifier) {
    Column {
        Spacer(modifier = modifier.height(24.dp))
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(
                space = (-24).dp,
                alignment = Alignment.CenterHorizontally
            ),
        ) {
            items(listOf(0, 1)) { avatarItem ->
                Image(
                    painter = painterResource(id = R.drawable.avatar_mock),
                    contentDescription = null,
                    modifier = modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
            }
        }
        Spacer(modifier = modifier.height(24.dp))
        Text(
            text = "Wait a moment while transferring...",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = modifier.fillMaxWidth()
        )
        Row(
            modifier = modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Sending to")
            Spacer(modifier = modifier.width(6.dp))
            Text(
                text = "Bob",
                color = BlueDark600,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Preview
@Composable
fun PreviewTransferParticipantHeader() {
    TransferParticipantHeader()
}