package dev.arkbuilders.drop.app.ui.receive.components

import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowForward
import compose.icons.tablericons.CameraOff
import dev.arkbuilders.drop.app.ui.receive.ReceiveError
import dev.arkbuilders.drop.app.ui.theme.DesignTokens
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.text.toUByte

@Composable
fun ReceiveScanningCard(
    onQRCodeScanned: (String, UByte) -> Unit,
    onError: (ReceiveError) -> Unit,
    onStopScanning: () -> Unit,
    onEnterManually: () -> Unit
) {
    Column {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(DesignTokens.CornerRadius.xl),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = DesignTokens.Elevation.lg)
        ) {
            QRCodeScanner(
                onQRCodeScanned = onQRCodeScanned,
                onError = onError
            )
        }

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

        Text(
            text = "Point your camera at the QR code",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
        ) {
            OutlinedButton(
                onClick = onStopScanning,
                modifier = Modifier
                    .weight(1f)
                    .height(DesignTokens.TouchTarget.comfortable),
                shape = RoundedCornerShape(DesignTokens.CornerRadius.lg)
            ) {
                Icon(
                    TablerIcons.CameraOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(DesignTokens.Spacing.sm))
                Text(
                    "Stop Scanning",
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = onEnterManually,
                modifier = Modifier
                    .weight(1f)
                    .height(DesignTokens.TouchTarget.comfortable),
                shape = RoundedCornerShape(DesignTokens.CornerRadius.lg)
            ) {
                Icon(
                    TablerIcons.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(DesignTokens.Spacing.sm))
                Text(
                    "Enter Code",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun QRCodeScanner(
    onQRCodeScanned: (String, UByte) -> Unit,
    onError: (ReceiveError) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                processImageProxy(imageProxy, onQRCodeScanned, onError)
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    onError(ReceiveError.CameraInitializationFailed)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

@ExperimentalGetImage
private fun processImageProxy(
    imageProxy: ImageProxy,
    onQRCodeScanned: (String, UByte) -> Unit,
    onError: (ReceiveError) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    when (barcode.valueType) {
                        Barcode.TYPE_TEXT, Barcode.TYPE_URL -> {
                            barcode.rawValue?.let { value ->
                                // Parse Drop QR code format: drop://receive?ticket=...&confirmation=...
                                if (value.startsWith("drop://receive?")) {
                                    try {
                                        val uri = value.toUri()
                                        val ticket = uri.getQueryParameter("ticket")
                                        val confirmationStr = uri.getQueryParameter("confirmation")

                                        if (ticket != null && confirmationStr != null) {
                                            val confirmation = confirmationStr.toUByte()
                                            onQRCodeScanned(ticket, confirmation)
                                            return@addOnSuccessListener
                                        }
                                    } catch (_: Exception) {
                                        onError(ReceiveError.InvalidQRCode)
                                        return@addOnSuccessListener
                                    }
                                } else {
                                    onError(ReceiveError.InvalidQRCode)
                                    return@addOnSuccessListener
                                }
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                onError(
                    when {
                        exception.message?.contains(
                            "camera",
                            ignoreCase = true
                        ) == true -> ReceiveError.CameraInitializationFailed

                        else -> ReceiveError.UnknownError
                    }
                )
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}