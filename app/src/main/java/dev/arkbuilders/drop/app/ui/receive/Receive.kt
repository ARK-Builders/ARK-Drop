package dev.arkbuilders.drop.app.ui.receive

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.arkbuilders.drop.app.TransferManager
import dev.arkbuilders.drop.app.ui.components.DropLogoIcon
import dev.arkbuilders.drop.app.ui.receive.components.ReceiveCompleteCard
import dev.arkbuilders.drop.app.ui.receive.components.ReceiveErrorCard
import dev.arkbuilders.drop.app.ui.receive.components.ReceiveLoadingCard
import dev.arkbuilders.drop.app.ui.receive.components.ReceiveManualInputCard
import dev.arkbuilders.drop.app.ui.receive.components.ReceivePermissionRequestCard
import dev.arkbuilders.drop.app.ui.receive.components.ReceiveProgressCard
import dev.arkbuilders.drop.app.ui.receive.components.ReceiveQRCodeScannedCard
import dev.arkbuilders.drop.app.ui.receive.components.ReceiveReadyToScanCard
import dev.arkbuilders.drop.app.ui.receive.components.ReceiveScanningCard
import dev.arkbuilders.drop.app.ui.theme.DesignTokens
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

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
    transferManager: TransferManager,
) {
    val viewModel: ReceiveViewModel = hiltViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onCameraPermissionGranted(isGranted)
    }

    val uiState by viewModel.collectAsState()
    viewModel.collectSideEffect { effect ->
        when (effect) {
            ReceiveScreenEffect.HideKeyboard -> {
                keyboardController?.hide()
            }
            ReceiveScreenEffect.NavigateBack -> {
                navController.navigateUp()
            }
            ReceiveScreenEffect.RequestCameraPermission -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            ReceiveScreenEffect.ShowSuccessAnimation -> {

            }
        }
    }

    var showSuccessAnimation by remember { mutableStateOf(false) }

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
            targetState = uiState.workflowState,
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
                        ReceivePermissionRequestCard(
                            onRequestPermission = {
                                viewModel.onRequestCameraPermission()
                            },
                            onEnterManually = {
                                viewModel.onEnterManually()
                            }
                        )
                    } else {
                        ReceiveReadyToScanCard(
                            onStartScanning = { viewModel.onStartScanning() },
                            onEnterManually = { viewModel.onEnterManually() }
                        )
                    }
                }

                is ReceiveWorkflowState.RequestingPermission -> {
                    ReceiveLoadingCard(message = "Requesting camera permission...")
                }

                is ReceiveWorkflowState.Scanning -> {
                    ReceiveScanningCard(
                        onQRCodeScanned = { ticket, confirmation ->
                            viewModel.onQrCodeScanned(ticket, confirmation)
                        },
                        onError = { error ->
                            viewModel.onError(error)
                        },
                        onStopScanning = { viewModel.onStopScanning() },
                        onEnterManually = { viewModel.onEnterManually() }
                    )
                }

                is ReceiveWorkflowState.ManualInput -> {
                    ReceiveManualInputCard(
                        inputText = uiState.manualInputText,
                        onInputChange = {
                            viewModel.onManualInputChanged(it)
                        },
                        inputError = uiState.manualInputError,
                        onPasteFromClipboard = { viewModel.onPasteFromClipboard(clipboardManager.getText()?.text) },
                        onSubmit = { viewModel.handleManualInputSubmit() },
                        onCancel = {
                            viewModel.onCancelManualInput()
                        }
                    )
                }

                is ReceiveWorkflowState.QRCodeScanned -> {
                    ReceiveQRCodeScannedCard(
                        onAccept = {
                            viewModel.onAccept()
                        },
                        onScanAgain = {
                            viewModel.onScanAgain()
                        }
                    )
                }

                is ReceiveWorkflowState.Connecting -> {
                    ReceiveLoadingCard(message = "Connecting to sender...")
                }

                is ReceiveWorkflowState.Receiving -> {
                    uiState.receiveProgress?.let { progress ->
                        ReceiveProgressCard(
                            progress = progress,
                            onCancel = {
                                viewModel.onCancelReceiving()
                            }
                        )
                    }
                }

                is ReceiveWorkflowState.Success -> {
                    if (!showSuccessAnimation) {
                        ReceiveCompleteCard(
                            receivedFiles = uiState.receivedFiles,
                            onReceiveMore = {
                                viewModel.onReceiveMore()
                            },
                            onDone = {
                                viewModel.onDone()
                            }
                        )
                    }
                }

                is ReceiveWorkflowState.Error -> {
                    ReceiveErrorCard(
                        error = state.error,
                        onRetry = {
                            viewModel.onErrorRetry()
                        },
                        onDismiss = {
                            viewModel.onErrorDismiss()
                        }
                    )
                }
            }
        }

        if (uiState.workflowState !is ReceiveWorkflowState.Success
            && uiState.workflowState !is ReceiveWorkflowState.Error) {
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
