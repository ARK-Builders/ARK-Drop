package dev.arkbuilders.drop.domain.repository

import dev.arkbuilders.drop.domain.model.UserAvatar
import dev.arkbuilders.drop.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepo {
    val profile: Flow<UserProfile>

    suspend fun updateProfile(profile: UserProfile)

    suspend fun updateName(name: String)

    suspend fun updateAvatar(avatar: UserAvatar)
}
