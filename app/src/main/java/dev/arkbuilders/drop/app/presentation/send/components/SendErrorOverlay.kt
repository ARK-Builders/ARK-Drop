package dev.arkbuilders.drop.app.presentation.send.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.arkbuilders.drop.app.presentation.send.SendException

@Composable
fun SendErrorOverlay(
    error: SendException, onDismiss: () -> Unit, onAction: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null
            ) { onDismiss() }, contentAlignment = Alignment.Center
    ) {
        SendCard(
            modifier = Modifier
                .padding(20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) { /* Prevent dismiss on card click */ }) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = error.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = error.title, style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ), color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (error.isRecoverable && error.actionLabel != null) {
                        SendButton(
                            onClick = { onAction(error.actionLabel) },
                            variant = ButtonVariant.Primary,
                            size = ButtonSize.Medium
                        ) {
                            Text(
                                error.actionLabel, fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    SendButton(
                        onClick = onDismiss,
                        variant = ButtonVariant.Secondary,
                        size = ButtonSize.Medium
                    ) {
                        Text(
                            "Dismiss", fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}