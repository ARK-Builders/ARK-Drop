package dev.arkbuilders.drop.app.presentation.receive

import dev.arkbuilders.drop.app.data.ReceivingProgress
import dev.arkbuilders.drop.app.domain.model.ReceiveSession

sealed class ReceiveScreenState {
    data class Initial(val cameraPermissionGranted: Boolean) : ReceiveScreenState()

    data object RequestingPermission : ReceiveScreenState()

    data object Scanning : ReceiveScreenState()

    data class ManualInput(val inputText: String, val inputError: String?) : ReceiveScreenState()

    data class QRCodeScanned(val ticket: String, val confirmation: UByte) : ReceiveScreenState()

    data object Connecting : ReceiveScreenState()

    data class Receiving(
        val session: ReceiveSession,
        val progress: ReceivingProgress,
    ) : ReceiveScreenState()

    data class Success(
        val session: ReceiveSession,
        val receivedFiles: List<String>,
    ) : ReceiveScreenState()

    data class Error(
        val session: ReceiveSession? = null,
        val error: ReceiveError,
    ) : ReceiveScreenState()
}

sealed class ReceiveScreenEffect {
    data object HideKeyboard : ReceiveScreenEffect()

    data object NavigateBack : ReceiveScreenEffect()

    data object ShowSuccessAnimation : ReceiveScreenEffect()

    data object RequestCameraPermission : ReceiveScreenEffect()
}

enum class ReceiveError {
    CameraPermissionDenied,
    CameraInitializationFailed,
    InvalidQRCode,
    InvalidManualInput,
    ConnectionFailed,
    TransferInterrupted,
    NoFilesReceived,
    StorageError,
    NetworkError,
    UnknownError,
}
