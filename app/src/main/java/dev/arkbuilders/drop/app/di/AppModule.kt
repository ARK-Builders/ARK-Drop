package dev.arkbuilders.drop.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.arkbuilders.drop.app.data.repository.TransferManager
import dev.arkbuilders.drop.app.data.helper.ResourcesHelperImpl
import dev.arkbuilders.drop.app.data.db.Database
import dev.arkbuilders.drop.app.data.db.dao.TransferHistoryItemDao
import dev.arkbuilders.drop.app.data.helper.AvatarHelperImpl
import dev.arkbuilders.drop.app.data.helper.PermissionsHelperImpl
import dev.arkbuilders.drop.app.data.repository.NetworkStatusImpl
import dev.arkbuilders.drop.app.data.repository.ProfileRepoImpl
import dev.arkbuilders.drop.app.data.repository.TransferHistoryItemRepositoryImpl
import dev.arkbuilders.drop.app.domain.AvatarHelper
import dev.arkbuilders.drop.app.domain.PermissionsHelper
import dev.arkbuilders.drop.app.domain.ResourcesHelper
import dev.arkbuilders.drop.app.domain.repository.NetworkStatus
import dev.arkbuilders.drop.app.domain.repository.ProfileRepo
import dev.arkbuilders.drop.app.domain.repository.TransferHistoryItemRepository
import org.koin.dsl.module
import javax.inject.Singleton

val appModule = module {
    single<ProfileRepo> { ProfileRepoImpl(get(), get()) }
    single<TransferManager> { TransferManager(get(), get(), get()) }
    single<ResourcesHelper> { ResourcesHelperImpl(get()) }
    single<Database> { Database.build(get()) }
    factory<TransferHistoryItemDao> { get() }
    single<TransferHistoryItemRepository> { TransferHistoryItemRepositoryImpl(get()) }
    single<PermissionsHelper> { PermissionsHelperImpl(get()) }
    single<NetworkStatus> { NetworkStatusImpl(get()) }
    single<AvatarHelper> { AvatarHelperImpl(get()) }
}

/*@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideProfileRepo(impl: ProfileRepoImpl): ProfileRepo = impl

    @Provides
    @Singleton
    fun provideTransferManager(
        @ApplicationContext context: Context,
        profileRepo: ProfileRepo,
        transferHistoryItemRepository: TransferHistoryItemRepository
    ): TransferManager {
        return TransferManager(context, profileRepo, transferHistoryItemRepository)
    }

    @Provides
    @Singleton
    fun provideResourcesHelper(
        impl: ResourcesHelperImpl
    ): ResourcesHelper = impl

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ) = Database.build(context)

    @Provides
    fun provideTransferHistoryItemDao(
        db: Database,
    ) = db.transferHistoryDao()

    @Provides
    @Singleton
    fun provideTransferHistoryItemRepository(
        impl: TransferHistoryItemRepositoryImpl
    ): TransferHistoryItemRepository = impl

    @Provides
    @Singleton
    fun providePermissionHelper(
        impl: PermissionsHelperImpl
    ): PermissionsHelper = impl

    @Provides
    @Singleton
    fun provideNetworkStatus(
        impl: NetworkStatusImpl
    ): NetworkStatus = impl

    @Provides
    @Singleton
    fun provideAvatarHelper(
        impl: AvatarHelperImpl
    ): AvatarHelper = impl
}
*/