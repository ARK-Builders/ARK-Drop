package dev.arkbuilders.drop.app.data

import android.util.Log
import dev.arkbuilders.drop.SendFilesConnectingEvent
import dev.arkbuilders.drop.SendFilesSendingEvent
import dev.arkbuilders.drop.SendFilesSubscriber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class SendingProgress(
    val fileName: String = "",
    val sent: ULong = 0UL,
    val remaining: ULong = 0UL,
    val isConnected: Boolean = false,
    val receiverName: String = "",
    val receiverAvatar: String? = null
)

class SendFilesSubscriberImpl : SendFilesSubscriber {
    
    companion object {
        private const val TAG = "SendFilesSubscriber"
    }
    
    private val id = UUID.randomUUID().toString()
    
    private val _progress = MutableStateFlow(SendingProgress())
    val progress: StateFlow<SendingProgress> = _progress.asStateFlow()
    
    override fun getId(): String = id

    override fun log(message: String) {
        Log.d(TAG, message)
    }

    override fun notifySending(event: SendFilesSendingEvent) {
        Log.d(TAG, "Sending progress: ${event.name} - sent: ${event.sent}, remaining: ${event.remaining}")
        
        _progress.value = _progress.value.copy(
            fileName = event.name,
            sent = event.sent,
            remaining = event.remaining
        )
    }
    
    override fun notifyConnecting(event: SendFilesConnectingEvent) {
        Log.d(TAG, "Connected to receiver: ${event.receiver.name}")
        
        _progress.value = _progress.value.copy(
            isConnected = true,
            receiverName = event.receiver.name,
            receiverAvatar = event.receiver.avatarB64
        )
    }
    
    fun reset() {
        _progress.value = SendingProgress()
    }
}
