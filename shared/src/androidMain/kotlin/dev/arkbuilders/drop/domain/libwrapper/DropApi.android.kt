package dev.arkbuilders.drop.domain.libwrapper

import dev.arkbuilders.drop.ReceiveFilesRequest
import dev.arkbuilders.drop.ReceiverConfig
import dev.arkbuilders.drop.ReceiverProfile
import dev.arkbuilders.drop.SendFilesRequest
import dev.arkbuilders.drop.SenderConfig
import dev.arkbuilders.drop.SenderFile
import dev.arkbuilders.drop.SenderFileData
import dev.arkbuilders.drop.SenderProfile
import dev.arkbuilders.drop.domain.libwrapper.receive.DropReceiveFilesBubble
import dev.arkbuilders.drop.domain.libwrapper.receive.DropReceiveFilesBubbleImpl
import dev.arkbuilders.drop.domain.libwrapper.receive.DropReceiveFilesSubscriber
import dev.arkbuilders.drop.domain.libwrapper.receive.DropReceiveFilesSubscriberImpl
import dev.arkbuilders.drop.domain.libwrapper.receive.ReceiveFilesSubscriberImpl
import dev.arkbuilders.drop.domain.libwrapper.receive.request.DropReceiveFilesRequest
import dev.arkbuilders.drop.domain.libwrapper.send.DropSendFilesBubble
import dev.arkbuilders.drop.domain.libwrapper.send.DropSendFilesBubbleImpl
import dev.arkbuilders.drop.domain.libwrapper.send.DropSendFilesSubscriber
import dev.arkbuilders.drop.domain.libwrapper.send.DropSendFilesSubscriberImpl
import dev.arkbuilders.drop.domain.libwrapper.send.SendFilesSubscriberImpl
import dev.arkbuilders.drop.domain.libwrapper.send.request.DropSendFilesRequest

class AndroidDropApi : DropApi {
    override fun createSendSubscriber(): DropSendFilesSubscriber {
        return DropSendFilesSubscriberImpl(SendFilesSubscriberImpl())
    }

    override suspend fun sendFiles(request: DropSendFilesRequest): DropSendFilesBubble {
        val senderProfile = SenderProfile(request.profile.name, request.profile.avatarB64)
        val files: List<SenderFile> =
            request.files.map {
                val data =
                    object : SenderFileData {
                        override fun len(): ULong {
                            return it.data.len()
                        }

                        override fun read(): UByte? {
                            return it.data.read()
                        }

                        override fun readChunk(size: Int): ByteArray {
                            return it.data.readChunk(size)
                        }
                    }
                SenderFile(it.name, data)
            }
        val config =
            request.config?.let {
                SenderConfig(it.chunkSize, it.parallelStreams)
            }
        val nativeBubble =
            dev.arkbuilders.drop.sendFiles(SendFilesRequest(senderProfile, files, config))
        return DropSendFilesBubbleImpl(nativeBubble)
    }

    override fun createReceiveSubscriber(): DropReceiveFilesSubscriber {
        return DropReceiveFilesSubscriberImpl(ReceiveFilesSubscriberImpl())
    }

    override suspend fun receiveFiles(request: DropReceiveFilesRequest): DropReceiveFilesBubble {
        val request =
            ReceiveFilesRequest(
                request.ticket,
                request.confirmation,
                ReceiverProfile(request.profile.name, request.profile.avatarB64),
                ReceiverConfig(request.config.chunkSize, request.config.parallelStreams),
            )
        val nativeBubble = dev.arkbuilders.drop.receiveFiles(request)
        return DropReceiveFilesBubbleImpl(nativeBubble)
    }
}

actual fun getDropApi(): DropApi {
    return AndroidDropApi()
}
