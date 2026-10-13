package com.moorthy.statussaver.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.moorthy.statussaver.StatusSaverApp
import com.moorthy.statussaver.data.StatusSourceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Decides once per app start which screen comes first: the status list when at least
 * one statuses folder is still readable, onboarding otherwise. Kept in a ViewModel so a
 * rotation does not decide again under a restored back stack.
 */
class StartViewModel(
    private val statusSourceStore: StatusSourceStore,
) : ViewModel() {

    private val mutableStartRoute = MutableStateFlow<String?>(null)

    /** The first route, or null while the stored grants are still being read. */
    val startRoute: StateFlow<String?> = mutableStartRoute.asStateFlow()

    init {
        viewModelScope.launch {
            val hasReadableSource = statusSourceStore.validSources().isNotEmpty()
            mutableStartRoute.value = if (hasReadableSource) Routes.STATUSES else Routes.ONBOARDING
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as StatusSaverApp
                StartViewModel(app.statusSourceStore)
            }
        }
    }
}
