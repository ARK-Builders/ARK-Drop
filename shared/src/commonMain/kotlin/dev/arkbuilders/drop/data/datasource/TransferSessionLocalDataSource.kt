@file:OptIn(ExperimentalTime::class)

package dev.arkbuilders.drop.data.datasource

import dev.arkbuilders.drop.data.db.dao.TransferSessionDao
import dev.arkbuilders.drop.data.db.entity.RoomTransferSession
import dev.arkbuilders.drop.domain.model.TransferSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class TransferSessionLocalDataSource(
    private val dao: TransferSessionDao,
) {
    fun flow(): Flow<List<TransferSession>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    suspend fun add(item: TransferSession): Long = dao.insert(item.toEntity())

    suspend fun addAll(items: List<TransferSession>) = dao.insertAll(items.map { it.toEntity() })

    suspend fun delete(itemId: Long) = dao.deleteById(itemId)

    suspend fun clear() = dao.clear()
}

fun RoomTransferSession.toDomain() =
    TransferSession(
        id = id,
        files = files,
        type = type,
        timestamp = Instant.fromEpochMilliseconds(timestamp),
        status = status,
        peerName = peerName,
        peerAvatar = peerAvatar,
    )

fun TransferSession.toEntity() =
    RoomTransferSession(
        id = id,
        files = files,
        type = type,
        timestamp = timestamp.toEpochMilliseconds(),
        status = status,
        peerName = peerName,
        peerAvatar = peerAvatar,
    )
