package dev.arkbuilders.drop.app.domain.repository

import dev.arkbuilders.drop.app.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface ProfileRepo {
    val profile: StateFlow<UserProfile>
    fun getCurrentProfile() = profile.value
    fun updateProfile(profile: UserProfile)
    fun updateName(name: String)
    fun updateAvatar(avatarId: String)
    fun updateCustomAvatar(base64: String)
}