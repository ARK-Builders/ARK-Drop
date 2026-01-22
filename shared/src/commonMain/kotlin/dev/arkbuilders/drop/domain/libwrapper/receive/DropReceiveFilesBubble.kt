package dev.arkbuilders.drop.domain.libwrapper.receive

interface DropReceiveFilesBubble {
    fun cancel()

    fun isCancelled(): Boolean

    fun isFinished(): Boolean

    fun start()

    fun subscribe(subscriber: DropReceiveFilesSubscriber)

    fun unsubscribe(subscriber: DropReceiveFilesSubscriber)
}
