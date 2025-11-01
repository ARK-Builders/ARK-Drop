package dev.arkbuilders.drop.app.presentation.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.arkbuilders.drop.app.data.repository.TransferManager
import dev.arkbuilders.drop.app.data.ReceivingProgress
import dev.arkbuilders.drop.app.domain.PermissionsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

sealed class ReceiveScreenState {
    data class Initial(val cameraPermissionGranted: Boolean) : ReceiveScreenState()
    data object RequestingPermission : ReceiveScreenState()
    data object Scanning : ReceiveScreenState()
    data class ManualInput(val inputText: String, val inputError: String?) : ReceiveScreenState()
    data class QRCodeScanned(val ticket: String, val confirmation: UByte) : ReceiveScreenState()
    data object Connecting : ReceiveScreenState()
    data class Receiving(val progress: ReceivingProgress) : ReceiveScreenState()
    data class Success(val receivedFiles: List<String>) : ReceiveScreenState()
    data class Error(val error: ReceiveError) : ReceiveScreenState()
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
        container(ReceiveScreenState.Initial(false))

    init {
        intent {
            val receiveProgress = transferManager.receiveProgress?.value
            val state = if (receiveProgress != null && receiveProgress.isConnected) {
                listenToProgress()
                ReceiveScreenState.Receiving(receiveProgress)
            } else {
                ReceiveScreenState.Initial(permissionsHelper.isCameraGranted())
            }
            reduce {
                state
            }
        }
    }

    fun onRequestCameraPermission() = intent {
        reduce {
            ReceiveScreenState.RequestingPermission
        }
        postSideEffect(ReceiveScreenEffect.RequestCameraPermission)
    }

    fun onEnterManually() = intent {
        reduce {
            ReceiveScreenState.ManualInput(inputText = "", inputError = null)
        }
    }

    fun onStartScanning() = intent {
        reduce {
            ReceiveScreenState.Scanning
        }
    }

    fun onStopScanning() = intent {
        reduce {
            ReceiveScreenState.Initial(permissionsHelper.isCameraGranted())
        }
    }

    fun onError(error: ReceiveError) = intent {
        reduce {
            ReceiveScreenState.Error(error)
        }
    }

    fun onAccept() = intent {
        try {
            val s = state
            if (s !is ReceiveScreenState.QRCodeScanned) {
                return@intent
            }
            val ticket = s.ticket
            val confirmation = s.confirmation

            reduce {
                ReceiveScreenState.Connecting
            }

            val bubble =
                transferManager.receiveFiles(ticket, confirmation)
            if (bubble != null) {
                reduce {
                    ReceiveScreenState.Receiving(transferManager.receiveProgress!!.value)
                }
                listenToProgress()
            } else {
                reduce {
                    ReceiveScreenState.Error(ReceiveError.ConnectionFailed)
                }
            }
        } catch (e: Exception) {
            val error = when {
                e.message?.contains(
                    "network",
                    ignoreCase = true
                ) == true -> ReceiveError.NetworkError

                else -> ReceiveError.ConnectionFailed
            }

            reduce {
                ReceiveScreenState.Error(error)
            }
        }
    }

    fun onCameraPermissionGranted(isGranted: Boolean) = intent {
        val state = if (isGranted) {
            ReceiveScreenState.Scanning
        } else {
            ReceiveScreenState.Error(ReceiveError.CameraPermissionDenied)
        }
        reduce {
            state
        }
    }

    fun onScanAgain() = intent {
        val state = if (permissionsHelper.isCameraGranted()) {
            ReceiveScreenState.Scanning
        } else {
            ReceiveScreenState.ManualInput(inputText = "", inputError = null)
        }
        reduce {
            state
        }
    }

    fun onReceiveMore() = intent {
        transferManager.cancelReceive()
        reduce {
            ReceiveScreenState.Initial(permissionsHelper.isCameraGranted())
        }
    }

    fun onDone() = intent {
        transferManager.cancelReceive()
        postSideEffect(ReceiveScreenEffect.NavigateBack)
    }

    fun onPasteFromClipboard(clipText: String?) = intent {
        val s = state
        if (s !is ReceiveScreenState.ManualInput)
            return@intent

        if (!clipText.isNullOrEmpty()) {
            reduce {
                s.copy(
                    inputText = clipText,
                    inputError = null,
                )
            }
        }
    }

    fun onErrorRetry() = intent {
        reduce {
            ReceiveScreenState.Initial(permissionsHelper.isCameraGranted())
        }
    }

    fun onErrorDismiss() = intent {
        transferManager.cancelReceive()
        postSideEffect(ReceiveScreenEffect.NavigateBack)
    }

    fun onQrCodeScanned(ticket: String, confirmation: UByte) = intent {
        reduce {
            ReceiveScreenState.QRCodeScanned(ticket, confirmation)
        }
    }

    fun onManualInputChanged(input: String) = blockingIntent {
        val s = state
        if (s !is ReceiveScreenState.ManualInput)
            return@blockingIntent

        reduce {
            s.copy(
                inputText = input,
                inputError = null,
            )
        }
    }

    fun onCancelReceiving() = intent {
        transferManager.cancelReceive()
        reduce {
            ReceiveScreenState.Initial(permissionsHelper.isCameraGranted())
        }
    }

    fun onCancelManualInput() = intent {
        reduce {
            ReceiveScreenState.Initial(permissionsHelper.isCameraGranted())
        }
        postSideEffect(ReceiveScreenEffect.HideKeyboard)
    }

    fun handleManualInputSubmit() = intent {
        val s = state
        if (s !is ReceiveScreenState.ManualInput)
            return@intent

        val parsed = parseManualInput(s.inputText)
        if (parsed != null) {
            reduce {
                ReceiveScreenState.QRCodeScanned(
                    ticket = parsed.first,
                    confirmation = parsed.second
                )
            }
            postSideEffect(ReceiveScreenEffect.HideKeyboard)
        } else {
            reduce {
                s.copy(
                    inputError =  "Invalid format. Please enter: ticket confirmation",
                )
            }
        }
    }

    private fun listenToProgress() {
        transferManager.receiveProgress!!.onEach { progress ->
            intent {
                val s = state
                if (s !is ReceiveScreenState.Receiving)
                    return@intent

                reduce {
                    s.copy(
                        progress = progress
                    )
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
                                reduce {
                                    ReceiveScreenState.Success(
                                        receivedFiles = savedFiles.map { it.name }
                                    )
                                }
                                postSideEffect(ReceiveScreenEffect.ShowSuccessAnimation)
                            } else {
                                reduce {
                                    ReceiveScreenState.Error(
                                        ReceiveError.NoFilesReceived
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            val error = when {
                                e.message?.contains("storage", ignoreCase = true) == true ->
                                    ReceiveError.StorageError

                                e.message?.contains("network", ignoreCase = true) == true ->
                                    ReceiveError.NetworkError

                                else -> ReceiveError.UnknownError
                            }
                            reduce {
                                ReceiveScreenState.Error(error)
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