package dev.arkbuilders.drop.app.ui.receive

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import compose.icons.TablerIcons
import compose.icons.tablericons.AlertCircle
import compose.icons.tablericons.ArrowForward
import compose.icons.tablericons.Camera
import compose.icons.tablericons.CameraOff
import compose.icons.tablericons.Qrcode
import dev.arkbuilders.drop.app.TransferManager
import dev.arkbuilders.drop.app.data.ReceiveFileInfo
import dev.arkbuilders.drop.app.data.ReceivingProgress
import dev.arkbuilders.drop.app.ui.components.DropLogoIcon
import dev.arkbuilders.drop.app.ui.profile.AvatarUtils
import dev.arkbuilders.drop.app.ui.theme.DesignTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

sealed class ReceiveError(val message: String, val isRecoverable: Boolean = true) {
    object CameraPermissionDenied :
        ReceiveError("Camera permission is required to scan QR codes", true)

    object CameraInitializationFailed :
        ReceiveError("Unable to initialize camera. Please try again.", true)

    object InvalidQRCode :
        ReceiveError("This QR code is not from Drop. Please scan a valid Drop QR code.", true)

    object InvalidManualInput :
        ReceiveError("Invalid format. Please enter: ticket confirmation", true)

    object ConnectionFailed :
        ReceiveError("Unable to connect to sender. Please ensure you're on the same network.", true)

    object TransferInterrupted :
        ReceiveError("File transfer was interrupted. Please try again.", true)

    object NoFilesReceived : ReceiveError("No files were received from the sender.", true)
    object StorageError :
        ReceiveError("Unable to save files. Please check your storage permissions.", true)

    object NetworkError :
        ReceiveError("Network connection lost. Please check your connection and try again.", true)

    object UnknownError : ReceiveError("An unexpected error occurred. Please try again.", true)
}

sealed class ReceiveWorkflowState {
    object Initial : ReceiveWorkflowState()
    object RequestingPermission : ReceiveWorkflowState()
    object Scanning : ReceiveWorkflowState()
    object ManualInput : ReceiveWorkflowState()
    object QRCodeScanned : ReceiveWorkflowState()
    object Connecting : ReceiveWorkflowState()
    object Receiving : ReceiveWorkflowState()
    object Success : ReceiveWorkflowState()
    data class Error(val error: ReceiveError) : ReceiveWorkflowState()
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Receive(
    navController: NavController,
    transferManager: TransferManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var workflowState by remember {
        val receiveProgress = transferManager.receiveProgress?.value
        if (receiveProgress != null && receiveProgress.isConnected) {
            mutableStateOf<ReceiveWorkflowState>(ReceiveWorkflowState.Receiving)
        } else {
            mutableStateOf<ReceiveWorkflowState>(ReceiveWorkflowState.Initial)
        }
    }
    var scannedTicket by remember { mutableStateOf<String?>(null) }
    var scannedConfirmation by remember { mutableStateOf<UByte?>(null) }
    var manualInputText by remember { mutableStateOf("") }
    var manualInputError by remember { mutableStateOf<String?>(null) }
    var receivedFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val receiveProgress by (transferManager.receiveProgress?.collectAsState()
        ?: remember { mutableStateOf(null) })

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            workflowState = ReceiveWorkflowState.Scanning
        } else {
            workflowState = ReceiveWorkflowState.Error(ReceiveError.CameraPermissionDenied)
        }
    }

    // Function to parse manual input
    fun parseManualInput(input: String): Pair<String, UByte>? {
        return try {
            val trimmed = input.trim()
            val parts = trimmed.split(" ")

            if (parts.size == 2) {
                val ticket = parts[0].trim()
                val confirmation = parts[1].trim().toUByte()

                if (ticket.isNotEmpty()) {
                    Pair(ticket, confirmation)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Function to handle manual input submission
    fun handleManualInputSubmit() {
        val parsed = parseManualInput(manualInputText)
        if (parsed != null) {
            scannedTicket = parsed.first
            scannedConfirmation = parsed.second
            workflowState = ReceiveWorkflowState.QRCodeScanned
            manualInputError = null
            keyboardController?.hide()
        } else {
            manualInputError = "Invalid format. Please enter: ticket confirmation"
        }
    }

    // Function to paste from clipboard
    fun pasteFromClipboard() {
        val clipText = clipboardManager.getText()?.text
        if (!clipText.isNullOrEmpty()) {
            manualInputText = clipText
            manualInputError = null
        }
    }

    // Monitor receive progress and handle completion
    LaunchedEffect(receiveProgress) {
        receiveProgress?.let { progress ->
            // Check if we're connected and have files
            if (progress.isConnected && progress.files.isNotEmpty()) {
                // Check if all files are complete
                val allFilesComplete = progress.files.all { file ->
                    val fileProgress = progress.fileProgress[file.id]
                    fileProgress?.isComplete == true
                }
                
                if (allFilesComplete) {
                    // Small delay to ensure UI updates are visible
                    delay(1000)
                    try {
                        val savedFiles = transferManager.saveReceivedFiles()
                        if (savedFiles.isNotEmpty()) {
                            receivedFiles = savedFiles.map { it.name }
                            workflowState = ReceiveWorkflowState.Success
                            showSuccessAnimation = true
                        } else {
                            workflowState = ReceiveWorkflowState.Error(ReceiveError.NoFilesReceived)
                        }
                    } catch (e: Exception) {
                        workflowState = ReceiveWorkflowState.Error(
                            when {
                                e.message?.contains("storage", ignoreCase = true) == true ->
                                    ReceiveError.StorageError

                                e.message?.contains("network", ignoreCase = true) == true ->
                                    ReceiveError.NetworkError

                                else -> ReceiveError.UnknownError
                            }
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(showSuccessAnimation) {
        if (showSuccessAnimation) {
            delay(3000)
            showSuccessAnimation = false
        }
    }

    val successScale by animateFloatAsState(
        targetValue = if (showSuccessAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "successScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DesignTokens.Spacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { navController.navigateUp() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(DesignTokens.TouchTarget.minimum)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(DesignTokens.Spacing.md))

            DropLogoIcon(
                size = 32.dp,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(DesignTokens.Spacing.md))

            Text(
                text = "Receive Files",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

        AnimatedVisibility(
            visible = showSuccessAnimation,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(),
            exit = scaleOut(
                animationSpec = tween(DesignTokens.Animation.normal)
            ) + fadeOut()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(successScale),
                    shape = RoundedCornerShape(DesignTokens.CornerRadius.xl),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = DesignTokens.Elevation.xl)
                ) {
                    Column(
                        modifier = Modifier.padding(DesignTokens.Spacing.xxl),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

                        Text(
                            text = "Files Received!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

                        Text(
                            text = "All files have been successfully saved to your Downloads folder.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                        )
                    }
                }
            }
        }

        AnimatedContent(
            targetState = workflowState,
            transitionSpec = {
                slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeIn() togetherWith
                        slideOutVertically(
                            targetOffsetY = { -it / 3 },
                            animationSpec = tween(DesignTokens.Animation.fast)
                        ) + fadeOut()
            },
            label = "workflowStateTransition"
        ) { state ->
            when (state) {
                is ReceiveWorkflowState.Initial -> {
                    if (!cameraPermissionState.status.isGranted) {
                        PermissionRequestCard(
                            onRequestPermission = {
                                workflowState = ReceiveWorkflowState.RequestingPermission
                                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onEnterManually = {
                                workflowState = ReceiveWorkflowState.ManualInput
                            }
                        )
                    } else {
                        ReadyToScanCard(
                            onStartScanning = { workflowState = ReceiveWorkflowState.Scanning },
                            onEnterManually = { workflowState = ReceiveWorkflowState.ManualInput }
                        )
                    }
                }

                is ReceiveWorkflowState.RequestingPermission -> {
                    LoadingCard(message = "Requesting camera permission...")
                }

                is ReceiveWorkflowState.Scanning -> {
                    ScanningCard(
                        onQRCodeScanned = { ticket, confirmation ->
                            scannedTicket = ticket
                            scannedConfirmation = confirmation
                            workflowState = ReceiveWorkflowState.QRCodeScanned
                        },
                        onError = { error ->
                            workflowState = ReceiveWorkflowState.Error(error)
                        },
                        onStopScanning = { workflowState = ReceiveWorkflowState.Initial },
                        onEnterManually = { workflowState = ReceiveWorkflowState.ManualInput }
                    )
                }

                is ReceiveWorkflowState.ManualInput -> {
                    ManualInputCard(
                        inputText = manualInputText,
                        onInputChange = {
                            manualInputText = it
                            manualInputError = null
                        },
                        inputError = manualInputError,
                        onPasteFromClipboard = { pasteFromClipboard() },
                        onSubmit = { handleManualInputSubmit() },
                        onCancel = {
                            workflowState = ReceiveWorkflowState.Initial
                            manualInputText = ""
                            manualInputError = null
                            keyboardController?.hide()
                        }
                    )
                }

                is ReceiveWorkflowState.QRCodeScanned -> {
                    QRCodeScannedCard(
                        onAccept = {
                            scope.launch {
                                try {
                                    workflowState = ReceiveWorkflowState.Connecting
                                    val ticket = scannedTicket!!
                                    val confirmation = scannedConfirmation!!

                                    val bubble = transferManager.receiveFiles(ticket, confirmation)
                                    if (bubble != null) {
                                        workflowState = ReceiveWorkflowState.Receiving
                                    } else {
                                        workflowState =
                                            ReceiveWorkflowState.Error(ReceiveError.ConnectionFailed)
                                    }
                                } catch (e: Exception) {
                                    workflowState = ReceiveWorkflowState.Error(
                                        when {
                                            e.message?.contains(
                                                "network",
                                                ignoreCase = true
                                            ) == true -> ReceiveError.NetworkError

                                            else -> ReceiveError.ConnectionFailed
                                        }
                                    )
                                }
                            }
                        },
                        onScanAgain = {
                            scannedTicket = null
                            scannedConfirmation = null
                            manualInputText = ""
                            manualInputError = null
                            if (cameraPermissionState.status.isGranted) {
                                workflowState = ReceiveWorkflowState.Scanning
                            } else {
                                workflowState = ReceiveWorkflowState.ManualInput
                            }
                        }
                    )
                }

                is ReceiveWorkflowState.Connecting -> {
                    LoadingCard(message = "Connecting to sender...")
                }

                is ReceiveWorkflowState.Receiving -> {
                    receiveProgress?.let { progress ->
                        ReceivingCard(
                            progress = progress,
                            onCancel = {
                                transferManager.cancelReceive()
                                workflowState = ReceiveWorkflowState.Initial
                                scannedTicket = null
                                scannedConfirmation = null
                                manualInputText = ""
                                manualInputError = null
                            }
                        )
                    }
                }

                is ReceiveWorkflowState.Success -> {
                    if (!showSuccessAnimation) {
                        TransferCompleteCard(
                            receivedFiles = receivedFiles,
                            onReceiveMore = {
                                receivedFiles = emptyList()
                                workflowState = ReceiveWorkflowState.Initial
                                scannedTicket = null
                                scannedConfirmation = null
                                manualInputText = ""
                                manualInputError = null
                                transferManager.cancelReceive()
                            },
                            onDone = {
                                transferManager.cancelReceive()
                                navController.navigateUp()
                            }
                        )
                    }
                }

                is ReceiveWorkflowState.Error -> {
                    ErrorCard(
                        error = state.error,
                        onRetry = {
                            workflowState = ReceiveWorkflowState.Initial
                            scannedTicket = null
                            scannedConfirmation = null
                            manualInputText = ""
                            manualInputError = null
                        },
                        onDismiss = {
                            transferManager.cancelReceive()
                            navController.navigateUp()

                        }
                    )
                }
            }
        }

        if (workflowState !is ReceiveWorkflowState.Success && workflowState !is ReceiveWorkflowState.Error) {
            Spacer(modifier = Modifier.weight(1f))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(DesignTokens.CornerRadius.lg)
            ) {
                Column(
                    modifier = Modifier.padding(DesignTokens.Spacing.lg)
                ) {
                    Text(
                        text = "How to receive files:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

                    val steps = listOf(
                        "Ask the sender to start a transfer",
                        "Scan QR code OR enter transfer code manually",
                        "Accept the transfer",
                        "Files will be saved to your Downloads folder"
                    )

                    steps.forEachIndexed { index, step ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(DesignTokens.Spacing.sm))
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestCard(
    onRequestPermission: () -> Unit,
    onEnterManually: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.CornerRadius.xl),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DesignTokens.Elevation.lg)
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    TablerIcons.Camera,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

            Text(
                text = "We need camera access to scan QR codes for receiving files. Your privacy is protected - we only use the camera for QR code scanning.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DesignTokens.TouchTarget.comfortable),
                shape = RoundedCornerShape(DesignTokens.CornerRadius.lg),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Grant Permission",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

            Text(
                text = "Or",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

            OutlinedButton(
                onClick = onEnterManually,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DesignTokens.TouchTarget.comfortable),
                shape = RoundedCornerShape(DesignTokens.CornerRadius.lg)
            ) {
                Icon(
                    TablerIcons.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(DesignTokens.Spacing.sm))
                Text(
                    "Enter Code Manually",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ReadyToScanCard(
    onStartScanning: () -> Unit,
    onEnterManually: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.CornerRadius.xl),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DesignTokens.Elevation.lg)
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    TablerIcons.Qrcode,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

            Text(
                text = "Ready to Receive",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

            Text(
                text = "Scan the QR code from the sender's device or enter the transfer code manually to start receiving files securely.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

            Button(
                onClick = onStartScanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DesignTokens.TouchTarget.comfortable),
                shape = RoundedCornerShape(DesignTokens.CornerRadius.lg),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    TablerIcons.Camera,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(DesignTokens.Spacing.sm))
                Text(
                    "Start Scanning",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

            Text(
                text = "Or",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

            OutlinedButton(
                onClick = onEnterManually,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DesignTokens.TouchTarget.comfortable),
                shape = RoundedCornerShape(DesignTokens.CornerRadius.lg)
            ) {
                Icon(
                    TablerIcons.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(DesignTokens.Spacing.sm))
                Text(
                    "Enter Code Manually",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LoadingCard(
    message: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.CornerRadius.lg),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DesignTokens.Elevation.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(DesignTokens.Spacing.lg))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ScanningCard(
    onQRCodeScanned: (String, UByte) -> Unit,
    onError: (ReceiveError) -> Unit,
    onStopScanning: () -> Unit,
    onEnterManually: () -> Unit
) {
    Column {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(DesignTokens.CornerRadius.xl),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = DesignTokens.Elevation.lg)
        ) {
            QRCodeScanner(
                onQRCodeScanned = onQRCodeScanned,
                onError = onError
            )
        }

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

        Text(
            text = "Point your camera at the QR code",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
        ) {
            OutlinedButton(
                onClick = onStopScanning,
                modifier = Modifier
                    .weight(1f)
                    .height(DesignTokens.TouchTarget.comfortable),
                shape = RoundedCornerShape(DesignTokens.CornerRadius.lg)
            ) {
                Icon(
                    TablerIcons.CameraOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(DesignTokens.Spacing.sm))
                Text(
                    "Stop Scanning",
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = onEnterManually,
                modifier = Modifier
                    .weight(1f)
                    .height(DesignTokens.TouchTarget.comfortable),
                shape = RoundedCornerShape(DesignTokens.CornerRadius.lg)
            ) {
                Icon(
                    TablerIcons.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(DesignTokens.Spacing.sm))
                Text(
                    "Enter Code",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ManualInputCard(
    inputText: String,
    onInputChange: (String) -> Unit,
    inputError: String?,
    onPasteFromClipboard: () -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.CornerRadius.xl),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DesignTokens.Elevation.lg)
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    TablerIcons.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

            Text(
                text = "Enter Transfer Code",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

            Text(
                text = "Paste or type the transfer code from the sender in the format: ticket confirmation",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                label = { Text("Transfer Code") },
                placeholder = { Text("ticket confirmation") },
                modifier = Modifier.fillMaxWidth(),
                isError = inputError != null,
                supportingText = inputError?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
                trailingIcon = {
                    IconButton(onClick = onPasteFromClipboard) {
                        Icon(
                            TablerIcons.ArrowForward,
                            contentDescription = "Paste",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onSubmit() }
                ),
                shape = RoundedCornerShape(DesignTokens.CornerRadius.lg)
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(DesignTokens.TouchTarget.comfortable),
                    shape = RoundedCornerShape(DesignTokens.CornerRadius.lg)
                ) {
                    Text(
                        "Cancel",
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = onSubmit,
                    modifier = Modifier
                        .weight(1f)
                        .height(DesignTokens.TouchTarget.comfortable),
                    shape = RoundedCornerShape(DesignTokens.CornerRadius.lg),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = inputText.trim().isNotEmpty()
                ) {
                    Text(
                        "Connect",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun QRCodeScannedCard(
    onAccept: () -> Unit,
    onScanAgain: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.CornerRadius.xl),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DesignTokens.Elevation.lg)
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

            Text(
                text = "Code Received!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

            Text(
                text = "Ready to receive files from sender. Tap Accept to start the transfer.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
            ) {
                OutlinedButton(
                    onClick = onScanAgain,
                    modifier = Modifier
                        .weight(1f)
                        .height(DesignTokens.TouchTarget.comfortable),
                    shape = RoundedCornerShape(DesignTokens.CornerRadius.lg)
                ) {
                    Text(
                        "Try Again",
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .weight(1f)
                        .height(DesignTokens.TouchTarget.comfortable),
                    shape = RoundedCornerShape(DesignTokens.CornerRadius.lg),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        "Accept",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceivingCard(
    progress: ReceivingProgress,
    onCancel: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.CornerRadius.lg),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DesignTokens.Elevation.lg)
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (progress.isConnected) "Receiving Files..." else "Connecting...",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    onClick = onCancel,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (progress.isConnected) {
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
                ) {
                    AvatarUtils.AvatarImageWithFallback(
                        base64String = progress.senderAvatar,
                        fallbackText = progress.senderName,
                        size = 36.dp
                    )

                    Column {
                        Text(
                            text = "From:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = progress.senderName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

                Text(
                    text = "Files (${progress.files.size}):",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm)
                ) {
                    items(progress.files) { file ->
                        val fileProgress = progress.fileProgress[file.id]
                        ReceivingFileItem(
                            file = file,
                            progress = if (file.size > 0UL && fileProgress != null) {
                                (fileProgress.receivedBytes.toFloat() / file.size.toFloat()).coerceIn(0f, 1f)
                            } else 0f,
                            receivedBytes = fileProgress?.receivedBytes ?: 0L,
                            isComplete = fileProgress?.isComplete ?: false
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferCompleteCard(
    receivedFiles: List<String>,
    onReceiveMore: () -> Unit,
    onDone: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.CornerRadius.lg),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DesignTokens.Elevation.lg)
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Complete",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

            Text(
                text = "Files Received Successfully!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

            Text(
                text = "${receivedFiles.size} file${if (receivedFiles.size != 1) "s" else ""} saved to Downloads",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )

            if (receivedFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(DesignTokens.CornerRadius.md)
                ) {
                    Column(
                        modifier = Modifier.padding(DesignTokens.Spacing.lg)
                    ) {
                        Text(
                            text = "Received Files:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

                        // Show first 3 files, then "... and X more" if needed
                        receivedFiles.take(3).forEach { fileName ->
                            Text(
                                text = "• $fileName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (receivedFiles.size > 3) {
                            Text(
                                text = "• ... and ${receivedFiles.size - 3} more",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
            ) {
                OutlinedButton(
                    onClick = onReceiveMore,
                    modifier = Modifier
                        .weight(1f)
                        .height(DesignTokens.TouchTarget.comfortable),
                    shape = RoundedCornerShape(DesignTokens.CornerRadius.md)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.Spacing.sm))
                    Text("Receive More", fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .weight(1f)
                        .height(DesignTokens.TouchTarget.comfortable),
                    shape = RoundedCornerShape(DesignTokens.CornerRadius.md)
                ) {
                    Text("Done", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    error: ReceiveError,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.CornerRadius.lg),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DesignTokens.Elevation.lg)
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (error.isRecoverable) Icons.Default.Warning else TablerIcons.AlertCircle,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

            Text(
                text = if (error.isRecoverable) "Something went wrong" else "Error occurred",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(DesignTokens.TouchTarget.comfortable),
                    shape = RoundedCornerShape(DesignTokens.CornerRadius.md)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Medium)
                }

                if (error.isRecoverable) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .weight(1f)
                            .height(DesignTokens.TouchTarget.comfortable),
                        shape = RoundedCornerShape(DesignTokens.CornerRadius.md),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Try Again", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceivingFileItem(
    file: ReceiveFileInfo,
    progress: Float,
    receivedBytes: Long,
    isComplete: Boolean
) {

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(DesignTokens.CornerRadius.md),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DesignTokens.Elevation.xs)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${formatBytes(receivedBytes)} / ${formatBytes(file.size.toLong())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isComplete) {
                Spacer(modifier = Modifier.width(DesignTokens.Spacing.md))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Complete",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun QRCodeScanner(
    onQRCodeScanned: (String, UByte) -> Unit,
    onError: (ReceiveError) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                processImageProxy(imageProxy, onQRCodeScanned, onError)
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    onError(ReceiveError.CameraInitializationFailed)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

@ExperimentalGetImage
private fun processImageProxy(
    imageProxy: ImageProxy,
    onQRCodeScanned: (String, UByte) -> Unit,
    onError: (ReceiveError) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    when (barcode.valueType) {
                        Barcode.TYPE_TEXT, Barcode.TYPE_URL -> {
                            barcode.rawValue?.let { value ->
                                // Parse Drop QR code format: drop://receive?ticket=...&confirmation=...
                                if (value.startsWith("drop://receive?")) {
                                    try {
                                        val uri = value.toUri()
                                        val ticket = uri.getQueryParameter("ticket")
                                        val confirmationStr = uri.getQueryParameter("confirmation")

                                        if (ticket != null && confirmationStr != null) {
                                            val confirmation = confirmationStr.toUByte()
                                            onQRCodeScanned(ticket, confirmation)
                                            return@addOnSuccessListener
                                        }
                                    } catch (_: Exception) {
                                        onError(ReceiveError.InvalidQRCode)
                                        return@addOnSuccessListener
                                    }
                                } else {
                                    onError(ReceiveError.InvalidQRCode)
                                    return@addOnSuccessListener
                                }
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                onError(
                    when {
                        exception.message?.contains(
                            "camera",
                            ignoreCase = true
                        ) == true -> ReceiveError.CameraInitializationFailed

                        else -> ReceiveError.UnknownError
                    }
                )
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
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
