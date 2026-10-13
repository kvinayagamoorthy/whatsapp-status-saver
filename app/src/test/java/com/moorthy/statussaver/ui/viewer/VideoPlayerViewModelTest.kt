package com.moorthy.statussaver.ui.viewer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.media3.exoplayer.ExoPlayer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Runs the ViewModel with a real ExoPlayer. The files do not exist, so nothing actually
 * plays; the tests check what the player was asked to do.
 */
@RunWith(RobolectricTestRunner::class)
class VideoPlayerViewModelTest {

    private lateinit var player: ExoPlayer
    private val viewModelStore = ViewModelStore()
    private lateinit var viewModel: VideoPlayerViewModel

    @Before
    fun setUp() {
        player = ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                VideoPlayerViewModel(player) as T
        }
        viewModel = ViewModelProvider(viewModelStore, factory)[VideoPlayerViewModel::class.java]
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
    }

    @Test
    fun showingAVideoLoadsItAndStartsPlaying() {
        viewModel.show(FIRST)

        assertEquals(FIRST, player.currentMediaItem?.localConfiguration?.uri)
        assertTrue(player.playWhenReady)
    }

    @Test
    fun showingTheSameVideoAgainCarriesOnWhereItWas() {
        viewModel.show(FIRST)
        player.seekTo(1_500)

        viewModel.show(FIRST)

        assertEquals(1_500, player.currentPosition)
    }

    @Test
    fun showingAnotherVideoPlaysThatOneFromTheStart() {
        viewModel.show(FIRST)
        player.seekTo(1_500)

        viewModel.show(SECOND)

        assertEquals(SECOND, player.currentMediaItem?.localConfiguration?.uri)
        assertEquals(1, player.mediaItemCount)
        assertEquals(0, player.currentPosition)
        assertTrue(player.playWhenReady)
    }

    @Test
    fun pauseStopsPlaying() {
        viewModel.show(FIRST)

        viewModel.pause()

        assertFalse(player.playWhenReady)
    }

    @Test
    fun leavingTheViewerReleasesThePlayer() {
        viewModel.show(FIRST)

        viewModelStore.clear()

        assertTrue(player.isReleased)
    }

    private companion object {
        val FIRST: Uri = Uri.parse("content://test/first.mp4")
        val SECOND: Uri = Uri.parse("content://test/second.mp4")
    }
}
