package dev.arkbuilders.drop.domain.libwrapper.send

import dev.arkbuilders.drop.SendFilesConnectingEvent
import dev.arkbuilders.drop.SendFilesSendingEvent
import dev.arkbuilders.drop.SendFilesSubscriber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.UUID

class DropSendFilesSubscriberImpl(val native: SendFilesSubscriberImpl) : DropSendFilesSubscriber {
    override val progress: StateFlow<DropSendingProgress> = native.progress
}

class SendFilesSubscriberImpl : SendFilesSubscriber {
    companion object {
        private const val TAG = "SendFilesSubscriber"
    }

    private val id = UUID.randomUUID().toString()

    private val _progress = MutableStateFlow(DropSendingProgress())
    val progress: StateFlow<DropSendingProgress> = _progress.asStateFlow()

    override fun getId(): String = id

    override fun log(message: String) {
        Timber.tag(TAG).d(message)
    }

    override fun notifySending(event: SendFilesSendingEvent) {
        log("Sending progress: ${event.name} - sent: ${event.sent}, remaining: ${event.remaining}")

        _progress.value =
            _progress.value.copy(
                fileName = event.name,
                sent = event.sent,
                remaining = event.remaining,
            )
    }

    override fun notifyConnecting(event: SendFilesConnectingEvent) {
        log("Connected to receiver: ${event.receiver.name}")

        _progress.value =
            _progress.value.copy(
                isConnected = true,
                receiverName = event.receiver.name,
                receiverAvatar = event.receiver.avatarB64,
            )
    }

    fun reset() {
        _progress.value = DropSendingProgress()
    }
}
