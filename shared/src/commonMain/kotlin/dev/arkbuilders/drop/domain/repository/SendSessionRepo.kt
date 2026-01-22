package dev.arkbuilders.drop.domain.repository

import dev.arkbuilders.drop.domain.model.SendSession

interface SendSessionRepo {
    suspend fun sendFiles(fileUris: List<String>): SendSession?

    suspend fun recordSendCompletion(
        fileUris: List<String>,
        session: SendSession,
    )

    fun cancelSend(session: SendSession)
}
