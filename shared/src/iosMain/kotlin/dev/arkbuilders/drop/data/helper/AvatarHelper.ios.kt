@file:OptIn(ExperimentalForeignApi::class)

package dev.arkbuilders.drop.data.helper

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.*
import platform.UIKit.*
import platform.CoreGraphics.*

actual class AvatarHelper {

    actual fun uriToBase64(uri: String): String? {
        return try {
            val imageUrl = NSURL.URLWithString(uri) ?: return null
            val imageData = NSData.dataWithContentsOfURL(imageUrl) ?: return null
            val image = UIImage.imageWithData(imageData) ?: return null

            val optimizedImage = optimizeImage(image) ?: return null

            val jpegData = UIImageJPEGRepresentation(
                optimizedImage,
                JPEG_QUALITY / 100.0
            ) ?: return null

            if (jpegData.length.toULong() > MAX_FILE_SIZE) {
                return null
            }

            jpegData.base64EncodedStringWithOptions(0u)
        } catch (_: Throwable) {
            null
        }
    }

    private fun optimizeImage(image: UIImage): UIImage? {
        val (width, height) = image.size.useContents {
            Pair(width, height)
        }

        val scaleFactor = if (width > height) {
            MAX_IMAGE_SIZE / width
        } else {
            MAX_IMAGE_SIZE / height
        }

        return if (scaleFactor < 1.0) {
            val newWidth = width * scaleFactor
            val newHeight = height * scaleFactor

            val newSize = CGSizeMake(newWidth, newHeight)
            UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
            image.drawInRect(CGRectMake(0.0, 0.0, newWidth, newHeight))
            val resizedImage = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()
            resizedImage
        } else {
            image
        }
    }

    actual fun getDefaultAvatarBase64(avatarId: String): String {
        // Requires bundle resource mapping
        return ""
    }

    companion object {
        const val MAX_IMAGE_SIZE = 512.0
        const val JPEG_QUALITY = 85.0
        const val MAX_FILE_SIZE: ULong = 512000uL // 500 KB (500 * 1024)
    }

}

