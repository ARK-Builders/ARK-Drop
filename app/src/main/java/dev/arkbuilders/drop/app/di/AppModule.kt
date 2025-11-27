package dev.arkbuilders.drop.app.di

import dev.arkbuilders.drop.app.data.datasource.ProfileLocalDataSource
import dev.arkbuilders.drop.app.data.datasource.TransferSessionLocalDataSource
import dev.arkbuilders.drop.app.data.db.Database
import dev.arkbuilders.drop.app.data.db.dao.TransferSessionDao
import dev.arkbuilders.drop.app.data.helper.AvatarHelperImpl
import dev.arkbuilders.drop.app.data.helper.PermissionsHelperImpl
import dev.arkbuilders.drop.app.data.helper.ResourcesHelperImpl
import dev.arkbuilders.drop.app.data.repository.NetworkStatusImpl
import dev.arkbuilders.drop.app.data.repository.ProfileRepoImpl
import dev.arkbuilders.drop.app.data.repository.ReceiveSessionRepo
import dev.arkbuilders.drop.app.data.repository.SendSessionRepo
import dev.arkbuilders.drop.app.data.repository.TransferSessionRepoImpl
import dev.arkbuilders.drop.app.domain.AvatarHelper
import dev.arkbuilders.drop.app.domain.PermissionsHelper
import dev.arkbuilders.drop.app.domain.ResourcesHelper
import dev.arkbuilders.drop.app.domain.repository.NetworkStatus
import dev.arkbuilders.drop.app.domain.repository.ProfileRepo
import dev.arkbuilders.drop.app.domain.repository.TransferSessionRepo
import dev.arkbuilders.drop.app.domain.usecase.ReceiveFilesUseCase
import dev.arkbuilders.drop.app.domain.usecase.SendFilesUseCase
import org.koin.dsl.module

val appModule =
    module {
        single<ProfileRepo> { ProfileRepoImpl(get(), get()) }
        single<ResourcesHelper> { ResourcesHelperImpl(get()) }
        single<Database> { Database.build(get()) }
        single<TransferSessionRepo> { TransferSessionRepoImpl(get()) }
        single<PermissionsHelper> { PermissionsHelperImpl(get()) }
        single<NetworkStatus> { NetworkStatusImpl(get()) }
        single<AvatarHelper> { AvatarHelperImpl(get()) }
        single { ProfileLocalDataSource(get(), get()) }
        single { TransferSessionLocalDataSource(get()) }
        single { SendSessionRepo(get(), get(), get()) }
        single { ReceiveSessionRepo(get(), get(), get()) }
        factory<TransferSessionDao> {
            val db: Database = get()
            db.transferHistoryDao()
        }
        factory<SendFilesUseCase> { SendFilesUseCase(get(), get(), get()) }
        factory<ReceiveFilesUseCase> { ReceiveFilesUseCase(get(), get(), get()) }
    }
