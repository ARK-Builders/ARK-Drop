package dev.arkbuilders.drop.domain.libwrapper.send

import dev.arkbuilders.drop.SendFilesBubble

class DropSendFilesBubbleImpl(
    private val bubble: SendFilesBubble,
) : DropSendFilesBubble {
    override suspend fun cancel() {
        bubble.cancel()
    }

    override fun getConfirmation(): UByte {
        return bubble.getConfirmation()
    }

    override fun getCreatedAt(): String {
        return bubble.getCreatedAt()
    }

    override fun getTicket(): String {
        return bubble.getTicket()
    }

    override fun isConnected(): Boolean {
        return bubble.isConnected()
    }

    override fun isFinished(): Boolean {
        return bubble.isFinished()
    }

    override fun subscribe(subscriber: DropSendFilesSubscriber) {
        val native = (subscriber as DropSendFilesSubscriberImpl).native
        bubble.subscribe(native)
    }

    override fun unsubscribe(subscriber: DropSendFilesSubscriber) {
        val native = (subscriber as DropSendFilesSubscriberImpl).native
        bubble.unsubscribe(native)
    }
}
