package dev.arkbuilders.drop.app.presentation.send

import android.graphics.Bitmap
import dev.arkbuilders.drop.app.domain.model.ISendSession

sealed class SendScreenState {
    data class FileSelection(
        val files: List<String> = emptyList<String>(),
        val size: Long = 0L,
        val canStartTransfer: Boolean = false,
    ) : SendScreenState()

    data class GeneratingQR(
        val files: List<String>,
    ) : SendScreenState()

    data class Scanning(val files: List<String>) : SendScreenState()

    data class QRCodeScanned(
        val ticket: String,
        val confirmation: UByte,
        val files: List<String>,
    ) : SendScreenState()

    data class Connecting(
        val session: ISendSession,
        val files: List<String>,
    ) : SendScreenState()

    data class WaitingForReceiver(
        val session: ISendSession,
        val files: List<String>,
        val qrBitmap: Bitmap,
        val copyString: String,
    ) : SendScreenState()

    data class Transfer(
        val session: ISendSession,
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
        val estimatedTimeRemaining: Long = 0L,
    ) : SendScreenState()

    data class Complete(
        val session: ISendSession,
        val files: List<String>,
    ) : SendScreenState()

    data class Error(
        val session: ISendSession? = null,
        val files: List<String>? = null,
        val error: SendException,
    ) : SendScreenState()
}

sealed class SendScreenEffect {
    data object LaunchFilePicker : SendScreenEffect()

    data object NavigateBack : SendScreenEffect()
}

enum class SendException {
    TransferInitializationFailed,
    QRGenerationFailed,
    TransferInterrupted,
}
