package dev.arkbuilders.drop.domain.repository

import dev.arkbuilders.drop.domain.model.DropFileInfo
import dev.arkbuilders.drop.domain.model.TransferSession
import dev.arkbuilders.drop.domain.model.TransferStatus
import kotlinx.coroutines.flow.Flow

interface TransferSessionRepo {
    val historyItems: Flow<List<TransferSession>>

    suspend fun addSentTransfer(
        files: List<DropFileInfo>,
        peerName: String,
        peerAvatar: String?,
        status: TransferStatus = TransferStatus.COMPLETED,
    )

    suspend fun addReceivedTransfer(
        files: List<DropFileInfo>,
        peerName: String,
        peerAvatar: String?,
        status: TransferStatus = TransferStatus.COMPLETED,
    )

    suspend fun deleteSession(itemId: Long)

    suspend fun clearHistory()
}
