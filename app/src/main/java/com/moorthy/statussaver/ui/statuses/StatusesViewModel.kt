package com.moorthy.statussaver.ui.statuses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.moorthy.statussaver.StatusSaverApp
import com.moorthy.statussaver.data.model.MediaType
import com.moorthy.statussaver.data.model.StatusItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the status list can show (FR2). */
sealed interface StatusesUiState {

    /** The folders are still being read. */
    data object Loading : StatusesUiState

    /** Statuses from all granted folders, newest first. */
    data class Content(val items: List<StatusItem>) : StatusesUiState {

        /** The items of one tab (Images or Videos), still newest first. */
        fun itemsOf(type: MediaType): List<StatusItem> = items.filter { item -> item.type == type }
    }

    /**
     * A folder is readable but holds no statuses: the user has not viewed any in
     * WhatsApp in the last day or so.
     */
    data object Empty : StatusesUiState

    /** No granted folder is readable any more, so the user has to grant one again. */
    data object NeedsAccess : StatusesUiState
}

/**
 * Holds the status list for the statuses screen. [loadStatuses] is
 * `StatusRepository.load`: null means no readable folder, an empty list means empty
 * folders.
 */
class StatusesViewModel(
    private val loadStatuses: suspend () -> List<StatusItem>?,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow<StatusesUiState>(StatusesUiState.Loading)
    val uiState: StateFlow<StatusesUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            val items = loadStatuses()
            mutableUiState.value = when {
                items == null -> StatusesUiState.NeedsAccess
                items.isEmpty() -> StatusesUiState.Empty
                else -> StatusesUiState.Content(items)
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as StatusSaverApp
                StatusesViewModel(app.statusRepository::load)
            }
        }
    }
}
