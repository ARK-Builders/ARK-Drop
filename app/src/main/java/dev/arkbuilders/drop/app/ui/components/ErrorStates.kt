package dev.arkbuilders.drop.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.AlertCircle
import compose.icons.tablericons.CloudOff
import compose.icons.tablericons.FileX
import compose.icons.tablericons.WifiOff
import dev.arkbuilders.drop.app.ui.theme.DesignTokens

enum class ErrorType {
    Network, FileTransfer, Permission, Generic, Offline
}

data class ErrorState(
    val type: ErrorType,
    val title: String,
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

@Composable
fun ErrorStateDisplay(
    errorState: ErrorState,
    modifier: Modifier = Modifier
) {
    val icon = when (errorState.type) {
        ErrorType.Network -> TablerIcons.WifiOff
        ErrorType.FileTransfer -> TablerIcons.FileX
        ErrorType.Permission -> TablerIcons.AlertCircle
        ErrorType.Offline -> TablerIcons.CloudOff
        ErrorType.Generic -> Icons.Default.Warning
    }
    
    val iconColor = when (errorState.type) {
        ErrorType.Network, ErrorType.Offline -> MaterialTheme.colorScheme.error
        ErrorType.FileTransfer -> MaterialTheme.colorScheme.error
        ErrorType.Permission -> MaterialTheme.colorScheme.error
        ErrorType.Generic -> MaterialTheme.colorScheme.error
    }
    
    DropCard(
        modifier = modifier,
        variant = DropCardVariant.Outlined,
        size = DropCardSize.Large,
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        DropCardContent(size = DropCardSize.Large) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = iconColor
                )
                
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))
                
                Text(
                    text = errorState.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))
                
                Text(
                    text = errorState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                errorState.actionLabel?.let { label ->
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))
                    
                    DropButton(
                        onClick = { errorState.onAction?.invoke() },
                        variant = DropButtonVariant.Primary,
                        size = DropButtonSize.Medium,
                        contentDescription = "Retry action"
                    ) {
                        Text(text = label)
                    }
                }
            }
        }
    }
}

// Predefined error states for common scenarios
object CommonErrors {
    fun networkError(onRetry: () -> Unit) = ErrorState(
        type = ErrorType.Network,
        title = "Connection Problem",
        message = "Unable to connect to the network. Please check your internet connection and try again.",
        actionLabel = "Retry",
        onAction = onRetry
    )
    
    fun fileTransferError(onRetry: () -> Unit) = ErrorState(
        type = ErrorType.FileTransfer,
        title = "Transfer Failed",
        message = "The file transfer was interrupted. This might be due to network issues or insufficient storage space.",
        actionLabel = "Try Again",
        onAction = onRetry
    )
    
    fun permissionError(onRequestPermission: () -> Unit) = ErrorState(
        type = ErrorType.Permission,
        title = "Permission Required",
        message = "This feature requires additional permissions to work properly. Please grant the necessary permissions.",
        actionLabel = "Grant Permission",
        onAction = onRequestPermission
    )
    
    fun offlineError() = ErrorState(
        type = ErrorType.Offline,
        title = "You're Offline",
        message = "This feature requires an internet connection. Please check your network settings and try again.",
        actionLabel = null,
        onAction = null
    )
    
    fun genericError(onRetry: () -> Unit) = ErrorState(
        type = ErrorType.Generic,
        title = "Something Went Wrong",
        message = "An unexpected error occurred. Please try again or contact support if the problem persists.",
        actionLabel = "Retry",
        onAction = onRetry
    )
}
