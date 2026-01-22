package dev.arkbuilders.drop.presentation.receive

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.navigation.NavController
import dev.arkbuilders.drop.presentation.components.DropErrorCard
import dev.arkbuilders.drop.presentation.components.DropTopBarBack
import dev.arkbuilders.drop.presentation.receive.components.ReceiveCompleteCard
import dev.arkbuilders.drop.presentation.receive.components.ReceiveLoadingCard
import dev.arkbuilders.drop.presentation.receive.components.ReceiveManualInputCard
import dev.arkbuilders.drop.presentation.receive.components.ReceivePermissionRequestCard
import dev.arkbuilders.drop.presentation.receive.components.ReceiveProgressCard
import dev.arkbuilders.drop.presentation.receive.components.ReceiveQRCodeScannedCard
import dev.arkbuilders.drop.presentation.receive.components.ReceiveReadyToScanCard
import dev.arkbuilders.drop.presentation.receive.components.ReceiveScanningCard
import org.koin.compose.koinInject
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Receive(navController: NavController) {
    val viewModel: ReceiveViewModel = koinInject()
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            viewModel.onCameraPermissionGranted(isGranted)
        }

    val state by viewModel.collectAsState()
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
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        DropTopBarBack(
            title = "Receive files",
            onBackClick = { navController.navigateUp() },
        )

        val receiveScreenState = state
        when (receiveScreenState) {
            is ReceiveScreenState.Initial -> {
                if (receiveScreenState.cameraPermissionGranted) {
                    ReceiveReadyToScanCard(
                        onStartScanning = { viewModel.onStartScanning() },
                        onEnterManually = { viewModel.onEnterManually() },
                    )
                } else {
                    ReceivePermissionRequestCard(
                        onRequestPermission = {
                            viewModel.onRequestCameraPermission()
                        },
                        onEnterManually = {
                            viewModel.onEnterManually()
                        },
                    )
                }
            }

            ReceiveScreenState.RequestingPermission -> {
                ReceiveLoadingCard(message = "Requesting camera permission...")
            }

            ReceiveScreenState.Scanning -> {
                ReceiveScanningCard(
                    onQRCodeScanned = { ticket, confirmation ->
                        viewModel.onQrCodeScanned(ticket, confirmation)
                    },
                    onError = { error ->
                        viewModel.onError(error)
                    },
                    onStopScanning = { viewModel.onStopScanning() },
                    onEnterManually = { viewModel.onEnterManually() },
                )
            }

            is ReceiveScreenState.ManualInput -> {
                ReceiveManualInputCard(
                    inputText = receiveScreenState.inputText,
                    onInputChange = {
                        viewModel.onManualInputChanged(it)
                    },
                    inputError = receiveScreenState.inputError,
                    onPasteFromClipboard = {
                        viewModel.onPasteFromClipboard(
                            clipboardManager.getText()?.text,
                        )
                    },
                    onSubmit = { viewModel.handleManualInputSubmit() },
                    onCancel = {
                        viewModel.onCancelManualInput()
                    },
                )
            }

            is ReceiveScreenState.QRCodeScanned -> {
                ReceiveQRCodeScannedCard(
                    onAccept = {
                        viewModel.onAccept()
                    },
                    onScanAgain = {
                        viewModel.onScanAgain()
                    },
                )
            }

            ReceiveScreenState.Connecting -> {
                ReceiveLoadingCard(message = "Connecting to sender...")
            }

            is ReceiveScreenState.Receiving -> {
                ReceiveProgressCard(
                    progress = receiveScreenState.progress,
                    onCancel = {
                        viewModel.onCancelReceiving()
                    },
                )
            }

            is ReceiveScreenState.Success -> {
                ReceiveCompleteCard(
                    receivedFiles = receiveScreenState.receivedFiles,
                    onReceiveMore = {
                        viewModel.onReceiveMore()
                    },
                    onDone = {
                        viewModel.onDone()
                    },
                )
            }

            is ReceiveScreenState.Error -> {
                DropErrorCard(
                    message = receiveScreenState.error.toMessage(),
                    onRetry = {
                        viewModel.onErrorRetry()
                    },
                    onDismiss = {
                        viewModel.onErrorDismiss()
                    },
                )
            }
        }
    }
}

private fun ReceiveError.toMessage() =
    when (this) {
        ReceiveError.CameraInitializationFailed -> "Unable to initialize camera. Please try again."
        ReceiveError.CameraPermissionDenied -> "Camera permission is required to scan QR codes"
        ReceiveError.ConnectionFailed -> "Unable to connect to sender."
        ReceiveError.InvalidManualInput -> "Invalid format. Please enter: ticket confirmation"
        ReceiveError.InvalidQRCode ->
            "This QR code is not from Drop. Please scan a valid Drop QR code."

        ReceiveError.NetworkError ->
            "Network connection lost. Please check your connection and try again."

        ReceiveError.NoFilesReceived -> "No files were received from the sender."
        ReceiveError.StorageError -> "Unable to save files. Please check your storage permissions."
        ReceiveError.TransferInterrupted -> "File transfer was interrupted. Please try again."
        ReceiveError.UnknownError -> "An unexpected error occurred. Please try again."
    }
