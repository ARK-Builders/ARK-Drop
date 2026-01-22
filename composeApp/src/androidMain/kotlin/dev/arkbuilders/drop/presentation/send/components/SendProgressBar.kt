package dev.arkbuilders.drop.presentation.send.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun SendProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier =
            modifier
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    )
}
