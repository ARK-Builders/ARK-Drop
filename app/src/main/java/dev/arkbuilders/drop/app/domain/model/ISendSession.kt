package dev.arkbuilders.drop.app.domain.model

import dev.arkbuilders.drop.app.data.SendingProgress
import kotlinx.coroutines.flow.StateFlow

interface ISendSession {
    fun isFinished(): Boolean

    suspend fun cancel()

    fun ticket(): String?

    fun confirmation(): UByte?

    val progress: StateFlow<SendingProgress>
}
