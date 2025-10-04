package dev.arkbuilders.drop.app.ui.receive

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.isGranted
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.arkbuilders.drop.app.TransferManager
import dev.arkbuilders.drop.app.data.ReceivingProgress
import dev.arkbuilders.drop.app.domain.PermissionsHelper
import dev.arkbuilders.drop.app.domain.model.TransferHistoryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

data class ReceiveScreenState(
    val workflowState: ReceiveWorkflowState,
    val scannedTicket: String?,
    val scannedConfirmation: UByte?,
    val manualInputText: String,
    val manualInputError: String?,
    val receivedFiles: List<String>,
    val isCameraPermissionGranted: Boolean,
    val receiveProgress: ReceivingProgress?,
) {
    companion object {
        fun initial() = ReceiveScreenState(
            workflowState = ReceiveWorkflowState.Initial,
            scannedTicket = null,
            scannedConfirmation = null,
            manualInputText = "",
            manualInputError = null,
            receivedFiles = emptyList(),
            isCameraPermissionGranted = false,
            receiveProgress = null,
        )
    }
}

sealed class ReceiveScreenEffect {
    data object HideKeyboard : ReceiveScreenEffect()
    data object NavigateBack : ReceiveScreenEffect()
    data object ShowSuccessAnimation : ReceiveScreenEffect()
    data object RequestCameraPermission : ReceiveScreenEffect()
}

@HiltViewModel
class ReceiveViewModel @Inject constructor(
    private val transferManager: TransferManager,
    private val permissionsHelper: PermissionsHelper,
) : ViewModel(), ContainerHost<ReceiveScreenState, ReceiveScreenEffect> {
    override val container: Container<ReceiveScreenState, ReceiveScreenEffect> =
        container(ReceiveScreenState.initial())

    init {
        intent {
            val receiveProgress = transferManager.receiveProgress?.value
            val workflowState = if (receiveProgress != null && receiveProgress.isConnected) {
                listenToProgress()
                ReceiveWorkflowState.Receiving
            } else {
                ReceiveWorkflowState.Initial
            }
            reduce {
                state.copy(
                    workflowState = workflowState,
                    receiveProgress = receiveProgress,
                )
            }
        }
    }

    fun onRequestCameraPermission() = intent {
        reduce {
            state.copy(
                workflowState = ReceiveWorkflowState.RequestingPermission
            )
        }
        postSideEffect(ReceiveScreenEffect.RequestCameraPermission)
    }

    fun onEnterManually() = intent {
        reduce {
            state.copy(
                workflowState = ReceiveWorkflowState.ManualInput
            )
        }
    }

    fun onStartScanning() = intent {
        reduce {
            state.copy(
                workflowState = ReceiveWorkflowState.Scanning
            )
        }
    }

    fun onStopScanning() = intent {
        reduce {
            state.copy(
                workflowState = ReceiveWorkflowState.Initial
            )
        }
    }

    fun onError(error: ReceiveError) = intent {
        reduce {
            state.copy(
                workflowState = ReceiveWorkflowState.Error(error)
            )
        }
    }

    fun onAccept() = intent {
        try {
            reduce {
                state.copy(
                    workflowState = ReceiveWorkflowState.Connecting
                )
            }

            val bubble =
                transferManager.receiveFiles(state.scannedTicket!!, state.scannedConfirmation!!)
            if (bubble != null) {
                reduce {
                    state.copy(
                        receiveProgress = transferManager.receiveProgress!!.value,
                        workflowState = ReceiveWorkflowState.Receiving
                    )
                }
                listenToProgress()
            } else {
                reduce {
                    state.copy(
                        workflowState = ReceiveWorkflowState.Error(ReceiveError.ConnectionFailed)
                    )
                }
            }
        } catch (e: Exception) {
            val workflowState = ReceiveWorkflowState.Error(
                when {
                    e.message?.contains(
                        "network",
                        ignoreCase = true
                    ) == true -> ReceiveError.NetworkError

                    else -> ReceiveError.ConnectionFailed
                }
            )
            reduce {
                state.copy(
                    workflowState = workflowState
                )
            }
        }
    }

    fun onCameraPermissionGranted(isGranted: Boolean) = intent {
        val workflowState = if (isGranted) {
            ReceiveWorkflowState.Scanning
        } else {
            ReceiveWorkflowState.Error(ReceiveError.CameraPermissionDenied)
        }
        reduce {
            state.copy(
                workflowState = workflowState,
                isCameraPermissionGranted = isGranted,
            )
        }
    }

    fun onScanAgain() = intent {
        val workflowState = if (permissionsHelper.isCameraGranted()) {
            ReceiveWorkflowState.Scanning
        } else {
            ReceiveWorkflowState.ManualInput
        }
        reduce {
            state.copy(
                workflowState = workflowState,
                scannedTicket = null,
                scannedConfirmation = null,
                manualInputText = "",
                manualInputError = null,
            )
        }
    }

    fun onReceiveMore() = intent {
        reduce {
            state.copy(
                workflowState = ReceiveWorkflowState.Initial,
                receivedFiles = emptyList(),
                scannedTicket = null,
                scannedConfirmation = null,
                manualInputText = "",
                manualInputError = null,
            )
        }
        transferManager.cancelReceive()
    }

    fun onDone() = intent {
        transferManager.cancelReceive()
        postSideEffect(ReceiveScreenEffect.NavigateBack)
    }

    fun onPasteFromClipboard(clipText: String?) = intent {
        if (!clipText.isNullOrEmpty()) {
            reduce {
                state.copy(
                    manualInputText = clipText,
                    manualInputError = null,
                )
            }
        }
    }

    fun onErrorRetry() = intent {
        reduce {
            state.copy(
                workflowState = ReceiveWorkflowState.Initial,
                scannedTicket = null,
                scannedConfirmation = null,
                manualInputText = "",
                manualInputError = null,
            )
        }
    }

    fun onErrorDismiss() = intent {
        transferManager.cancelReceive()
        postSideEffect(ReceiveScreenEffect.NavigateBack)
    }

    fun onQrCodeScanned(ticket: String, confirmation: UByte) = intent {
        reduce {
            state.copy(
                scannedTicket = ticket,
                scannedConfirmation = confirmation,
                workflowState = ReceiveWorkflowState.QRCodeScanned,
            )
        }
    }

    fun onManualInputChanged(input: String) = blockingIntent {
        reduce {
            state.copy(
                manualInputText = input,
                manualInputError = null,
            )
        }
    }

    fun onCancelReceiving() = intent {
        transferManager.cancelReceive()
        reduce {
            state.copy(
                workflowState = ReceiveWorkflowState.Initial,
                scannedTicket = null,
                scannedConfirmation = null,
                manualInputText = "",
                manualInputError = null,
            )
        }
    }

    fun onCancelManualInput() = intent {
        reduce {
            state.copy(
                workflowState = ReceiveWorkflowState.Initial,
                manualInputText = "",
                manualInputError = null,
            )
        }
        postSideEffect(ReceiveScreenEffect.HideKeyboard)
    }

    fun handleManualInputSubmit() = intent {
        val parsed = parseManualInput(state.manualInputText)
        if (parsed != null) {
            reduce {
                state.copy(
                    scannedTicket = parsed.first,
                    scannedConfirmation = parsed.second,
                    workflowState = ReceiveWorkflowState.QRCodeScanned,
                    manualInputError = null,
                )
            }
            postSideEffect(ReceiveScreenEffect.HideKeyboard)
        } else {
            reduce {
                state.copy(
                    manualInputError = "Invalid format. Please enter: ticket confirmation"
                )
            }
        }
    }

    private fun listenToProgress() {
        transferManager.receiveProgress!!.onEach { progress ->
            intent {
                reduce {
                    state.copy(
                        receiveProgress = progress
                    )
                }
            }

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
                            intent {
                                reduce {
                                    state.copy(
                                        receivedFiles = savedFiles.map { it.name },
                                        workflowState = ReceiveWorkflowState.Success,
                                    )
                                }
                                postSideEffect(ReceiveScreenEffect.ShowSuccessAnimation)
                            }
                        } else {
                            intent {
                                reduce {
                                    state.copy(
                                        workflowState = ReceiveWorkflowState.Error(ReceiveError.NoFilesReceived)
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        val workflowState = ReceiveWorkflowState.Error(
                            when {
                                e.message?.contains("storage", ignoreCase = true) == true ->
                                    ReceiveError.StorageError

                                e.message?.contains("network", ignoreCase = true) == true ->
                                    ReceiveError.NetworkError

                                else -> ReceiveError.UnknownError
                            }
                        )
                        intent {
                            reduce {
                                state.copy(
                                    workflowState = workflowState
                                )
                            }
                        }
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun parseManualInput(input: String): Pair<String, UByte>? {
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
}