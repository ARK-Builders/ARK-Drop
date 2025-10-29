package dev.arkbuilders.drop.app.ui.send.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SendCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ), elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp, pressedElevation = 2.dp
        )
    ) {
        content()
    }
}