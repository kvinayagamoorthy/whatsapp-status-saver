package com.moorthy.statussaver.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.moorthy.statussaver.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The user's choices on the settings screen, kept across restarts (FR7). */
class SettingsStore(
    private val dataStore: DataStore<Preferences>,
) {

    /**
     * The chosen theme mode, and every later change to it. [ThemeMode.SYSTEM] until the
     * user picks another, and also for a stored value this version does not know.
     */
    val themeMode: Flow<ThemeMode> = dataStore.data
        .map { preferences ->
            val storedName = preferences[THEME_MODE_KEY]
            ThemeMode.entries.firstOrNull { mode -> mode.name == storedName } ?: ThemeMode.SYSTEM
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    private companion object {
        // Stored by enum name, so renaming a ThemeMode entry resets the choice to System.
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
}
