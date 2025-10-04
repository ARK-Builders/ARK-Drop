package dev.arkbuilders.drop.app.data.datasource

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.arkbuilders.drop.app.ui.profile.AvatarUtils
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import androidx.core.content.edit
import dev.arkbuilders.drop.app.data.model.UserProfileDto
import dev.arkbuilders.drop.app.domain.model.UserProfile

class ProfileLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    fun loadProfile(): UserProfile {
        val profileJson = prefs.getString(KEY_PROFILE, null)
        if (profileJson == null) {
            return createDefaultProfile()
        }

        return runCatching {
            json
                .decodeFromString<UserProfileDto>(profileJson)
                .toDomain()
        }.getOrElse {
            createDefaultProfile()
        }
    }

    private fun createDefaultProfile(): UserProfile {
        val default = UserProfile(
            name = "Anonymous",
            avatarB64 = AvatarUtils.getDefaultAvatarBase64(context, "avatar_00"),
            avatarId = "avatar_00"
        )
        saveProfile(default)
        return default
    }

    fun saveProfile(profile: UserProfile) {
        runCatching {
            val profileJson = json.encodeToString(profile.toDto())
            prefs.edit { putString(KEY_PROFILE, profileJson) }
        }
    }

    companion object {
        private const val PREFS_NAME = "drop_profile"
        private const val KEY_PROFILE = "user_profile"
    }
}

private fun UserProfileDto.toDomain() = UserProfile(
    name = name,
    avatarB64 = avatarB64,
    avatarId = avatarId,
)

private fun UserProfile.toDto() = UserProfileDto(
    name = name,
    avatarB64 = avatarB64,
    avatarId = avatarId,
)