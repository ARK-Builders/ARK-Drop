@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.arkbuilders.drop.domain.libwrapper.send

import dev.arkbuilders.drop.bridge.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import platform.darwin.NSObject

class DropSendFilesBubbleImpl(
    private val bubble: ArkDropSendFilesBubbleProtocol,
) : DropSendFilesBubble {
    override suspend fun cancel() {
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                bubble.cancelWithCompletion { error ->
                    if (error != null) {
                        cont.resumeWithException(Exception(error.localizedDescription))
                    } else {
                        cont.resume(Unit)
                    }
                }
                cont.invokeOnCancellation {
                    // Handle cancellation if needed
                }
            }
        }
    }

    override fun getConfirmation(): UByte {
        return bubble.getConfirmation()
    }

    override fun getCreatedAt(): String {
        return bubble.getCreatedAt()
    }

    override fun getTicket(): String {
        return bubble.getTicket()
    }

    override fun isConnected(): Boolean {
        return bubble.isConnected()
    }

    override fun isFinished(): Boolean {
        return bubble.isFinished()
    }

    override fun subscribe(subscriber: DropSendFilesSubscriber) {
        val adapter = ArkDropSendFilesSubscriberAdapter(subscriber)
        bubble.subscribeWithSubscriber(adapter)
    }

    override fun unsubscribe(subscriber: DropSendFilesSubscriber) {
        val adapter = ArkDropSendFilesSubscriberAdapter(subscriber)
        bubble.unsubscribeWithSubscriber(adapter)
    }
}

/**
 * Adapter that wraps DropSendFilesSubscriber to implement ArkDropSendFilesSubscriber protocol
 */
private class ArkDropSendFilesSubscriberAdapter(
    private val subscriber: DropSendFilesSubscriber
) : NSObject(), ArkDropSendFilesSubscriberProtocol {
    private val native = (subscriber as? DropSendFilesSubscriberImpl)
        ?.native as? SendFilesSubscriberImpl
        ?: throw IllegalArgumentException("Invalid subscriber type")

    override fun getId(): String = native.getId()

    override fun logWithMessage(message: String) {
        native.log(message)
    }

    override fun notifySendingWithName(name: String, sent: ULong, remaining: ULong) {
        native.updateSendingProgress(name, sent, remaining)
    }

    override fun notifyConnectingWithReceiverName(receiverName: String, receiverAvatarB64: String?) {
        native.updateConnectionStatus(receiverName, receiverAvatarB64)
    }
}
