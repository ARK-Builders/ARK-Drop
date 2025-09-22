package dev.arkbuilders.drop.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.arkbuilders.drop.app.TransferManager
import dev.arkbuilders.drop.app.ProfileManager
import dev.arkbuilders.drop.app.data.HistoryRepository
import dev.arkbuilders.drop.app.data.ResourcesHelperImpl
import dev.arkbuilders.drop.app.domain.ResourcesHelper
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideProfileManager(@ApplicationContext context: Context): ProfileManager {
        return ProfileManager(context)
    }

    @Provides
    @Singleton
    fun provideTransferManager(
        @ApplicationContext context: Context,
        profileManager: ProfileManager,
        historyRepository: HistoryRepository
    ): TransferManager {
        return TransferManager(context, profileManager, historyRepository)
    }

    @Provides
    @Singleton
    fun provideResourcesHelper(
        impl: ResourcesHelperImpl
    ): ResourcesHelper = impl
}
