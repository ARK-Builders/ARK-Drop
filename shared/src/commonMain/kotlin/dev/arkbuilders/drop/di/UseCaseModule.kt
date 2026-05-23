package dev.arkbuilders.drop.di

import dev.arkbuilders.drop.domain.usecase.ReceiveFilesUseCase
import dev.arkbuilders.drop.domain.usecase.SendFilesUseCase
import org.koin.dsl.module

val useCaseModule =
    module {
        factory<SendFilesUseCase> { SendFilesUseCase(get(), get(), get()) }
        factory<ReceiveFilesUseCase> { ReceiveFilesUseCase(get(), get()) }
    }
