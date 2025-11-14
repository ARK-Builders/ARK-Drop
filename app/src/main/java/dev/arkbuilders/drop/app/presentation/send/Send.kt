package dev.arkbuilders.drop.app.presentation.send

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import compose.icons.TablerIcons
import compose.icons.tablericons.AlertCircle
import compose.icons.tablericons.Copy
import compose.icons.tablericons.Qrcode
import dev.arkbuilders.drop.app.data.repository.TransferManager
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

// Comprehensive exception handling
sealed class SendException(
    val title: String,
    val message: String,
    val icon: ImageVector,
    val isRecoverable: Boolean = true,
    val actionLabel: String? = null,
) {
    object NetworkUnavailable : SendException(
        title = "No Network Connection",
        message = "Please check your Wi-Fi or mobile data connection and try again.",
        icon = Icons.Default.Warning,
        actionLabel = "Retry",
    )

    object FileTooLarge : SendException(
        title = "File Too Large",
        message =
            "Some files exceed the 2GB limit and were skipped." +
                " You can send the remaining files.",
        icon = Icons.Default.Warning,
        actionLabel = "Continue",
    )

    object NoFilesSelected : SendException(
        title = "No Files Selected",
        message = "Please select at least one file to send.",
        icon = Icons.Default.Warning,
        isRecoverable = false,
    )

    object TransferInitializationFailed : SendException(
        title = "Transfer Setup Failed",
        message = "Unable to prepare files for transfer. Please try again.",
        icon = TablerIcons.AlertCircle,
        actionLabel = "Retry",
    )

    object QRGenerationFailed : SendException(
        title = "QR Code Generation Failed",
        message = "Unable to create QR code. Please restart the transfer.",
        icon = TablerIcons.AlertCircle,
        actionLabel = "Retry",
    )

    object TransferInterrupted : SendException(
        title = "Transfer Interrupted",
        message = "The connection was lost during transfer. You can try sending again.",
        icon = TablerIcons.AlertCircle,
        actionLabel = "Retry",
    )

    object ReceiverDisconnected : SendException(
        title = "Receiver Disconnected",
        message = "The receiving device disconnected. Please try again.",
        icon = Icons.Default.Warning,
        actionLabel = "Retry",
    )

    class UnknownError(details: String) : SendException(
        title = "Something Went Wrong",
        message = "An unexpected error occurred: $details",
        icon = TablerIcons.AlertCircle,
        actionLabel = "Retry",
    )
}

data class TransferProgressState(
    val isConnected: Boolean = false,
    val receiverName: String = "",
    val receiverAvatar: String? = null,
    val currentFileName: String = "",
    val filesCompleted: Int = 0,
    val totalFiles: Int = 0,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val transferSpeedBps: Long = 0L,
    val estimatedTimeRemaining: Long = 0L,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Send(
    navController: NavController,
    transferManager: TransferManager,
) {
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

    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .windowInsetsPadding(WindowInsets.ime),
        topBar = {
            DropTopBarBack(
                title = "Send files",
                onBackClick = { navController.navigateUp() },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
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
//                    ErrorPhase(
//                        error = sendState.error,
//                        onRetry = { handleError("Retry") },
//                        onCancel = {
//                            transferManager.cancelSend()
//                            navController.navigateUp()
//                        }
//                    )
                }
            }

//            sendState.error?.let { error ->
//                if (sendState.phase != SendPhase.Error) {
//                    SendErrorOverlay(
//                        error = error,
//                        onDismiss = { sendState = sendState.copy(error = null) },
//                        onAction = { action -> handleError(action) })
//                }
//            }

            val s = state
            if (s is SendScreenState.WaitingForReceiver) {
                SendQRDialog(
                    qrBitmap = s.qrBitmap,
                    fileCount = s.files.size,
                    copyString = s.copyString,
                    onDismiss = { },
                    onCancel = {},
                )
            }
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
