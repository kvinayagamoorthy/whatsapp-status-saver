package com.moorthy.statussaver.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.moorthy.statussaver.StatusSaverApp
import com.moorthy.statussaver.data.SettingsStore
import com.moorthy.statussaver.data.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The settings screen's state. A new theme mode is only stored here; `MainActivity`
 * follows the stored choice, so the whole app changes at once, without a restart (FR7).
 */
class SettingsViewModel(
    private val settingsStore: SettingsStore,
) : ViewModel() {

    /** The chosen theme mode, or null while it is still being read. */
    val themeMode: StateFlow<ThemeMode?> = settingsStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue = null)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsStore.setThemeMode(mode)
        }
    }

    companion object {
        // Keeps the stored value flowing through a rotation.
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as StatusSaverApp
                SettingsViewModel(app.settingsStore)
            }
        }
    }
}
