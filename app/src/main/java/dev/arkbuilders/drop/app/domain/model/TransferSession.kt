package dev.arkbuilders.drop.app.domain.model

import java.time.OffsetDateTime

data class TransferSession(
    val id: Long = 0,
    val files: List<DropFileInfo>,
    val type: TransferType,
    val timestamp: OffsetDateTime,
    val status: TransferStatus,
    val peerName: String,
    val peerAvatar: String?,
)
