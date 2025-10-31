package dev.arkbuilders.drop.app.data.model

import dev.arkbuilders.drop.app.domain.model.UserAvatar
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val name: String,
    val avatar: UserAvatar,
)