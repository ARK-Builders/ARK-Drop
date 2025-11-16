package dev.arkbuilders.drop.app.presentation.send

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import compose.icons.TablerIcons
import compose.icons.tablericons.Copy
import compose.icons.tablericons.Qrcode
import dev.arkbuilders.drop.app.presentation.components.DropErrorCard
import dev.arkbuilders.drop.app.presentation.components.DropTopBarBack
import dev.arkbuilders.drop.app.presentation.send.components.ButtonSize
import dev.arkbuilders.drop.app.presentation.send.components.ButtonVariant
import dev.arkbuilders.drop.app.presentation.send.components.SendButton
import dev.arkbuilders.drop.app.presentation.send.components.SendLoadingIndicator
import dev.arkbuilders.drop.app.presentation.send.components.phase.FileSelectionPhase
import dev.arkbuilders.drop.app.presentation.send.components.phase.GeneratingQRPhase
import dev.arkbuilders.drop.app.presentation.send.components.phase.TransferCompletePhase
import dev.arkbuilders.drop.app.presentation.send.components.phase.TransferringPhase
import dev.arkbuilders.drop.app.presentation.send.components.phase.WaitingForReceiverPhase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Send(navController: NavController) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val viewModel: SendViewModel = koinInject()

    val state by viewModel.collectAsState()

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents(),
        ) { uris ->
            viewModel.onFilesAdded(uris.map { it.toString() })
        }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            SendScreenEffect.LaunchFilePicker -> {
                filePickerLauncher.launch("*/*")
            }

            SendScreenEffect.NavigateBack -> {
                navController.popBackStack()
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize(),
    ) {
        DropTopBarBack(
            title = "Send files",
            onBackClick = { navController.navigateUp() },
        )

        val sendScreenState = state
        when (sendScreenState) {
            is SendScreenState.FileSelection -> {
                FileSelectionPhase(
                    selectedFiles = sendScreenState.files,
                    totalFileSize = sendScreenState.size,
                    onAddFiles = {
                        viewModel.onAddFiles()
                    },
                    onRemoveFile = { uri ->
                        viewModel.onFileRemove(uri)
                    },
                    onStartTransfer = {
                        viewModel.onStartTransfer()
                    },
                    canStartTransfer = sendScreenState.canStartTransfer,
                    listState = listState,
                )
            }

            is SendScreenState.GeneratingQR -> {
                GeneratingQRPhase(onCancel = { viewModel.onCancelQrGeneration() })
            }

            is SendScreenState.WaitingForReceiver -> {
                WaitingForReceiverPhase(
                    fileCount = sendScreenState.files.size,
                    onCancel = { viewModel.onCancelTransfer() },
                )
            }

            is SendScreenState.Transfer -> {
                TransferringPhase(
                    progress = sendScreenState,
                    onCancel = { viewModel.onCancelTransfer() },
                )
            }

            is SendScreenState.Complete -> {
                TransferCompletePhase(
                    fileCount = sendScreenState.files.size,
                    onSendMore = {
                        viewModel.onSendMore()
                    },
                    onDone = {
                        viewModel.onDone()
                    },
                )
            }

            is SendScreenState.Error -> {
                DropErrorCard(
                    message = sendScreenState.error.toMessage(),
                    onRetry = {
                        viewModel.onErrorRetry()
                    },
                    onDismiss = {
                        viewModel.onErrorDismiss()
                    },
                )
            }
        }

        if (sendScreenState is SendScreenState.WaitingForReceiver) {
            SendQRDialog(
                qrBitmap = sendScreenState.qrBitmap,
                fileCount = sendScreenState.files.size,
                copyString = sendScreenState.copyString,
                onDismiss = { },
                onCancel = {},
            )
        }
    }
}

private fun copyToClipboard(
    context: Context,
    text: String,
    label: String = "Transfer Info",
) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText(label, text)
    clipboardManager.setPrimaryClip(clipData)
}

@Composable
private fun SendQRDialog(
    qrBitmap: Bitmap,
    fileCount: Int,
    copyString: String?,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCopySuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    TablerIcons.Qrcode,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "QR Code for Transfer",
                    style =
                        MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR code for file transfer",
                        modifier =
                            Modifier
                                .size(220.dp)
                                .padding(16.dp),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Show this QR code to the receiver",
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$fileCount file${if (fileCount != 1) "s" else ""} ready to transfer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )

                // Copy functionality
                if (!copyString.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    SendButton(
                        onClick = {
                            copyToClipboard(context, copyString, "Transfer Code")
                            showCopySuccess = true
                            scope.launch {
                                delay(2000)
                                showCopySuccess = false
                            }
                        },
                        variant = ButtonVariant.Secondary,
                        size = ButtonSize.Medium,
                    ) {
                        Icon(
                            TablerIcons.Copy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (showCopySuccess) "Copied!" else "Copy Code",
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    if (showCopySuccess) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Transfer code copied to clipboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SendLoadingIndicator()
                    Text(
                        text = "Waiting for receiver to scan...",
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onCancel,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    "Cancel Transfer",
                    fontWeight = FontWeight.Medium,
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

private fun SendException.toMessage() =
    when (this) {
        SendException.TransferInitializationFailed -> "Transfer initialization failed"
        SendException.QRGenerationFailed -> "QR generation failed"
        SendException.TransferInterrupted -> "Transfer interrupted"
    }
