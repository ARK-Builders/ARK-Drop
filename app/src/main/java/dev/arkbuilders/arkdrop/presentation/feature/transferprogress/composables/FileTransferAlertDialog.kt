package dev.arkbuilders.arkdrop.presentation.feature.transferprogress.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.arkbuilders.arkdrop.ui.theme.Background
import dev.arkbuilders.arkdrop.ui.theme.DarkRed

@Composable
fun FileTransferAlertDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    content: @Composable ColumnScope.() -> Unit
) {

    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
               , shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Background
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = modifier.height(8.dp))
                Text(text = dialogText)
                Spacer(modifier = modifier.height(12.dp))
                content()
                Spacer(modifier = modifier.height(12.dp))
                Row(
                    modifier = modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.DarkGray
                        )
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirmation()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkRed
                        )
                    ) {
                        Text("Remove")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewFileTransferAlertDialog() {
    FileTransferAlertDialog(
        onConfirmation = {},
        onDismissRequest = {},
        dialogTitle = "Cancel this file",
        dialogText = "When you remove this file it cannot be undone.",
    ) {
        FileItem {

        }
    }
}