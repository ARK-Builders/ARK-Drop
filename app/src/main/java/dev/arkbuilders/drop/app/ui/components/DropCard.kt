package dev.arkbuilders.drop.app.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.arkbuilders.drop.app.ui.theme.DesignTokens

enum class DropCardVariant {
    Filled, Elevated, Outlined
}

enum class DropCardSize {
    Small, Medium, Large
}

@Composable
fun DropCard(
    modifier: Modifier = Modifier,
    variant: DropCardVariant = DropCardVariant.Filled,
    size: DropCardSize = DropCardSize.Medium,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    shape: Shape = RoundedCornerShape(
        when (size) {
            DropCardSize.Small -> DesignTokens.CornerRadius.sm
            DropCardSize.Medium -> DesignTokens.CornerRadius.md
            DropCardSize.Large -> DesignTokens.CornerRadius.lg
        }
    ),
    colors: CardColors = when (variant) {
        DropCardVariant.Filled -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        DropCardVariant.Elevated -> CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        DropCardVariant.Outlined -> CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    },
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = modifier
        .fillMaxWidth()
        .semantics {
            contentDescription?.let { this.contentDescription = it }
        }
    
    val cardPadding = when (size) {
        DropCardSize.Small -> DesignTokens.Spacing.md
        DropCardSize.Medium -> DesignTokens.Spacing.lg
        DropCardSize.Large -> DesignTokens.Spacing.xl
    }
    
    val elevation = when (variant) {
        DropCardVariant.Filled -> CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.none)
        DropCardVariant.Elevated -> CardDefaults.elevatedCardElevation(defaultElevation = DesignTokens.Elevation.md)
        DropCardVariant.Outlined -> CardDefaults.outlinedCardElevation(defaultElevation = DesignTokens.Elevation.none)
    }
    
    when (variant) {
        DropCardVariant.Filled -> {
            Card(
                modifier = cardModifier,
                onClick = onClick ?: { },
                shape = shape,
                colors = colors,
                elevation = elevation
            ) {
                content()
            }
        }
        DropCardVariant.Elevated -> {
            ElevatedCard(
                modifier = cardModifier,
                onClick = onClick ?: { },
                shape = shape,
                colors = colors,
                elevation = elevation
            ) {
                content()
            }
        }
        DropCardVariant.Outlined -> {
            OutlinedCard(
                modifier = cardModifier,
                onClick = onClick ?: { },
                shape = shape,
                colors = colors,
                elevation = elevation
            ) {
                content()
            }
        }
    }
}

@Composable
fun DropCardContent(
    modifier: Modifier = Modifier,
    size: DropCardSize = DropCardSize.Medium,
    content: @Composable ColumnScope.() -> Unit
) {
    val padding = when (size) {
        DropCardSize.Small -> DesignTokens.Spacing.md
        DropCardSize.Medium -> DesignTokens.Spacing.lg
        DropCardSize.Large -> DesignTokens.Spacing.xl
    }
    
    androidx.compose.foundation.layout.Column(
        modifier = modifier.padding(padding)
    ) {
        content()
    }
}
