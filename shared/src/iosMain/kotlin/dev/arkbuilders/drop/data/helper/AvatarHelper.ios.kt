package dev.arkbuilders.drop.data.helper

actual class AvatarHelper {
    actual fun uriToBase64(uri: String): String? {
        throw NotImplementedError()
    }

    actual fun getDefaultAvatarBase64(avatarId: String): String {
        throw NotImplementedError()
    }
}
