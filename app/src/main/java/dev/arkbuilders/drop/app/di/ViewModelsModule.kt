package dev.arkbuilders.drop.app.di

import dev.arkbuilders.drop.app.presentation.history.HistoryViewModel
import dev.arkbuilders.drop.app.presentation.home.HomeViewModel
import dev.arkbuilders.drop.app.presentation.profile.EditProfileViewModel
import dev.arkbuilders.drop.app.presentation.receive.ReceiveViewModel
import dev.arkbuilders.drop.app.presentation.send.SendViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val viewModelsModule =
    module {
        viewModel { HistoryViewModel(get()) }
        viewModel { HomeViewModel(get(), get(), get()) }
        viewModel { EditProfileViewModel(get(), get()) }
        viewModel { ReceiveViewModel(get(), get()) }
        viewModel { (isScanToSend: Boolean) ->
            SendViewModel(
                get { parametersOf(isScanToSend) },
                get(),
                get(),
                get(),
            )
        }
    }
