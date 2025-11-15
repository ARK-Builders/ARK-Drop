package dev.arkbuilders.drop.app.domain.model

import dev.arkbuilders.drop.SendFilesBubble
import dev.arkbuilders.drop.app.data.SendFilesSubscriberImpl

class SendSession(
    val bubble: SendFilesBubble,
    val subscriber: SendFilesSubscriberImpl,
)
