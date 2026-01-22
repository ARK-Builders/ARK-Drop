package dev.arkbuilders.drop.presentation.send.components.phase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.arkbuilders.drop.presentation.DisplayUtils.formatBytes
import dev.arkbuilders.drop.presentation.DisplayUtils.formatDuration
import dev.arkbuilders.drop.presentation.components.AvatarImageWithFallback
import dev.arkbuilders.drop.presentation.send.SendScreenState
import dev.arkbuilders.drop.presentation.send.components.SendCard
import dev.arkbuilders.drop.presentation.send.components.SendProgressBar

@Composable
fun TransferringPhase(
    progress: SendScreenState.Transfer,
    onCancel: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SendCard(
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Sending Files",
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )

                    IconButton(
                        onClick = onCancel,
                        modifier =
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel transfer",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                if (progress.receiverName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AvatarImageWithFallback(
                            avatarB64 = progress.receiverAvatar,
                            fallbackText = progress.receiverName,
                            size = 32.dp,
                        )

                        Text(
                            text = "Connected to: ${progress.receiverName}",
                            style = MaterialTheme.typography.bodyLarge,
                            color =
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    alpha = 0.8f,
                                ),
                        )
                    }
                }

                if (progress.currentFileName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Sending: ${progress.currentFileName}",
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    val progressValue =
                        if (progress.totalBytes > 0) {
                            (progress.bytesTransferred.toFloat() / progress.totalBytes.toFloat())
                                .coerceIn(
                                    0f,
                                    1f,
                                )
                        } else {
                            0f
                        }

                    Spacer(modifier = Modifier.height(16.dp))

                    SendProgressBar(
                        progress = progressValue,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${
                                formatBytes(
                                    progress.bytesTransferred,
                                )
                            } / ${formatBytes(progress.totalBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    alpha = 0.7f,
                                ),
                        )

                        if (progress.transferSpeedBps > 0) {
                            Text(
                                text = "${formatBytes(progress.transferSpeedBps)}/s",
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                        alpha = 0.7f,
                                    ),
                            )
                        }
                    }

                    if (progress.estimatedTimeRemaining > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Time remaining: ${
                                formatDuration(
                                    progress.estimatedTimeRemaining,
                                )
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    alpha = 0.6f,
                                ),
                        )
                    }
                }
            }
        }
    }
}
