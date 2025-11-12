package dev.arkbuilders.drop.app.presentation.send.components.phase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.FileText
import compose.icons.tablericons.Plus
import dev.arkbuilders.drop.app.presentation.DisplayUtils.formatBytes
import dev.arkbuilders.drop.app.presentation.send.components.ButtonSize
import dev.arkbuilders.drop.app.presentation.send.components.ButtonVariant
import dev.arkbuilders.drop.app.presentation.send.components.SendButton
import dev.arkbuilders.drop.app.presentation.send.components.SendCard
import dev.arkbuilders.drop.app.presentation.send.components.SendEmptyState
import dev.arkbuilders.drop.app.presentation.send.components.SendFileItem
import dev.arkbuilders.drop.app.presentation.send.components.SendInstructionsCard

@Composable
fun FileSelectionPhase(
    selectedFiles: List<String>,
    totalFileSize: Long,
    onAddFiles: () -> Unit,
    onRemoveFile: (String) -> Unit,
    onStartTransfer: () -> Unit,
    canStartTransfer: Boolean,
    listState: LazyListState,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // File selection section
        item {
            SendCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Selected Files",
                                style =
                                    MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            if (selectedFiles.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${selectedFiles.size} file${
                                        if (selectedFiles.size != 1) "s" else ""
                                    } • ${
                                        formatBytes(
                                            totalFileSize,
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        SendButton(
                            onClick = onAddFiles,
                            variant = ButtonVariant.Secondary,
                            size = ButtonSize.Medium,
                        ) {
                            Icon(
                                TablerIcons.Plus,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Add Files",
                                style =
                                    MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                    ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (selectedFiles.isEmpty()) {
                        SendEmptyState(
                            title = "No Files Selected",
                            description = "Tap 'Add Files' to choose files you want to send.",
                            icon = TablerIcons.FileText,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(selectedFiles) { uri ->
                                SendFileItem(
                                    uri = uri,
                                    onRemove = { onRemoveFile(uri) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Transfer button
        item {
            SendButton(
                onClick = onStartTransfer,
                variant = ButtonVariant.Primary,
                size = ButtonSize.Large,
                enabled = canStartTransfer,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
            ) {
                Text(
                    text =
                        when {
                            selectedFiles.isEmpty() -> "Select Files First"
                            else ->
                                "Send ${selectedFiles.size} File${
                                    if (selectedFiles.size != 1) "s" else ""
                                }"
                        },
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                )
            }
        }

        // Instructions
        item {
            SendInstructionsCard()
        }
    }
}
