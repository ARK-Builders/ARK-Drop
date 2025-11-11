package dev.arkbuilders.drop.app.di

import dev.arkbuilders.drop.app.presentation.Home.HomeViewModel
import dev.arkbuilders.drop.app.presentation.history.HistoryViewModel
import dev.arkbuilders.drop.app.presentation.profile.EditProfileViewModel
import dev.arkbuilders.drop.app.presentation.receive.ReceiveViewModel
import dev.arkbuilders.drop.app.presentation.send.SendViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelsModule = module {
    viewModelOf(::HistoryViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::EditProfileViewModel)
    viewModelOf(::ReceiveViewModel)
    viewModelOf(::SendViewModel)
}