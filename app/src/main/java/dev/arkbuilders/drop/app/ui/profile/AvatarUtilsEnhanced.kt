package dev.arkbuilders.drop.app.ui.profile

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream
import java.io.IOException
import androidx.core.graphics.scale

object AvatarUtils {

    private const val MAX_IMAGE_SIZE = 512 // Maximum width/height in pixels
    private const val JPEG_QUALITY = 85 // JPEG compression quality
    private const val MAX_FILE_SIZE = 500 * 1024 // 500KB max file size

    /**
     * Convert URI to Base64 with comprehensive error handling and optimization
     */
    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val bitmap = loadBitmapFromUri(context, uri) ?: return null
            val optimizedBitmap = optimizeBitmap(bitmap)
            bitmapToBase64(optimizedBitmap)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load bitmap from URI with proper error handling
     */
    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    /**
     * Optimize bitmap for storage and performance
     */
    private fun optimizeBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate scaling factor
        val scaleFactor = if (width > height) {
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

    /**
     * Convert bitmap to Base64 with size validation
     */
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
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get default avatar Base64 string
     */
    @SuppressLint("DiscouragedApi")
    fun getDefaultAvatarBase64(context: Context, avatarId: String): String {
        return try {
            val resourceId = context.resources.getIdentifier(
                avatarId, "drawable", context.packageName
            )
            if (resourceId != 0) {
                val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
                bitmapToBase64(bitmap) ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Enhanced avatar image composable with error handling
     */
    @Composable
    fun AvatarImage(
        base64String: String,
        modifier: Modifier = Modifier,
        contentDescription: String? = null
    ) {
        if (base64String.isNotEmpty()) {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            if (bitmap != null) {
                Image(
                    painter = BitmapPainter(bitmap.asImageBitmap()),
                    contentDescription = contentDescription,
                    modifier = modifier
                        .clip(CircleShape)
                        .semantics {
                            this.contentDescription = contentDescription ?: "Profile avatar"
                        },
                    contentScale = ContentScale.Crop
                )
            } else {
                AvatarFallback(modifier = modifier, contentDescription = contentDescription)
            }
        } else {
            AvatarFallback(modifier = modifier, contentDescription = contentDescription)
        }
    }

    /**
     * Avatar with fallback for better UX
     */
    @Composable
    fun AvatarImageWithFallback(
        base64String: String?,
        fallbackText: String = "",
        size: Dp = 48.dp,
        contentDescription: String? = null
    ) {
        if (base64String != null && base64String.isNotEmpty()) {
            AvatarImage(
                base64String = base64String,
                modifier = Modifier.size(size),
                contentDescription = contentDescription
            )
        } else {
            AvatarFallback(
                modifier = Modifier.size(size),
                fallbackText = fallbackText,
                contentDescription = contentDescription
            )
        }
    }

    /**
     * Fallback avatar component
     */
    @Composable
    private fun AvatarFallback(
        modifier: Modifier = Modifier,
        fallbackText: String = "",
        contentDescription: String? = null
    ) {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .semantics {
                    this.contentDescription = contentDescription ?: "Default profile avatar"
                },
            contentAlignment = Alignment.Center
        ) {
            if (fallbackText.isNotEmpty()) {
                androidx.compose.material3.Text(
                    text = fallbackText.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(0.6f),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    /**
     * Validate image format and size
     */
    fun validateImage(context: Context, uri: Uri): ValidationResult {
        return try {
            val bitmap = loadBitmapFromUri(context, uri)
            when {
                bitmap == null -> ValidationResult.InvalidFormat
                bitmap.width < 64 || bitmap.height < 64 -> ValidationResult.TooSmall
                bitmap.width > 2048 || bitmap.height > 2048 -> ValidationResult.TooLarge
                else -> ValidationResult.Valid
            }
        } catch (e: Exception) {
            ValidationResult.Error
        }
    }

    enum class ValidationResult {
        Valid,
        InvalidFormat,
        TooSmall,
        TooLarge,
        Error
    }
}
