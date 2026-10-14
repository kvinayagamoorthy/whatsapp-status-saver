package com.moorthy.statussaver.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.moorthy.statussaver.data.model.ThemeMode
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()
    private lateinit var file: File

    @Before
    fun setUp() {
        file = File(temporaryFolder.root, "settings.preferences_pb")
    }

    @After
    fun tearDown() {
        for (scope in scopes) {
            scope.cancel()
        }
    }

    @Test
    fun followsTheSystemUntilTheUserChooses() = runBlocking {
        assertEquals(ThemeMode.SYSTEM, SettingsStore(dataStore()).themeMode.first())
    }

    @Test
    fun choiceIsKeptAcrossRestarts() = runBlocking {
        val firstRun = dataStore()
        SettingsStore(firstRun).setThemeMode(ThemeMode.DARK)
        closeAll()

        // A new DataStore on the same file, as after the app was restarted.
        assertEquals(ThemeMode.DARK, SettingsStore(dataStore()).themeMode.first())
    }

    @Test
    fun aChangeReachesThoseAlreadyWatching() = runBlocking {
        val store = SettingsStore(dataStore())
        val firstSeen = CompletableDeferred<Unit>()
        val seen = async(Dispatchers.Default) {
            // Fails rather than waiting forever if the change never arrives.
            withTimeout(WAIT_MILLIS) {
                store.themeMode
                    .onEach { firstSeen.complete(Unit) }
                    .take(2)
                    .toList()
            }
        }
        // The change comes after the watcher has seen the first value.
        firstSeen.await()

        store.setThemeMode(ThemeMode.LIGHT)

        assertEquals(listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT), seen.await())
    }

    @Test
    fun unknownStoredValueFallsBackToSystem() = runBlocking {
        val dataStore = dataStore()
        // For example written by a later version with another mode.
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("theme_mode")] = "SEPIA"
        }

        assertEquals(ThemeMode.SYSTEM, SettingsStore(dataStore).themeMode.first())
    }

    private fun dataStore(): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scopes += scope
        return PreferenceDataStoreFactory.create(scope = scope) { file }
    }

    /** DataStore allows one instance per file; cancelling its scope closes it. */
    private suspend fun closeAll() {
        for (scope in scopes) {
            scope.coroutineContext.job.cancelAndJoin()
        }
        scopes.clear()
    }

    private companion object {
        const val WAIT_MILLIS = 5_000L
    }
}
