package dev.arkbuilders.drop.presentation.send.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class ButtonVariant { Primary, Secondary }

enum class ButtonSize { Medium, Large }

@Composable
fun SendButton(
    onClick: () -> Unit,
    variant: ButtonVariant,
    size: ButtonSize,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    content: @Composable () -> Unit,
) {
    val height =
        when (size) {
            ButtonSize.Medium -> 44.dp
            ButtonSize.Large -> 56.dp
        }

    when (variant) {
        ButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(height),
                enabled = enabled && !loading,
                shape = RoundedCornerShape(12.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor =
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.3f,
                            ),
                        disabledContentColor =
                            MaterialTheme.colorScheme.onPrimary.copy(
                                alpha = 0.5f,
                            ),
                    ),
                elevation =
                    ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp,
                        disabledElevation = 0.dp,
                    ),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                content()
            }
        }

        ButtonVariant.Secondary -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = modifier.height(height),
                enabled = enabled && !loading,
                shape = RoundedCornerShape(12.dp),
                colors =
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                content()
            }
        }
    }
}
