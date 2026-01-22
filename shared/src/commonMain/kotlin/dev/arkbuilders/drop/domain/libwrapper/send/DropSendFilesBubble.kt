package dev.arkbuilders.drop.domain.libwrapper.send

interface DropSendFilesBubble {
    suspend fun cancel()

    fun getConfirmation(): UByte

    fun getCreatedAt(): String

    fun getTicket(): String

    fun isConnected(): Boolean

    fun isFinished(): Boolean

    fun subscribe(subscriber: DropSendFilesSubscriber)

    fun unsubscribe(subscriber: DropSendFilesSubscriber)
}
