package dev.arkbuilders.drop.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DropFileInfo(
    val name: String,
    val size: Long,
)
