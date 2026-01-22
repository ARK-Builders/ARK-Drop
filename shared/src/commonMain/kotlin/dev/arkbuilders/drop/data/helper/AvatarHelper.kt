package dev.arkbuilders.drop.data.helper

expect class AvatarHelper {
    fun uriToBase64(uri: String): String?

    fun getDefaultAvatarBase64(avatarId: String): String
}
