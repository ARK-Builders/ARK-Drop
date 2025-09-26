package dev.arkbuilders.drop.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.arkbuilders.drop.app.data.datasource.ProfileLocalDataSource
import dev.arkbuilders.drop.app.domain.model.UserProfile
import dev.arkbuilders.drop.app.domain.repository.ProfileRepo
import dev.arkbuilders.drop.app.ui.profile.AvatarUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepoImpl @Inject constructor(
    private val localDataSource: ProfileLocalDataSource,
    @ApplicationContext private val context: Context
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

    override fun updateAvatar(avatarId: String) {
        val avatarB64 = AvatarUtils.getDefaultAvatarBase64(context, avatarId)
        updateProfile(_profile.value.copy(avatarId = avatarId, avatarB64 = avatarB64))
    }

    override fun updateCustomAvatar(base64: String) {
        updateProfile(_profile.value.copy(avatarId = "custom", avatarB64 = base64))
    }
}