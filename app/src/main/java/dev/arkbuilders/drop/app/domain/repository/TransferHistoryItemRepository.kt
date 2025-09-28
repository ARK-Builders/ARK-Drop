package dev.arkbuilders.drop.app.domain.repository

import dev.arkbuilders.drop.app.domain.model.TransferHistoryItem
import dev.arkbuilders.drop.app.domain.model.TransferStatus
import kotlinx.coroutines.flow.Flow

interface TransferHistoryItemRepository {
    val historyItems: Flow<List<TransferHistoryItem>>

    suspend fun addSentTransfer(
        fileName: String,
        fileSize: Long,
        peerName: String,
        peerAvatar: String?,
        fileCount: Int = 1,
        status: TransferStatus = TransferStatus.COMPLETED
    )

    suspend fun addReceivedTransfer(
        fileName: String,
        fileSize: Long,
        peerName: String,
        peerAvatar: String?,
        fileCount: Int = 1,
        status: TransferStatus = TransferStatus.COMPLETED
    )

    suspend fun deleteHistoryItem(itemId: Long)

    suspend fun clearHistory()
}