package dev.arkbuilders.drop.app.presentation.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.arkbuilders.drop.app.data.repository.ReceiveSessionRepo
import dev.arkbuilders.drop.app.domain.PermissionsHelper
import dev.arkbuilders.drop.app.domain.model.ReceiveSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import timber.log.Timber

class ReceiveViewModel(
    private val receiveSessionRepo: ReceiveSessionRepo,
    private val permissionsHelper: PermissionsHelper,
) : ViewModel(), ContainerHost<ReceiveScreenState, ReceiveScreenEffect> {
    override val container: Container<ReceiveScreenState, ReceiveScreenEffect> =
        container(ReceiveScreenState.Initial(false))

    init {
        intent {
            reduce {
                ReceiveScreenState.Initial(permissionsHelper.isCameraGranted())
            }
        }
    }

    fun onRequestCameraPermission() =
        intent {
            reduce {
                ReceiveScreenState.RequestingPermission
            }
            postSideEffect(ReceiveScreenEffect.RequestCameraPermission)
        }

    fun onEnterManually() =
        intent {
            reduce {
                ReceiveScreenState.ManualInput(inputText = "", inputError = null)
            }
        }

    fun onStartScanning() =
        intent {
            reduce {
                ReceiveScreenState.Scanning
            }
        }

    fun onStopScanning() =
        intent {
            reduce {
                ReceiveScreenState.Initial(permissionsHelper.isCameraGranted())
            }
        }

    fun onError(error: ReceiveError) =
        intent {
            reduce {
                ReceiveScreenState.Error(error = error)
            }
        }

    fun onAccept() =
        intent {
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

                val session =
                    receiveSessionRepo.receiveFiles(ticket, confirmation)
                if (session != null) {
                    reduce {
                        ReceiveScreenState.Receiving(
                            session,
                            session.subscriber.progress.value,
                        )
                    }
                    listenToProgress(session)
                } else {
                    reduce {
                        ReceiveScreenState.Error(error = ReceiveError.ConnectionFailed)
                    }
                }
            } catch (e: Exception) {
                val error =
                    when {
                        e.message?.contains(
                            "network",
                            ignoreCase = true,
                        ) == true -> ReceiveError.NetworkError

                        else -> ReceiveError.ConnectionFailed
                    }

                reduce {
                    ReceiveScreenState.Error(error = error)
                }
            }
        }

    fun onCameraPermissionGranted(isGranted: Boolean) =
        intent {
            val state =
                if (isGranted) {
                    ReceiveScreenState.Scanning
                } else {
                    ReceiveScreenState.Error(error = ReceiveError.CameraPermissionDenied)
                }
            reduce {
                state
            }
        }

    fun onScanAgain() =
        intent {
            val state =
                if (permissionsHelper.isCameraGranted()) {
                    ReceiveScreenState.Scanning
                } else {
                    ReceiveScreenState.ManualInput(inputText = "", inputError = null)
                }
            reduce {
                state
            }
        }

    fun onReceiveMore() =
        intent {
            val s = state
            if (s is ReceiveScreenState.Success) {
                receiveSessionRepo.cancelReceive(s.session)
            }
            reduce {
                ReceiveScreenState.Initial(permissionsHelper.isCameraGranted())
            }
        }

    fun onDone() =
        intent {
            val s = state
            if (s is ReceiveScreenState.Success) {
                receiveSessionRepo.cancelReceive(s.session)
            }
            postSideEffect(ReceiveScreenEffect.NavigateBack)
        }

    fun onPasteFromClipboard(clipText: String?) =
        intent {
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

    fun onErrorRetry() =
        intent {
            reduce {
                ReceiveScreenState.Initial(permissionsHelper.isCameraGranted())
            }
        }

    fun onErrorDismiss() =
        intent {
            val s = state
            if (s is ReceiveScreenState.Error) {
                s.session?.let {
                    receiveSessionRepo.cancelReceive(it)
                }
            }
            postSideEffect(ReceiveScreenEffect.NavigateBack)
        }

    fun onQrCodeScanned(
        ticket: String,
        confirmation: UByte,
    ) = intent {
        reduce {
            ReceiveScreenState.QRCodeScanned(ticket, confirmation)
        }
    }

    fun onManualInputChanged(input: String) =
        blockingIntent {
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

    fun onCancelReceiving() =
        intent {
            val s = state
            if (s is ReceiveScreenState.Receiving) {
                receiveSessionRepo.cancelReceive(s.session)
            }

            reduce {
                ReceiveScreenState.Initial(permissionsHelper.isCameraGranted())
            }
        }

    fun onCancelManualInput() =
        intent {
            reduce {
                ReceiveScreenState.Initial(permissionsHelper.isCameraGranted())
            }
            postSideEffect(ReceiveScreenEffect.HideKeyboard)
        }

    fun handleManualInputSubmit() =
        intent {
            val s = state
            if (s !is ReceiveScreenState.ManualInput)
                return@intent

            val parsed = parseManualInput(s.inputText)
            if (parsed != null) {
                reduce {
                    ReceiveScreenState.QRCodeScanned(
                        ticket = parsed.first,
                        confirmation = parsed.second,
                    )
                }
                postSideEffect(ReceiveScreenEffect.HideKeyboard)
            } else {
                reduce {
                    s.copy(
                        inputError = "Invalid format. Please enter: ticket confirmation",
                    )
                }
            }
        }

    private fun listenToProgress(session: ReceiveSession) {
        session.subscriber.progress.onEach { progress ->
            intent {
                val s = state
                if (s !is ReceiveScreenState.Receiving)
                    return@intent

                reduce {
                    s.copy(
                        progress = progress,
                    )
                }

                if (progress.isConnected && progress.files.isNotEmpty()) {
                    // Check if all files are complete
                    val allFilesComplete =
                        progress.files.all { file ->
                            val fileProgress = progress.fileProgress[file.id]
                            fileProgress?.isComplete == true
                        }

                    if (allFilesComplete) {
                        // Small delay to ensure UI updates are visible
                        delay(1000)
                        try {
                            val savedFiles = receiveSessionRepo.saveReceivedFiles(session)
                            if (savedFiles.isNotEmpty()) {
                                reduce {
                                    ReceiveScreenState.Success(
                                        session = session,
                                        receivedFiles = savedFiles,
                                    )
                                }
                            } else {
                                reduce {
                                    ReceiveScreenState.Error(
                                        session = session,
                                        error = ReceiveError.NoFilesReceived,
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Timber.w("Save failed: ${e::class.simpleName} ${e.message}")
                            val error =
                                when {
                                    e.message?.contains("storage", ignoreCase = true) == true ->
                                        ReceiveError.StorageError

                                    e.message?.contains("network", ignoreCase = true) == true ->
                                        ReceiveError.NetworkError

                                    else -> ReceiveError.UnknownError
                                }
                            reduce {
                                ReceiveScreenState.Error(
                                    session = session,
                                    error = error,
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
