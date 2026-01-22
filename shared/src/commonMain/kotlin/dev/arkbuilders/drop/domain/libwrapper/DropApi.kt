package dev.arkbuilders.drop.domain.libwrapper

import dev.arkbuilders.drop.domain.libwrapper.receive.DropReceiveFilesBubble
import dev.arkbuilders.drop.domain.libwrapper.receive.DropReceiveFilesSubscriber
import dev.arkbuilders.drop.domain.libwrapper.receive.request.DropReceiveFilesRequest
import dev.arkbuilders.drop.domain.libwrapper.send.DropSendFilesBubble
import dev.arkbuilders.drop.domain.libwrapper.send.DropSendFilesSubscriber
import dev.arkbuilders.drop.domain.libwrapper.send.request.DropSendFilesRequest

interface DropApi {
    fun createSendSubscriber(): DropSendFilesSubscriber

    suspend fun sendFiles(request: DropSendFilesRequest): DropSendFilesBubble

    fun createReceiveSubscriber(): DropReceiveFilesSubscriber

    suspend fun receiveFiles(request: DropReceiveFilesRequest): DropReceiveFilesBubble
}

expect fun getDropApi(): DropApi
