package dev.arkbuilders.drop.app.domain.model

import dev.arkbuilders.drop.ReceiveFilesBubble
import dev.arkbuilders.drop.app.data.ReceiveFilesSubscriberImpl

class ReceiveSession(
    val bubble: ReceiveFilesBubble,
    val subscriber: ReceiveFilesSubscriberImpl,
)
