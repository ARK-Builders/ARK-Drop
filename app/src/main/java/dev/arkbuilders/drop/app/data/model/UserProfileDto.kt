package dev.arkbuilders.drop.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val name: String,
    val avatarB64: String,
    val avatarId: String
)