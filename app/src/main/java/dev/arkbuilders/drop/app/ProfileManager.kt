package dev.arkbuilders.drop.app

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.arkbuilders.drop.app.ui.profile.AvatarUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class UserProfile(
    val name: String = "",
    val avatarB64: String = "",
    val avatarId: String = "avatar_00"
)

@Singleton
class ProfileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "drop_profile"
        private const val KEY_PROFILE = "user_profile"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private fun loadProfile(): UserProfile {
        val profileJson = prefs.getString(KEY_PROFILE, null)
        return if (profileJson != null) {
            try {
                json.decodeFromString<UserProfile>(profileJson)
            } catch (e: Exception) {
                createDefaultProfile()
            }
        } else {
            createDefaultProfile()
        }
    }

    private fun createDefaultProfile(): UserProfile {
        val defaultProfile = UserProfile(
            name = "Anonymous",
            avatarB64 = AvatarUtils.getDefaultAvatarBase64(context, "avatar_00"),
            avatarId = "avatar_00"
        )
        saveProfile(defaultProfile)
        return defaultProfile
    }

    fun updateProfile(profile: UserProfile) {
        _profile.value = profile
        saveProfile(profile)
    }

    fun updateName(name: String) {
        val updatedProfile = _profile.value.copy(name = name)
        updateProfile(updatedProfile)
    }

    fun updateAvatar(avatarId: String) {
        val avatarBase64 = AvatarUtils.getDefaultAvatarBase64(context, avatarId)
        val updatedProfile = _profile.value.copy(
            avatarId = avatarId,
            avatarB64 = avatarBase64
        )
        updateProfile(updatedProfile)
    }

    fun updateCustomAvatar(base64: String) {
        val updatedProfile = _profile.value.copy(
            avatarB64 = base64,
            avatarId = "custom"
        )
        updateProfile(updatedProfile)
    }

    private fun saveProfile(profile: UserProfile) {
        try {
            val profileJson = json.encodeToString(profile)
            prefs.edit().putString(KEY_PROFILE, profileJson).apply()
        } catch (e: Exception) {
            // Handle serialization error
        }
    }

    fun getCurrentProfile(): UserProfile = _profile.value
}
