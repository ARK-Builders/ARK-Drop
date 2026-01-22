package dev.arkbuilders.drop.di

import dev.arkbuilders.drop.presentation.edit.EditProfileViewModel
import dev.arkbuilders.drop.presentation.history.HistoryViewModel
import dev.arkbuilders.drop.presentation.home.HomeViewModel
import dev.arkbuilders.drop.presentation.receive.ReceiveViewModel
import dev.arkbuilders.drop.presentation.send.SendViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object ViewModels : KoinComponent {
    fun homeViewModel() = get<HomeViewModel>()

    fun profileViewModel() = get<EditProfileViewModel>()

    fun historyViewModel() = get<HistoryViewModel>()

    fun receiveViewModel() = get<ReceiveViewModel>()

    fun sendViewModel() = get<SendViewModel>()
}
