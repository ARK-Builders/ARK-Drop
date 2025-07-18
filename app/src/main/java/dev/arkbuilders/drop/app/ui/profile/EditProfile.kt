package dev.arkbuilders.drop.app.ui.profile

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.arkbuilders.drop.app.ProfileManager
import dev.arkbuilders.drop.app.R
import android.graphics.Bitmap
import androidx.compose.material3.ExperimentalMaterial3Api

data class Avatar(
    val id: String,
    @DrawableRes val drawableRes: Int? = null,
    val bitmap: Bitmap? = null,
    val bgColor: Color,
    val isCustom: Boolean = false
)

// Predefined avatars with nice background colors
val defaultAvatars = listOf(
    Avatar("avatar_00", R.drawable.avatar_00, null, Color(0xFFE8F5E8)),
    Avatar("avatar_01", R.drawable.avatar_01, null, Color(0xFFE0E0E0)),
    Avatar("avatar_02", R.drawable.avatar_02, null, Color(0xFFFCE4EC)),
    Avatar("avatar_03", R.drawable.avatar_03, null, Color(0xFFE3F2FD)),
    Avatar("avatar_04", R.drawable.avatar_04, null, Color(0xFFFFF3E0)),
    Avatar("avatar_05", R.drawable.avatar_05, null, Color(0xFFFDE7F3)),
    Avatar("avatar_06", R.drawable.avatar_06, null, Color(0xFFE1F5FE)),
    Avatar("avatar_07", R.drawable.avatar_07, null, Color(0xFFF3E5F5)),
    Avatar("avatar_08", R.drawable.avatar_08, null, Color(0xFFE8F5E8))
)

@Composable
fun EditProfileHeader(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black
            )
        }

        Text(
            text = "Edit Profile",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )

        Spacer(modifier = Modifier.size(24.dp))
    }
}

@Composable
fun AvatarSection(
    modifier: Modifier = Modifier,
    avatarImage: @Composable () -> Unit,
    onChangeAvatarClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            avatarImage()
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .clickable(onClick = onChangeAvatarClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Change Avatar",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AvatarPickerSheet(
    context: Context,
    defaultAvatars: List<Avatar>,
    customAvatars: List<Avatar>,
    selectedAvatar: Avatar,
    onSelect: (Avatar) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Avatar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Close"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Avatar Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Create New Avatar Button
            item {
                CreateNewAvatarButton(
                    onClick = onCreateNew
                )
            }

            // Default Avatars
            items(defaultAvatars) { avatar ->
                AvatarItem(
                    avatar = avatar,
                    isSelected = avatar.id == selectedAvatar.id,
                    onClick = { onSelect(avatar) }
                )
            }

            // Custom Avatars
            items(customAvatars) { avatar ->
                AvatarItem(
                    avatar = avatar,
                    isSelected = avatar.id == selectedAvatar.id,
                    onClick = { onSelect(avatar) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun CreateNewAvatarButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color(0xFFF0F0F0))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Create new avatar",
            modifier = Modifier.size(24.dp),
            tint = Color.Gray
        )
    }
}

@Composable
fun AvatarItem(
    avatar: Avatar,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) avatar.bgColor.copy(alpha = 0.8f)
                else avatar.bgColor
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (avatar.isCustom && avatar.bitmap != null) {
            Image(
                bitmap = avatar.bitmap.asImageBitmap(),
                contentDescription = "Avatar ${avatar.id}",
                modifier = Modifier.size(48.dp)
            )
        } else {
            avatar.drawableRes?.let { drawableRes ->
                Image(
                    painter = painterResource(drawableRes),
                    contentDescription = "Avatar ${avatar.id}",
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun NameInput(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Color.Black
                ),
                modifier = Modifier.weight(1f),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = "Enter your name",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )

            if (value.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SaveButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4285F4)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = "Save",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfile(
    modifier: Modifier = Modifier,
    navController: NavController,
    profileManager: ProfileManager
) {
    val context = navController.context
    var sheetVisible by remember { mutableStateOf(false) }

    // Load existing profile or create default
    var profile by remember { mutableStateOf(profileManager.loadOrDefault()) }

    // Find current avatar based on profile data
    var selectedAvatarImage by remember {
        mutableStateOf(
            if (profile.avatarB64.isNotEmpty() == true) {
                // If we have a custom avatar, we'll need to handle this differently
                // For now, default to first avatar
                defaultAvatars.first()

            } else {
                defaultAvatars.first()
            }
        )
    }

// Initialize default avatar Base64 if profile is new
    LaunchedEffect(profile) {
        if (profile.avatarB64.isEmpty()) {
            val defaultAvatarBase64 = AvatarUtils.getDefaultAvatarBase64(context, "avatar_00")
            profile = profile.copy(avatarB64 = defaultAvatarBase64)
        }
    }

    var customAvatars by remember { mutableStateOf<List<Avatar>>(emptyList()) }
    var showAvatarCreator by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        EditProfileHeader(
            onBackClick = { navController.popBackStack() }
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Avatar Section
        AvatarSection(
            onChangeAvatarClick = { sheetVisible = true },
            avatarImage = {
                AvatarUtils.AvatarImage(
                    base64String = profile.avatarB64,
                        modifier = Modifier.size(80.dp)
                )
            }
        )

        // Avatar Picker Bottom Sheet
        if (sheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { sheetVisible = false }
            ) {
                AvatarPickerSheet(
                    context = context,
                    defaultAvatars = defaultAvatars,
                    customAvatars = customAvatars,
                    selectedAvatar = selectedAvatarImage,
                    onSelect = { avatar ->
                        selectedAvatarImage = avatar
                        // Update profile with new avatar Base64
                        val avatarBase64 = AvatarUtils.getDefaultAvatarBase64(context, avatar.id)
                        profile = profile.copy(avatarB64 = avatarBase64)
                        sheetVisible = false
                    },
                    onCreateNew = {
                        showAvatarCreator = true
                        sheetVisible = false
                    },
                    onDismiss = { sheetVisible = false }
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Name Input
        NameInput(
            value = profile.name,
            onValueChange = { newName ->
                profile = profile.copy(name = newName)
            },
            onClear = { profile = profile.copy(name = "") }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Save Button
        SaveButton(
            onClick = {
                profileManager.save(profile)
                navController.popBackStack()
            }
        )

        Spacer(modifier = Modifier.weight(1f))
    }

    // Avatar Creator Sheet
    if (showAvatarCreator) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarCreator = false }
        ) {
            AvatarCreatorSheet(
                onAvatarCreated = { customAvatar ->
                    // Convert CustomAvatar to Avatar
                    val newAvatar = Avatar(
                        id = customAvatar.id,
                        drawableRes = null,
                        bitmap = customAvatar.bitmap,
                        bgColor = Color(0xFFE3F2FD), // Default background for custom avatars
                        isCustom = true
                    )

                    // Save custom avatar locally
                    CustomAvatarManager.saveCustomAvatar(context, customAvatar)

                    // Add to custom avatars list
                    customAvatars = customAvatars + newAvatar

                    // Set as selected avatar
                    selectedAvatarImage = newAvatar

                    // Update profile with custom avatar Base64
                    profile = profile.copy(avatarB64 = customAvatar.base64)

                    showAvatarCreator = false
                },
                onDismiss = { showAvatarCreator = false }
            )
        }
    }
}