package dev.arkbuilders.drop.app.ui.send

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import compose.icons.TablerIcons
import compose.icons.tablericons.AlertCircle
import compose.icons.tablericons.CloudUpload
import compose.icons.tablericons.FileText
import compose.icons.tablericons.Plus
import compose.icons.tablericons.Qrcode
import dev.arkbuilders.drop.app.TransferManager
import dev.arkbuilders.drop.app.ui.components.DropLogoIcon
import dev.arkbuilders.drop.app.ui.profile.AvatarUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SendPhase {
    FileSelection,
    GeneratingQR,
    WaitingForReceiver,
    Transferring,
    Complete,
    Error
}

// Enhanced UI State with comprehensive error handling
private data class SendState(
    val phase: SendPhase = SendPhase.FileSelection,
    val isLoading: Boolean = false,
    val error: SendException? = null,
    val transferProgress: TransferProgressState? = null,
    val qrBitmap: Bitmap? = null,
    val showQRDialog: Boolean = false,
    val showSuccessAnimation: Boolean = false,
    val successCountdown: Int = 0,
    val networkConnected: Boolean = true
)

// Comprehensive exception handling
sealed class SendException(
    val title: String,
    val message: String,
    val icon: ImageVector,
    val isRecoverable: Boolean = true,
    val actionLabel: String? = null
) {
    object NetworkUnavailable : SendException(
        title = "No Network Connection",
        message = "Please check your Wi-Fi or mobile data connection and try again.",
        icon = Icons.Default.Warning,
        actionLabel = "Retry"
    )

    object FileTooLarge : SendException(
        title = "File Too Large",
        message = "Some files exceed the 2GB limit and were skipped. You can send the remaining files.",
        icon = Icons.Default.Warning,
        actionLabel = "Continue"
    )

    object NoFilesSelected : SendException(
        title = "No Files Selected",
        message = "Please select at least one file to send.",
        icon = Icons.Default.Warning,
        isRecoverable = false
    )

    object TransferInitializationFailed : SendException(
        title = "Transfer Setup Failed",
        message = "Unable to prepare files for transfer. Please try again.",
        icon = TablerIcons.AlertCircle,
        actionLabel = "Retry"
    )

    object QRGenerationFailed : SendException(
        title = "QR Code Generation Failed",
        message = "Unable to create QR code. Please restart the transfer.",
        icon = TablerIcons.AlertCircle,
        actionLabel = "Retry"
    )

    object TransferInterrupted : SendException(
        title = "Transfer Interrupted",
        message = "The connection was lost during transfer. You can try sending again.",
        icon = TablerIcons.AlertCircle,
        actionLabel = "Retry"
    )

    object ReceiverDisconnected : SendException(
        title = "Receiver Disconnected",
        message = "The receiving device disconnected. Please try again.",
        icon = Icons.Default.Warning,
        actionLabel = "Retry"
    )

    class UnknownError(details: String) : SendException(
        title = "Something Went Wrong",
        message = "An unexpected error occurred: $details",
        icon = TablerIcons.AlertCircle,
        actionLabel = "Retry"
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
    val estimatedTimeRemaining: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendEnhanced(
    navController: NavController,
    transferManager: TransferManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    // State management
    var sendState by remember { mutableStateOf(SendState()) }
    var selectedFiles by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }

    // Observe transfer progress with error handling
    val sendProgress by (transferManager.sendProgress?.collectAsState()
        ?: remember { mutableStateOf(null) })

    // Derived states
    val totalFileSize by remember {
        derivedStateOf {
            selectedFiles.sumOf { uri ->
                getFileSize(context, uri)
            }
        }
    }

    val canStartTransfer by remember {
        derivedStateOf {
            selectedFiles.isNotEmpty() &&
                    sendState.phase == SendPhase.FileSelection &&
                    !sendState.isLoading &&
                    sendState.networkConnected
        }
    }

    // File picker with comprehensive validation
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            try {
                val validatedFiles = validateAndFilterFiles(context, uris)

                if (validatedFiles.skippedCount > 0) {
                    sendState = sendState.copy(
                        error = SendException.FileTooLarge
                    )
                }

                selectedFiles = selectedFiles + validatedFiles.validFiles
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

            } catch (e: Exception) {
                sendState = sendState.copy(
                    error = SendException.UnknownError("File validation failed: ${e.message}")
                )
            }
        }
    }

    // Network monitoring
    LaunchedEffect(Unit) {
        while (true) {
            val isConnected = checkNetworkConnection(context)
            if (sendState.networkConnected != isConnected) {
                sendState = sendState.copy(networkConnected = isConnected)

                if (!isConnected && sendState.phase in listOf(SendPhase.WaitingForReceiver, SendPhase.Transferring)) {
                    sendState = sendState.copy(
                        phase = SendPhase.Error,
                        error = SendException.NetworkUnavailable
                    )
                }
            }
            delay(2000)
        }
    }

    // Transfer progress monitoring with error handling
    LaunchedEffect(sendProgress) {
        sendProgress?.let { progress ->
            try {
                val progressState = TransferProgressState(
                    isConnected = progress.isConnected,
                    receiverName = progress.receiverName,
                    receiverAvatar = progress.receiverAvatar,
                    currentFileName = progress.fileName,
                    bytesTransferred = progress.sent.toLong(),
                    totalBytes = (progress.sent + progress.remaining).toLong(),
                    transferSpeedBps = calculateTransferSpeed(progress.sent.toLong()),
                    estimatedTimeRemaining = calculateETA(progress.sent.toLong(), progress.remaining.toLong())
                )

                when {
                    progress.isConnected && sendState.phase == SendPhase.WaitingForReceiver -> {
                        sendState = sendState.copy(
                            phase = SendPhase.Transferring,
                            transferProgress = progressState,
                            showQRDialog = false,
                            error = null
                        )
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }

                    transferManager.isSendFinished() -> {
                        if (sendState.phase != SendPhase.Complete) {
                            sendState = sendState.copy(
                                phase = SendPhase.Complete,
                                showSuccessAnimation = true,
                                successCountdown = 3000,
                                error = null
                            )
                            transferManager.recordSendCompletion(selectedFiles)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }

                    !progress.isConnected && sendState.phase == SendPhase.Transferring -> {
                        sendState = sendState.copy(
                            phase = SendPhase.Error,
                            error = SendException.ReceiverDisconnected
                        )
                    }

                    else -> {
                        sendState = sendState.copy(transferProgress = progressState)
                    }
                }
            } catch (e: Exception) {
                sendState = sendState.copy(
                    phase = SendPhase.Error,
                    error = SendException.UnknownError("Progress monitoring failed: ${e.message}")
                )
            }
        }
    }

    // Success animation countdown
    LaunchedEffect(sendState.successCountdown) {
        if (sendState.successCountdown > 0) {
            delay(1000)
            sendState = sendState.copy(successCountdown = sendState.successCountdown - 1000)
        }
    }

    // Core functions
    val startTransfer = {
        if (canStartTransfer) {
            scope.launch {
                try {
                    sendState = sendState.copy(
                        phase = SendPhase.GeneratingQR,
                        isLoading = true,
                        error = null
                    )

                    val bubble = transferManager.sendFiles(selectedFiles)
                    if (bubble != null) {
                        val ticket = transferManager.getCurrentSendTicket() ?: ""
                        val confirmation = transferManager.getCurrentSendConfirmation() ?: 0u

                        if (ticket.isEmpty()) {
                            throw Exception("Invalid transfer ticket")
                        }

                        val qrBitmap = generateQRCodeSafely(ticket, confirmation)
                        sendState = sendState.copy(
                            phase = SendPhase.WaitingForReceiver,
                            isLoading = false,
                            qrBitmap = qrBitmap,
                            showQRDialog = true
                        )

                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        throw Exception("Transfer initialization returned null")
                    }
                } catch (e: Exception) {
                    sendState = sendState.copy(
                        phase = SendPhase.Error,
                        isLoading = false,
                        error = when {
                            e.message?.contains("QR") == true -> SendException.QRGenerationFailed
                            e.message?.contains("network") == true -> SendException.NetworkUnavailable
                            else -> SendException.TransferInitializationFailed
                        }
                    )
                }
            }
        } else if (selectedFiles.isEmpty()) {
            sendState = sendState.copy(error = SendException.NoFilesSelected)
        } else if (!sendState.networkConnected) {
            sendState = sendState.copy(error = SendException.NetworkUnavailable)
        }
        Unit
    }

    val cancelTransfer = {
        try {
            transferManager.cancelSend()
            sendState = SendState(networkConnected = sendState.networkConnected)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (e: Exception) {
            // Silent fail for cancel operation
        }
    }

    val resetForNewTransfer = {
        selectedFiles = emptyList()
        sendState = SendState(networkConnected = sendState.networkConnected)
        transferManager.cancelSend()
    }

    val handleError = { action: String ->
        when (action) {
            "Retry" -> {
                sendState = sendState.copy(error = null, phase = SendPhase.FileSelection)
            }
            "Continue" -> {
                sendState = sendState.copy(error = null)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.ime),
        topBar = {
            SendTopBar(
                onBackClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigateUp()
                },
                networkConnected = sendState.networkConnected
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content with phase-based transitions
            AnimatedContent(
                targetState = sendState.phase,
                transitionSpec = {
                    slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(animationSpec = tween(300)) togetherWith
                            slideOutVertically(
                                targetOffsetY = { -it / 3 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeOut(animationSpec = tween(200))
                },
                label = "phaseTransition"
            ) { phase ->
                when (phase) {
                    SendPhase.FileSelection -> {
                        FileSelectionPhase(
                            selectedFiles = selectedFiles,
                            totalFileSize = totalFileSize,
                            onAddFiles = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                filePickerLauncher.launch("*/*")
                            },
                            onRemoveFile = { uri ->
                                selectedFiles = selectedFiles - uri
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onStartTransfer = startTransfer,
                            canStartTransfer = canStartTransfer,
                            isLoading = sendState.isLoading,
                            networkConnected = sendState.networkConnected,
                            listState = listState
                        )
                    }

                    SendPhase.GeneratingQR -> {
                        GeneratingQRPhase(onCancel = cancelTransfer)
                    }

                    SendPhase.WaitingForReceiver -> {
                        WaitingForReceiverPhase(
                            fileCount = selectedFiles.size,
                            onCancel = cancelTransfer
                        )
                    }

                    SendPhase.Transferring -> {
                        TransferringPhase(
                            progress = sendState.transferProgress,
                            onCancel = cancelTransfer
                        )
                    }

                    SendPhase.Complete -> {
                        TransferCompletePhase(
                            fileCount = selectedFiles.size,
                            onSendMore = resetForNewTransfer,
                            onDone = { navController.navigateUp() },
                            showSuccessAnimation = sendState.showSuccessAnimation,
                            successCountdown = sendState.successCountdown
                        )
                    }

                    SendPhase.Error -> {
                        ErrorPhase(
                            error = sendState.error,
                            onRetry = { handleError("Retry") },
                            onCancel = { navController.navigateUp() }
                        )
                    }
                }
            }

            sendState.error?.let { error ->
                if (sendState.phase != SendPhase.Error) {
                    SendErrorOverlay(
                        error = error,
                        onDismiss = { sendState = sendState.copy(error = null) },
                        onAction = { action -> handleError(action) }
                    )
                }
            }

            // QR Code Dialog
            if (sendState.showQRDialog && sendState.qrBitmap != null) {
                SendQRDialog(
                    qrBitmap = sendState.qrBitmap!!,
                    fileCount = selectedFiles.size,
                    onDismiss = { sendState = sendState.copy(showQRDialog = false) },
                    onCancel = cancelTransfer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendTopBar(
    onBackClick: () -> Unit,
    networkConnected: Boolean
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DropLogoIcon(
                    size = 24.dp,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Send Files",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .semantics { contentDescription = "Go back" }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        actions = {
            // Network status indicator
            NetworkStatusIndicator(
                connected = networkConnected,
                modifier = Modifier.padding(end = 8.dp)
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun NetworkStatusIndicator(
    connected: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (connected) 0.7f else 1f,
        animationSpec = tween(300),
        label = "networkAlpha"
    )

    Icon(
        imageVector = if (connected) TablerIcons.CloudUpload else Icons.Default.Warning,
        contentDescription = if (connected) "Network connected" else "Network disconnected",
        modifier = modifier
            .size(20.dp)
            .alpha(alpha),
        tint = if (connected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.error
    )
}

@Composable
private fun FileSelectionPhase(
    selectedFiles: List<Uri>,
    totalFileSize: Long,
    onAddFiles: () -> Unit,
    onRemoveFile: (Uri) -> Unit,
    onStartTransfer: () -> Unit,
    canStartTransfer: Boolean,
    isLoading: Boolean,
    networkConnected: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // File selection section
        item {
            SendCard {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Selected Files",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (selectedFiles.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${selectedFiles.size} file${if (selectedFiles.size != 1) "s" else ""} • ${formatBytes(totalFileSize)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        SendButton(
                            onClick = onAddFiles,
                            variant = ButtonVariant.Secondary,
                            size = ButtonSize.Medium
                        ) {
                            Icon(
                                TablerIcons.Plus,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Add Files",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (selectedFiles.isEmpty()) {
                        SendEmptyState(
                            title = "No Files Selected",
                            description = "Tap 'Add Files' to choose files you want to send.",
                            icon = TablerIcons.FileText
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(selectedFiles) { uri ->
                                SendFileItem(
                                    uri = uri,
                                    onRemove = { onRemoveFile(uri) }
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
                enabled = canStartTransfer && networkConnected,
                loading = isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (!isLoading) {
                    Icon(
                        TablerIcons.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = when {
                        isLoading -> "Starting Transfer..."
                        !networkConnected -> "No Network Connection"
                        selectedFiles.isEmpty() -> "Select Files First"
                        else -> "Send ${selectedFiles.size} File${if (selectedFiles.size != 1) "s" else ""}"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        // Instructions
        item {
            SendInstructionsCard()
        }
    }
}

@Composable
private fun GeneratingQRPhase(
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        SendCard {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SendLoadingIndicator(
                    message = "Generating QR Code..."
                )

                Spacer(modifier = Modifier.height(32.dp))

                SendButton(
                    onClick = onCancel,
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun WaitingForReceiverPhase(
    fileCount: Int,
    onCancel: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            SendCard {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ready to Send",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$fileCount file${if (fileCount != 1) "s" else ""} ready for transfer",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SendLoadingIndicator()
                        Text(
                            text = "Waiting for receiver to scan...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            SendButton(
                onClick = onCancel,
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Cancel Transfer",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun TransferringPhase(
    progress: TransferProgressState?,
    onCancel: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            progress?.let { p ->
                SendCard(
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sending Files",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            IconButton(
                                onClick = onCancel,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel transfer",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        if (p.receiverName.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AvatarUtils.AvatarImageWithFallback(
                                    base64String = p.receiverAvatar ?: "",
                                    fallbackText = p.receiverName,
                                    size = 32.dp
                                )

                                Text(
                                    text = "Connected to: ${p.receiverName}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        if (p.currentFileName.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Sending: ${p.currentFileName}",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val progressValue = if (p.totalBytes > 0) {
                                (p.bytesTransferred.toFloat() / p.totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            Spacer(modifier = Modifier.height(16.dp))

                            SendProgressBar(
                                progress = progressValue,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${formatBytes(p.bytesTransferred)} / ${formatBytes(p.totalBytes)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )

                                if (p.transferSpeedBps > 0) {
                                    Text(
                                        text = "${formatBytes(p.transferSpeedBps)}/s",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            if (p.estimatedTimeRemaining > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Time remaining: ${formatDuration(p.estimatedTimeRemaining)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferCompletePhase(
    fileCount: Int,
    onSendMore: () -> Unit,
    onDone: () -> Unit,
    showSuccessAnimation: Boolean,
    successCountdown: Int
) {
    val haptic = LocalHapticFeedback.current
    val successScale = remember { Animatable(0f) }

    LaunchedEffect(showSuccessAnimation) {
        if (showSuccessAnimation) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            successScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Success animation
            AnimatedVisibility(
                visible = showSuccessAnimation && successCountdown > 0,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                SendCard(
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.scale(successScale.value)
                ) {
                    Column(
                        modifier = Modifier.padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Transfer Complete!",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "$fileCount file${if (fileCount != 1) "s" else ""} sent successfully",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Action buttons
            AnimatedVisibility(
                visible = !showSuccessAnimation || successCountdown <= 0,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                SendCard(
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Files Sent Successfully!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "$fileCount file${if (fileCount != 1) "s" else ""} transferred successfully",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SendButton(
                                onClick = onSendMore,
                                variant = ButtonVariant.Secondary,
                                size = ButtonSize.Large,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Send More",
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            SendButton(
                                onClick = onDone,
                                variant = ButtonVariant.Primary,
                                size = ButtonSize.Large,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "Done",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorPhase(
    error: SendException?,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            error?.let { err ->
                SendErrorCard(
                    error = err,
                    onAction = if (err.isRecoverable) onRetry else null
                )
            }

            SendButton(
                onClick = onCancel,
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun SendCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp
        )
    ) {
        content()
    }
}

enum class ButtonVariant { Primary, Secondary }
enum class ButtonSize { Medium, Large }

@Composable
private fun SendButton(
    onClick: () -> Unit,
    variant: ButtonVariant,
    size: ButtonSize,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    content: @Composable () -> Unit
) {
    val height = when (size) {
        ButtonSize.Medium -> 44.dp
        ButtonSize.Large -> 56.dp
    }

    when (variant) {
        ButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(height),
                enabled = enabled && !loading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp,
                    disabledElevation = 0.dp
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                content()
            }
        }

        ButtonVariant.Secondary -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = modifier.height(height),
                enabled = enabled && !loading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                content()
            }
        }
    }
}

@Composable
private fun SendFileItem(
    uri: Uri,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var fileName by remember { mutableStateOf("Loading...") }
    var fileSize by remember { mutableStateOf(0L) }

    LaunchedEffect(uri) {
        try {
            val fileInfo = getFileInfo(context, uri)
            fileName = fileInfo.first
            fileSize = fileInfo.second
        } catch (e: Exception) {
            fileName = "Unknown file"
            fileSize = 0L
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                TablerIcons.FileText,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (fileSize > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatBytes(fileSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRemove()
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove file",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SendEmptyState(
    title: String,
    description: String,
    icon: ImageVector
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SendInstructionsCard() {
    SendCard(
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "How to Send Files",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            val instructions = listOf(
                "Select files you want to send",
                "Tap 'Send Files' to generate QR code",
                "Let the receiver scan the QR code",
                "Files transfer automatically"
            )

            instructions.forEachIndexed { index, instruction ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(20.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (index < instructions.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SendLoadingIndicator(
    message: String? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )

        message?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SendProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    )
}

@Composable
private fun SendErrorCard(
    error: SendException,
    onAction: (() -> Unit)? = null
) {
    SendCard(
        backgroundColor = MaterialTheme.colorScheme.errorContainer
    ) {
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
                text = error.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            if (onAction != null && error.actionLabel != null) {
                Spacer(modifier = Modifier.height(20.dp))

                SendButton(
                    onClick = onAction,
                    variant = ButtonVariant.Primary,
                    size = ButtonSize.Medium
                ) {
                    Text(
                        error.actionLabel,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SendErrorOverlay(
    error: SendException,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        SendCard(
            modifier = Modifier
                .padding(20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Prevent dismiss on card click */ }
        ) {
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
                    text = error.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
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
                                error.actionLabel,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    SendButton(
                        onClick = onDismiss,
                        variant = ButtonVariant.Secondary,
                        size = ButtonSize.Medium
                    ) {
                        Text(
                            "Dismiss",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SendQRDialog(
    qrBitmap: Bitmap,
    fileCount: Int,
    onDismiss: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    TablerIcons.Qrcode,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "QR Code for Transfer",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR code for file transfer",
                        modifier = Modifier
                            .size(220.dp)
                            .padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Show this QR code to the receiver",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$fileCount file${if (fileCount != 1) "s" else ""} ready to transfer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SendLoadingIndicator()
                    Text(
                        text = "Waiting for receiver to scan...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onCancel,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "Cancel Transfer",
                    fontWeight = FontWeight.Medium
                )
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// Utility Functions with Error Handling

data class FileValidationResult(
    val validFiles: List<Uri>,
    val skippedCount: Int
)

private fun validateAndFilterFiles(context: android.content.Context, uris: List<Uri>): FileValidationResult {
    val validFiles = mutableListOf<Uri>()
    var skippedCount = 0

    uris.forEach { uri ->
        try {
            val size = getFileSize(context, uri)
            if (size > 0 && size <= 2_000_000_000L) { // 2GB limit
                validFiles.add(uri)
            } else {
                skippedCount++
            }
        } catch (e: Exception) {
            skippedCount++
        }
    }

    return FileValidationResult(validFiles, skippedCount)
}

private fun generateQRCodeSafely(ticket: String, confirmation: UByte): Bitmap {
    val writer = QRCodeWriter()
    try {
        if (ticket.isEmpty()) {
            throw IllegalArgumentException("Ticket cannot be empty")
        }

        val qrData = "drop://receive?ticket=$ticket&confirmation=$confirmation"
        val bitMatrix: BitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap[x, y] = if (bitMatrix[x, y]) {
                    android.graphics.Color.BLACK
                } else {
                    android.graphics.Color.WHITE
                }
            }
        }
        return bitmap
    } catch (e: WriterException) {
        throw RuntimeException("QR code generation failed: ${e.message}", e)
    } catch (e: Exception) {
        throw RuntimeException("Unexpected error during QR code generation: ${e.message}", e)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "0 B"
    if (bytes < 1024) return "$bytes B"

    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)

    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)

    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
}

private fun formatDuration(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}

private fun getFileSize(context: android.content.Context, uri: Uri): Long {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
            } else 0L
        } ?: 0L
    } catch (e: Exception) {
        0L
    }
}

private fun getFileInfo(context: android.content.Context, uri: Uri): Pair<String, Long> {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)

                val name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "Unknown" else "Unknown"
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L

                Pair(name, size)
            } else Pair("Unknown", 0L)
        } ?: Pair("Unknown", 0L)
    } catch (e: Exception) {
        Pair("Unknown", 0L)
    }
}

private fun checkNetworkConnection(context: android.content.Context): Boolean {
    return try {
        val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                as? android.net.ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetworkInfo
        activeNetwork?.isConnected == true
    } catch (e: Exception) {
        true // Assume connected if we can't check
    }
}

private fun calculateTransferSpeed(bytesTransferred: Long): Long {
    // This would be implemented with actual timing data
    // For now, return 0 to indicate no speed calculation
    return 0L
}

private fun calculateETA(bytesTransferred: Long, bytesRemaining: Long): Long {
    // This would be implemented with actual transfer speed data
    // For now, return 0 to indicate no ETA calculation
    return 0L
}
