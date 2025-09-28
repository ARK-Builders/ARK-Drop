package dev.arkbuilders.drop.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.arkbuilders.drop.app.domain.model.TransferStatus
import dev.arkbuilders.drop.app.domain.model.TransferType
import java.time.OffsetDateTime

@Entity
data class TransferHistoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val fileSize: Long,
    val type: TransferType,
    val timestamp: OffsetDateTime,
    val status: TransferStatus,
    val peerName: String,
    val peerAvatar: String?,
    val fileCount: Int,
)