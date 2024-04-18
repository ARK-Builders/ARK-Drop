package com.arkbuilders.arkdrop.presentation.transferconfirmation

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.arkbuilders.arkdrop.R
import com.arkbuilders.arkdrop.ui.theme.BlueDark600
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

@Composable
fun TransferConfirmation(
    modifier: Modifier = Modifier,
    navController: NavController
) {

    val isConfirmationCodeShown = remember {
        mutableStateOf(false)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFCFCFD))
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = {
                    navController.navigateUp()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBackIosNew,
                    contentDescription = null
                )
                Spacer(modifier = modifier.width(ButtonDefaults.IconSpacing))
                Text("Back")
            }
            val context = LocalContext.current
            TextButton(
                onClick = {
                    // Launch camera for QR scanning
//                    launcher.launch(Uri.parse("image/*"))
                    startCamera(context)
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = BlueDark600
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = null
                )
                Spacer(modifier = modifier.width(ButtonDefaults.IconSpacing))
                Text("Scan")
            }
        }
        Row(
            modifier = modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
            Spacer(modifier = modifier.width(12.dp))
            Text("Confirmation code", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = modifier.height(12.dp))
        Text(
            modifier = modifier
                .clip(RoundedCornerShape(100 / 2))
                .background(Color.Gray.copy(0.2f))
                .padding(12.dp),
            text = maskedText(isConfirmationCodeShown.value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = modifier.height(8.dp))
        TextButton(
            onClick = {
                isConfirmationCodeShown.value = !isConfirmationCodeShown.value
            },
            colors = ButtonDefaults.textButtonColors(
                contentColor = BlueDark600
            )
        ) {
            Icon(
                imageVector = extractIcon(isConfirmationCodeShown.value),
                contentDescription = null
            )
            Spacer(modifier = modifier.width(ButtonDefaults.IconSpacing))
            Text(extractText(isConfirmationCodeShown.value))
        }
        Spacer(modifier = modifier.height(64.dp))
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_background),
            modifier = modifier.size(320.dp),
            contentDescription = null
        )
        Spacer(modifier = modifier.height(24.dp))
        Text(
            modifier = modifier.fillMaxWidth(),
            text = "Waiting for connect...",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = modifier.height(12.dp))
        Row(
            modifier = modifier
                .border(
                    width = 1.dp, color = Color.LightGray,
                    shape = RoundedCornerShape(4.dp)
                )
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Link, contentDescription = null,
                modifier = modifier.size(24.dp)
            )
            Text("Hash Code:", modifier = modifier.padding(horizontal = 6.dp))
            Text(
                "3910-LKA9-28HS-HAXX-72LA",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun extractIcon(isShown: Boolean): ImageVector {
    return if (isShown) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility
}

private fun extractText(isShown: Boolean): String {
    return if (isShown) "Hide" else "Show"
}

private fun maskedText(isShown: Boolean): String {
    return if (isShown) "23" else "• •"
}


private fun startCamera(context: Context) {
    var cameraController = LifecycleCameraController(context)

    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    val barcodeScanner = BarcodeScanning.getClient(options)

    cameraController.setImageAnalysisAnalyzer(
        ContextCompat.getMainExecutor(context),
        MlKitAnalyzer(
            listOf(barcodeScanner),
            ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
            ContextCompat.getMainExecutor(context)
        ) { result: MlKitAnalyzer.Result? ->
            val barcodeResults = result?.getValue(barcodeScanner)
            if ((barcodeResults == null) ||
                (barcodeResults.size == 0) ||
                (barcodeResults.first() == null)
            ) {
                return@MlKitAnalyzer
            }

        }
    )

//    cameraController.bindToLifecycle(this)
}


@Preview
@Composable
private fun PreviewTransferConfirmation() {
    TransferConfirmation(navController = rememberNavController())
}