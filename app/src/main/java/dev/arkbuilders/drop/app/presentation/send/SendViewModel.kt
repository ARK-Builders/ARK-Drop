package dev.arkbuilders.drop.app.presentation.send

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import dev.arkbuilders.drop.app.data.repository.TransferManager
import dev.arkbuilders.drop.app.domain.ResourcesHelper
import dev.arkbuilders.drop.app.domain.repository.NetworkStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

sealed class SendScreenState {
    data class FileSelection(
        val files: List<String> = emptyList<String>(),
        val size: Long = 0L,
        val canStartTransfer: Boolean = false,
    ) : SendScreenState()

    data class GeneratingQR(
        val files: List<String>,
    ) : SendScreenState()

    data class WaitingForReceiver(
        val files: List<String>,
        val qrBitmap: Bitmap,
        val copyString: String,
    ) : SendScreenState()

    data class Transfer(
        val files: List<String>,
        val isConnected: Boolean = false,
        val receiverName: String = "",
        val receiverAvatar: String? = null,
        val currentFileName: String = "",
        val filesCompleted: Int = 0,
        val totalFiles: Int = 0,
        val bytesTransferred: Long = 0L,
        val totalBytes: Long = 0L,
        val transferSpeedBps: Long = 0L,
        val estimatedTimeRemaining: Long = 0L
    ) : SendScreenState()

    data class Complete(val files: List<String>) : SendScreenState()

    data class Error(val error: Throwable) : SendScreenState()
}

sealed class SendScreenEffect {
    data object LaunchFilePicker : SendScreenEffect()
    data object NavigateBack : SendScreenEffect()
}

class SendViewModel(
    private val resourcesHelper: ResourcesHelper,
    private val networkStatus: NetworkStatus,
    private val transferManager: TransferManager,
): ViewModel(), ContainerHost<SendScreenState, SendScreenEffect> {
    override val container: Container<SendScreenState, SendScreenEffect> =
        container(SendScreenState.FileSelection())

    fun onAddFiles() = intent {
        postSideEffect(SendScreenEffect.LaunchFilePicker)
    }

    fun onFilesAdded(newFiles: List<String>) = intent {
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

    fun onFileRemove(file: String) = intent {
        val s = state
        if (s is SendScreenState.FileSelection) {
            val newFiles = s.files - file
            val size = newFiles.sumOf { resourcesHelper.getFileSize(it) }
            reduce {
                s.copy(files = newFiles, size = size)
            }
        }
    }

    fun onStartTransfer() = intent {
        val s = state
        if (s !is SendScreenState.FileSelection) {
            reduce {
                SendScreenState.Error(Error(""))
            }
            return@intent
        }
        val bubble = transferManager.sendFiles(s.files.map { it.toUri() })
        if (bubble == null) {
            reduce {
                SendScreenState.Error(Error(""))
            }
            return@intent
        }
        val ticket = transferManager.getCurrentSendTicket() ?: ""
        val confirmation = transferManager.getCurrentSendConfirmation() ?: 0u

        if (ticket.isEmpty()) {
            reduce {
                SendScreenState.Error(Error(""))
            }
            return@intent
        }
        val copyString = "${bubble.getTicket()} ${bubble.getConfirmation()}"

        val qrBitmap = generateQRCodeSafely(ticket, confirmation)
        if (qrBitmap == null) {
            reduce {
                SendScreenState.Error(Error(""))
            }
            return@intent
        }
        listenToSendProgress()
        monitorTransferCompletion()
        reduce {
            SendScreenState.WaitingForReceiver(
                files = s.files,
                qrBitmap = qrBitmap,
                copyString = copyString,
            )
        }
    }

    fun onCancelTransfer() = intent {
        transferManager.cancelSend()
        postSideEffect(SendScreenEffect.NavigateBack)
    }

    fun onCancelQrGeneration() = intent {
        reduce {
            SendScreenState.FileSelection()
        }
    }

    fun onComplete() = intent {
        val s = state
        if (s is SendScreenState.Transfer) {
            transferManager.recordSendCompletion(s.files.map { it.toUri() })
            reduce {
                SendScreenState.Complete(files = s.files)
            }
        }
    }

    fun onDone() = intent {
        transferManager.cancelSend()
        postSideEffect(SendScreenEffect.NavigateBack)
    }

    fun onSendMore() = intent {
        transferManager.cancelSend()
        reduce {
            SendScreenState.FileSelection()
        }
    }

    private fun listenToSendProgress() {
        transferManager.sendProgress!!.onEach { progress ->
            intent {
                if (progress.isConnected.not())
                    return@intent

                val s = state
                val files = when (s) {
                    is SendScreenState.Transfer -> s.files
                    is SendScreenState.WaitingForReceiver -> s.files
                    else -> return@intent
                }

                val transfer = SendScreenState.Transfer(
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
            }
        }.launchIn(viewModelScope)
    }

    private fun monitorTransferCompletion() {
        viewModelScope.launch {
           while (coroutineContext.isActive) {
               val isFinished = transferManager.isSendFinished()
               if (isFinished) {
                   onComplete()
                   break
               }
               delay(500)
           }
        }
    }

    private fun generateQRCodeSafely(ticket: String, confirmation: UByte): Bitmap? {
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
                    bitmap[x, y] = if (bitMatrix[x, y]) {
                        Color.BLACK
                    } else {
                        Color.WHITE
                    }
                }
            }
            return bitmap
        } catch (e: WriterException) {
            // TODO
 //           throw RuntimeException("QR code generation failed: ${e.message}", e)
            return null
        } catch (e: Exception) {
 //           throw RuntimeException("Unexpected error during QR code generation: ${e.message}", e)
            return null
        }
    }
}