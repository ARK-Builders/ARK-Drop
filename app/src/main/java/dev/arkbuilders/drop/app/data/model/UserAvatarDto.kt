package dev.arkbuilders.drop.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserAvatarDto(
    val base64: String,
    val predefinedId: String?
)