package dev.arkbuilders.drop.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.arkbuilders.drop.data.helper.AvatarHelper
import dev.arkbuilders.drop.domain.model.UserAvatar
import dev.arkbuilders.drop.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class UserAvatarDto(
    val base64: String,
    val predefinedId: String?,
)

@Serializable
data class UserProfileDto(
    val name: String,
    val avatar: UserAvatarDto,
)

class ProfileLocalDataSource(
    private val dataStore: DataStore<Preferences>,
    private val avatarHelper: AvatarHelper,
) {
    fun observeProfile(): Flow<UserProfile> =
        dataStore.data
            .map { preferences ->
                val json = preferences[PROFILE_KEY]
                if (json == null) {
                    createAndSaveDefaultProfile()
                } else {
                    runCatching {
                        Json.decodeFromString<UserProfileDto>(json).toDomain()
                    }.getOrElse {
                        createAndSaveDefaultProfile()
                    }
                }
            }
            .distinctUntilChanged()

    private suspend fun createAndSaveDefaultProfile(): UserProfile {
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

    suspend fun saveProfile(profile: UserProfile) {
        runCatching {
            val profileJson = Json.encodeToString(profile.toDto())
            dataStore.edit { preferences ->
                preferences[PROFILE_KEY] = profileJson
            }
        }
    }

    companion object {
        private val PROFILE_KEY = stringPreferencesKey("profile")
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
