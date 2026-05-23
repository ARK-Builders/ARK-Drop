@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.arkbuilders.drop.domain.libwrapper.receive

import dev.arkbuilders.drop.bridge.*
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.darwin.NSObject

class DropReceiveFilesBubbleImpl(
    private val bubble: ArkDropReceiveFilesBubbleProtocol,
) : DropReceiveFilesBubble {
    override fun cancel() {
        crashlytics_log("DropReceiveFilesBubble: cancel called")
        bubble.cancel()
    }

    override fun isCancelled(): Boolean {
        val cancelled = bubble.isCancelled()
        crashlytics_log("DropReceiveFilesBubble: isCancelled=$cancelled")
        return cancelled
    }

    override fun isFinished(): Boolean {
        val finished = bubble.isFinished()
        crashlytics_log("DropReceiveFilesBubble: isFinished=$finished")
        return finished
    }

    override fun start() {
        crashlytics_log("DropReceiveFilesBubble: starting bubble")
        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            bubble.startWithError(errorPtr.ptr)
            val error = errorPtr.value
            if (error != null) {
                crashlytics_recordError("DropReceiveFilesBubble: start failed error=${error.localizedDescription}", null)
                throw Exception("Failed to start: ${error.localizedDescription}")
            }
        }
        crashlytics_log("DropReceiveFilesBubble: started successfully")
    }

    override fun subscribe(subscriber: DropReceiveFilesSubscriber) {
        crashlytics_log("DropReceiveFilesBubble: subscribing subscriber")
        val adapter = ArkDropReceiveFilesSubscriberAdapter(subscriber)
        bubble.subscribeWithSubscriber(adapter)
        crashlytics_log("DropReceiveFilesBubble: subscribed")
    }

    override fun unsubscribe(subscriber: DropReceiveFilesSubscriber) {
        crashlytics_log("DropReceiveFilesBubble: unsubscribing subscriber")
        val adapter = ArkDropReceiveFilesSubscriberAdapter(subscriber)
        bubble.unsubscribeWithSubscriber(adapter)
        crashlytics_log("DropReceiveFilesBubble: unsubscribed")
    }
}

private class ArkDropReceiveFilesSubscriberAdapter(
    private val subscriber: DropReceiveFilesSubscriber
) : NSObject(), ArkDropReceiveFilesSubscriberProtocol {
    private val native = (subscriber as? DropReceiveFilesSubscriberImpl)
        ?.native as? ReceiveFilesSubscriberImpl
        ?: throw IllegalArgumentException("Invalid subscriber type")

    override fun getId(): String = native.getId()

    override fun logWithMessage(message: String) {
        native.log(message)
    }

    override fun notifyReceivingWithFileId(fileId: String, data: NSData) {
        val length = data.length.toInt()
        crashlytics_log("ArkDropReceiveFilesSubscriberAdapter: receiving data fileId=$fileId bytes=$length")
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            data.getBytes(pinned.addressOf(0), length = length.toULong())
        }
        native.appendReceivedData(fileId, bytes)
    }

    override fun notifyConnectingWithSenderName(
        senderName: String,
        senderAvatarB64: String?,
        files: List<*>
    ) {
        crashlytics_log("ArkDropReceiveFilesSubscriberAdapter: connected to sender=$senderName fileCount=${files.size}")

        val fileInfos = files.mapNotNull { fileDict ->
            val dict = fileDict as? Map<*, *> ?: return@mapNotNull null
            val id = dict["id"] as? String ?: return@mapNotNull null
            val name = dict["name"] as? String ?: return@mapNotNull null
            val len = (dict["len"] as? Number)?.toLong()?.toULong() ?: return@mapNotNull null

            ReceiveFileInfo(
                id = id,
                name = name,
                size = len
            )
        }

        crashlytics_log("ArkDropReceiveFilesSubscriberAdapter: parsed fileInfos count=${fileInfos.size}")
        fileInfos.forEach { info ->
            crashlytics_log("ArkDropReceiveFilesSubscriberAdapter: file id=${info.id} name=${info.name} size=${info.size}")
        }

        val currentProgress = native.progress.value
        native._progress.value = currentProgress.copy(
            isConnected = true,
            senderName = senderName,
            senderAvatar = senderAvatarB64,
            files = fileInfos
        )
    }
}
