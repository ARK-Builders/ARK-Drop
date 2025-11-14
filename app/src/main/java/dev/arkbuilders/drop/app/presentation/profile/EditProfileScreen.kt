package dev.arkbuilders.drop.app.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import compose.icons.TablerIcons
import compose.icons.tablericons.Camera
import dev.arkbuilders.drop.app.domain.AvatarHelper
import dev.arkbuilders.drop.app.domain.model.UserAvatar
import dev.arkbuilders.drop.app.presentation.components.AvatarImage
import dev.arkbuilders.drop.app.presentation.components.DropButton
import dev.arkbuilders.drop.app.presentation.components.DropButtonSize
import dev.arkbuilders.drop.app.presentation.components.DropButtonVariant
import dev.arkbuilders.drop.app.presentation.components.DropCard
import dev.arkbuilders.drop.app.presentation.components.DropCardContent
import dev.arkbuilders.drop.app.presentation.components.DropCardSize
import dev.arkbuilders.drop.app.presentation.components.DropCardVariant
import dev.arkbuilders.drop.app.presentation.components.DropTopBarBack
import dev.arkbuilders.drop.app.presentation.components.ErrorState
import dev.arkbuilders.drop.app.presentation.components.ErrorStateDisplay
import dev.arkbuilders.drop.app.presentation.components.ErrorType
import dev.arkbuilders.drop.app.presentation.theme.DesignTokens
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileEnhanced(navController: NavController) {
    val viewModel: EditProfileViewModel = koinInject()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val nameFocusRequester = remember { FocusRequester() }

    val state by viewModel.collectAsState()

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            if (uri != null) {
                viewModel.onImagePicked(uri.toString())
            }
        }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            EditProfileScreenEffect.LaunchImagePicker -> {
                imagePickerLauncher.launch("image/*")
            }

            EditProfileScreenEffect.NavigateBack -> {
                navController.popBackStack()
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime)
                .verticalScroll(rememberScrollState()),
    ) {
        DropTopBarBack(
            title = "Edit Profile",
            onBackClick = { navController.navigateUp() },
            trailingContent = {
                AnimatedVisibility(
                    modifier = Modifier.padding(end = DesignTokens.Spacing.lg),
                    visible = state.hasChanges,
                    enter = scaleIn(spring(stiffness = Spring.StiffnessHigh)) + fadeIn(),
                    exit = scaleOut(spring(stiffness = Spring.StiffnessHigh)) + fadeOut(),
                ) {
                    DropButton(
                        onClick = { viewModel.onSave() },
                        variant = DropButtonVariant.Primary,
                        size = DropButtonSize.Medium,
                        contentDescription = "Save profile changes",
                    ) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            },
        )

        AnimatedVisibility(
            visible = state.avatarImageLoadingFailed,
            enter =
                slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                ) + fadeIn(),
            exit =
                slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                ) + fadeOut(),
        ) {
            ErrorStateDisplay(
                errorState =
                    ErrorState(
                        type = ErrorType.Generic,
                        title = "Profile Update Failed",
                        message =
                            "Failed to load image." +
                                " Please check your storage permissions and try again.",
                        actionLabel = "Dismiss",
                        onAction = {
                            viewModel.clearAvatarLoadingError()
                        },
                    ),
                modifier = Modifier.Companion.padding(DesignTokens.Spacing.lg),
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(DesignTokens.Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xl),
        ) {
            ProfilePreviewSection(
                name = state.name,
                avatar = state.avatar,
                onNameChange = { newName ->
                    viewModel.onNameChanged(newName)
                },
                nameError = state.nameError,
                nameFocusRequester = nameFocusRequester,
            )

            CustomAvatarSection(
                onUploadClick = {
                    viewModel.onPickImage()
                },
                hasError = state.avatarImageLoadingFailed,
            )

            AvatarSelectionSection(
                availableAvatars = UserAvatar.predefinedIds,
                avatar = state.avatar,
                onAvatarSelected = { avatarId ->
                    viewModel.onAvatarSelected(avatarId)
                },
            )

            PrivacyNoticeSection()

            Spacer(modifier = Modifier.Companion.height(DesignTokens.Spacing.xxxl))
        }
    }
}

@Composable
private fun ProfilePreviewSection(
    name: String,
    avatar: UserAvatar,
    onNameChange: (String) -> Unit,
    nameError: EditProfileNameError?,
    nameFocusRequester: FocusRequester,
) {
    DropCard(
        variant = DropCardVariant.Elevated,
        size = DropCardSize.Large,
        contentDescription = "Profile preview and name editing",
    ) {
        DropCardContent(size = DropCardSize.Large) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                var avatarScale by remember { mutableStateOf(0.8f) }
                val animatedAvatarScale by animateFloatAsState(
                    targetValue = avatarScale,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    label = "avatarScale",
                )

                LaunchedEffect(avatar) {
                    avatarScale = 0.8f
                    delay(100)
                    avatarScale = 1f
                }

                Box(
                    modifier =
                        Modifier
                            .size(120.dp)
                            .scale(animatedAvatarScale),
                    contentAlignment = Alignment.Center,
                ) {
                    AvatarImage(
                        modifier =
                            Modifier
                                .size(120.dp)
                                .semantics {
                                    contentDescription = "Current profile avatar"
                                },
                        avatarB64 = avatar.base64,
                    )

                    Surface(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp),
                        shape = CircleShape,
                        color = colorScheme.primary,
                        shadowElevation = DesignTokens.Elevation.sm,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit avatar",
                                modifier = Modifier.size(16.dp),
                                tint = colorScheme.onPrimary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.Companion.height(DesignTokens.Spacing.xl))

                // Enhanced Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = {
                        Text(
                            "Display Name",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(nameFocusRequester)
                            .semantics {
                                contentDescription = "Enter your display name"
                            },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = {
                        AnimatedVisibility(
                            visible = nameError != null,
                            enter = slideInVertically() + fadeIn(),
                            exit = slideOutVertically() + fadeOut(),
                        ) {
                            nameError?.let {
                                Text(
                                    text = it.toString(),
                                    color = colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        if (name.isNotEmpty()) {
                            IconButton(
                                onClick = { onNameChange("") },
                                modifier =
                                    Modifier.semantics {
                                        contentDescription = "Clear name field"
                                    },
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                        ),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outline,
                            errorBorderColor = colorScheme.error,
                        ),
                    shape = RoundedCornerShape(DesignTokens.CornerRadius.md),
                )

                // Character count
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = DesignTokens.Spacing.xs),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "${name.length}/50",
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (name.length > 45) {
                                colorScheme.error
                            } else {
                                colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomAvatarSection(
    onUploadClick: () -> Unit,
    hasError: Boolean,
) {
    DropCard(
        variant = DropCardVariant.Outlined,
        size = DropCardSize.Medium,
        contentDescription = "Upload custom avatar option",
    ) {
        DropCardContent(size = DropCardSize.Medium) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Custom Avatar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.Companion.height(DesignTokens.Spacing.xs))
                    Text(
                        text = "Upload your own profile picture",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.Companion.width(DesignTokens.Spacing.lg))

                DropButton(
                    onClick = onUploadClick,
                    variant =
                        if (hasError)
                            DropButtonVariant.Destructive
                        else
                            DropButtonVariant.Secondary,
                    size = DropButtonSize.Medium,
                    contentDescription = "Upload custom avatar image",
                ) {
                    Icon(
                        TablerIcons.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.Companion.width(DesignTokens.Spacing.sm))
                    Text(
                        "Upload",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarSelectionSection(
    availableAvatars: List<String>,
    avatar: UserAvatar,
    onAvatarSelected: (String) -> Unit,
) {
    val columns = 3

    Column {
        Text(
            text = "Choose Default Avatar",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            modifier =
                Modifier.semantics {
                    contentDescription = "Avatar selection section"
                },
        )

        Spacer(modifier = Modifier.Companion.height(DesignTokens.Spacing.lg))

        Column(
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.lg),
        ) {
            availableAvatars.chunked(columns).forEach { rowAvatars ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.lg),
                ) {
                    rowAvatars.forEach { avatarId ->
                        EnhancedAvatarOption(
                            modifier =
                                Modifier
                                    .aspectRatio(1f)
                                    .weight(1f),
                            avatarId = avatarId,
                            isSelected = avatar.predefinedId == avatarId,
                            onClick = { onAvatarSelected(avatarId) },
                        )
                    }

                    if (rowAvatars.size < columns) {
                        repeat(columns - rowAvatars.size) {
                            Spacer(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedAvatarOption(
    modifier: Modifier,
    avatarId: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val avatarHelper: AvatarHelper = koinInject()

    var scale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh,
            ),
        label = "avatarScale",
    )

    Card(
        modifier =
            modifier
                .scale(animatedScale),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scale = 0.95f
            onClick()
        },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        colorScheme.primaryContainer
                    } else {
                        colorScheme.surface
                    },
            ),
        border =
            if (isSelected) {
                CardDefaults.outlinedCardBorder().copy(
                    width = 3.dp,
                    brush = SolidColor(colorScheme.primary),
                )
            } else {
                CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = SolidColor(colorScheme.outline),
                )
            },
        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    if (isSelected)
                        DesignTokens.Elevation.md
                    else
                        DesignTokens.Elevation.xs,
            ),
        shape = RoundedCornerShape(DesignTokens.CornerRadius.lg),
    ) {
        Box {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                AvatarImage(
                    avatarB64 = avatarHelper.getDefaultAvatarBase64(avatarId),
                    modifier = Modifier.size(56.dp),
                )
            }
            this@Card.AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(spring(stiffness = Spring.StiffnessHigh)) + fadeIn(),
                exit = scaleOut(spring(stiffness = Spring.StiffnessHigh)) + fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Surface(
                    modifier =
                        Modifier.Companion
                            .padding(DesignTokens.Spacing.sm)
                            .size(20.dp),
                    shape = CircleShape,
                    color = colorScheme.primary,
                    shadowElevation = DesignTokens.Elevation.sm,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(DesignTokens.Spacing.xs),
                        tint = colorScheme.onPrimary,
                    )
                }
            }
        }
    }

    // Reset scale after animation
    LaunchedEffect(isSelected) {
        if (scale != 1f) {
            delay(150)
            scale = 1f
        }
    }
}

@Composable
private fun PrivacyNoticeSection() {
    DropCard(
        variant = DropCardVariant.Filled,
        size = DropCardSize.Medium,
        colors =
            CardDefaults.cardColors(
                containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = colorScheme.onSurfaceVariant,
            ),
        contentDescription = "Privacy information about profile data",
    ) {
        DropCardContent(size = DropCardSize.Medium) {
            Row(
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(20.dp)
                            .padding(top = 2.dp),
                    tint = colorScheme.primary,
                )

                Spacer(modifier = Modifier.Companion.width(DesignTokens.Spacing.md))

                Column {
                    Text(
                        text = "Privacy & Security",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.Companion.height(DesignTokens.Spacing.xs))

                    Text(
                        text =
                            "Your profile information is only shared during file transfers and" +
                                " is stored locally on your device." +
                                " Custom avatars are processed and" +
                                " stored securely without being uploaded to any server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2,
                    )
                }
            }
        }
    }
}
