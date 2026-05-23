package dev.arkbuilders.drop.di

import dev.arkbuilders.drop.data.datasource.ProfileLocalDataSource
import dev.arkbuilders.drop.data.datasource.TransferSessionLocalDataSource
import dev.arkbuilders.drop.data.repository.ProfileRepoImpl
import dev.arkbuilders.drop.data.repository.ReceiveSessionRepoImpl
import dev.arkbuilders.drop.data.repository.SendSessionRepoImpl
import dev.arkbuilders.drop.data.repository.TransferSessionRepoImpl
import dev.arkbuilders.drop.domain.repository.ProfileRepo
import dev.arkbuilders.drop.domain.repository.ReceiveSessionRepo
import dev.arkbuilders.drop.domain.repository.SendSessionRepo
import dev.arkbuilders.drop.domain.repository.TransferSessionRepo
import dev.arkbuilders.drop.instrumentation.FirebaseReporter
import org.koin.dsl.module

val repositoriesModule =
    module {
        single { ProfileLocalDataSource(get(), get()) }
        single { TransferSessionLocalDataSource(get()) }

        single<ProfileRepo> { ProfileRepoImpl(get()) }
        single<SendSessionRepo> { SendSessionRepoImpl(get(), get(), get(), get<FirebaseReporter>()) }
        single<ReceiveSessionRepo> { ReceiveSessionRepoImpl(get(), get(), get(), get<FirebaseReporter>()) }
        single<TransferSessionRepo> { TransferSessionRepoImpl(get()) }
    }
