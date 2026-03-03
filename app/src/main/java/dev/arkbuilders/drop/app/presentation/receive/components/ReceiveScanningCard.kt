package dev.arkbuilders.drop.app.presentation.receive.components

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
import androidx.compose.foundation.layout.padding
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
import dev.arkbuilders.drop.app.presentation.receive.ReceiveError
import dev.arkbuilders.drop.app.presentation.theme.DesignTokens
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.text.toUByte

@Composable
fun ReceiveScanningCard(
    onQRCodeScanned: (String, UByte) -> Unit,
    onError: (ReceiveError) -> Unit,
    onStopScanning: () -> Unit,
    onEnterManually: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        ElevatedCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            shape = RoundedCornerShape(DesignTokens.CornerRadius.xl),
            elevation =
                CardDefaults.elevatedCardElevation(
                    defaultElevation = DesignTokens.Elevation.lg,
                ),
        ) {
            QRCodeScanner(
                onQRCodeScanned = onQRCodeScanned,
                onError = onError,
            )
        }

        Spacer(modifier = Modifier.Companion.height(DesignTokens.Spacing.xl))

        Text(
            text = "Point your camera at the QR code",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.Companion.height(DesignTokens.Spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md),
        ) {
            OutlinedButton(
                onClick = onStopScanning,
                modifier =
                    Modifier
                        .weight(1f)
                        .height(DesignTokens.TouchTarget.comfortable),
                shape = RoundedCornerShape(DesignTokens.CornerRadius.lg),
            ) {
                Icon(
                    TablerIcons.CameraOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.Companion.width(DesignTokens.Spacing.sm))
                Text(
                    "Stop Scanning",
                    fontWeight = FontWeight.Medium,
                )
            }

            Button(
                onClick = onEnterManually,
                modifier =
                    Modifier
                        .weight(1f)
                        .height(DesignTokens.TouchTarget.comfortable),
                shape = RoundedCornerShape(DesignTokens.CornerRadius.lg),
            ) {
                Icon(
                    TablerIcons.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.Companion.width(DesignTokens.Spacing.sm))
                Text(
                    "Enter Code",
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun QRCodeScanner(
    onQRCodeScanned: (String, UByte) -> Unit,
    onError: (ReceiveError) -> Unit,
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

                    val preview =
                        Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    val imageAnalyzer =
                        ImageAnalysis.Builder()
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
                        imageAnalyzer,
                    )
                } catch (exc: Exception) {
                    onError(ReceiveError.CameraInitializationFailed)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize(),
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
    onError: (ReceiveError) -> Unit,
) {
    val mediaImage =
        imageProxy.image ?: run {
            imageProxy.close()
            return
        }

    val image =
        InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees,
        )

    BarcodeScanning.getClient()
        .process(image)
        .addOnSuccessListener { barcodes ->
            handleBarcodes(barcodes, onQRCodeScanned)
        }
        .addOnFailureListener { exception ->
            onError(mapScannerError(exception))
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

private fun handleBarcodes(
    barcodes: List<Barcode>,
    onQRCodeScanned: (String, UByte) -> Unit,
) {
    barcodes.firstNotNullOfOrNull { barcode ->
        if (barcode.valueType == Barcode.TYPE_TEXT ||
            barcode.valueType == Barcode.TYPE_URL
        ) {
            barcode.rawValue?.let { parseDropQr(it) }
        } else {
            null
        }
    }?.let { (ticket, confirmation) ->
        onQRCodeScanned(ticket, confirmation)
    }
}

/**
 * Parses Drop QR format:
 * drop://{action}?ticket=...&confirmation=...
 *
 * Supported actions: receive, send
 *
 * @return Pair(ticket, confirmation) or null if invalid
 */
private fun parseDropQr(value: String): Pair<String, UByte>? {
    if (!value.startsWith("drop://")) return null

    return try {
        val uri = value.toUri()

        val ticket = uri.getQueryParameter("ticket")
        val confirmation = uri.getQueryParameter("confirmation")?.toUByte()

        if (ticket != null && confirmation != null) {
            ticket to confirmation
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

private fun mapScannerError(exception: Exception): ReceiveError {
    return if (exception.message?.contains("camera", ignoreCase = true) == true) {
        ReceiveError.CameraInitializationFailed
    } else {
        ReceiveError.UnknownError
    }
}
