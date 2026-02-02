package dev.arkbuilders.drop.di

import dev.arkbuilders.drop.presentation.edit.EditProfileViewModel
import dev.arkbuilders.drop.presentation.history.HistoryViewModel
import dev.arkbuilders.drop.presentation.home.HomeViewModel
import dev.arkbuilders.drop.presentation.receive.ReceiveViewModel
import dev.arkbuilders.drop.presentation.send.SendViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Helper object to expose Koin dependencies to iOS
 * SKIE will make these accessible from Swift
 */
object KoinHelper : KoinComponent {
    fun getHomeViewModel(): HomeViewModel {
        val viewModel: HomeViewModel by inject()
        return viewModel
    }

    fun getSendViewModel(): SendViewModel {
        val viewModel: SendViewModel by inject()
        return viewModel
    }

    fun getReceiveViewModel(): ReceiveViewModel {
        val viewModel: ReceiveViewModel by inject()
        return viewModel
    }

    fun getEditProfileViewModel(): EditProfileViewModel {
        val viewModel: EditProfileViewModel by inject()
        return viewModel
    }

    fun getHistoryViewModel(): HistoryViewModel {
        val viewModel: HistoryViewModel by inject()
        return viewModel
    }
}
