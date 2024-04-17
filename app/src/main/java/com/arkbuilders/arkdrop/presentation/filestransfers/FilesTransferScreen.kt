package com.arkbuilders.arkdrop.presentation.filestransfers

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkbuilders.arkdrop.R
import com.arkbuilders.arkdrop.presentation.filestransfers.composables.UserWelcomeHeader
import com.arkbuilders.arkdrop.ui.theme.BlueDark600

@Composable
fun FilesTransferScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFCFCFD))
    ) {
        UserWelcomeHeader(modifier = modifier)
        HorizontalDivider(
            color = Color.LightGray,
            modifier = modifier
                .height(1.dp)
                .fillMaxWidth()
        )
        Spacer(modifier = modifier.height(64.dp))
        Image(
            modifier = modifier
                .fillMaxWidth()
                .height(256.dp),
            painter = painterResource(id = R.drawable.transfer_background),
            contentDescription = null,
        )
        Text(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            text = "Seamless to transfer your files",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = modifier.height(8.dp))
        Text(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            textAlign = TextAlign.Center,
            text = "Simple, fast, and limitless start sharing your files now.",
        )
        Row(
            modifier = modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                modifier = modifier
                    .weight(1.0f)
                    .padding(8.dp),
                onClick = { /*TODO*/ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueDark600,
                ),
            ) {
                Icon(imageVector = Icons.Filled.ArrowCircleUp, contentDescription = null)
                Text("Send")
            }
            Button(
                modifier = modifier
                    .weight(1.0f)
                    .padding(8.dp),
                onClick = { /*TODO*/ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueDark600,
                ),
            ) {
                Icon(imageVector = Icons.Filled.ArrowCircleDown, contentDescription = null)
                Text("Receive")
            }
        }
    }
}

@Preview
@Composable
fun PreviewFilesTransferScreen() {
    FilesTransferScreen()
}