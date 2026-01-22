package dev.arkbuilders.drop.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.arkbuilders.drop.data.helper.PermissionsHelper
import dev.arkbuilders.drop.domain.model.TransferSession
import dev.arkbuilders.drop.domain.model.UserProfile
import dev.arkbuilders.drop.domain.repository.ProfileRepo
import dev.arkbuilders.drop.domain.repository.TransferSessionRepo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

data class HomeScreenState(
    val historyItems: List<TransferSession>,
    val profile: UserProfile,
)

sealed class HomeScreenEffect {
    object AskWritePermission : HomeScreenEffect()

    object NavigateToReceiveScreen : HomeScreenEffect()
}

class HomeViewModel(
    private val historyItemRepository: TransferSessionRepo,
    private val profileRepo: ProfileRepo,
    private val permissionsHelper: PermissionsHelper,
) : ViewModel(), ContainerHost<HomeScreenState, HomeScreenEffect> {
    override val container: Container<HomeScreenState, HomeScreenEffect> =
        container(HomeScreenState(emptyList(), UserProfile.empty()))

    init {
        historyItemRepository.historyItems.onEach {
            intent {
                reduce {
                    state.copy(historyItems = it)
                }
            }
        }.launchIn(viewModelScope)

        intent {
            val items = historyItemRepository.historyItems.first()
            val profile = profileRepo.profile.first()
            reduce {
                state.copy(
                    historyItems = items,
                    profile = profile,
                )
            }
        }
    }

    fun onReceiveClick() =
        intent {
            if (permissionsHelper.isWritePermissionGranted()) {
                postSideEffect(HomeScreenEffect.NavigateToReceiveScreen)
            } else {
                postSideEffect(HomeScreenEffect.AskWritePermission)
            }
        }
}
