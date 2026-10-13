package com.moorthy.statussaver.ui.viewer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Owns the one video player of the viewer (FR3, design.md §6). It lives as long as the
 * viewer is on the back stack, so a rotation keeps the video playing where it was, and
 * it is released when the user leaves the viewer, so no sound or decoder outlives it.
 *
 * Never call `setWakeMode` on the player: the wake-lock permission is removed (D-011).
 * The screen stays on while playing through the view instead.
 */
class VideoPlayerViewModel(val player: Player) : ViewModel() {

    private var shownUri: Uri? = null

    /**
     * Plays [uri] from the start, unless it is already the video being shown, in which
     * case playback carries on (the page was recomposed, or the screen rotated).
     */
    fun show(uri: Uri) {
        if (uri == shownUri) {
            return
        }
        shownUri = uri
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true
    }

    /** The user left the screen (Home, another app): the video must not keep playing. */
    fun pause() {
        player.pause()
    }

    override fun onCleared() {
        player.release()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                val player = ExoPlayer.Builder(app)
                    // Pauses for calls and other apps' audio, and when headphones are
                    // unplugged, instead of playing over them or out of the speaker.
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                            .build(),
                        true,
                    )
                    .setHandleAudioBecomingNoisy(true)
                    .build()
                VideoPlayerViewModel(player)
            }
        }
    }
}
