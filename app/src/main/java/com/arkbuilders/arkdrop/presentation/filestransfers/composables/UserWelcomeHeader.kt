package com.arkbuilders.arkdrop.presentation.filestransfers.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkbuilders.arkdrop.R

@Composable
fun UserWelcomeHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Hi, Alice")
            Text(
                "Welcome back!",
                style = MaterialTheme.typography.titleMedium
            )
        }
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = modifier
                .size(64.dp)
                .clip(CircleShape)
        )
    }
}

@Preview
@Composable
fun PreviewUserWelcomeHeader() {
    UserWelcomeHeader()
}