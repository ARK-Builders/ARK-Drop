package dev.arkbuilders.drop.app.ui.profile

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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import compose.icons.TablerIcons
import compose.icons.tablericons.Camera
import compose.icons.tablericons.Check
import dev.arkbuilders.drop.app.domain.model.UserProfile
import dev.arkbuilders.drop.app.domain.repository.ProfileRepo
import dev.arkbuilders.drop.app.ui.components.DropButton
import dev.arkbuilders.drop.app.ui.components.DropButtonSize
import dev.arkbuilders.drop.app.ui.components.DropButtonVariant
import dev.arkbuilders.drop.app.ui.components.DropCard
import dev.arkbuilders.drop.app.ui.components.DropCardContent
import dev.arkbuilders.drop.app.ui.components.DropCardSize
import dev.arkbuilders.drop.app.ui.components.DropCardVariant
import dev.arkbuilders.drop.app.ui.components.ErrorStateDisplay
import dev.arkbuilders.drop.app.ui.components.ErrorType
import dev.arkbuilders.drop.app.ui.components.LoadingIndicator
import dev.arkbuilders.drop.app.ui.theme.DesignTokens
import kotlinx.coroutines.delay

// UI State Management
data class EditProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showSuccess: Boolean = false,
    val nameError: String? = null,
    val avatarError: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileEnhanced(
    navController: NavController,
    profileRepo: ProfileRepo
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // State management
    val profile by profileRepo.profile.collectAsState()
    var uiState by remember { mutableStateOf(EditProfileUiState()) }
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var selectedAvatarId by rememberSaveable { mutableStateOf(profile.avatarId) }
    var customAvatarBase64 by remember { mutableStateOf<String?>(null) }

    // Focus management
    val nameFocusRequester = remember { FocusRequester() }

    // Validation
    val isNameValid by remember {
        derivedStateOf {
            name.isNotBlank() && name.length <= 50 && name.trim().length >= 2
        }
    }

    val hasChanges by remember {
        derivedStateOf {
            name != profile.name ||
                    selectedAvatarId != profile.avatarId ||
                    customAvatarBase64 != null
        }
    }

    val canSave by remember {
        derivedStateOf {
            isNameValid && hasChanges && !uiState.isSaving
        }
    }

    // Available avatars
    val availableAvatars = remember {
        listOf(
            "avatar_00", "avatar_01", "avatar_02", "avatar_03",
            "avatar_04", "avatar_05", "avatar_06", "avatar_07", "avatar_08"
        )
    }

    // Image picker launcher with error handling
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                uiState = uiState.copy(isLoading = true, error = null)
                val base64 = AvatarUtils.uriToBase64(context, uri)
                if (base64 != null) {
                    customAvatarBase64 = base64
                    selectedAvatarId = "custom"
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } else {
                    uiState = uiState.copy(
                        error = "Unable to process the selected image. Please try a different image.",
                        avatarError = "Invalid image format"
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    error = "Failed to load image. Please check your storage permissions and try again.",
                    avatarError = "Image loading failed"
                )
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    // Save profile function
    val saveProfile =  {
        if (canSave) {
            uiState = uiState.copy(isSaving = true, error = null)
            profileRepo.updateName(name.trim())
            if (selectedAvatarId == "custom" && customAvatarBase64 != null) {
                profileRepo.updateCustomAvatar(customAvatarBase64!!)
            } else {
                profileRepo.updateAvatar(selectedAvatarId)
            }

            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            uiState = uiState.copy(isSaving = false, showSuccess = true)
        }
    }

    LaunchedEffect(uiState.showSuccess) {
        if (uiState.showSuccess) {
            delay(1500)
            navController.navigateUp()
        }
    }


    // Real-time name validation
    LaunchedEffect(name) {
        uiState = uiState.copy(
            nameError = when {
                name.isBlank() -> "Name cannot be empty"
                name.trim().length < 2 -> "Name must be at least 2 characters"
                name.length > 50 -> "Name cannot exceed 50 characters"
                else -> null
            }
        )
    }

    // Clear success state when user makes changes
    LaunchedEffect(name, selectedAvatarId, customAvatarBase64) {
        if (uiState.showSuccess) {
            uiState = uiState.copy(showSuccess = false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.ime)
    ) {
        // Enhanced Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigateUp()
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Go back to previous screen"
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = colorScheme.onSurface
                    )
                }
            },
            actions = {
                AnimatedVisibility(
                    visible = canSave,
                    enter = scaleIn(spring(stiffness = Spring.StiffnessHigh)) + fadeIn(),
                    exit = scaleOut(spring(stiffness = Spring.StiffnessHigh)) + fadeOut()
                ) {
                    DropButton(
                        onClick = saveProfile,
                        variant = DropButtonVariant.Primary,
                        size = DropButtonSize.Medium,
                        loading = uiState.isSaving,
                        contentDescription = "Save profile changes"
                    ) {
                        if (!uiState.isSaving) {
                            Icon(
                                if (uiState.showSuccess) TablerIcons.Check else Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(DesignTokens.Spacing.xs))
                        }
                        Text(
                            text = when {
                                uiState.showSuccess -> "Saved!"
                                uiState.isSaving -> "Saving..."
                                else -> "Save"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colorScheme.surface,
                titleContentColor = colorScheme.onSurface
            )
        )

        // Error Display
        AnimatedVisibility(
            visible = uiState.error != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeOut()
        ) {
            uiState.error?.let { error ->
                ErrorStateDisplay(
                    errorState = dev.arkbuilders.drop.app.ui.components.ErrorState(
                        type = ErrorType.Generic,
                        title = "Profile Update Failed",
                        message = error,
                        actionLabel = "Dismiss",
                        onAction = { uiState = uiState.copy(error = null) }
                    ),
                    modifier = Modifier.padding(DesignTokens.Spacing.lg)
                )
            }
        }

        // Loading Overlay
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.lg),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(message = "Processing image...")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(DesignTokens.Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xl)
            ) {
                // Profile Preview Section
                item {
                    ProfilePreviewSection(
                        name = name,
                        selectedAvatarId = selectedAvatarId,
                        customAvatarBase64 = customAvatarBase64,
                        profile = profile,
                        onNameChange = { newName ->
                            name = newName
                            uiState = uiState.copy(error = null)
                        },
                        nameError = uiState.nameError,
                        nameFocusRequester = nameFocusRequester,
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    )
                }

                // Custom Avatar Upload Section
                item {
                    CustomAvatarSection(
                        onUploadClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            imagePickerLauncher.launch("image/*")
                        },
                        hasError = uiState.avatarError != null
                    )
                }

                // Avatar Selection Section
                item {
                    AvatarSelectionSection(
                        availableAvatars = availableAvatars,
                        selectedAvatarId = selectedAvatarId,
                        onAvatarSelected = { avatarId ->
                            selectedAvatarId = avatarId
                            customAvatarBase64 = null
                            uiState = uiState.copy(error = null, avatarError = null)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                // Privacy Notice Section
                item {
                    PrivacyNoticeSection()
                }

                // Bottom spacing for better UX
                item {
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.xxxl))
                }
            }
        }
    }
}

@Composable
private fun ProfilePreviewSection(
    name: String,
    selectedAvatarId: String,
    customAvatarBase64: String?,
    profile: UserProfile,
    onNameChange: (String) -> Unit,
    nameError: String?,
    nameFocusRequester: FocusRequester,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    DropCard(
        variant = DropCardVariant.Elevated,
        size = DropCardSize.Large,
        contentDescription = "Profile preview and name editing"
    ) {
        DropCardContent(size = DropCardSize.Large) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar Preview with Animation
                val displayAvatarBase64 = when {
                    selectedAvatarId == "custom" && customAvatarBase64 != null -> customAvatarBase64!!
                    selectedAvatarId == "custom" && profile.avatarId == "custom" -> profile.avatarB64
                    else -> AvatarUtils.getDefaultAvatarBase64(context, selectedAvatarId)
                }

                var avatarScale by remember { mutableStateOf(0.8f) }
                val animatedAvatarScale by animateFloatAsState(
                    targetValue = avatarScale,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "avatarScale"
                )

                LaunchedEffect(selectedAvatarId, customAvatarBase64) {
                    avatarScale = 0.8f
                    delay(100)
                    avatarScale = 1f
                }

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(animatedAvatarScale),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarUtils.AvatarImage(
                        base64String = displayAvatarBase64,
                        modifier = Modifier
                            .size(120.dp)
                            .semantics {
                                contentDescription = "Current profile avatar"
                            }
                    )

                    // Edit indicator
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp),
                        shape = CircleShape,
                        color = colorScheme.primary,
                        shadowElevation = DesignTokens.Elevation.sm
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit avatar",
                                modifier = Modifier.size(16.dp),
                                tint = colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

                // Enhanced Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = {
                        Text(
                            "Display Name",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier
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
                            exit = slideOutVertically() + fadeOut()
                        ) {
                            nameError?.let {
                                Text(
                                    text = it,
                                    color = colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        if (name.isNotEmpty()) {
                            IconButton(
                                onClick = { onNameChange("") },
                                modifier = Modifier.semantics {
                                    contentDescription = "Clear name field"
                                }
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onDone() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        errorBorderColor = colorScheme.error
                    ),
                    shape = RoundedCornerShape(DesignTokens.CornerRadius.md)
                )

                // Character count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DesignTokens.Spacing.xs),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "${name.length}/50",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (name.length > 45) {
                            colorScheme.error
                        } else {
                            colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomAvatarSection(
    onUploadClick: () -> Unit,
    hasError: Boolean
) {
    DropCard(
        variant = DropCardVariant.Outlined,
        size = DropCardSize.Medium,
        contentDescription = "Upload custom avatar option"
    ) {
        DropCardContent(size = DropCardSize.Medium) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Custom Avatar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.xs))
                    Text(
                        text = "Upload your own profile picture",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(DesignTokens.Spacing.lg))

                DropButton(
                    onClick = onUploadClick,
                    variant = if (hasError) DropButtonVariant.Destructive else DropButtonVariant.Secondary,
                    size = DropButtonSize.Medium,
                    contentDescription = "Upload custom avatar image"
                ) {
                    Icon(
                        TablerIcons.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.Spacing.sm))
                    Text(
                        "Upload",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarSelectionSection(
    availableAvatars: List<String>,
    selectedAvatarId: String,
    onAvatarSelected: (String) -> Unit
) {
    Column {
        Text(
            text = "Choose Default Avatar",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            modifier = Modifier.semantics {
                contentDescription = "Avatar selection section"
            }
        )

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.lg),
            modifier = Modifier.height(300.dp) // Fixed height to prevent layout issues
        ) {
            items(availableAvatars) { avatarId ->
                EnhancedAvatarOption(
                    avatarId = avatarId,
                    isSelected = selectedAvatarId == avatarId,
                    onClick = { onAvatarSelected(avatarId) }
                )
            }
        }
    }
}

@Composable
private fun EnhancedAvatarOption(
    avatarId: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var scale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "avatarScale"
    )

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(animatedScale)
            .semantics {
                contentDescription = "Avatar option ${avatarId.replace("avatar_", "")}"
            },
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scale = 0.95f
            onClick()
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                colorScheme.primaryContainer
            } else {
                colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                width = 3.dp,
                brush = androidx.compose.ui.graphics.SolidColor(colorScheme.primary)
            )
        } else {
            CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outline)
            )
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) DesignTokens.Elevation.md else DesignTokens.Elevation.xs
        ),
        shape = RoundedCornerShape(DesignTokens.CornerRadius.lg)
    ) {
        // Selection indicator
        AnimatedVisibility(
            visible = isSelected,
            enter = scaleIn(spring(stiffness = Spring.StiffnessHigh)) + fadeIn(),
            exit = scaleOut(spring(stiffness = Spring.StiffnessHigh)) + fadeOut(),
            modifier = Modifier.align(Alignment.End)
        ) {
            Surface(
                modifier = Modifier
                    .padding(DesignTokens.Spacing.sm)
                    .size(24.dp),
                shape = CircleShape,
                color = colorScheme.primary,
                shadowElevation = DesignTokens.Elevation.sm
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(DesignTokens.Spacing.xs),
                    tint = colorScheme.onPrimary
                )
            }
        }


        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AvatarUtils.AvatarImage(
                base64String = AvatarUtils.getDefaultAvatarBase64(context, avatarId),
                modifier = Modifier.size(56.dp)
            )
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
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = colorScheme.onSurfaceVariant
        ),
        contentDescription = "Privacy information about profile data"
    ) {
        DropCardContent(size = DropCardSize.Medium) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp),
                    tint = colorScheme.primary
                )

                Spacer(modifier = Modifier.width(DesignTokens.Spacing.md))

                Column {
                    Text(
                        text = "Privacy & Security",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.xs))

                    Text(
                        text = "Your profile information is only shared during file transfers and is stored locally on your device. Custom avatars are processed and stored securely without being uploaded to any server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2
                    )
                }
            }
        }
    }
}
