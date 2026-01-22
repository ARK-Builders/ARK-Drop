package dev.arkbuilders.drop.presentation.send.components.phase

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.arkbuilders.drop.presentation.send.components.ButtonSize
import dev.arkbuilders.drop.presentation.send.components.ButtonVariant
import dev.arkbuilders.drop.presentation.send.components.SendButton
import dev.arkbuilders.drop.presentation.send.components.SendCard
import dev.arkbuilders.drop.presentation.send.components.SendLoadingIndicator

@Composable
fun GeneratingQRPhase(onCancel: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        SendCard {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SendLoadingIndicator(
                    message = "Generating QR Code...",
                )

                Spacer(modifier = Modifier.height(32.dp))

                SendButton(
                    onClick = onCancel,
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
