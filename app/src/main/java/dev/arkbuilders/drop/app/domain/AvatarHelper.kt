package dev.arkbuilders.drop.app.domain

import android.content.Context
import android.net.Uri

interface AvatarHelper {
    fun uriToBase64(uri: String): String?
    fun getDefaultAvatarBase64(avatarId: String): String

    companion object {
        const val MAX_IMAGE_SIZE = 512 // Maximum width/height in pixels
        const val JPEG_QUALITY = 85 // JPEG compression quality
        const val MAX_FILE_SIZE = 500 * 1024 // 500KB max file size
    }
}