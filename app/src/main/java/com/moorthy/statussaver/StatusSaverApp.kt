package com.moorthy.statussaver

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.moorthy.statussaver.data.SavedMediaRepository
import com.moorthy.statussaver.data.StatusRepository
import com.moorthy.statussaver.data.StatusSourceStore

private val Context.statusSourcesDataStore by preferencesDataStore(name = "status_sources")

/** Creates the app's repositories once and hands them to ViewModels (no DI framework, D-005). */
class StatusSaverApp : Application(), SingletonImageLoader.Factory {

    val statusSourceStore: StatusSourceStore by lazy {
        StatusSourceStore(statusSourcesDataStore, contentResolver)
    }

    val statusRepository: StatusRepository by lazy {
        StatusRepository(statusSourceStore, contentResolver)
    }

    /** One instance, so the list and the viewer see the same saved names. */
    val savedMediaRepository: SavedMediaRepository by lazy {
        SavedMediaRepository(this)
    }

    /**
     * Coil's shared loader for grid thumbnails. Video tiles show a frame decoded from the
     * file itself, so videos need the frame decoder; there is no network fetcher (I5).
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
}
