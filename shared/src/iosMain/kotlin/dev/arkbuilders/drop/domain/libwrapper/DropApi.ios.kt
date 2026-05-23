@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.arkbuilders.drop.domain.libwrapper

import dev.arkbuilders.drop.bridge.crashlytics_log
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
        crashlytics_log("IOSDropApi: createSendSubscriber")
        return DropSendFilesSubscriberImpl(SendFilesSubscriberImpl())
    }

    override suspend fun sendFiles(request: DropSendFilesRequest): DropSendFilesBubble {
        // Use the bridge wrapper to call the Objective-C bridge
        crashlytics_log("IOSDropApi: sendFiles profile=${request.profile.name} files=${request.files.size}")
        return ArkDropBridgeWrapper.sendFiles(request)
    }

    override fun createReceiveSubscriber(): DropReceiveFilesSubscriber {
        crashlytics_log("IOSDropApi: createReceiveSubscriber")
        return DropReceiveFilesSubscriberImpl(ReceiveFilesSubscriberImpl())
    }

    override suspend fun receiveFiles(request: DropReceiveFilesRequest): DropReceiveFilesBubble {
        // Use the bridge wrapper to call the Objective-C bridge
        crashlytics_log("IOSDropApi: receiveFiles ticket=${request.ticket} confirmation=${request.confirmation}")
        return ArkDropBridgeWrapper.receiveFiles(request)
    }
}

actual fun getDropApi(): DropApi {
    crashlytics_log("getDropApi: returning IOSDropApi instance")
    return IOSDropApi()
}
