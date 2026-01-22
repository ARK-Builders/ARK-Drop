package dev.arkbuilders.drop.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.arkbuilders.drop.domain.model.DropFileInfo
import dev.arkbuilders.drop.domain.model.TransferStatus
import dev.arkbuilders.drop.domain.model.TransferType

@Entity
data class RoomTransferSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val files: List<DropFileInfo>,
    val type: TransferType,
    val timestamp: Long,
    val status: TransferStatus,
    val peerName: String,
    val peerAvatar: String?,
)
