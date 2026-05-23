package dev.arkbuilders.drop.di

import dev.arkbuilders.drop.presentation.edit.EditProfileViewModel
import dev.arkbuilders.drop.presentation.history.HistoryViewModel
import dev.arkbuilders.drop.presentation.home.HomeViewModel
import dev.arkbuilders.drop.presentation.receive.ReceiveViewModel
import dev.arkbuilders.drop.presentation.send.SendViewModel
import dev.arkbuilders.drop.instrumentation.FirebaseReporter
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule =
    module {
        viewModel { HistoryViewModel(get()) }
        viewModel { HomeViewModel(get(), get(), get()) }
        viewModel { EditProfileViewModel(get(), get()) }
        viewModel { ReceiveViewModel(get(), get(), get()) }
        viewModel { SendViewModel(get(), get(), get(), get<FirebaseReporter>()) }
    }
