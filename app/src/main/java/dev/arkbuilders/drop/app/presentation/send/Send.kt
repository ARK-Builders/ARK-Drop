package dev.arkbuilders.drop.app.presentation.send

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import dev.arkbuilders.drop.app.presentation.components.DropErrorCard
import dev.arkbuilders.drop.app.presentation.components.DropTopBarBack
import dev.arkbuilders.drop.app.presentation.send.components.phase.FileSelectionPhase
import dev.arkbuilders.drop.app.presentation.send.components.phase.GeneratingQRPhase
import dev.arkbuilders.drop.app.presentation.send.components.phase.TransferCompletePhase
import dev.arkbuilders.drop.app.presentation.send.components.phase.TransferringPhase
import dev.arkbuilders.drop.app.presentation.send.components.phase.WaitingForReceiverPhase
import org.koin.compose.koinInject
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Send(navController: NavController) {
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
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
                )
            }

            is SendScreenState.GeneratingQR -> {
                GeneratingQRPhase(onCancel = { viewModel.onCancelQrGeneration() })
            }

            is SendScreenState.WaitingForReceiver -> {
                WaitingForReceiverPhase(
                    qrBitmap = sendScreenState.qrBitmap,
                    copyString = sendScreenState.copyString,
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
    }
}

private fun SendException.toMessage() =
    when (this) {
        SendException.TransferInitializationFailed -> "Transfer initialization failed"
        SendException.QRGenerationFailed -> "QR generation failed"
        SendException.TransferInterrupted -> "Transfer interrupted"
    }
