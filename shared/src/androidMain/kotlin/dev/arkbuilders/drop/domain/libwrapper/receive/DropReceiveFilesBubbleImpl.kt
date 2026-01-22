package dev.arkbuilders.drop.domain.libwrapper.receive

import dev.arkbuilders.drop.ReceiveFilesBubble

class DropReceiveFilesBubbleImpl(
    private val bubble: ReceiveFilesBubble,
) : DropReceiveFilesBubble {
    override fun cancel() {
        bubble.cancel()
    }

    override fun isCancelled(): Boolean {
        return bubble.isCancelled()
    }

    override fun isFinished(): Boolean {
        return bubble.isFinished()
    }

    override fun start() {
        bubble.start()
    }

    override fun subscribe(subscriber: DropReceiveFilesSubscriber) {
        val native = (subscriber as DropReceiveFilesSubscriberImpl).native
        bubble.subscribe(native)
    }

    override fun unsubscribe(subscriber: DropReceiveFilesSubscriber) {
        val native = (subscriber as DropReceiveFilesSubscriberImpl).native
        bubble.unsubscribe(native)
    }
}
