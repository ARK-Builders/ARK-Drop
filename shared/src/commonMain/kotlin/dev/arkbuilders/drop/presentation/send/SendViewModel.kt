@file:OptIn(ExperimentalTime::class)

package dev.arkbuilders.drop.presentation.send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.arkbuilders.drop.data.helper.NetworkStatus
import dev.arkbuilders.drop.data.helper.ResourcesHelper
import dev.arkbuilders.drop.domain.model.SendSession
import dev.arkbuilders.drop.domain.repository.SendSessionRepo
import dev.arkbuilders.drop.instrumentation.FirebaseReporter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.orbitmvi.orbit.viewmodel.container

class SendViewModel(
    private val resourcesHelper: ResourcesHelper,
    private val networkStatus: NetworkStatus,
    private val sendSessionRepo: SendSessionRepo,
    private val firebaseReporter: FirebaseReporter,
) : ViewModel(), ContainerHost<SendScreenState, SendScreenEffect> {
    override val container: Container<SendScreenState, SendScreenEffect> =
        container(SendScreenState.FileSelection())

    init {
        firebaseReporter.log("SendViewModel: initialized")
    }

    fun onAddFiles() =
        intent {
            firebaseReporter.log("SendViewModel: user tapped add files")
            postSideEffect(SendScreenEffect.LaunchFilePicker)
        }

    fun onFilesAdded(newFiles: List<String>) =
        intent {
            val s = state
            if (s is SendScreenState.FileSelection) {
                firebaseReporter.log("SendViewModel: files added - new: ${newFiles.size}, current: ${s.files.size}")

                val validated = resourcesHelper.validateUris(newFiles)
                val allFiles = s.files + validated.first
                val canStartTransfer = allFiles.isNotEmpty() && networkStatus.isOnline()

                val size = allFiles.sumOf { resourcesHelper.getFileSize(it) }

                firebaseReporter.log("SendViewModel: files validated - valid: ${validated.first.size}, skipped: ${validated.second}, total: ${allFiles.size}, totalSize: $size bytes, canStart: $canStartTransfer")

                reduce {
                    s.copy(files = allFiles, size = size, canStartTransfer = canStartTransfer)
                }
            }
        }

    fun onFileRemove(file: String) =
        intent {
            val s = state
            if (s is SendScreenState.FileSelection) {
                val removedName = resourcesHelper.getFileName(file) ?: file
                val newFiles = s.files - file
                val size = newFiles.sumOf { resourcesHelper.getFileSize(it) }
                firebaseReporter.log("SendViewModel: file removed - name: $removedName, remaining: ${newFiles.size}, remainingSize: $size bytes")
                reduce {
                    s.copy(files = newFiles, size = size)
                }
            }
        }

    fun onStartTransfer() =
        intent {
            val s = state
            if (s !is SendScreenState.FileSelection) {
                firebaseReporter.log("SendViewModel: start transfer ignored - wrong state: ${s::class.simpleName}")
                return@intent
            }

            firebaseReporter.setCustomKey("send_file_count", s.files.size.toString())
            firebaseReporter.setCustomKey("send_total_bytes", s.size.toString())
            firebaseReporter.log("SendViewModel: starting transfer - files: ${s.files.size}, totalSize: ${s.size} bytes, online: ${networkStatus.isOnline()}")

            reduce {
                SendScreenState.GeneratingQR(s.files)
            }

            val session = sendSessionRepo.sendFiles(s.files)
            if (session == null) {
                firebaseReporter.recordError("SendViewModel: session creation failed - could not initialize transfer")
                reduce {
                    SendScreenState.Error(
                        files = s.files,
                        error = SendException.TransferInitializationFailed,
                    )
                }
                return@intent
            }
            val ticket = session.bubble.getTicket()
            val confirmation = session.bubble.getConfirmation()
            firebaseReporter.log("SendViewModel: session created - ticket: $ticket, confirmation: $confirmation")

            if (ticket.isEmpty()) {
                firebaseReporter.recordError("SendViewModel: empty ticket received from bridge")
                reduce {
                    SendScreenState.Error(
                        session = session,
                        files = s.files,
                        error = SendException.TransferInitializationFailed,
                    )
                }
                return@intent
            }
            val copyString = "${session.bubble.getTicket()} ${session.bubble.getConfirmation()}"

            firebaseReporter.log("SendViewModel: generating QR code for ticket: $ticket")
            val qrBitmap = resourcesHelper.generateQRCode(ticket, confirmation)
            if (qrBitmap == null) {
                firebaseReporter.recordError("SendViewModel: QR generation failed for ticket: $ticket")
                reduce {
                    SendScreenState.Error(
                        session = session,
                        files = s.files,
                        error = SendException.QRGenerationFailed,
                    )
                }
                return@intent
            }
            firebaseReporter.log("SendViewModel: QR code generated successfully - size: ${qrBitmap.size} bytes")

            listenToSendProgress(session)
            monitorTransferCompletion(session)
            firebaseReporter.log("SendViewModel: waiting for receiver - ticket: $ticket")
            reduce {
                SendScreenState.WaitingForReceiver(
                    session = session,
                    files = s.files,
                    qrBitmap = qrBitmap,
                    copyString = copyString,
                )
            }
        }

    fun onCancelTransfer() =
        intent {
            val s = state
            val session =
                when (s) {
                    is SendScreenState.WaitingForReceiver -> {
                        firebaseReporter.log("SendViewModel: user cancelled while waiting - ticket: ${s.session.bubble.getTicket()}")
                        s.session
                    }
                    is SendScreenState.Transfer -> {
                        firebaseReporter.log("SendViewModel: user cancelled during transfer - ticket: ${s.session.bubble.getTicket()}, fileName: ${s.currentFileName}")
                        s.session
                    }
                    else -> {
                        firebaseReporter.log("SendViewModel: cancel ignored - state: ${s::class.simpleName}")
                        null
                    }
                }
            session?.let { sendSessionRepo.cancelSend(it) }
            firebaseReporter.setCustomKey("send_file_count", "0")
            firebaseReporter.setCustomKey("send_total_bytes", "0")
            postSideEffect(SendScreenEffect.NavigateBack)
        }

    fun onCancelQrGeneration() =
        intent {
            val s = state
            val files =
                if (s is SendScreenState.GeneratingQR) {
                    firebaseReporter.log("SendViewModel: user cancelled QR generation")
                    s.files
                } else {
                    emptyList()
                }

            reduce {
                SendScreenState.FileSelection(files)
            }
        }

    fun onComplete() =
        intent {
            val s = state
            if (s is SendScreenState.Transfer) {
                val progress = s.session.subscriber.progress.value
                firebaseReporter.log("SendViewModel: transfer completed - receiver: ${s.receiverName}, files: ${s.files.size}, sent: ${progress.sent}, remaining: ${progress.remaining}")
                firebaseReporter.setCustomKey("send_completed_at", Clock.System.now().toEpochMilliseconds().toString())

                sendSessionRepo.recordSendCompletion(
                    s.files,
                    s.session,
                )
                reduce {
                    SendScreenState.Complete(session = s.session, files = s.files)
                }
            }
        }

    fun onDone() =
        intent {
            val s = state
            if (s is SendScreenState.Complete) {
                firebaseReporter.log("SendViewModel: user done - ${s.files.size} files sent")
                sendSessionRepo.cancelSend(s.session)
            }
            firebaseReporter.setCustomKey("send_file_count", "0")
            firebaseReporter.setCustomKey("send_total_bytes", "0")
            postSideEffect(SendScreenEffect.NavigateBack)
        }

    fun onSendMore() =
        intent {
            val s = state
            if (s is SendScreenState.Complete) {
                firebaseReporter.log("SendViewModel: user wants to send more files")
                sendSessionRepo.cancelSend(s.session)
            }
            firebaseReporter.setCustomKey("send_file_count", "0")
            firebaseReporter.setCustomKey("send_total_bytes", "0")
            reduce {
                SendScreenState.FileSelection()
            }
        }

    fun onErrorRetry() =
        intent {
            val s = state
            firebaseReporter.log("SendViewModel: user retrying after error - errorType: ${(s as? SendScreenState.Error)?.error}")

            if (s is SendScreenState.Error) {
                s.session?.let {
                    sendSessionRepo.cancelSend(it)
                }
            }
            val files =
                when (s) {
                    is SendScreenState.Error -> s.files
                    else -> emptyList()
                } ?: emptyList()

            val validated = resourcesHelper.validateUris(files).first
            val canStartTransfer = validated.isNotEmpty() && networkStatus.isOnline()
            val size = validated.sumOf { resourcesHelper.getFileSize(it) }

            firebaseReporter.log("SendViewModel: retry with ${validated.size} valid files, canStart: $canStartTransfer")

            reduce {
                SendScreenState.FileSelection(
                    files = validated,
                    size = size,
                    canStartTransfer = canStartTransfer,
                )
            }
        }

    fun onErrorDismiss() =
        intent {
            val s = state
            if (s is SendScreenState.Error) {
                firebaseReporter.log("SendViewModel: user dismissed error - errorType: ${s.error}")
                s.session?.let {
                    sendSessionRepo.cancelSend(it)
                }
            }
            firebaseReporter.setCustomKey("send_file_count", "0")
            firebaseReporter.setCustomKey("send_total_bytes", "0")
            postSideEffect(SendScreenEffect.NavigateBack)
        }

    private fun listenToSendProgress(session: SendSession) {
        val chunkBytes = 10L * 1024L * 1024L
        var lastSent = -1L
        var lastLoggedChunk = -1L

        session.subscriber.progress.onEach { progress ->
            intent {
                val s = state
                val files =
                    when (s) {
                        is SendScreenState.Transfer -> s.files
                        is SendScreenState.WaitingForReceiver -> s.files
                        else -> return@intent
                    }

                try {
                    val wasConnected = (s as? SendScreenState.Transfer)?.isConnected ?: false
                    val justConnected = progress.isConnected && !wasConnected

                    if (progress.isConnected.not())
                        return@intent

                    if (justConnected) {
                        firebaseReporter.log("SendViewModel: receiver connected - name: ${progress.receiverName}, fileName: ${progress.fileName}")
                    }

                    val sent = progress.sent.toLong()
                    val fileReset = sent < lastSent
                    lastSent = sent

                    val currentChunk = sent / chunkBytes
                    if (fileReset || currentChunk > lastLoggedChunk) {
                        lastLoggedChunk = currentChunk
                        firebaseReporter.log("SendViewModel: transfer progress - file: ${progress.fileName}, sent: ${progress.sent}, remaining: ${progress.remaining}")
                    }

                    val transfer =
                        SendScreenState.Transfer(
                            session = session,
                            files = files,
                            isConnected = progress.isConnected,
                            receiverName = progress.receiverName,
                            receiverAvatar = progress.receiverAvatar,
                            currentFileName = progress.fileName,
                            bytesTransferred = progress.sent.toLong(),
                            totalBytes = (progress.sent + progress.remaining).toLong(),
                            transferSpeedBps = 0L,
                            estimatedTimeRemaining = 0L,
                        )

                    reduce {
                        transfer
                    }
                } catch (e: Throwable) {
                    firebaseReporter.recordError("SendViewModel: transfer progress error - ${e::class.simpleName}: ${e.message}", e)
                    Logger.e("Transfer interrupted: ${e::class.simpleName} ${e.message}")
                    reduce {
                        SendScreenState.Error(
                            session = session,
                            files = files,
                            error = SendException.TransferInterrupted,
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun monitorTransferCompletion(session: SendSession) {
        viewModelScope.launch {
            val ticket = session.bubble.getTicket()
            firebaseReporter.log("SendViewModel: monitoring transfer completion - ticket: $ticket")

            while (coroutineContext.isActive) {
                val isFinished = session.bubble.isFinished()
                if (isFinished) {
                    firebaseReporter.log("SendViewModel: transfer finished signal detected - ticket: $ticket")
                    onComplete()
                    break
                }
                delay(500)
            }
        }
    }
}
