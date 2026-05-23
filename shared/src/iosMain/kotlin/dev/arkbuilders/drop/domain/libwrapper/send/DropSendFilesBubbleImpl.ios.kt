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
        crashlytics_log("DropSendFilesBubble: cancel called")
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                bubble.cancelWithCompletion { error ->
                    if (error != null) {
                        crashlytics_recordError("DropSendFilesBubble: cancel failed error=${error.localizedDescription}", null)
                        cont.resumeWithException(Exception(error.localizedDescription))
                    } else {
                        crashlytics_log("DropSendFilesBubble: cancel completed")
                        cont.resume(Unit)
                    }
                }
                cont.invokeOnCancellation {
                    crashlytics_log("DropSendFilesBubble: cancel was cancelled")
                }
            }
        }
    }

    override fun getConfirmation(): UByte {
        val confirmation = bubble.getConfirmation()
        crashlytics_log("DropSendFilesBubble: getConfirmation=$confirmation")
        return confirmation
    }

    override fun getCreatedAt(): String {
        val createdAt = bubble.getCreatedAt()
        crashlytics_log("DropSendFilesBubble: getCreatedAt=$createdAt")
        return createdAt
    }

    override fun getTicket(): String {
        val ticket = bubble.getTicket()
        crashlytics_log("DropSendFilesBubble: getTicket=$ticket")
        return ticket
    }

    override fun isConnected(): Boolean {
        val connected = bubble.isConnected()
        crashlytics_log("DropSendFilesBubble: isConnected=$connected")
        return connected
    }

    override fun isFinished(): Boolean {
        val finished = bubble.isFinished()
        crashlytics_log("DropSendFilesBubble: isFinished=$finished")
        return finished
    }

    override fun subscribe(subscriber: DropSendFilesSubscriber) {
        crashlytics_log("DropSendFilesBubble: subscribing subscriber")
        val adapter = ArkDropSendFilesSubscriberAdapter(subscriber)
        bubble.subscribeWithSubscriber(adapter)
        crashlytics_log("DropSendFilesBubble: subscribed")
    }

    override fun unsubscribe(subscriber: DropSendFilesSubscriber) {
        crashlytics_log("DropSendFilesBubble: unsubscribing subscriber")
        val adapter = ArkDropSendFilesSubscriberAdapter(subscriber)
        bubble.unsubscribeWithSubscriber(adapter)
        crashlytics_log("DropSendFilesBubble: unsubscribed")
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
        crashlytics_log("ArkDropSendFilesSubscriberAdapter: sending progress name=$name sent=$sent remaining=$remaining")
        native.updateSendingProgress(name, sent, remaining)
    }

    override fun notifyConnectingWithReceiverName(receiverName: String, receiverAvatarB64: String?) {
        crashlytics_log("ArkDropSendFilesSubscriberAdapter: connecting to receiver=$receiverName")
        native.updateConnectionStatus(receiverName, receiverAvatarB64)
    }
}
