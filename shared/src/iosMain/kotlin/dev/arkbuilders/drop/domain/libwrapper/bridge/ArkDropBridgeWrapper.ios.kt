@file:OptIn(ExperimentalForeignApi::class)

package dev.arkbuilders.drop.domain.libwrapper.bridge

import dev.arkbuilders.drop.bridge.*
import dev.arkbuilders.drop.domain.libwrapper.receive.DropReceiveFilesBubble
import dev.arkbuilders.drop.domain.libwrapper.receive.DropReceiveFilesSubscriber
import dev.arkbuilders.drop.domain.libwrapper.receive.request.DropReceiveFilesRequest
import dev.arkbuilders.drop.domain.libwrapper.send.DropSendFilesBubble
import dev.arkbuilders.drop.domain.libwrapper.send.DropSendFilesSubscriber
import dev.arkbuilders.drop.domain.libwrapper.send.request.DropSendFilesRequest
import dev.arkbuilders.drop.domain.libwrapper.send.request.DropSenderFileData
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.darwin.NSObject

/**
 * Wrapper for ArkDrop Objective-C bridge
 * This bridges between Kotlin types and Objective-C bridge types
 */
object ArkDropBridgeWrapper {

    suspend fun sendFiles(request: DropSendFilesRequest): DropSendFilesBubble {
        val profile = ArkDropSenderProfile().apply {
            this.name = request.profile.name
            this.avatarB64 = request.profile.avatarB64
        }

        val files = request.files.map { file ->
            val fileData = ArkDropSenderFileDataAdapter(file.data)
            ArkDropSenderFile().apply {
                this.name = file.name
                this.data = fileData
            }
        }

        val config = request.config?.let { c ->
            ArkDropSenderConfig().apply {
                this.chunkSize = c.chunkSize
                this.parallelStreams = c.parallelStreams
            }
        }

        val bridgeRequest = ArkDropSendFilesRequest().apply {
            this.profile = profile
            this.files = files
            this.config = config
        }

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            memScoped {
                val bubblePtr = alloc<ObjCObjectVar<dev.arkbuilders.drop.bridge.ArkDropSendFilesBubbleProtocol?>>()
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()

                dev.arkbuilders.drop.bridge.ArkDropBridge.sendFilesWithRequest(bridgeRequest, bubble = bubblePtr.ptr, error = errorPtr.ptr)

                val error = errorPtr.value
                if (error != null) {
                    throw Exception("Failed to send files: ${error.localizedDescription}")
                }

                val bubble = bubblePtr.value ?: throw Exception("Failed to create send bubble")
                ArkDropSendFilesBubbleWrapper(bubble)
            }
        }
    }

    suspend fun receiveFiles(request: DropReceiveFilesRequest): DropReceiveFilesBubble {
        val profile = ArkDropReceiverProfile().apply {
            this.name = request.profile.name
            this.avatarB64 = request.profile.avatarB64
        }

        val config = ArkDropReceiverConfig().apply {
            this.chunkSize = request.config.chunkSize
            this.parallelStreams = request.config.parallelStreams
        }

        val bridgeRequest = ArkDropReceiveFilesRequest().apply {
            this.ticket = request.ticket
            this.confirmation = request.confirmation
            this.profile = profile
            this.config = config
        }

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            memScoped {
                val bubblePtr = alloc<ObjCObjectVar<dev.arkbuilders.drop.bridge.ArkDropReceiveFilesBubbleProtocol?>>()
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()

                dev.arkbuilders.drop.bridge.ArkDropBridge.receiveFilesWithRequest(bridgeRequest, bubble = bubblePtr.ptr, error = errorPtr.ptr)

                val error = errorPtr.value
                if (error != null) {
                    throw Exception("Failed to receive files: ${error.localizedDescription}")
                }

                val bubble = bubblePtr.value ?: throw Exception("Failed to create receive bubble")
                ArkDropReceiveFilesBubbleWrapper(bubble)
            }
        }
    }
}

/**
 * Adapter that wraps DropSenderFileData to implement ArkDropSenderFileData protocol
 * CRITICAL: Must catch ALL exceptions to prevent crossing into Rust
 */
private class ArkDropSenderFileDataAdapter(
    private val data: DropSenderFileData
) : NSObject(), dev.arkbuilders.drop.bridge.ArkDropSenderFileDataProtocol {
    override fun len(): ULong {
        return try {
            data.len()
        } catch (e: Exception) {
            println("⚠️ ArkDropSenderFileDataAdapter.len() error: $e")
            0u
        }
    }

    override fun read(): NSNumber? {
        return try {
            val byte = data.read()
            byte?.let { NSNumber.numberWithUnsignedChar(it) }
        } catch (e: Exception) {
            println("⚠️ ArkDropSenderFileDataAdapter.read() error: $e")
            null
        }
    }

    override fun readChunkWithSize(size: Int): NSData {
        return try {
            val bytes = data.readChunk(size)
            bytes.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), length = bytes.size.toULong())
            }
        } catch (e: Exception) {
            println("⚠️ ArkDropSenderFileDataAdapter.readChunk() error: $e")
            // Return empty NSData on error
            NSData.data()
        }
    }
}

/**
 * Wrapper for ArkDropSendFilesBubble
 */
private class ArkDropSendFilesBubbleWrapper(
    private val bubble: dev.arkbuilders.drop.bridge.ArkDropSendFilesBubbleProtocol
) : DropSendFilesBubble {
    override suspend fun cancel() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                bubble.cancelWithCompletion { error ->
                    if (error != null) {
                        cont.resumeWith(Result.failure(Exception(error.localizedDescription)))
                    } else {
                        cont.resumeWith(Result.success(Unit))
                    }
                }
                cont.invokeOnCancellation {
                    // Handle cancellation if needed
                }
            }
        }
    }

    override fun getConfirmation(): UByte = bubble.getConfirmation()

    override fun getCreatedAt(): String = bubble.getCreatedAt()

    override fun getTicket(): String = bubble.getTicket()

    override fun isConnected(): Boolean = bubble.isConnected()

    override fun isFinished(): Boolean = bubble.isFinished()

    override fun subscribe(subscriber: DropSendFilesSubscriber) {
        val bridgeSubscriber = ArkDropSendFilesSubscriberAdapter(subscriber)
        bubble.subscribeWithSubscriber(bridgeSubscriber)
    }

    override fun unsubscribe(subscriber: DropSendFilesSubscriber) {
        val bridgeSubscriber = ArkDropSendFilesSubscriberAdapter(subscriber)
        bubble.unsubscribeWithSubscriber(bridgeSubscriber)
    }
}

/**
 * Wrapper for ArkDropReceiveFilesBubble
 */
private class ArkDropReceiveFilesBubbleWrapper(
    private val bubble: dev.arkbuilders.drop.bridge.ArkDropReceiveFilesBubbleProtocol
) : DropReceiveFilesBubble {
    override fun cancel() {
        bubble.cancel()
    }

    override fun isCancelled(): Boolean = bubble.isCancelled()

    override fun isFinished(): Boolean = bubble.isFinished()

    override fun start() {
        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            bubble.startWithError(errorPtr.ptr)
            val error = errorPtr.value
            if (error != null) {
                throw Exception("Failed to start receive: ${error.localizedDescription}")
            }
        }
    }

    override fun subscribe(subscriber: DropReceiveFilesSubscriber) {
        val bridgeSubscriber = ArkDropReceiveFilesSubscriberAdapter(subscriber)
        bubble.subscribeWithSubscriber(bridgeSubscriber)
    }

    override fun unsubscribe(subscriber: DropReceiveFilesSubscriber) {
        val bridgeSubscriber = ArkDropReceiveFilesSubscriberAdapter(subscriber)
        bubble.unsubscribeWithSubscriber(bridgeSubscriber)
    }
}

/**
 * Adapter that wraps DropSendFilesSubscriber to implement ArkDropSendFilesSubscriber protocol
 */
private class ArkDropSendFilesSubscriberAdapter(
    private val subscriber: DropSendFilesSubscriber
) : NSObject(), dev.arkbuilders.drop.bridge.ArkDropSendFilesSubscriberProtocol {
    private val native = (subscriber as? dev.arkbuilders.drop.domain.libwrapper.send.DropSendFilesSubscriberImpl)
        ?.native as? dev.arkbuilders.drop.domain.libwrapper.send.SendFilesSubscriberImpl
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

/**
 * Adapter that wraps DropReceiveFilesSubscriber to implement ArkDropReceiveFilesSubscriber protocol
 */
private class ArkDropReceiveFilesSubscriberAdapter(
    private val subscriber: DropReceiveFilesSubscriber
) : NSObject(), dev.arkbuilders.drop.bridge.ArkDropReceiveFilesSubscriberProtocol {
    private val native = (subscriber as? dev.arkbuilders.drop.domain.libwrapper.receive.DropReceiveFilesSubscriberImpl)
        ?.native as? dev.arkbuilders.drop.domain.libwrapper.receive.ReceiveFilesSubscriberImpl
        ?: throw IllegalArgumentException("Invalid subscriber type")

    override fun getId(): String = native.getId()

    override fun logWithMessage(message: String) {
        native.log(message)
    }

    override fun notifyReceivingWithFileId(fileId: String, data: NSData) {
        try {
            val length = data.length.toInt()
            val bytes = ByteArray(length)
            bytes.usePinned { pinned ->
                data.getBytes(pinned.addressOf(0).reinterpret(), length.toULong())
            }
            native.appendReceivedData(fileId, bytes)
        } catch (e: Exception) {
            println("⚠️ ArkDropReceiveFilesSubscriberAdapter.notifyReceiving error: $e")
        }
    }

    override fun notifyConnectingWithSenderName(
        senderName: String,
        senderAvatarB64: String?,
        files: List<*>
    ) {
        try {
            val fileInfos = files.mapNotNull { fileDict ->
                val dict = fileDict as? Map<*, *> ?: return@mapNotNull null
                val id = dict["id"] as? String ?: return@mapNotNull null
                val name = dict["name"] as? String ?: return@mapNotNull null
                val len = (dict["len"] as? Number)?.toLong()?.toULong() ?: return@mapNotNull null

                dev.arkbuilders.drop.domain.libwrapper.receive.ReceiveFileInfo(
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
        } catch (e: Exception) {
            println("⚠️ ArkDropReceiveFilesSubscriberAdapter.notifyConnecting error: $e")
        }
    }
}
