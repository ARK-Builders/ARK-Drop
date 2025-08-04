package dev.arkbuilders.drop.app.ui.send

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import compose.icons.TablerIcons
import compose.icons.tablericons.CloudUpload
import compose.icons.tablericons.FileText
import compose.icons.tablericons.Plus
import compose.icons.tablericons.Qrcode
import compose.icons.tablericons.Wifi
import compose.icons.tablericons.WifiOff
import dev.arkbuilders.drop.app.TransferManager
import dev.arkbuilders.drop.app.ui.components.DropButton
import dev.arkbuilders.drop.app.ui.components.DropButtonSize
import dev.arkbuilders.drop.app.ui.components.DropButtonVariant
import dev.arkbuilders.drop.app.ui.components.DropCard
import dev.arkbuilders.drop.app.ui.components.DropCardContent
import dev.arkbuilders.drop.app.ui.components.DropCardSize
import dev.arkbuilders.drop.app.ui.components.DropCardVariant
import dev.arkbuilders.drop.app.ui.components.DropLogoIcon
import dev.arkbuilders.drop.app.ui.components.DropOutlinedButton
import dev.arkbuilders.drop.app.ui.components.EmptyState
import dev.arkbuilders.drop.app.ui.components.ErrorStateDisplay
import dev.arkbuilders.drop.app.ui.components.ErrorType
import dev.arkbuilders.drop.app.ui.components.LoadingIndicator
import dev.arkbuilders.drop.app.ui.profile.AvatarUtils
import dev.arkbuilders.drop.app.ui.theme.DesignTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Enhanced UI State Management
data class SendUiState(
    val phase: SendPhase = SendPhase.FileSelection,
    val isLoading: Boolean = false,
    val error: SendError? = null,
    val networkStatus: NetworkStatus = NetworkStatus.Unknown,
    val transferProgress: TransferProgress? = null,
    val showQRCode: Boolean = false,
    val qrBitmap: Bitmap? = null,
    val showSuccess: Boolean = false,
    val successCountdown: Int = 0
)

enum class SendPhase {
    FileSelection,
    GeneratingQR,
    WaitingForReceiver,
    Transferring,
    Complete,
    Error
}

enum class NetworkStatus {
    Unknown,
    Connected,
    Disconnected,
    Poor
}

data class SendError(
    val type: ErrorType,
    val title: String,
    val message: String,
    val actionLabel: String? = null,
    val isRecoverable: Boolean = true
)

data class TransferProgress(
    val isConnected: Boolean = false,
    val receiverName: String = "",
    val receiverAvatar: String? = null,
    val currentFileName: String = "",
    val filesCompleted: Int = 0,
    val totalFiles: Int = 0,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val transferSpeed: String = ""
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

    // State Management
    var uiState by remember { mutableStateOf(SendUiState()) }
    var selectedFiles by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }

    // Observe transfer progress
    val sendProgress by (transferManager.sendProgress?.collectAsState()
        ?: remember { mutableStateOf(null) })

    // Derived states
    val canStartTransfer by remember {
        derivedStateOf {
            selectedFiles.isNotEmpty() &&
                    uiState.phase == SendPhase.FileSelection &&
                    !uiState.isLoading
        }
    }

    val totalFileSize by remember {
        derivedStateOf {
            selectedFiles.sumOf { uri ->
                getFileSize(context, uri)
            }
        }
    }

    // File picker launcher with enhanced error handling
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val validFiles = uris.filter { uri ->
                validateFile(context, uri)
            }

            if (validFiles.size != uris.size) {
                uiState = uiState.copy(
                    error = SendError(
                        type = ErrorType.FileTransfer,
                        title = "Some Files Skipped",
                        message = "${uris.size - validFiles.size} files were skipped due to size or format restrictions.",
                        actionLabel = "Continue",
                        isRecoverable = true
                    )
                )
            }

            selectedFiles = selectedFiles + validFiles
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Network status monitoring
    LaunchedEffect(Unit) {
        // Monitor network status
        while (true) {
            uiState = uiState.copy(
                networkStatus = checkNetworkStatus(context)
            )
            delay(5000) // Check every 5 seconds
        }
    }

    // Transfer progress monitoring
    LaunchedEffect(sendProgress) {
        sendProgress?.let { progress ->
            val transferProgress = TransferProgress(
                isConnected = progress.isConnected,
                receiverName = progress.receiverName,
                receiverAvatar = progress.receiverAvatar,
                currentFileName = progress.fileName,
                bytesTransferred = progress.sent.toLong(),
                totalBytes = (progress.sent + progress.remaining).toLong(),
                transferSpeed = calculateTransferSpeed(progress.sent.toLong())
            )

            uiState = when {
                progress.isConnected && uiState.phase == SendPhase.WaitingForReceiver -> {
                    uiState.copy(
                        phase = SendPhase.Transferring,
                        transferProgress = transferProgress,
                        showQRCode = false
                    )
                }
                transferManager.isSendFinished() -> {
                    uiState.copy(
                        phase = SendPhase.Complete,
                        showSuccess = true,
                        successCountdown = 3000
                    )
                }
                else -> uiState.copy(transferProgress = transferProgress)
            }
        }
    }

    // Success countdown
    LaunchedEffect(uiState.successCountdown) {
        if (uiState.successCountdown > 0) {
            delay(1000)
            uiState = uiState.copy(successCountdown = uiState.successCountdown - 1000)
        }
    }

    // Start transfer function
    val startTransfer = {
        if (canStartTransfer) {
            scope.launch {
                try {
                    uiState = uiState.copy(
                        phase = SendPhase.GeneratingQR,
                        isLoading = true,
                        error = null
                    )

                    val bubble = transferManager.sendFiles(selectedFiles)
                    if (bubble != null) {
                        val ticket = transferManager.getCurrentSendTicket() ?: ""
                        val confirmation = transferManager.getCurrentSendConfirmation() ?: 0u

                        val qrBitmap = generateQRCode(ticket, confirmation)
                        uiState = uiState.copy(
                            phase = SendPhase.WaitingForReceiver,
                            isLoading = false,
                            qrBitmap = qrBitmap,
                            showQRCode = true
                        )

                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        throw Exception("Failed to initialize transfer")
                    }
                } catch (e: Exception) {
                    uiState = uiState.copy(
                        phase = SendPhase.Error,
                        isLoading = false,
                        error = SendError(
                            type = ErrorType.Network,
                            title = "Transfer Failed to Start",
                            message = "Unable to initialize the file transfer. Please check your network connection and try again.",
                            actionLabel = "Retry",
                            isRecoverable = true
                        )
                    )
                }
            }
        }
    }

    // Cancel transfer function
    val cancelTransfer = {
        transferManager.cancelSend()
        uiState = SendUiState() // Reset to initial state
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Reset for new transfer
    val resetForNewTransfer = {
        selectedFiles = emptyList()
        uiState = SendUiState()
        transferManager.cancelSend()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.ime)
    ) {
        // Enhanced Top App Bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
                ) {
                    DropLogoIcon(
                        size = 24.dp,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Send Files",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigateUp()
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Go back to home screen"
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                // Network status indicator
                NetworkStatusIndicator(
                    status = uiState.networkStatus,
                    modifier = Modifier.padding(end = DesignTokens.Spacing.sm)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Main Content with Phase-based UI
        AnimatedContent(
            targetState = uiState.phase,
            transitionSpec = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ) togetherWith slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                )
            },
            label = "phaseTransition"
        ) { phase ->
            when (phase) {
                SendPhase.FileSelection -> {
                    FileSelectionPhase(
                        selectedFiles = selectedFiles,
                        totalFileSize = totalFileSize,
                        onAddFiles = { filePickerLauncher.launch("*/*") },
                        onRemoveFile = { uri ->
                            selectedFiles = selectedFiles - uri
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onStartTransfer = startTransfer,
                        canStartTransfer = canStartTransfer,
                        isLoading = uiState.isLoading,
                        error = uiState.error,
                        onDismissError = { uiState = uiState.copy(error = null) }
                    )
                }

                SendPhase.GeneratingQR -> {
                    GeneratingQRPhase(
                        onCancel = cancelTransfer
                    )
                }

                SendPhase.WaitingForReceiver -> {
                    WaitingForReceiverPhase(
                        qrBitmap = uiState.qrBitmap,
                        onCancel = cancelTransfer,
                        fileCount = selectedFiles.size
                    )
                }

                SendPhase.Transferring -> {
                    TransferringPhase(
                        progress = uiState.transferProgress,
                        onCancel = cancelTransfer
                    )
                }

                SendPhase.Complete -> {
                    TransferCompletePhase(
                        fileCount = selectedFiles.size,
                        onSendMore = resetForNewTransfer,
                        onDone = { navController.navigateUp() },
                        showSuccess = uiState.showSuccess,
                        countdown = uiState.successCountdown
                    )
                }

                SendPhase.Error -> {
                    ErrorPhase(
                        error = uiState.error,
                        onRetry = {
                            uiState = uiState.copy(phase = SendPhase.FileSelection, error = null)
                        },
                        onCancel = { navController.navigateUp() }
                    )
                }
            }
        }
    }

    // QR Code Dialog
    if (uiState.showQRCode && uiState.qrBitmap != null) {
        QRCodeDialog(
            qrBitmap = uiState.qrBitmap!!,
            fileCount = selectedFiles.size,
            onDismiss = { uiState = uiState.copy(showQRCode = false) },
            onCancel = cancelTransfer
        )
    }
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
    error: SendError?,
    onDismissError: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(DesignTokens.Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xl)
    ) {
        // Error Display
        if (error != null) {
            item {
                ErrorStateDisplay(
                    errorState = dev.arkbuilders.drop.app.ui.components.ErrorState(
                        type = error.type,
                        title = error.title,
                        message = error.message,
                        actionLabel = error.actionLabel,
                        onAction = if (error.isRecoverable) onDismissError else null
                    )
                )
            }
        }

        // File Selection Section
        item {
            FileSelectionSection(
                selectedFiles = selectedFiles,
                totalFileSize = totalFileSize,
                onAddFiles = onAddFiles,
                onRemoveFile = onRemoveFile
            )
        }

        // Transfer Button
        item {
            TransferActionSection(
                onStartTransfer = onStartTransfer,
                canStartTransfer = canStartTransfer,
                isLoading = isLoading,
                fileCount = selectedFiles.size
            )
        }

        // Instructions
        item {
            InstructionsSection()
        }
    }
}

@Composable
private fun FileSelectionSection(
    selectedFiles: List<Uri>,
    totalFileSize: Long,
    onAddFiles: () -> Unit,
    onRemoveFile: (Uri) -> Unit
) {
    DropCard(
        variant = DropCardVariant.Elevated,
        size = DropCardSize.Large,
        contentDescription = "File selection area"
    ) {
        DropCardContent(size = DropCardSize.Large) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Selected Files",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (selectedFiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.xs))
                        Text(
                            text = "${selectedFiles.size} file${if (selectedFiles.size != 1) "s" else ""} • ${formatBytes(totalFileSize)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropButton(
                    onClick = onAddFiles,
                    variant = DropButtonVariant.Secondary,
                    size = DropButtonSize.Medium,
                    contentDescription = "Add files to send"
                ) {
                    Icon(
                        TablerIcons.Plus,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.Spacing.sm))
                    Text(
                        "Add Files",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

            if (selectedFiles.isEmpty()) {
                EmptyFileSelection()
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
                ) {
                    items(selectedFiles) { uri ->
                        EnhancedFileItem(
                            uri = uri,
                            onRemove = { onRemoveFile(uri) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFileSelection() {
    DropCard(
        variant = DropCardVariant.Outlined,
        size = DropCardSize.Medium,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        EmptyState(
            title = "No Files Selected",
            description = "Tap 'Add Files' to choose files you want to send to another device."
        ) {
            Icon(
                TablerIcons.FileText,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun EnhancedFileItem(
    uri: Uri,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var fileName by remember { mutableStateOf("Loading...") }
    var fileSize by remember { mutableStateOf(0L) }

    LaunchedEffect(uri) {
        val fileInfo = getFileInfo(context, uri)
        fileName = fileInfo.first
        fileSize = fileInfo.second
    }

    DropCard(
        variant = DropCardVariant.Filled,
        size = DropCardSize.Small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        DropCardContent(size = DropCardSize.Small) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File icon
                Icon(
                    TablerIcons.FileText,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(DesignTokens.Spacing.md))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (fileSize > 0) {
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.xs))
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
                    modifier = Modifier.semantics {
                        contentDescription = "Remove $fileName from selection"
                    }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferActionSection(
    onStartTransfer: () -> Unit,
    canStartTransfer: Boolean,
    isLoading: Boolean,
    fileCount: Int
) {
    DropButton(
        onClick = onStartTransfer,
        variant = DropButtonVariant.Primary,
        size = DropButtonSize.Large,
        enabled = canStartTransfer,
        loading = isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(DesignTokens.TouchTarget.large),
        contentDescription = "Start file transfer"
    ) {
        if (!isLoading) {
            Icon(
                TablerIcons.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(DesignTokens.Spacing.md))
        }
        Text(
            text = if (isLoading) {
                "Starting Transfer..."
            } else {
                "Send $fileCount File${if (fileCount != 1) "s" else ""}"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InstructionsSection() {
    DropCard(
        variant = DropCardVariant.Outlined,
        size = DropCardSize.Medium,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        DropCardContent(size = DropCardSize.Medium) {
            Column {
                Text(
                    text = "How to Send Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

                val instructions = listOf(
                    "Select files you want to send",
                    "Tap 'Send Files' to generate QR code",
                    "Let the receiver scan the QR code",
                    "Files transfer automatically"
                )

                instructions.forEachIndexed { index, instruction ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = instruction,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (index < instructions.size - 1) {
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))
                    }
                }
            }
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
        DropCard(
            variant = DropCardVariant.Elevated,
            size = DropCardSize.Large
        ) {
            DropCardContent(size = DropCardSize.Large) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LoadingIndicator(
                        message = "Generating QR Code..."
                    )

                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

                    DropOutlinedButton(
                        onClick = onCancel,
                        size = DropButtonSize.Medium,
                        contentDescription = "Cancel transfer setup"
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun WaitingForReceiverPhase(
    qrBitmap: Bitmap?,
    onCancel: () -> Unit,
    fileCount: Int
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(DesignTokens.Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xl)
    ) {
        item {
            DropCard(
                variant = DropCardVariant.Elevated,
                size = DropCardSize.Large
            ) {
                DropCardContent(size = DropCardSize.Large) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Ready to Send",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

                        Text(
                            text = "$fileCount file${if (fileCount != 1) "s" else ""} ready for transfer",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

                        // QR Code Display
                        qrBitmap?.let { bitmap ->
                            DropCard(
                                variant = DropCardVariant.Outlined,
                                size = DropCardSize.Medium
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "QR code for file transfer",
                                    modifier = Modifier
                                        .size(200.dp)
                                        .padding(DesignTokens.Spacing.lg)
                                        .semantics {
                                            contentDescription = "QR code containing transfer information"
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

                        Text(
                            text = "Show this QR code to the receiver",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

                        // Waiting indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
                        ) {
                            LoadingIndicator()
                            Text(
                                text = "Waiting for receiver to scan...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        item {
            DropOutlinedButton(
                onClick = onCancel,
                size = DropButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = "Cancel file transfer"
            ) {
                Text(
                    "Cancel Transfer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TransferringPhase(
    progress: TransferProgress?,
    onCancel: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(DesignTokens.Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xl)
    ) {
        item {
            progress?.let { p ->
                DropCard(
                    variant = DropCardVariant.Elevated,
                    size = DropCardSize.Large,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    DropCardContent(size = DropCardSize.Large) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sending Files",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = onCancel,
                                    modifier = Modifier.semantics {
                                        contentDescription = "Cancel file transfer"
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            if (p.receiverName.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
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
                                Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

                                Text(
                                    text = "Sending: ${p.currentFileName}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val progressValue = if (p.totalBytes > 0) {
                                    p.bytesTransferred.toFloat() / p.totalBytes.toFloat()
                                } else 0f

                                Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

                                LinearProgressIndicator(
                                    progress = { progressValue },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )

                                Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${formatBytes(p.bytesTransferred)} / ${formatBytes(p.totalBytes)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )

                                    if (p.transferSpeed.isNotEmpty()) {
                                        Text(
                                            text = p.transferSpeed,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
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
    showSuccess: Boolean,
    countdown: Int
) {
    val haptic = LocalHapticFeedback.current

    // Success animation
    val successScale by animateFloatAsState(
        targetValue = if (showSuccess && countdown > 0) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "successScale"
    )

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xl)
        ) {
            // Success Animation
            AnimatedVisibility(
                visible = showSuccess && countdown > 0,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                DropCard(
                    variant = DropCardVariant.Elevated,
                    size = DropCardSize.Large,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.scale(successScale)
                ) {
                    DropCardContent(size = DropCardSize.Large) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

                            Text(
                                text = "Transfer Complete!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

                            Text(
                                text = "$fileCount file${if (fileCount != 1) "s" else ""} sent successfully",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Action buttons (shown after success animation)
            AnimatedVisibility(
                visible = !showSuccess || countdown <= 0,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                DropCard(
                    variant = DropCardVariant.Elevated,
                    size = DropCardSize.Large,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    DropCardContent(size = DropCardSize.Large) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Complete",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

                            Text(
                                text = "Files Sent Successfully!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

                            Text(
                                text = "$fileCount file${if (fileCount != 1) "s" else ""} transferred successfully",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
                            ) {
                                DropOutlinedButton(
                                    onClick = onSendMore,
                                    size = DropButtonSize.Large,
                                    modifier = Modifier.weight(1f),
                                    contentDescription = "Send more files"
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(DesignTokens.Spacing.sm))
                                    Text(
                                        "Send More",
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                DropButton(
                                    onClick = onDone,
                                    variant = DropButtonVariant.Primary,
                                    size = DropButtonSize.Large,
                                    modifier = Modifier.weight(1f),
                                    contentDescription = "Return to home screen"
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
}

@Composable
private fun ErrorPhase(
    error: SendError?,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xl)
        ) {
            error?.let { err ->
                ErrorStateDisplay(
                    errorState = dev.arkbuilders.drop.app.ui.components.ErrorState(
                        type = err.type,
                        title = err.title,
                        message = err.message,
                        actionLabel = if (err.isRecoverable) "Try Again" else null,
                        onAction = if (err.isRecoverable) onRetry else null
                    )
                )
            }

            DropOutlinedButton(
                onClick = onCancel,
                size = DropButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = "Cancel and return to home"
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun NetworkStatusIndicator(
    status: NetworkStatus,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (status) {
        NetworkStatus.Connected -> TablerIcons.Wifi to MaterialTheme.colorScheme.primary
        NetworkStatus.Disconnected -> TablerIcons.WifiOff to MaterialTheme.colorScheme.error
        NetworkStatus.Poor -> TablerIcons.Wifi to MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        NetworkStatus.Unknown -> TablerIcons.Wifi to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Icon(
        imageVector = icon,
        contentDescription = "Network status: ${status.name.lowercase()}",
        modifier = modifier
            .size(20.dp)
            .semantics {
                contentDescription = "Network connection status: ${status.name.lowercase()}"
            },
        tint = color
    )
}

@Composable
private fun QRCodeDialog(
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
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
            ) {
                Icon(
                    TablerIcons.Qrcode,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "QR Code for Transfer",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DropCard(
                    variant = DropCardVariant.Outlined,
                    size = DropCardSize.Medium
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR code for file transfer",
                        modifier = Modifier
                            .size(220.dp)
                            .padding(DesignTokens.Spacing.lg)
                    )
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

                Text(
                    text = "Show this QR code to the receiver",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

                Text(
                    text = "$fileCount file${if (fileCount != 1) "s" else ""} ready to transfer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm)
                ) {
                    LoadingIndicator()
                    Text(
                        text = "Waiting for receiver to scan...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            // No confirm button while waiting
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                shape = RoundedCornerShape(DesignTokens.CornerRadius.sm)
            ) {
                Text(
                    "Cancel Transfer",
                    fontWeight = FontWeight.Medium
                )
            }
        },
        shape = RoundedCornerShape(DesignTokens.CornerRadius.xl)
    )
}

// Utility Functions
private fun generateQRCode(ticket: String, confirmation: UByte): Bitmap {
    val writer = QRCodeWriter()
    try {
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
        throw RuntimeException("Error generating QR code", e)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
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

private fun validateFile(context: android.content.Context, uri: Uri): Boolean {
    val size = getFileSize(context, uri)
    return size > 0 && size <= 2_000_000_000L // 2GB limit
}

private fun checkNetworkStatus(context: android.content.Context): NetworkStatus {
    // Implementation would check actual network connectivity
    // For now, return Connected as default
    return NetworkStatus.Connected
}

private fun calculateTransferSpeed(bytesTransferred: Long): String {
    // Implementation would calculate actual transfer speed
    // For now, return empty string
    return ""
}
