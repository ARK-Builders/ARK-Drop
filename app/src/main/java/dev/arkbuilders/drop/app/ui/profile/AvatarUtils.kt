//package dev.arkbuilders.drop.app.ui.profile
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.graphics.Canvas
//import android.graphics.Paint
//import android.graphics.PorterDuff
//import android.graphics.PorterDuffXfermode
//import android.graphics.Rect
//import android.graphics.RectF
//import android.net.Uri
//import android.util.Base64
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.asImageBitmap
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//import java.io.ByteArrayOutputStream
//import java.io.InputStream
//
//object AvatarUtils {
//    private const val AVATAR_SIZE = 512
//    private const val COMPRESSION_QUALITY = 85
//
//    fun getDefaultAvatarBase64(context: Context, avatarId: String): String {
//        return try {
//            val resourceId = context.resources.getIdentifier(
//                avatarId,
//                "drawable",
//                context.packageName
//            )
//
//            if (resourceId != 0) {
//                val inputStream = context.resources.openRawResource(resourceId)
//                val bitmap = BitmapFactory.decodeStream(inputStream)
//                inputStream.close()
//
//                val resizedBitmap = resizeBitmap(bitmap, AVATAR_SIZE, AVATAR_SIZE)
//                val circularBitmap = getCircularBitmap(resizedBitmap)
//                bitmapToBase64(circularBitmap)
//            } else {
//                // Fallback to generated avatar
//                generateDefaultAvatar(avatarId)
//            }
//        } catch (e: Exception) {
//            generateDefaultAvatar(avatarId)
//        }
//    }
//
//    fun uriToBase64(context: Context, uri: Uri): String? {
//        return try {
//            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
//            inputStream?.use { stream ->
//                val bitmap = BitmapFactory.decodeStream(stream)
//                val resizedBitmap = resizeBitmap(bitmap, AVATAR_SIZE, AVATAR_SIZE)
//                val circularBitmap = getCircularBitmap(resizedBitmap)
//                bitmapToBase64(circularBitmap)
//            }
//        } catch (e: Exception) {
//            null
//        }
//    }
//
//    private fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
//        return Bitmap.createScaledBitmap(bitmap, width, height, true)
//    }
//
//    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
//        val size = minOf(bitmap.width, bitmap.height)
//        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
//
//        val canvas = Canvas(output)
//        val paint = Paint().apply {
//            isAntiAlias = true
//        }
//
//        val rect = Rect(0, 0, size, size)
//        val rectF = RectF(rect)
//
//        canvas.drawOval(rectF, paint)
//
//        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
//
//        val sourceRect = Rect(
//            (bitmap.width - size) / 2,
//            (bitmap.height - size) / 2,
//            (bitmap.width + size) / 2,
//            (bitmap.height + size) / 2
//        )
//
//        canvas.drawBitmap(bitmap, sourceRect, rect, paint)
//
//        return output
//    }
//
//    private fun bitmapToBase64(bitmap: Bitmap): String {
//        val byteArrayOutputStream = ByteArrayOutputStream()
//        bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, byteArrayOutputStream)
//        val byteArray = byteArrayOutputStream.toByteArray()
//        return Base64.encodeToString(byteArray, Base64.DEFAULT)
//    }
//
//    private fun base64ToBitmap(base64String: String): Bitmap? {
//        return try {
//            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
//            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
//        } catch (e: Exception) {
//            null
//        }
//    }
//
//    private fun generateDefaultAvatar(avatarId: String): String {
//        // Generate a simple colored circle as fallback
//        val bitmap = Bitmap.createBitmap(AVATAR_SIZE, AVATAR_SIZE, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bitmap)
//        val paint = Paint().apply {
//            isAntiAlias = true
//            color = when (avatarId.hashCode() % 6) {
//                0 -> android.graphics.Color.parseColor("#FF6B6B")
//                1 -> android.graphics.Color.parseColor("#4ECDC4")
//                2 -> android.graphics.Color.parseColor("#45B7D1")
//                3 -> android.graphics.Color.parseColor("#96CEB4")
//                4 -> android.graphics.Color.parseColor("#FFEAA7")
//                else -> android.graphics.Color.parseColor("#DDA0DD")
//            }
//        }
//
//        canvas.drawCircle(
//            AVATAR_SIZE / 2f,
//            AVATAR_SIZE / 2f,
//            AVATAR_SIZE / 2f,
//            paint
//        )
//
//        return bitmapToBase64(bitmap)
//    }
//
//    @Composable
//    fun AvatarImage(
//        base64String: String,
//        modifier: Modifier = Modifier,
//        size: Dp = 48.dp
//    ) {
//        val bitmap = base64ToBitmap(base64String)
//
//        if (bitmap != null) {
//            Image(
//                bitmap = bitmap.asImageBitmap(),
//                contentDescription = "Avatar",
//                modifier = modifier
//                    .size(size)
//                    .clip(CircleShape),
//                contentScale = ContentScale.Crop
//            )
//        } else {
//            // Fallback to colored circle
//            Box(
//                modifier = modifier
//                    .size(size)
//                    .clip(CircleShape)
//                    .background(MaterialTheme.colorScheme.primary),
//                contentAlignment = Alignment.Center
//            ) {
//                // Empty fallback
//            }
//        }
//    }
//
//    @Composable
//    fun AvatarImageWithFallback(
//        base64String: String?,
//        fallbackText: String = "?",
//        modifier: Modifier = Modifier,
//        size: Dp = 48.dp
//    ) {
//        if (!base64String.isNullOrEmpty()) {
//            AvatarImage(
//                base64String = base64String,
//                modifier = modifier,
//                size = size
//            )
//        } else {
//            // Text-based fallback
//            Box(
//                modifier = modifier
//                    .size(size)
//                    .clip(CircleShape)
//                    .background(MaterialTheme.colorScheme.primary),
//                contentAlignment = Alignment.Center
//            ) {
//                androidx.compose.material3.Text(
//                    text = fallbackText.take(1).uppercase(),
//                    color = MaterialTheme.colorScheme.onPrimary,
//                    style = MaterialTheme.typography.titleMedium
//                )
//            }
//        }
//    }
//}
