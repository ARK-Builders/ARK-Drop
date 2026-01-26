package dev.arkbuilders.drop.domain.libwrapper.send

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUUID

class DropSendFilesSubscriberImpl(val native: SendFilesSubscriberImpl) : DropSendFilesSubscriber {
    override val progress: StateFlow<DropSendingProgress> = native.progress
}

class SendFilesSubscriberImpl {
    companion object {
        private const val TAG = "SendFilesSubscriber"
    }

    private val id = NSUUID().UUIDString

    private val _progress = MutableStateFlow(DropSendingProgress())
    val progress: StateFlow<DropSendingProgress> = _progress.asStateFlow()

    fun getId(): String = id

    fun log(message: String) {
        // On iOS, we can use NSLog or a logging framework
        // For now, empty implementation as requested
    }

    // Note: These methods are called via the adapter from the bridge
    // The original event-based methods are no longer used directly

    fun reset() {
        _progress.value = DropSendingProgress()
    }
    
    /**
     * Helper method to update sending progress directly (for bridge use)
     */
    fun updateSendingProgress(fileName: String, sent: ULong, remaining: ULong) {
        _progress.value = _progress.value.copy(
            fileName = fileName,
            sent = sent,
            remaining = remaining
        )
    }
    
    /**
     * Helper method to update connection status (for bridge use)
     */
    fun updateConnectionStatus(receiverName: String, receiverAvatar: String?) {
        _progress.value = _progress.value.copy(
            isConnected = true,
            receiverName = receiverName,
            receiverAvatar = receiverAvatar
        )
    }
}
