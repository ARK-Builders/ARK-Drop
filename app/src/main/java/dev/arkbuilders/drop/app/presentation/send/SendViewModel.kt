package dev.arkbuilders.drop.app.presentation.send

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import dev.arkbuilders.drop.app.data.repository.SendSessionRepo
import dev.arkbuilders.drop.app.domain.ResourcesHelper
import dev.arkbuilders.drop.app.domain.model.SendSession
import dev.arkbuilders.drop.app.domain.repository.NetworkStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import timber.log.Timber
import kotlin.collections.isNotEmpty
import kotlin.collections.plus
import kotlin.collections.sumOf

class SendViewModel(
    private val resourcesHelper: ResourcesHelper,
    private val networkStatus: NetworkStatus,
    private val sendSessionRepo: SendSessionRepo,
) : ViewModel(), ContainerHost<SendScreenState, SendScreenEffect> {
    override val container: Container<SendScreenState, SendScreenEffect> =
        container(SendScreenState.FileSelection())

    fun onAddFiles() =
        intent {
            postSideEffect(SendScreenEffect.LaunchFilePicker)
        }

    fun onFilesAdded(newFiles: List<String>) =
        intent {
            val s = state
            if (s is SendScreenState.FileSelection) {
                val validated = resourcesHelper.validateUris(newFiles)
                val allFiles = s.files + validated.first
                val canStartTransfer = allFiles.isNotEmpty() && networkStatus.isOnline()

                val size = allFiles.sumOf { resourcesHelper.getFileSize(it) }

                reduce {
                    s.copy(files = allFiles, size = size, canStartTransfer = canStartTransfer)
                }
            }
        }

    fun onFileRemove(file: String) =
        intent {
            val s = state
            if (s is SendScreenState.FileSelection) {
                val newFiles = s.files - file
                val size = newFiles.sumOf { resourcesHelper.getFileSize(it) }
                reduce {
                    s.copy(files = newFiles, size = size)
                }
            }
        }

    fun onStartTransfer() =
        intent {
            val s = state
            if (s !is SendScreenState.FileSelection) {
                return@intent
            }
            reduce {
                SendScreenState.GeneratingQR(s.files)
            }

            val session = sendSessionRepo.sendFiles(s.files.map { it.toUri() })
            if (session == null) {
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

            if (ticket.isEmpty()) {
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

            val qrBitmap = generateQRCodeSafely(ticket, confirmation)
            if (qrBitmap == null) {
                reduce {
                    SendScreenState.Error(
                        session = session,
                        files = s.files,
                        error = SendException.QRGenerationFailed,
                    )
                }
                return@intent
            }
            listenToSendProgress(session)
            monitorTransferCompletion(session)
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
                    is SendScreenState.WaitingForReceiver -> s.session
                    is SendScreenState.Transfer -> s.session
                    else -> null
                }
            session?.let { sendSessionRepo.cancelSend(it) }
            postSideEffect(SendScreenEffect.NavigateBack)
        }

    fun onCancelQrGeneration() =
        intent {
            val s = state
            val files =
                if (s is SendScreenState.GeneratingQR) {
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
                sendSessionRepo.recordSendCompletion(
                    s.files.map { it.toUri() },
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
                sendSessionRepo.cancelSend(s.session)
            }
            postSideEffect(SendScreenEffect.NavigateBack)
        }

    fun onSendMore() =
        intent {
            val s = state
            if (s is SendScreenState.Complete) {
                sendSessionRepo.cancelSend(s.session)
            }
            reduce {
                SendScreenState.FileSelection()
            }
        }

    fun onErrorRetry() =
        intent {
            val s = state
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
                s.session?.let {
                    sendSessionRepo.cancelSend(it)
                }
            }
            postSideEffect(SendScreenEffect.NavigateBack)
        }

    private fun listenToSendProgress(session: SendSession) {
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
                    if (progress.isConnected.not())
                        return@intent

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
                    Timber.e("Transfer interrupted: ${e::class.simpleName} ${e.message}")
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
            while (coroutineContext.isActive) {
                val isFinished = session.bubble.isFinished()
                if (isFinished) {
                    onComplete()
                    break
                }
                delay(500)
            }
        }
    }

    private fun generateQRCodeSafely(
        ticket: String,
        confirmation: UByte,
    ): Bitmap? {
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
                    bitmap[x, y] =
                        if (bitMatrix[x, y]) {
                            Color.BLACK
                        } else {
                            Color.WHITE
                        }
                }
            }
            return bitmap
        } catch (e: Throwable) {
            Timber.e("Unexpected error during QR code generation: ${e.message}")
            return null
        }
    }
}
