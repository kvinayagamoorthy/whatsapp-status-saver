package com.moorthy.statussaver

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.moorthy.statussaver.data.StatusSourceStore

private val Context.statusSourcesDataStore by preferencesDataStore(name = "status_sources")

/** Creates the app's repositories once and hands them to ViewModels (no DI framework, D-005). */
class StatusSaverApp : Application() {

    val statusSourceStore: StatusSourceStore by lazy {
        StatusSourceStore(statusSourcesDataStore, contentResolver)
    }
}
