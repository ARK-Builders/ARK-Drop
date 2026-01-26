package dev.arkbuilders.drop.domain.libwrapper

import dev.arkbuilders.drop.domain.libwrapper.bridge.ArkDropBridgeWrapper
import dev.arkbuilders.drop.domain.libwrapper.receive.DropReceiveFilesBubble
import dev.arkbuilders.drop.domain.libwrapper.receive.DropReceiveFilesSubscriber
import dev.arkbuilders.drop.domain.libwrapper.receive.DropReceiveFilesSubscriberImpl
import dev.arkbuilders.drop.domain.libwrapper.receive.ReceiveFilesSubscriberImpl
import dev.arkbuilders.drop.domain.libwrapper.receive.request.DropReceiveFilesRequest
import dev.arkbuilders.drop.domain.libwrapper.send.DropSendFilesBubble
import dev.arkbuilders.drop.domain.libwrapper.send.DropSendFilesSubscriber
import dev.arkbuilders.drop.domain.libwrapper.send.DropSendFilesSubscriberImpl
import dev.arkbuilders.drop.domain.libwrapper.send.SendFilesSubscriberImpl
import dev.arkbuilders.drop.domain.libwrapper.send.request.DropSendFilesRequest

class IOSDropApi : DropApi {
    override fun createSendSubscriber(): DropSendFilesSubscriber {
        return DropSendFilesSubscriberImpl(SendFilesSubscriberImpl())
    }

    override suspend fun sendFiles(request: DropSendFilesRequest): DropSendFilesBubble {
        // Use the bridge wrapper to call the Objective-C bridge
        return ArkDropBridgeWrapper.sendFiles(request)
    }

    override fun createReceiveSubscriber(): DropReceiveFilesSubscriber {
        return DropReceiveFilesSubscriberImpl(ReceiveFilesSubscriberImpl())
    }

    override suspend fun receiveFiles(request: DropReceiveFilesRequest): DropReceiveFilesBubble {
        // Use the bridge wrapper to call the Objective-C bridge
        return ArkDropBridgeWrapper.receiveFiles(request)
    }
}

actual fun getDropApi(): DropApi {
    return IOSDropApi()
}
