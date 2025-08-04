package dev.arkbuilders.drop.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.arkbuilders.drop.app.ui.theme.DesignTokens

enum class DropButtonSize {
    Small, Medium, Large
}

enum class DropButtonVariant {
    Primary, Secondary, Tertiary, Destructive
}

@Composable
fun DropButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: DropButtonVariant = DropButtonVariant.Primary,
    size: DropButtonSize = DropButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "buttonScale"
    )
    
    val buttonHeight = when (size) {
        DropButtonSize.Small -> 40.dp
        DropButtonSize.Medium -> DesignTokens.TouchTarget.minimum
        DropButtonSize.Large -> DesignTokens.TouchTarget.large
    }
    
    val contentPadding = when (size) {
        DropButtonSize.Small -> PaddingValues(horizontal = DesignTokens.Spacing.md, vertical = DesignTokens.Spacing.xs)
        DropButtonSize.Medium -> PaddingValues(horizontal = DesignTokens.Spacing.lg, vertical = DesignTokens.Spacing.sm)
        DropButtonSize.Large -> PaddingValues(horizontal = DesignTokens.Spacing.xl, vertical = DesignTokens.Spacing.md)
    }
    
    val colors = when (variant) {
        DropButtonVariant.Primary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        DropButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        DropButtonVariant.Tertiary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            disabledContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        DropButtonVariant.Destructive -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
    
    Button(
        onClick = {
            if (!loading) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = buttonHeight)
            .semantics {
                role = Role.Button
                contentDescription?.let { this.contentDescription = it }
            },
        enabled = enabled && !loading,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(DesignTokens.CornerRadius.md)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            content()
        }
    }
}

@Composable
fun DropOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: DropButtonSize = DropButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "buttonScale"
    )
    
    val buttonHeight = when (size) {
        DropButtonSize.Small -> 40.dp
        DropButtonSize.Medium -> DesignTokens.TouchTarget.minimum
        DropButtonSize.Large -> DesignTokens.TouchTarget.large
    }
    
    val contentPadding = when (size) {
        DropButtonSize.Small -> PaddingValues(horizontal = DesignTokens.Spacing.md, vertical = DesignTokens.Spacing.xs)
        DropButtonSize.Medium -> PaddingValues(horizontal = DesignTokens.Spacing.lg, vertical = DesignTokens.Spacing.sm)
        DropButtonSize.Large -> PaddingValues(horizontal = DesignTokens.Spacing.xl, vertical = DesignTokens.Spacing.md)
    }
    
    OutlinedButton(
        onClick = {
            if (!loading) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = buttonHeight)
            .semantics {
                role = Role.Button
                contentDescription?.let { this.contentDescription = it }
            },
        enabled = enabled && !loading,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(DesignTokens.CornerRadius.md)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        } else {
            content()
        }
    }
}
