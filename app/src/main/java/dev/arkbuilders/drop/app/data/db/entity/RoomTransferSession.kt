package dev.arkbuilders.drop.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.arkbuilders.drop.app.domain.model.DropFileInfo
import dev.arkbuilders.drop.app.domain.model.TransferStatus
import dev.arkbuilders.drop.app.domain.model.TransferType
import java.time.OffsetDateTime

@Entity
data class RoomTransferSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val files: List<DropFileInfo>,
    val type: TransferType,
    val timestamp: OffsetDateTime,
    val status: TransferStatus,
    val peerName: String,
    val peerAvatar: String?,
)
