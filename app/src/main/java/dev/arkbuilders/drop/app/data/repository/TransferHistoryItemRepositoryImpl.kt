package dev.arkbuilders.drop.app.data.repository

import dev.arkbuilders.drop.app.data.datasource.TransferHistoryItemLocalDataSource
import dev.arkbuilders.drop.app.domain.model.TransferHistoryItem
import dev.arkbuilders.drop.app.domain.model.TransferStatus
import dev.arkbuilders.drop.app.domain.model.TransferType
import dev.arkbuilders.drop.app.domain.repository.TransferHistoryItemRepository
import kotlinx.coroutines.flow.Flow
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferHistoryItemRepositoryImpl @Inject constructor(
    private val localSource: TransferHistoryItemLocalDataSource
): TransferHistoryItemRepository {
    override val historyItems: Flow<List<TransferHistoryItem>> = localSource.flow()

    override suspend fun addSentTransfer(
        fileName: String,
        fileSize: Long,
        peerName: String,
        peerAvatar: String?,
        fileCount: Int,
        status: TransferStatus,
    ) {
        val newItem = TransferHistoryItem(
            fileName = if (fileCount > 1) "$fileName and ${fileCount - 1} more" else fileName,
            fileSize = fileSize,
            type = TransferType.SENT,
            timestamp = OffsetDateTime.now(),
            status = status,
            peerName = peerName,
            peerAvatar = peerAvatar,
            fileCount = fileCount
        )
        localSource.add(newItem)
    }

    override suspend fun addReceivedTransfer(
        fileName: String,
        fileSize: Long,
        peerName: String,
        peerAvatar: String?,
        fileCount: Int,
        status: TransferStatus,
    ) {
        val newItem = TransferHistoryItem(
            fileName = if (fileCount > 1) "$fileName and ${fileCount - 1} more" else fileName,
            fileSize = fileSize,
            type = TransferType.RECEIVED,
            timestamp = OffsetDateTime.now(),
            status = status,
            peerName = peerName,
            peerAvatar = peerAvatar,
            fileCount = fileCount
        )
        localSource.add(newItem)
    }

    override suspend fun deleteHistoryItem(itemId: Long) {
        localSource.delete(itemId)
    }

    override suspend fun clearHistory() {
        localSource.clear()
    }
}