package dev.arkbuilders.drop.app.data.datasource

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.arkbuilders.drop.app.data.model.UserAvatarDto
import dev.arkbuilders.drop.app.data.model.UserProfileDto
import dev.arkbuilders.drop.app.domain.AvatarHelper
import dev.arkbuilders.drop.app.domain.model.UserAvatar
import dev.arkbuilders.drop.app.domain.model.UserProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ProfileLocalDataSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val avatarHelper: AvatarHelper,
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
            val defaultAvatarId = "avatar_00"
            val default =
                UserProfile(
                    name = "Anonymous",
                    avatar =
                        UserAvatar(
                            base64 = avatarHelper.getDefaultAvatarBase64(defaultAvatarId),
                            predefinedId = defaultAvatarId,
                        ),
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

private fun UserProfileDto.toDomain() =
    UserProfile(
        name = name,
        avatar = avatar.toDomain(),
    )

private fun UserProfile.toDto() =
    UserProfileDto(
        name = name,
        avatar = avatar.toDto(),
    )

private fun UserAvatar.toDto() =
    UserAvatarDto(
        base64 = base64,
        predefinedId = predefinedId,
    )

private fun UserAvatarDto.toDomain() =
    UserAvatar(
        base64 = base64,
        predefinedId = predefinedId,
    )
