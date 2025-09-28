package dev.arkbuilders.drop.app.data.datasource

import dev.arkbuilders.drop.app.data.db.dao.TransferHistoryItemDao
import dev.arkbuilders.drop.app.data.db.entity.TransferHistoryItemEntity
import dev.arkbuilders.drop.app.domain.model.TransferHistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransferHistoryItemLocalDataSource  @Inject constructor(
    private val dao: TransferHistoryItemDao
) {
    fun flow(): Flow<List<TransferHistoryItem>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    suspend fun add(item: TransferHistoryItem): Long =
        dao.insert(item.toEntity())

    suspend fun addAll(items: List<TransferHistoryItem>) =
        dao.insertAll(items.map { it.toEntity() })

    suspend fun delete(itemId: Long) =
        dao.deleteById(itemId)

    suspend fun clear() = dao.clear()
}

fun TransferHistoryItemEntity.toDomain() = TransferHistoryItem(
    id = id,
    fileName = fileName,
    fileSize = fileSize,
    type = type,
    timestamp = timestamp,
    status = status,
    peerName = peerName,
    peerAvatar = peerAvatar,
    fileCount = fileCount
)

fun TransferHistoryItem.toEntity() = TransferHistoryItemEntity(
    id = id,
    fileName = fileName,
    fileSize = fileSize,
    type = type,
    timestamp = timestamp,
    status = status,
    peerName = peerName,
    peerAvatar = peerAvatar,
    fileCount = fileCount
)