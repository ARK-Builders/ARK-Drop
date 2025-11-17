package dev.arkbuilders.drop.app.data.repository

import dev.arkbuilders.drop.app.data.datasource.TransferSessionLocalDataSource
import dev.arkbuilders.drop.app.domain.model.DropFileInfo
import dev.arkbuilders.drop.app.domain.model.TransferSession
import dev.arkbuilders.drop.app.domain.model.TransferStatus
import dev.arkbuilders.drop.app.domain.model.TransferType
import dev.arkbuilders.drop.app.domain.repository.TransferSessionRepo
import kotlinx.coroutines.flow.Flow
import java.time.OffsetDateTime

class TransferSessionRepoImpl(
    private val localSource: TransferSessionLocalDataSource,
) : TransferSessionRepo {
    override val historyItems: Flow<List<TransferSession>> = localSource.flow()

    override suspend fun addSentTransfer(
        files: List<DropFileInfo>,
        peerName: String,
        peerAvatar: String?,
        status: TransferStatus,
    ) {
        val newItem =
            TransferSession(
                files = files,
                type = TransferType.SENT,
                timestamp = OffsetDateTime.now(),
                status = status,
                peerName = peerName,
                peerAvatar = peerAvatar,
            )
        localSource.add(newItem)
    }

    override suspend fun addReceivedTransfer(
        files: List<DropFileInfo>,
        peerName: String,
        peerAvatar: String?,
        status: TransferStatus,
    ) {
        val newItem =
            TransferSession(
                files = files,
                type = TransferType.RECEIVED,
                timestamp = OffsetDateTime.now(),
                status = status,
                peerName = peerName,
                peerAvatar = peerAvatar,
            )
        localSource.add(newItem)
    }

    override suspend fun deleteSession(itemId: Long) {
        localSource.delete(itemId)
    }

    override suspend fun clearHistory() {
        localSource.clear()
    }
}
