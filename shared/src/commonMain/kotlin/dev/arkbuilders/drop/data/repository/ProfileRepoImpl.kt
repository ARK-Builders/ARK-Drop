package dev.arkbuilders.drop.data.repository

import dev.arkbuilders.drop.data.datasource.ProfileLocalDataSource
import dev.arkbuilders.drop.domain.model.UserAvatar
import dev.arkbuilders.drop.domain.model.UserProfile
import dev.arkbuilders.drop.domain.repository.ProfileRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ProfileRepoImpl(
    private val localDataSource: ProfileLocalDataSource,
) : ProfileRepo {
    override val profile: Flow<UserProfile> = localDataSource.observeProfile()

    override suspend fun updateProfile(profile: UserProfile) {
        localDataSource.saveProfile(profile)
    }

    override suspend fun updateName(name: String) {
        updateProfile(profile.first().copy(name = name))
    }

    override suspend fun updateAvatar(avatar: UserAvatar) {
        updateProfile(profile.first().copy(avatar = avatar))
    }
}
