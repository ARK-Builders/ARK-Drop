package dev.arkbuilders.drop.app.ui.Home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowDownCircle
import compose.icons.tablericons.ArrowUpCircle
import compose.icons.tablericons.CloudDownload
import compose.icons.tablericons.CloudUpload
import compose.icons.tablericons.History
import dev.arkbuilders.drop.app.domain.model.TransferHistoryItem
import dev.arkbuilders.drop.app.domain.model.TransferType
import dev.arkbuilders.drop.app.domain.model.UserProfile
import dev.arkbuilders.drop.app.domain.repository.ProfileRepo
import dev.arkbuilders.drop.app.domain.repository.TransferHistoryItemRepository
import dev.arkbuilders.drop.app.navigation.DropDestination
import dev.arkbuilders.drop.app.ui.components.DropButton
import dev.arkbuilders.drop.app.ui.components.DropButtonSize
import dev.arkbuilders.drop.app.ui.components.DropButtonVariant
import dev.arkbuilders.drop.app.ui.components.DropCard
import dev.arkbuilders.drop.app.ui.components.DropCardContent
import dev.arkbuilders.drop.app.ui.components.DropCardSize
import dev.arkbuilders.drop.app.ui.components.DropCardVariant
import dev.arkbuilders.drop.app.ui.components.DropLogoWithBackground
import dev.arkbuilders.drop.app.ui.components.DropOutlinedButton
import dev.arkbuilders.drop.app.ui.components.EmptyState
import dev.arkbuilders.drop.app.ui.profile.AvatarUtils
import dev.arkbuilders.drop.app.ui.theme.DesignTokens
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
fun Home(
    navController: NavController,
    profileRepo: ProfileRepo,
    transferHistoryItemRepository: TransferHistoryItemRepository,
) {
    val profile = remember { profileRepo.getCurrentProfile() }
    val historyItems by transferHistoryItemRepository
        .historyItems.collectAsStateWithLifecycle(emptyList())

    var logoScale by remember { mutableStateOf(0f) }

    // Animate logo entrance
    LaunchedEffect(Unit) {
        delay(300)
        logoScale = 1f
    }

    val animatedLogoScale by animateFloatAsState(
        targetValue = logoScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(DesignTokens.Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xl)
    ) {
        // Header Section
        item {
            HeaderSection(
                logoScale = animatedLogoScale,
                onProfileClick = { navController.navigate(DropDestination.EditProfile.route) },
                profile = profile
            )
        }

        // Quick Actions Section
        item {
            QuickActionsSection(
                onSendClick = { navController.navigate(DropDestination.Send.route) },
                onReceiveClick = { navController.navigate(DropDestination.Receive.route) }
            )
        }

        // Recent Transfers Section
        item {
            if (historyItems.isNotEmpty()) {
                RecentTransfersSection(
                    historyItems = historyItems.take(5),
                    onViewAllClick = { navController.navigate(DropDestination.History.route) },
                    showViewAll = historyItems.isNotEmpty()
                )
            } else {
                EmptyTransfersSection()
            }
        }
    }
}

@Composable
private fun HeaderSection(
    logoScale: Float,
    onProfileClick: () -> Unit,
    profile: UserProfile
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "App header with logo and profile access" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App branding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.lg)
        ) {
            Box(
                modifier = Modifier.scale(logoScale)
            ) {
                DropLogoWithBackground(size = 56.dp)
            }

            Column {
                Text(
                    text = "Drop",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Share files instantly",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Profile access
        IconButton(
            onClick = onProfileClick,
            modifier = Modifier.semantics {
                contentDescription = "Open profile settings"
            }
        ) {
            AvatarUtils.AvatarImageWithFallback(profile.avatarB64)
        }
    }
}

@Composable
private fun QuickActionsSection(
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit
) {
    DropCard(
        variant = DropCardVariant.Elevated,
        size = DropCardSize.Large,
        contentDescription = "Quick actions for sending and receiving files"
    ) {
        DropCardContent(size = DropCardSize.Large) {
            Text(
                text = "What would you like to do?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

            // Send button
            DropButton(
                onClick = onSendClick,
                variant = DropButtonVariant.Primary,
                size = DropButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = "Send files to another device"
            ) {
                Icon(
                    TablerIcons.ArrowUpCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(DesignTokens.Spacing.md))
                Text(
                    "Send Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

            // Receive button
            DropOutlinedButton(
                onClick = onReceiveClick,
                size = DropButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = "Receive files from another device"
            ) {
                Icon(
                    TablerIcons.ArrowDownCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(DesignTokens.Spacing.md))
                Text(
                    "Receive Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RecentTransfersSection(
    historyItems: List<TransferHistoryItem>,
    onViewAllClick: () -> Unit,
    showViewAll: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Transfers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (showViewAll) {
                DropOutlinedButton(
                    onClick = onViewAllClick,
                    size = DropButtonSize.Small,
                    contentDescription = "View all transfer history"
                ) {
                    Icon(
                        TablerIcons.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.Spacing.xs))
                    Text("View All")
                }
            }
        }

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

        Column(
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
        ) {
            historyItems.forEach { item ->
                EnhancedTransferHistoryCard(item = item)
            }
        }
    }
}

@Composable
private fun EmptyTransfersSection() {
    DropCard(
        variant = DropCardVariant.Outlined,
        size = DropCardSize.Large,
        contentDescription = "No transfers yet - empty state"
    ) {
        EmptyState(
            title = "No transfers yet",
            description = "Start by sending or receiving files to see your transfer history here. Your recent activity will appear in this section."
        )
    }
}

@Composable
private fun EnhancedTransferHistoryCard(item: TransferHistoryItem) {
    DropCard(
        variant = DropCardVariant.Elevated,
        size = DropCardSize.Medium,
        contentDescription = "Transfer: ${if (item.type == TransferType.SENT) "Sent to" else "Received from"} ${item.peerName}"
    ) {
        DropCardContent(size = DropCardSize.Medium) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Transfer type icon with semantic color
                Icon(
                    imageVector = if (item.type == TransferType.SENT) TablerIcons.CloudUpload else TablerIcons.CloudDownload,
                    contentDescription = if (item.type == TransferType.SENT) "Sent" else "Received",
                    modifier = Modifier.size(24.dp),
                    tint = if (item.type == TransferType.SENT)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.width(DesignTokens.Spacing.lg))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (item.type == TransferType.SENT)
                            "Sent to ${item.peerName}"
                        else
                            "Received from ${item.peerName}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.xs))
                    Text(
                        text = "${item.fileCount} file${if (item.fileCount != 1) "s" else ""} • ${formatTimestamp(item.timestamp)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Peer avatar
                AvatarUtils.AvatarImageWithFallback(
                    base64String = item.peerAvatar,
                    fallbackText = item.peerName,
                    size = 40.dp
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: OffsetDateTime): String {
    val now = OffsetDateTime.now()
    val diff = Duration.between(timestamp, now)

    return when {
        diff.toMinutes() < 1 -> "Just now"
        diff.toHours() < 1 -> "${diff.toMinutes()}m ago"
        diff.toDays() < 1 -> "${diff.toHours()}h ago"
        diff.toDays() < 7 -> "${diff.toDays()}d ago"
        else -> timestamp.format(
            DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault())
        )
    }
}