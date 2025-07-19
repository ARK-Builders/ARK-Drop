@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.arkbuilders.drop.app.ui.send

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import dev.arkbuilders.drop.SendFilesBubble
import dev.arkbuilders.drop.SendFilesConnectingEvent
import dev.arkbuilders.drop.SendFilesRequest
import dev.arkbuilders.drop.SendFilesSendingEvent
import dev.arkbuilders.drop.SendFilesSubscriber
import dev.arkbuilders.drop.SenderFile
import dev.arkbuilders.drop.SenderFileData
import dev.arkbuilders.drop.SenderProfile
import dev.arkbuilders.drop.app.Profile
import dev.arkbuilders.drop.app.ProfileManager
import dev.arkbuilders.drop.app.ui.profile.AvatarUtils
import dev.arkbuilders.drop.sendFiles
import kotlinx.coroutines.delay
import java.io.InputStream
import java.text.DecimalFormat
import java.util.UUID
import kotlin.random.Random

class SenderFileDataFS : SenderFileData {
    private val len: ULong
    private val inputStream: InputStream

    private var isFinished: Boolean = false

    constructor(resolver: ContentResolver, contentURI: Uri) {
        this.len = getLen(resolver, contentURI)
        this.inputStream = resolver.openInputStream(contentURI)!!
    }

    fun getLen(resolver: ContentResolver, contentURI: Uri): ULong {
        return resolver.query(contentURI, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            cursor.getLong(sizeIndex).toULong()
        } ?: 0u
    }

    override fun len(): ULong {
        return len
    }

    override fun read(): UByte? {
        if (isFinished) {
            return null
        }
        var b: UByte? = null
        try {
            val readB = inputStream.read()
            if (readB == -1) {
                isFinished = true
                inputStream.close()
            } else {
                b = readB.toUByte()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return b
    }
}

class SendFilesSubscriberImpl : SendFilesSubscriber {
    private val id: UUID = UUID.randomUUID()
    var connectingEvent: SendFilesConnectingEvent? = null
    val sendingEvents = mutableListOf<SendFilesSendingEvent>()

    override fun getId(): String {
        return this.id.toString()
    }

    override fun notifyConnecting(event: SendFilesConnectingEvent) {
        this.connectingEvent = event
    }

    override fun notifySending(event: SendFilesSendingEvent) {
        this.sendingEvents.add(event)
    }
}

data class FileState(
    val name: String,
    var sent: ULong,
    val total: ULong,
)

fun formatFileSize(bytes: ULong): String {
    val df = DecimalFormat("#.#")
    return when {
        bytes < 1024u -> "${bytes} B"
        bytes < 1024u * 1024u -> "${df.format(bytes.toDouble() / 1024)} KB"
        bytes < 1024u * 1024u * 1024u -> "${df.format(bytes.toDouble() / (1024 * 1024))} MB"
        else -> "${df.format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
    }
}

@Composable
fun Send(
    modifier: Modifier = Modifier,
    navController: NavController,
    profileManager: ProfileManager
) {
    val context = navController.context
    val profile = remember { profileManager.loadOrDefault() }
    var isSending by remember { mutableStateOf(false) }
    var isCancelled by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var completionTime by remember { mutableStateOf<Long>(0L) }
    var visibleConfirmation by remember { mutableStateOf(false) }
    var bytesPerSecond by remember { mutableStateOf<ULong>(0u) }
    val bubble = remember { mutableStateOf<SendFilesBubble?>(null) }
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }
    val request = remember { mutableStateOf<SendFilesRequest?>(null) }
    val subscriber = remember { SendFilesSubscriberImpl() }
    val fileStates = remember { mutableStateOf<List<FileState>>(emptyList()) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isEmpty()) {
            navController.popBackStack()
            return@rememberLauncherForActivityResult
        }
        val files = uris.map {
            SenderFile(
                name = it.lastPathSegment ?: "Unknown",
                data = SenderFileDataFS(navController.context.contentResolver, it)
            )
        }
        fileStates.value = files.map { FileState(it.name, 0u, it.data.len()) }
        request.value = SendFilesRequest(
            profile = SenderProfile(
                name = profile.name,
            ), files = files
        )
    }

    LaunchedEffect(filePickerLauncher) {
        filePickerLauncher.launch("*/*")
    }

    LaunchedEffect(request.value) {
        if (request.value == null) return@LaunchedEffect
        bubble.value = sendFiles(request.value!!)
        bubble.value!!.subscribe(subscriber)
        val ticket = bubble.value!!.getTicket()
        val actualConfirmation = bubble.value!!.getConfirmation()
        val confirmations = createConfirmations(actualConfirmation)
        println("ticket: $ticket")
        println("confirmation: $actualConfirmation")
        bitmap.value = createQRCodeBitmap(
            data = "drop://receive?ticket=${Uri.encode(ticket)}&confirmations=${
                confirmations.map { it.toUByte() }.joinToString(",")
            }"
        )
    }

    LaunchedEffect(isCancelled) {
        if (isCancelled) {
            bubble.value?.cancel()
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (!isCancelled && !isCompleted) {
            if (!isSending && subscriber.connectingEvent != null) {
                isSending = true
            }
            var sentBytes: ULong = 0u
            val sendingEvents = subscriber.sendingEvents.toList()
            subscriber.sendingEvents.clear()
            var allFilesCompleted = true
            fileStates.value = fileStates.value.map { fileState ->
                val event = sendingEvents.findLast { event ->
                    event.name == fileState.name
                }
                if (event != null) {
                    sentBytes += event.sent - fileState.sent
                    fileState.sent = event.sent
                }
                if (fileState.sent < fileState.total) {
                    allFilesCompleted = false
                }
                fileState
            }
            bytesPerSecond = sentBytes

            // Check if all files are completed
            if (allFilesCompleted && isSending && fileStates.value.isNotEmpty()) {
                isCompleted = true
                completionTime = (System.currentTimeMillis() - startTime) / 1000
            }

            delay(1000L)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top bar with close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                isCancelled = true
            }) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
            Text(
                "Transferring Files",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(48.dp)) // Balance the close button
        }

        if (bitmap.value == null || bubble.value == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (isCompleted) {
            // Completion screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(80.dp))

                // Success checkmark
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = Color(0xFF4CAF50),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text = "File has been sent to ${subscriber.connectingEvent?.receiver?.name ?: "Unknown"}!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Complete in ${completionTime},5 Seconds",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(32.dp))

                // File completion card
                fileStates.value.forEach { fileState ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        fileState.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        formatFileSize(fileState.total),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = androidx.compose.ui.graphics.Color(0xFF2196F3),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Send more button
                OutlinedButton(
                    onClick = {
                        filePickerLauncher.launch("*/*")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Send more")
                }
            }
        } else if (isSending) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))

                // User avatars
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-12).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val sendingTo = Profile(name = subscriber.connectingEvent?.receiver?.name ?: "Unknown", avatarB64 = AvatarUtils.getDefaultAvatarBase64(context))
                        AvatarUtils.AvatarImage()
                    }
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    "Wait a moment while transferring…",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Sending to ${subscriber.connectingEvent?.receiver?.name ?: "Unknown"}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(32.dp))

                // File transfer progress
                fileStates.value.forEach { fileState ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        fileState.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${formatFileSize(fileState.sent)} of ${
                                            formatFileSize(
                                                fileState.total
                                            )
                                        } • ${
                                            if (bytesPerSecond > 0u) {
                                                "${(fileState.total - fileState.sent) / bytesPerSecond} secs left"
                                            } else {
                                                "calculating..."
                                            }
                                        }",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { /* Handle file removal */ }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove file",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            progress = {
                                if (fileState.total > 0u) {
                                    fileState.sent.toFloat() / fileState.total.toFloat()
                                } else {
                                    0f
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }

                // Send more button
                OutlinedButton(
                    onClick = {
                        filePickerLauncher.launch("*/*")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Send more")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Confirmation code", fontSize = 18.sp)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    bubble.value!!.getConfirmation().toUInt().toString().padStart(2, '0')
                        .toCharArray()
                        .forEach { char ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (visibleConfirmation) {
                                    Text(char.toString(), fontSize = 24.sp)
                                } else {
                                    Text("·", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    visibleConfirmation = !visibleConfirmation
                }) {
                    Text(if (visibleConfirmation) "Hide" else "Show")
                }
                Spacer(Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(16.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        modifier = Modifier.padding(16.dp),
                        bitmap = bitmap.value!!.asImageBitmap(),
                        contentDescription = "Send Files QR Code"
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("Waiting for connection…", fontSize = 16.sp)
            }
        }
    }
}

private fun createConfirmations(actualConfirmation: UByte): List<UByte> {
    val confirmations = mutableListOf<UByte>()
    confirmations.add(Random.nextInt(100).toUByte())
    confirmations.add(Random.nextInt(100).toUByte())
    confirmations.add(actualConfirmation)
    confirmations.shuffle()
    return confirmations
}

private fun createQRCodeBitmap(data: String, size: Int = 920): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size, hints)
    return createBitmap(size, size, Bitmap.Config.RGB_565).apply {
        for (x in 0 until size) {
            for (y in 0 until size) {
                set(
                    x,
                    y,
                    if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
    }
}