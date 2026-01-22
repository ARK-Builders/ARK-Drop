package dev.arkbuilders.drop.presentation.send.components.phase

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.Copy
import dev.arkbuilders.drop.presentation.send.components.ButtonSize
import dev.arkbuilders.drop.presentation.send.components.ButtonVariant
import dev.arkbuilders.drop.presentation.send.components.SendButton
import dev.arkbuilders.drop.presentation.send.components.SendLoadingIndicator

@Composable
fun WaitingForReceiverPhase(
    qrBitmap: ByteArray,
    fileCount: Int,
    copyString: String,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val bitmap =
        remember(qrBitmap) {
            BitmapFactory.decodeByteArray(qrBitmap, 0, qrBitmap.size)
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 4.dp,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR code for file transfer",
                modifier =
                    Modifier
                        .size(220.dp)
                        .padding(16.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Show this QR code to the receiver",
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$fileCount file${if (fileCount != 1) "s" else ""} ready to transfer",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        SendButton(
            onClick = {
                copyToClipboard(context, copyString)
                Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
            },
            variant = ButtonVariant.Secondary,
            size = ButtonSize.Medium,
        ) {
            Icon(
                TablerIcons.Copy,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Copy code",
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SendLoadingIndicator()
            Text(
                text = "Waiting for receiver to scan...",
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        TextButton(
            onClick = onCancel,
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                "Cancel Transfer",
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun copyToClipboard(
    context: Context,
    text: String,
) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText(null, text)
    clipboardManager.setPrimaryClip(clipData)
}
