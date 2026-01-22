@file:OptIn(ExperimentalTime::class)

package dev.arkbuilders.drop.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class TransferSession(
    val id: Long = 0,
    val files: List<DropFileInfo>,
    val type: TransferType,
    val timestamp: Instant,
    val status: TransferStatus,
    val peerName: String,
    val peerAvatar: String?,
)
