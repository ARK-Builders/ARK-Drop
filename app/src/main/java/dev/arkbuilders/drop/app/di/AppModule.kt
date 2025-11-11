package dev.arkbuilders.drop.app.di

import dev.arkbuilders.drop.app.data.datasource.ProfileLocalDataSource
import dev.arkbuilders.drop.app.data.datasource.TransferHistoryItemLocalDataSource
import dev.arkbuilders.drop.app.data.db.Database
import dev.arkbuilders.drop.app.data.db.dao.TransferHistoryItemDao
import dev.arkbuilders.drop.app.data.helper.AvatarHelperImpl
import dev.arkbuilders.drop.app.data.helper.PermissionsHelperImpl
import dev.arkbuilders.drop.app.data.helper.ResourcesHelperImpl
import dev.arkbuilders.drop.app.data.repository.NetworkStatusImpl
import dev.arkbuilders.drop.app.data.repository.ProfileRepoImpl
import dev.arkbuilders.drop.app.data.repository.TransferHistoryItemRepositoryImpl
import dev.arkbuilders.drop.app.data.repository.TransferManager
import dev.arkbuilders.drop.app.domain.AvatarHelper
import dev.arkbuilders.drop.app.domain.PermissionsHelper
import dev.arkbuilders.drop.app.domain.ResourcesHelper
import dev.arkbuilders.drop.app.domain.repository.NetworkStatus
import dev.arkbuilders.drop.app.domain.repository.ProfileRepo
import dev.arkbuilders.drop.app.domain.repository.TransferHistoryItemRepository
import dev.arkbuilders.drop.app.domain.usecase.ReceiveFilesUseCase
import dev.arkbuilders.drop.app.domain.usecase.SendFilesUseCase
import org.koin.dsl.module

val appModule = module {
    single<ProfileRepo> { ProfileRepoImpl(get(), get()) }
    single<TransferManager> { TransferManager(get(), get(), get()) }
    single<ResourcesHelper> { ResourcesHelperImpl(get()) }
    single<Database> { Database.build(get()) }
    single<TransferHistoryItemRepository> { TransferHistoryItemRepositoryImpl(get()) }
    single<PermissionsHelper> { PermissionsHelperImpl(get()) }
    single<NetworkStatus> { NetworkStatusImpl(get()) }
    single<AvatarHelper> { AvatarHelperImpl(get()) }
    single{ ProfileLocalDataSource(get(), get()) }
    single{ TransferHistoryItemLocalDataSource(get()) }
    factory<TransferHistoryItemDao> {
        val db: Database = get()
        db.transferHistoryDao()
    }
    factory<SendFilesUseCase> { SendFilesUseCase(get(), get(), get()) }
    factory<ReceiveFilesUseCase> { ReceiveFilesUseCase(get(), get(), get()) }
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