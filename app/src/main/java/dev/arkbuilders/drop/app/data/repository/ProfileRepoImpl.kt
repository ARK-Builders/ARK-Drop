package dev.arkbuilders.drop.app.data.repository

import android.content.Context
import dev.arkbuilders.drop.app.data.datasource.ProfileLocalDataSource
import dev.arkbuilders.drop.app.domain.model.UserAvatar
import dev.arkbuilders.drop.app.domain.model.UserProfile
import dev.arkbuilders.drop.app.domain.repository.ProfileRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileRepoImpl(
    private val localDataSource: ProfileLocalDataSource,
    private val context: Context,
) : ProfileRepo {

    private val _profile = MutableStateFlow(localDataSource.loadProfile())
    override val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    override fun updateProfile(profile: UserProfile) {
        _profile.value = profile
        localDataSource.saveProfile(profile)
    }

    override fun updateName(name: String) {
        updateProfile(_profile.value.copy(name = name))
    }

    override fun updateAvatar(avatar: UserAvatar) {
        updateProfile(_profile.value.copy(avatar = avatar))
    }
}