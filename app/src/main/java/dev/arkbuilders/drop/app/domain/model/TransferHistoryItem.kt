package dev.arkbuilders.drop.app.domain.model

import java.time.OffsetDateTime

data class TransferHistoryItem(
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