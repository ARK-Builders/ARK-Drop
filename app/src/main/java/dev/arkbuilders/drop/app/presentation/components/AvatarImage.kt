package dev.arkbuilders.drop.app.presentation.components

import android.graphics.BitmapFactory
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AvatarImage(
    avatarB64: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val imageBytes = Base64.decode(avatarB64, Base64.DEFAULT)
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

    if (bitmap != null) {
        Image(
            painter = BitmapPainter(bitmap.asImageBitmap()),
            contentDescription = contentDescription,
            modifier =
                modifier
                    .clip(CircleShape)
                    .semantics {
                        this.contentDescription = contentDescription ?: "Profile avatar"
                    },
            contentScale = ContentScale.Crop,
        )
    } else {
        AvatarFallback(modifier = modifier, contentDescription = contentDescription)
    }
}

@Composable
fun AvatarImageWithFallback(
    avatarB64: String?,
    fallbackText: String = "",
    size: Dp = 48.dp,
    contentDescription: String? = null,
) {
    if (avatarB64 != null && avatarB64.isNotEmpty()) {
        AvatarImage(
            avatarB64 = avatarB64,
            modifier = Modifier.size(size),
            contentDescription = contentDescription,
        )
    } else {
        AvatarFallback(
            modifier = Modifier.size(size),
            fallbackText = fallbackText,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun AvatarFallback(
    modifier: Modifier = Modifier,
    fallbackText: String = "",
    contentDescription: String? = null,
) {
    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .semantics {
                    this.contentDescription = contentDescription ?: "Default profile avatar"
                },
        contentAlignment = Alignment.Center,
    ) {
        if (fallbackText.isNotEmpty()) {
            Text(
                text = fallbackText.take(2).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.6f),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
