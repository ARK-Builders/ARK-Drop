package dev.arkbuilders.drop.app.ui.send

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import compose.icons.TablerIcons
import compose.icons.tablericons.PlayerPlay
import dev.arkbuilders.drop.app.TransferManager
import kotlinx.coroutines.launch
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Send(
    navController: NavController,
    transferManager: TransferManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isGeneratingQR by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showQRDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    // Observe sending progress
    val sendProgress by (transferManager.sendProgress?.collectAsState() ?: remember { mutableStateOf(null) })

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedFiles = uris
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Send Files",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sending progress
        sendProgress?.let { progress ->
            if (isSending) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (progress.isConnected) "Sending..." else "Waiting for receiver...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = {
                                    transferManager.cancelSend()
                                    isSending = false
                                    showQRDialog = false
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                        }

                        if (progress.isConnected) {
                            Text(
                                text = "Connected to: ${progress.receiverName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        if (progress.fileName.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sending: ${progress.fileName}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            val totalBytes = progress.sent + progress.remaining
                            val progressValue = if (totalBytes > 0UL) {
                                progress.sent.toFloat() / totalBytes.toFloat()
                            } else 0f

                            LinearProgressIndicator(
                                progress = progressValue,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )

                            Text(
                                text = "${formatBytes(progress.sent.toLong())} / ${formatBytes(totalBytes.toLong())}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // File selection section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected Files (${selectedFiles.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    FilledTonalButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        enabled = !isSending
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Files")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedFiles.isEmpty()) {
                    Text(
                        text = "No files selected. Tap 'Add Files' to choose files to send.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedFiles) { uri ->
                            FileItem(
                                uri = uri,
                                onRemove = {
                                    if (!isSending) {
                                        selectedFiles = selectedFiles - uri
                                    }
                                },
                                enabled = !isSending
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Send button
        Button(
            onClick = {
                if (selectedFiles.isNotEmpty()) {
                    scope.launch {
                        isGeneratingQR = true
                        errorMessage = null
                        try {
                            val bubble = transferManager.sendFiles(selectedFiles)
                            if (bubble != null) {
                                val ticket = transferManager.getCurrentSendTicket() ?: ""
                                val confirmation = transferManager.getCurrentSendConfirmation() ?: 0u

                                qrBitmap = generateQRCode(ticket, confirmation)
                                showQRDialog = true
                                isSending = true
                            } else {
                                errorMessage = "Failed to start file transfer"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Failed to generate QR code: ${e.message}"
                        } finally {
                            isGeneratingQR = false
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedFiles.isNotEmpty() && !isGeneratingQR && !isSending,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isGeneratingQR) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Starting Transfer...")
            } else {
                Icon(TablerIcons.PlayerPlay, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Transfer")
            }
        }

        // Error message
        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Instructions
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "How to send files:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Select files you want to send\n2. Start transfer to generate QR code\n3. Let the receiver scan the QR code\n4. Files will be transferred automatically",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // QR Code Dialog
    if (showQRDialog && qrBitmap != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isSending) {
                    showQRDialog = false
                }
            },
            title = { Text("QR Code for Transfer") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(200.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Show this QR code to the receiver",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isSending) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Waiting for receiver to scan...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                if (!isSending) {
                    TextButton(onClick = { showQRDialog = false }) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                if (isSending) {
                    TextButton(
                        onClick = {
                            transferManager.cancelSend()
                            isSending = false
                            showQRDialog = false
                        }
                    ) {
                        Text("Cancel Transfer")
                    }
                }
            }
        )
    }
}

@Composable
private fun FileItem(
    uri: Uri,
    onRemove: () -> Unit,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    var fileName by remember { mutableStateOf("Loading...") }
    var fileSize by remember { mutableStateOf(0L) }

    LaunchedEffect(uri) {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)

                fileName = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "Unknown" else "Unknown"
                fileSize = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatBytes(fileSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onRemove,
                enabled = enabled
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun generateQRCode(ticket: String, confirmation: UByte): Bitmap {
    val writer = QRCodeWriter()
    try {
        // Create QR data with ticket and confirmation
        val qrData = "drop://receive?ticket=$ticket&confirmation=$confirmation"
        val bitMatrix: BitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap[x, y] = if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        return bitmap
    } catch (e: WriterException) {
        throw RuntimeException("Error generating QR code", e)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
}
