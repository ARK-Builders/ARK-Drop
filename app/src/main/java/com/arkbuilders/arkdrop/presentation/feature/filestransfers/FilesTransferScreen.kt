package com.arkbuilders.arkdrop.presentation.feature.filestransfers

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.arkbuilders.arkdrop.R
import com.arkbuilders.arkdrop.presentation.navigation.TransferConfirmationDestination
import com.arkbuilders.arkdrop.presentation.feature.filestransfers.composables.UserWelcomeHeader
import com.arkbuilders.arkdrop.presentation.feature.qrcodescanner.QRCodeScannerActivity
import com.arkbuilders.arkdrop.presentation.permission.PermissionManager
import com.arkbuilders.arkdrop.ui.theme.BlueDark600

@Composable
fun FilesTransferScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val result = remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
//        result.value = it
        navController.navigate(TransferConfirmationDestination.route)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFCFCFD))
    ) {
        UserWelcomeHeader(modifier = modifier)
        HorizontalDivider(
            color = Color.LightGray,
            modifier = modifier
                .height(1.dp)
                .fillMaxWidth()
        )
        Spacer(modifier = modifier.height(64.dp))
        Image(
            modifier = modifier
                .fillMaxWidth()
                .height(256.dp),
            painter = painterResource(id = R.drawable.transfer_background),
            contentDescription = null,
        )
        Text(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            text = stringResource(R.string.files_transfer_seamless_to_transfer_your_files),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = modifier.height(8.dp))
        Text(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.files_transfer_simple_fast_and_limitless_start_sharing_your_files_now),
        )
        Row(
            modifier = modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                modifier = modifier
                    .weight(1.0f)
                    .padding(8.dp),
                onClick = {
                    launcher.launch("*/*")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueDark600,
                ),
            ) {
                Icon(imageVector = Icons.Filled.ArrowCircleUp, contentDescription = null)
                Spacer(modifier = modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.files_transfer_send))
            }
            val context = LocalContext.current
            Button(
                modifier = modifier
                    .weight(1.0f)
                    .padding(8.dp),
                onClick = {
                    if (PermissionManager.isCameraPermissionGranted(context)) {
                        Intent(context, QRCodeScannerActivity::class.java).run {
                            context.startActivity(this)
                        }
                    } else {
                        PermissionManager.requestCameraPermission()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueDark600,
                ),
            ) {
                Icon(imageVector = Icons.Filled.ArrowCircleDown, contentDescription = null)
                Spacer(modifier = modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.files_transfer_receive))
            }
        }
    }
}

@Preview
@Composable
fun PreviewFilesTransferScreen() {
    FilesTransferScreen(navController = rememberNavController())
}
