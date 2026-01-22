package dev.arkbuilders.drop.data.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.graphics.scale
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream

actual class AvatarHelper(private val context: Context) {
    actual fun uriToBase64(uri: String): String? {
        return try {
            val bitmap = loadBitmapFromUri(uri.toUri()) ?: return null
            val optimizedBitmap = optimizeBitmap(bitmap)
            bitmapToBase64(optimizedBitmap)
        } catch (_: Exception) {
            null
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun optimizeBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate scaling factor
        val scaleFactor =
            if (width > height) {
                MAX_IMAGE_SIZE.toFloat() / width
            } else {
                MAX_IMAGE_SIZE.toFloat() / height
            }

        return if (scaleFactor < 1f) {
            val newWidth = (width * scaleFactor).toInt()
            val newHeight = (height * scaleFactor).toInt()
            bitmap.scale(newWidth, newHeight)
        } else {
            bitmap
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String? {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            val byteArray = outputStream.toByteArray()

            // Check file size
            if (byteArray.size > MAX_FILE_SIZE) {
                return null
            }

            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    actual fun getDefaultAvatarBase64(avatarId: String): String {
        return try {
            val resourceId =
                context.resources.getIdentifier(
                    avatarId,
                    "drawable",
                    context.packageName,
                )
            if (resourceId != 0) {
                val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
                bitmapToBase64(bitmap) ?: ""
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        const val MAX_IMAGE_SIZE = 512 // Maximum width/height in pixels
        const val JPEG_QUALITY = 85 // JPEG compression quality
        const val MAX_FILE_SIZE = 500 * 1024 // 500KB max file size
    }
}
