package dev.arkbuilders.drop.app.domain.model

import dev.arkbuilders.drop.SendFilesToBubble
import dev.arkbuilders.drop.app.data.SendFilesToSubscriberImpl
import dev.arkbuilders.drop.app.data.SendingProgress
import kotlinx.coroutines.flow.StateFlow

class SendToSession(
    private val bubble: SendFilesToBubble,
    private val subscriber: SendFilesToSubscriberImpl,
) : ISendSession {
    override fun isFinished(): Boolean = bubble.isFinished()

    override suspend fun cancel() {
        bubble.cancel()
        bubble.unsubscribe(subscriber)
    }

    override fun ticket(): String? = null

    override fun confirmation(): UByte? = null

    override val progress: StateFlow<SendingProgress> = subscriber.progress
}
