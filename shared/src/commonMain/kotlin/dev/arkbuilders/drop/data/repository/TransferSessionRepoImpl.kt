@file:OptIn(ExperimentalTime::class)

package dev.arkbuilders.drop.data.repository

import dev.arkbuilders.drop.data.datasource.TransferSessionLocalDataSource
import dev.arkbuilders.drop.domain.model.DropFileInfo
import dev.arkbuilders.drop.domain.model.TransferSession
import dev.arkbuilders.drop.domain.model.TransferStatus
import dev.arkbuilders.drop.domain.model.TransferType
import dev.arkbuilders.drop.domain.repository.TransferSessionRepo
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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
                timestamp = Clock.System.now(),
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
                timestamp = Clock.System.now(),
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
