package com.arkbuilders.arkdrop.presentation.feature.transferprogress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.arkbuilders.arkdrop.presentation.feature.transferprogress.composables.FileItem
import com.arkbuilders.arkdrop.presentation.feature.transferprogress.composables.FileTransferAlertDialog
import com.arkbuilders.arkdrop.presentation.feature.transferprogress.composables.TransferParticipantHeader
import com.arkbuilders.arkdrop.ui.theme.BlueDark600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferProgressScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val openAlertDialog = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                title = { Text("Transferring Files") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigateUp()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null
                        )
                    }
                })
        }
    ) { padding ->
        Column(
            modifier = modifier
                .background(Color(0xFFFCFCFD))
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TransferParticipantHeader()
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(listOf(1, 2, 3)) { item ->
                    Box(
                        modifier = modifier
                            .fillMaxWidth()
                            .border(
                                width = 0.5.dp, color = Color.LightGray,
                                shape = RoundedCornerShape(25)
                            )
                            .padding(16.dp)
                    ) {
                        FileItem(modifier = modifier,
                            onCloseIconClick = {
                                openAlertDialog.value = true
                            }
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = {

                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BlueDark600
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = BlueDark600,
                )) {
                Icon(
                    imageVector = Icons.Outlined.AddCircleOutline,
                    contentDescription = null
                )
                Text("Send more")
            }

            if (openAlertDialog.value) {
                FileTransferAlertDialog(
                    onDismissRequest = { openAlertDialog.value = false },
                    onConfirmation = { /*TODO*/ },
                    dialogTitle = "Cancel this file",
                    dialogText = "When you remove this file it cannot be undone.",
                ) {
                    FileItem(modifier = modifier)
                }
            } else {

            }
        }
    }


}

@Preview
@Composable
fun PreviewTransferProgressScreen() {
    TransferProgressScreen(navController = rememberNavController())
}