package dev.arkbuilders.drop.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserAvatar(
    val base64: String,
    val predefinedId: String?
) {
    companion object {
        val predefinedIds = listOf(
            "avatar_00", "avatar_01", "avatar_02", "avatar_03",
            "avatar_04", "avatar_05", "avatar_06", "avatar_07", "avatar_08"
        )
    }
}