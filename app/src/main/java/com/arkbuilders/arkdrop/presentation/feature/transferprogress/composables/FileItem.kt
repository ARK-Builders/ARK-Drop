package com.arkbuilders.arkdrop.presentation.feature.transferprogress.composables

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkbuilders.arkdrop.ui.theme.BlueDark600

@Composable
fun FileItem(
    modifier: Modifier = Modifier,
    onCloseIconClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.FileCopy, contentDescription = null,
            tint = Color.Red,
            modifier = modifier
                .size(48.dp)
                .border(
                    width = 0.5.dp,
                    color = Color.LightGray,
                    shape = RoundedCornerShape(50)
                )
                .padding(8.dp)
        )
        Column(
            modifier = modifier
                .padding(horizontal = 12.dp)
                .weight(1.0f)
        ) {
            Text(
                text = "Img 2718.JPG",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = modifier.height(4.dp))
            Text(text = "1.5 MB of 4.7 MB • 4 secs left")
        }

        if (onCloseIconClick != null) {
            Icon(
                modifier = modifier
                    .clickable { onCloseIconClick.invoke() },
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = BlueDark600
            )
        }
    }
}

@Preview
@Composable
fun PreviewFileItem() {
    FileItem(onCloseIconClick = {})
}