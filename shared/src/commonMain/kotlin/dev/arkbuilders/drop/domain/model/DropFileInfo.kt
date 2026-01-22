package dev.arkbuilders.drop.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DropFileInfo(
    val name: String,
    val size: Long,
)
