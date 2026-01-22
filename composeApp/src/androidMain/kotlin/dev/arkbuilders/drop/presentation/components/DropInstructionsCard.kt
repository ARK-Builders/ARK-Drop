package dev.arkbuilders.drop.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.arkbuilders.drop.presentation.theme.DesignTokens

@Composable
fun DropInstructionsCard(
    modifier: Modifier = Modifier,
    title: String,
    steps: List<String>,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.5f,
                    ),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        shape = RoundedCornerShape(DesignTokens.CornerRadius.lg),
    ) {
        Column(
            modifier = Modifier.Companion.padding(DesignTokens.Spacing.lg),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.Companion.height(DesignTokens.Spacing.md))

            steps.forEachIndexed { index, step ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.Companion.width(DesignTokens.Spacing.sm))
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2,
                    )
                }
            }
        }
    }
}
