package dev.arkbuilders.drop.app.ui.Home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.arkbuilders.drop.app.domain.model.TransferHistoryItem
import dev.arkbuilders.drop.app.domain.model.UserProfile
import dev.arkbuilders.drop.app.domain.repository.ProfileRepo
import dev.arkbuilders.drop.app.domain.repository.TransferHistoryItemRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

data class HomeScreenState(
    val historyItems: List<TransferHistoryItem>,
    val profile: UserProfile,
)

sealed class HomeScreenEffect

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val historyItemRepository: TransferHistoryItemRepository,
    private val profileRepo: ProfileRepo,
): ViewModel(), ContainerHost<HomeScreenState, HomeScreenEffect>{
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
            val profile = profileRepo.getCurrentProfile()
            reduce {
                state.copy(
                    historyItems = items,
                    profile = profile,
                )
            }
        }
    }

}