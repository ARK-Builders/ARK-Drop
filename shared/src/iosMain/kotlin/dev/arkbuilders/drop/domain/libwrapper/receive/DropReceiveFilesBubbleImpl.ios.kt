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
        bubble.cancel()
    }

    override fun isCancelled(): Boolean {
        return bubble.isCancelled()
    }

    override fun isFinished(): Boolean {
        return bubble.isFinished()
    }

    override fun start() {
        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            bubble.startWithError(errorPtr.ptr)
            val error = errorPtr.value
            if (error != null) {
                throw Exception("Failed to start: ${error.localizedDescription}")
            }
        }
    }

    override fun subscribe(subscriber: DropReceiveFilesSubscriber) {
        val adapter = ArkDropReceiveFilesSubscriberAdapter(subscriber)
        bubble.subscribeWithSubscriber(adapter)
    }

    override fun unsubscribe(subscriber: DropReceiveFilesSubscriber) {
        val adapter = ArkDropReceiveFilesSubscriberAdapter(subscriber)
        bubble.unsubscribeWithSubscriber(adapter)
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

        val currentProgress = native.progress.value
        native._progress.value = currentProgress.copy(
            isConnected = true,
            senderName = senderName,
            senderAvatar = senderAvatarB64,
            files = fileInfos
        )
    }
}
