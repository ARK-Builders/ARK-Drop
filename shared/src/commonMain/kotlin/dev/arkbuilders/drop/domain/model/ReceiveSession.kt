package dev.arkbuilders.drop.domain.model

import dev.arkbuilders.drop.domain.libwrapper.receive.DropReceiveFilesBubble
import dev.arkbuilders.drop.domain.libwrapper.receive.DropReceiveFilesSubscriber

class ReceiveSession(
    val bubble: DropReceiveFilesBubble,
    val subscriber: DropReceiveFilesSubscriber,
)
