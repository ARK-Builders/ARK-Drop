package dev.arkbuilders.drop.app.presentation.send.components.phase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.arkbuilders.drop.app.presentation.send.SendException
import dev.arkbuilders.drop.app.presentation.send.components.ButtonSize
import dev.arkbuilders.drop.app.presentation.send.components.ButtonVariant
import dev.arkbuilders.drop.app.presentation.send.components.SendButton
import dev.arkbuilders.drop.app.presentation.send.components.SendCard

@Composable
fun ErrorPhase(
    error: SendException?,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            error?.let { err ->
                SendErrorCard(
                    error = err,
                    onAction = onRetry,
                )
            }

            SendButton(
                onClick = onCancel,
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Cancel",
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                )
            }
        }
    }
}

@Composable
private fun SendErrorCard(
    error: SendException,
    onAction: () -> Unit,
) {
    SendCard(
        backgroundColor = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = error.toMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            SendButton(
                onClick = onAction,
                variant = ButtonVariant.Primary,
                size = ButtonSize.Medium,
            ) {
                Text(
                    "Retry",
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private fun SendException.toMessage() =
    when (this) {
        SendException.TransferInitializationFailed -> "Transfer initialization failed"
        SendException.QRGenerationFailed -> "QR generation failed"
        SendException.TransferInterrupted -> "Transfer interrupted"
    }
