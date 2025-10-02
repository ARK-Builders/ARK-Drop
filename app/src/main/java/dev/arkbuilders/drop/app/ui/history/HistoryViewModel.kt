package dev.arkbuilders.drop.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.arkbuilders.drop.app.domain.model.TransferHistoryItem
import dev.arkbuilders.drop.app.domain.repository.TransferHistoryItemRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

data class HistoryScreenState(
    val historyItems: List<TransferHistoryItem>,
    val showClearDialog: Boolean,
    val showDeleteDialog: Boolean,
)

sealed class HistoryScreenEffect

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyItemRepository: TransferHistoryItemRepository,
) : ViewModel(), ContainerHost<HistoryScreenState, HistoryScreenEffect> {
    override val container: Container<HistoryScreenState, HistoryScreenEffect> = container(
        HistoryScreenState(
            historyItems = emptyList(),
            showClearDialog = false,
            showDeleteDialog = false
        )
    )

    init {
        historyItemRepository.historyItems.onEach { items ->
            intent {
                reduce {
                    state.copy(historyItems = items)
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onShowClearDialog() = intent {
        reduce {
            state.copy(showClearDialog = true)
        }
    }

    fun onClear() = intent {
        historyItemRepository.clearHistory()
        reduce {
            state.copy(showClearDialog = false)
        }
    }

    fun onDismissClearDialog() = intent {
        reduce {
            state.copy(showClearDialog = false)
        }
    }

    fun onShowDeleteDialog() = intent {
        reduce {
            state.copy(showDeleteDialog = true)
        }
    }

    fun onDelete(id: Long) = intent {
        historyItemRepository.deleteHistoryItem(id)
        reduce {
            state.copy(showDeleteDialog = false)
        }
    }

    fun onDismissDeleteDialog() = intent {
        reduce {
            state.copy(showDeleteDialog = false)
        }
    }
}