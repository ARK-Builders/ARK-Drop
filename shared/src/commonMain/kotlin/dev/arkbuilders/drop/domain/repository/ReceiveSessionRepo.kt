package dev.arkbuilders.drop.domain.repository

import dev.arkbuilders.drop.domain.model.ReceiveSession

interface ReceiveSessionRepo {
    suspend fun receiveFiles(
        ticket: String,
        confirmation: UByte,
    ): ReceiveSession?

    suspend fun saveReceivedFiles(session: ReceiveSession): List<String>

    fun cancelReceive(session: ReceiveSession)
}
