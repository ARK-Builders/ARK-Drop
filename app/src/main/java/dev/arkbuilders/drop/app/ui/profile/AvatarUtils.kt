package dev.arkbuilders.drop.app.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import dev.arkbuilders.drop.app.R
import java.io.ByteArrayOutputStream


object AvatarUtils {

    @Composable
    fun AvatarImage(
        base64String: String,
        modifier: Modifier = Modifier,
        placeholder: Painter? = null
    ) {
        val painter = remember(base64String) {
            try {
                val cleanBase64 = base64String.substringAfter("base64,")
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                BitmapPainter(bitmap.asImageBitmap())
            } catch (e: Exception) {
                null
            }
        }

        Image(
            painter = painter ?: placeholder ?: rememberVectorPainter(Icons.Default.Person),
            contentDescription = null,
            modifier = modifier
                .size(48.dp)
                .clip(CircleShape)
        )
    }

    /**
     * Convert a drawable resource to Base64 string
     */
    fun drawableToBase64(context: Context, @DrawableRes drawableRes: Int): String {
        val drawable = ContextCompat.getDrawable(context, drawableRes)
        return drawableToBase64(drawable)
    }

    /**
     * Convert a drawable to Base64 string
     */
    fun drawableToBase64(drawable: Drawable?): String {
        if (drawable == null) return ""

        val bitmap = drawableToBitmap(drawable)
        return bitmapToBase64(bitmap)
    }

    /**
     * Convert bitmap to Base64 string
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Convert drawable to bitmap
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    /**
     * Get default avatar Base64 for a given avatar ID
     */
    fun getDefaultAvatarBase64(context: Context, avatarId: String = "avatar_00"): String {
        val drawableRes = when (avatarId) {
            "avatar_00" -> R.drawable.avatar_00
            "avatar_01" -> R.drawable.avatar_01
            "avatar_02" -> R.drawable.avatar_02
            "avatar_03" -> R.drawable.avatar_03
            "avatar_04" -> R.drawable.avatar_04
            "avatar_05" -> R.drawable.avatar_05
            "avatar_06" -> R.drawable.avatar_06
            "avatar_07" -> R.drawable.avatar_07
            "avatar_08" -> R.drawable.avatar_08
            else -> R.drawable.avatar_00 // fallback
        }

        return drawableToBase64(context, drawableRes)
    }
}