package com.moorthy.statussaver.ui.statuses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.moorthy.statussaver.StatusSaverApp
import com.moorthy.statussaver.data.model.MediaType
import com.moorthy.statussaver.data.model.StatusItem
import kotlinx.coroutines.Job
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
 * Holds the status list for the statuses screen and the viewer opened from it.
 * [loadStatuses] is `StatusRepository.load`: null means no readable folder, an empty list
 * means empty folders. [statusStillExists] is `StatusRepository.stillExists`.
 *
 * WhatsApp adds a file whenever the user views a status, so the list is read again each
 * time the screen comes to the front ([reload]) and when the user pulls to refresh
 * ([refresh]). Nothing is loaded until the first [reload].
 */
class StatusesViewModel(
    private val loadStatuses: suspend () -> List<StatusItem>?,
    private val statusStillExists: suspend (StatusItem) -> Boolean,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow<StatusesUiState>(StatusesUiState.Loading)
    val uiState: StateFlow<StatusesUiState> = mutableUiState.asStateFlow()

    private val mutableIsRefreshing = MutableStateFlow(false)

    /** True while a load the user asked for by pulling down is running. */
    val isRefreshing: StateFlow<Boolean> = mutableIsRefreshing.asStateFlow()

    private val mutableShowVanishedNotice = MutableStateFlow(false)

    /**
     * True after the viewer closed because a status had expired; the list says so once
     * and then calls [vanishedNoticeShown].
     */
    val showVanishedNotice: StateFlow<Boolean> = mutableShowVanishedNotice.asStateFlow()

    private var loadJob: Job? = null

    /**
     * Reads the folders again without a visible indicator; the current list stays on
     * screen until the new one is ready. Ignored while a load is already running, since
     * that load started moments ago and will show the same files.
     */
    fun reload() {
        if (loadJob?.isActive == true) {
            return
        }
        loadJob = viewModelScope.launch {
            val items = loadStatuses()
            mutableUiState.value = when {
                items == null -> StatusesUiState.NeedsAccess
                items.isEmpty() -> StatusesUiState.Empty
                else -> StatusesUiState.Content(items)
            }
            mutableIsRefreshing.value = false
        }
    }

    /** Like [reload], but shows the pull-to-refresh indicator until the load finishes. */
    fun refresh() {
        mutableIsRefreshing.value = true
        reload()
    }

    /**
     * Called by the viewer when [item] could not be shown. Returns true when its file is
     * gone (WhatsApp expired it): the viewer then closes, the list tells the user, and the
     * list's reload on return drops the item. Returns false when the file is still there
     * but cannot be decoded, so the viewer stays and says so on that page.
     */
    suspend fun confirmVanished(item: StatusItem): Boolean {
        val vanished = !statusStillExists(item)
        if (vanished) {
            mutableShowVanishedNotice.value = true
        }
        return vanished
    }

    fun vanishedNoticeShown() {
        mutableShowVanishedNotice.value = false
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as StatusSaverApp
                StatusesViewModel(
                    loadStatuses = app.statusRepository::load,
                    statusStillExists = app.statusRepository::stillExists,
                )
            }
        }
    }
}
