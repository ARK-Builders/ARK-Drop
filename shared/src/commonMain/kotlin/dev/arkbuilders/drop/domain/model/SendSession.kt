package dev.arkbuilders.drop.domain.model

import dev.arkbuilders.drop.domain.libwrapper.send.DropSendFilesBubble
import dev.arkbuilders.drop.domain.libwrapper.send.DropSendFilesSubscriber

class SendSession(
    val bubble: DropSendFilesBubble,
    val subscriber: DropSendFilesSubscriber,
)
