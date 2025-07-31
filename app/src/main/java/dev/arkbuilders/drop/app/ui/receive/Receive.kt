@file:OptIn(ExperimentalMaterial3Api::class)

package dev.arkbuilders.drop.app.ui.receive

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import compose.icons.TablerIcons
import compose.icons.tablericons.History
import dev.arkbuilders.drop.ReceiveFilesBubble
import dev.arkbuilders.drop.ReceiveFilesConnectingEvent
import dev.arkbuilders.drop.ReceiveFilesReceivingEvent
import dev.arkbuilders.drop.ReceiveFilesRequest
import dev.arkbuilders.drop.ReceiveFilesSubscriber
import dev.arkbuilders.drop.ReceiverProfile
import dev.arkbuilders.drop.app.ProfileManager
import dev.arkbuilders.drop.app.ui.send.formatFileSize
import dev.arkbuilders.drop.receiveFiles
import kotlinx.coroutines.delay
import java.text.DecimalFormat
import java.util.UUID
import java.util.concurrent.Executors

class ReceiveFilesSubscriberImpl : ReceiveFilesSubscriber {
    private val id: UUID = UUID.randomUUID()
    val connectingEvent = mutableStateOf<ReceiveFilesConnectingEvent?>(null)
    val receivingEvents = mutableListOf<ReceiveFilesReceivingEvent>()

    override fun getId(): String {
        return this.id.toString()
    }

    override fun notifyConnecting(event: ReceiveFilesConnectingEvent) {
        Log.d("ReceiveFilesSubscriberImpl", "Connecting event received: sender=${event.sender.name}, files=${event.files.size}")
        this.connectingEvent.value = event
    }

    override fun notifyReceiving(event: ReceiveFilesReceivingEvent) {
        Log.d("ReceiveFilesSubscriberImpl", "Receiving event: file=${event.id}, data size=${event.data.size}")
        this.receivingEvents.add(event)
    }
}

data class FileState(
    val id: String,
    val name: String,
    var received: ULong,
    val total: ULong,
)

data class ReceiverChunk(
    val id: String,
    val name: String,
    val data: List<UByte>,
)

@Composable
fun Receive(
    modifier: Modifier = Modifier,
    navController: NavController,
    profileManager: ProfileManager
) {
    var ticket by remember { mutableStateOf<String?>(null) }
    var confirmations by remember { mutableStateOf<List<UByte>>(emptyList()) }
    var selectedConfirmation by remember { mutableStateOf<UByte?>(null) }

    when {
        ticket != null && selectedConfirmation != null -> {
            ReceiveFiles(
                ticket = ticket!!,
                confirmation = selectedConfirmation!!,
                onBack = { navController.popBackStack() },
                onReceive = { chunks ->
                    // Handle received file chunks - could save to storage here
                    Log.d("Receive", "Received ${chunks.size} chunks")
                },
                profileManager = profileManager
            )
        }

        confirmations.isNotEmpty() -> {
            SelectConfirmation(
                confirmations = confirmations,
                onBack = { navController.popBackStack() },
                onSelectConfirmation = { confirmation ->
                    Log.d("Receive", "Selected confirmation: $confirmation")
                    selectedConfirmation = confirmation
                }
            )
        }

        else -> {
            ScanQRCode { uri ->
                Log.d("Receive", "QR code scanned: $uri")
                processQRCode(uri) { processedTicket, processedConfirmations ->
                    ticket = processedTicket
                    confirmations = processedConfirmations
                }
            }
        }
    }
}

@Composable
fun ReceiveFiles(
    ticket: String,
    confirmation: UByte,
    onBack: () -> Unit = {},
    onReceive: (List<ReceiverChunk>) -> Unit = {},
    profileManager: ProfileManager
) {
    val context = LocalContext.current
    val profile = remember { profileManager.loadOrDefault() }
    val subscriber = remember { ReceiveFilesSubscriberImpl() }
    val request = remember {
        ReceiveFilesRequest(
            ticket = ticket,
            confirmation = confirmation,
            profile = ReceiverProfile(
                name = profile.name,
                avatarB64 = profile.avatarB64
            )
        )
    }
    val fileStates = remember { mutableStateOf<List<FileState>>(emptyList()) }
    var isCancelled by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var completionTime by remember { mutableStateOf<Long>(0L) }
    var bubble by remember { mutableStateOf<ReceiveFilesBubble?>(null) }
    var bytesPerSecond by remember { mutableStateOf<ULong>(0u) }

    LaunchedEffect(request) {
        try {
            Log.d("ReceiveFiles", "Creating ReceiveFilesBubble...")
            bubble = receiveFiles(request)
            bubble!!.subscribe(subscriber)
            bubble!!.start()
            Log.d("ReceiveFiles", "ReceiveFilesBubble started successfully")
        } catch (e: Exception) {
            Log.e("ReceiveFiles", "Error creating bubble", e)
            isCancelled = true
        }
    }

    LaunchedEffect(isCancelled) {
        if (isCancelled) {
            Log.d("ReceiveFiles", "Cancelling receive")
            bubble?.cancel()
            onBack()
        }
    }

    LaunchedEffect(subscriber.connectingEvent.value) {
        if (subscriber.connectingEvent.value == null) return@LaunchedEffect

        val connectingEvent = subscriber.connectingEvent.value!!
        Log.d("ReceiveFiles", "Connection established with: ${connectingEvent.sender.name}")

        fileStates.value = connectingEvent.files.map {
            FileState(it.id, it.name, 0u, it.len)
        }

        val startTime = System.currentTimeMillis()
        var emptyReceivingEventsCounter = 0

        // Continue processing until connection naturally terminates
        while (!isCancelled) {
            var receivedBytes: ULong = 0u
            val receivingEvents = subscriber.receivingEvents.toList()
            subscriber.receivingEvents.clear()

            if (receivingEvents.isEmpty()) {
                emptyReceivingEventsCounter++
            } else {
                emptyReceivingEventsCounter = 0
            }

            // Only exit when connection is idle for extended period
            if (emptyReceivingEventsCounter > 10) {
                bytesPerSecond = 0u
                // Check if all files are actually completed before marking as done
                val allFilesCompleted = fileStates.value.all { it.received >= it.total }
                if (allFilesCompleted && fileStates.value.isNotEmpty()) {
                    Log.d("ReceiveFiles", "All files completed successfully")
                    isCompleted = true
                    completionTime = (System.currentTimeMillis() - startTime) / 1000
                }
                break
            }

            val chunks: List<ReceiverChunk> =
                receivingEvents.groupBy { it.id }.map { (id, events) ->
                    val fileState = fileStates.value.find { it.id == id }!!
                    val accumulatedData = events.flatMap { it.data }
                    ReceiverChunk(id, fileState.name, accumulatedData)
                }

            onReceive(chunks)

            chunks.forEach { chunk ->
                fileStates.value = fileStates.value.map { fileState ->
                    if (fileState.id == chunk.id) {
                        receivedBytes += chunk.data.size.toULong()
                        fileState.received += chunk.data.size.toULong()
                    }
                    fileState
                }
            }

            bytesPerSecond = receivedBytes
            delay(1000L)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
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
                "Receiving Files",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.width(48.dp)) // Balance the close button
        }

        if (isCompleted) {
            CompletionScreen(
                senderName = subscriber.connectingEvent.value?.sender?.name ?: "Unknown",
                completionTime = completionTime,
                fileStates = fileStates.value,
                onOpenFolder = {
                    // Open file manager to download folder
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FDrop")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("ReceiveFiles", "No file manager available", e)
                    }
                },
                onReceiveMore = { onBack() }
            )
        } else {
            ReceivingScreen(
                senderName = subscriber.connectingEvent.value?.sender?.name ?: "Connecting...",
                fileStates = fileStates.value,
                bytesPerSecond = bytesPerSecond,
                onOpenFolder = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FDrop")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("ReceiveFiles", "Error opening file manager", e)
                    }
                }
            )
        }
    }
}

@Composable
private fun CompletionScreen(
    senderName: String,
    completionTime: Long,
    fileStates: List<FileState>,
    onOpenFolder: () -> Unit,
    onReceiveMore: () -> Unit
) {
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
            text = "Files received successfully from $senderName!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Completed in $completionTime seconds",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // File completion cards
        fileStates.forEach { fileState ->
            FileCompletionCard(fileState)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(32.dp))

        // Open folder button
        OutlinedButton(
            onClick = onOpenFolder,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                TablerIcons.History,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Open in File Manager")
        }

        Spacer(Modifier.height(16.dp))

        // Receive more button
        OutlinedButton(
            onClick = onReceiveMore,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Receive more files")
        }
    }
}

@Composable
private fun ReceivingScreen(
    senderName: String,
    fileStates: List<FileState>,
    bytesPerSecond: ULong,
    onOpenFolder: () -> Unit
) {
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
                    .background(Color(0xFFE1BEE7)), // Light purple background
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFF9C27B0)
                )
            }
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB3E5FC)), // Light blue background
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFF2196F3)
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
            "Receiving from $senderName",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(32.dp))

        // File transfer progress
        fileStates.forEach { fileState ->
            FileReceiveCard(fileState, bytesPerSecond)
            Spacer(Modifier.height(12.dp))
        }

        // Open folder button
        OutlinedButton(
            onClick = onOpenFolder,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                TablerIcons.History,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Open in File Manager")
        }
    }
}

@Composable
private fun FileCompletionCard(fileState: FileState) {
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
                            Color(0xFF4CAF50).copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
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
                        color = Color(0xFF4CAF50),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FileReceiveCard(fileState: FileState, bytesPerSecond: ULong) {
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
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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
                        "${formatFileSize(fileState.received)} of ${formatFileSize(fileState.total)} • ${
                            if (bytesPerSecond > 0u) {
                                "${(fileState.total - fileState.received) / bytesPerSecond} secs left"
                            } else {
                                "calculating..."
                            }
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                "${if (fileState.total > 0u) ((fileState.received.toFloat() / fileState.total.toFloat()) * 100).toInt() else 0}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = {
                if (fileState.total > 0u) {
                    fileState.received.toFloat() / fileState.total.toFloat()
                } else {
                    0f
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
fun ScanQRCode(onScanQRCode: (Uri) -> Unit = {}) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Log.w("ScanQRCode", "Camera permission denied")
        }
    }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        AndroidView(factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }, modifier = Modifier.fillMaxSize()) { previewView ->
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val previewUseCase = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val options =
                    BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                val scanner = BarcodeScanning.getClient(options)
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    .also { aa ->
                        val executor = Executors.newSingleThreadExecutor()
                        aa.setAnalyzer(executor) { proxy ->
                            processImageProxy(scanner, proxy) { qr ->
                                val data = qr.toUri()
                                Log.d("ScanQRCode", "QR code detected: $data")
                                onScanQRCode(data).also { proxy.close() }
                            }
                        }
                    }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase, analysis
                )
            }, ContextCompat.getMainExecutor(context))
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Camera permission required to scan QR codes",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: BarcodeScanner, imageProxy: ImageProxy, onQrCodeScanned: (String) -> Unit
) {
    imageProxy.image?.let { image ->
        val inputImage = InputImage.fromMediaImage(
            image, imageProxy.imageInfo.rotationDegrees
        )

        barcodeScanner.process(inputImage).addOnSuccessListener { barcodes ->
            barcodes.firstOrNull()?.rawValue?.let { qrValue ->
                Log.d("processImageProxy", "Barcode detected: $qrValue")
                if (qrValue.startsWith("drop://receive")) {
                    onQrCodeScanned(qrValue)
                }
            }
        }.addOnCompleteListener {
            imageProxy.close()
        }
    }
}

@Composable
fun SelectConfirmation(
    confirmations: List<UByte>,
    onBack: () -> Unit,
    onSelectConfirmation: (UByte) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }, title = { Text("Back") })
        }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(32.dp))
            Text("Choose the confirmation Code", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Make sure code confirmation are matched", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(32.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                confirmations.forEach { confirmation ->
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF0F3F5),
                        modifier = Modifier
                            .size(64.dp)
                            .clickable {
                                Log.d("SelectConfirmation", "Confirmation selected: $confirmation")
                                onSelectConfirmation(confirmation)
                            },
                        tonalElevation = 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                confirmation.toULong().toString().padStart(2, '0'),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper function to process QR code
private fun processQRCode(uri: Uri, onResult: (String, List<UByte>) -> Unit) {
    try {
        Log.d("processQRCode", "Processing QR code: $uri")

        if (!uri.toString().startsWith("drop://receive")) {
            Log.e("processQRCode", "Invalid QR code format: $uri")
            return
        }

        val ticket = uri.getQueryParameter("ticket")
        val confirmationsParam = uri.getQueryParameter("confirmations")

        Log.d("processQRCode", "QR code parameters: ticket=$ticket, confirmations=$confirmationsParam")

        if (ticket != null && confirmationsParam != null) {
            val confirmations = confirmationsParam.split(",")
                .mapNotNull {
                    try {
                        it.trim().toUByteOrNull()
                    } catch (e: Exception) {
                        Log.w("processQRCode", "Invalid confirmation value: $it", e)
                        null
                    }
                }

            if (confirmations.isNotEmpty()) {
                Log.d("processQRCode", "QR code processed successfully: ticket=$ticket, confirmations=$confirmations")
                onResult(ticket, confirmations)
            } else {
                Log.e("processQRCode", "No valid confirmations found in QR code")
            }
        } else {
            Log.e("processQRCode", "Missing required QR code parameters: ticket=$ticket, confirmations=$confirmationsParam")
        }
    } catch (e: Exception) {
        Log.e("processQRCode", "Error processing QR code: $uri", e)
    }
}
