package com.moorthy.statussaver.ui.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.material3.buttons.MuteButton
import androidx.media3.ui.compose.material3.buttons.PlayPauseButton
import androidx.media3.ui.compose.material3.indicator.DurationText
import androidx.media3.ui.compose.material3.indicator.PositionText
import androidx.media3.ui.compose.material3.indicator.ProgressSlider
import coil3.compose.AsyncImage
import com.moorthy.statussaver.data.model.StatusItem

/**
 * The video on the current page (FR3): the picture, and a bar with play/pause, position,
 * seek bar, duration and mute. The controls stay visible; status videos are short and
 * the viewer is mostly used to decide what to keep. [onError] is called when the video
 * cannot be played, for example because its file was deleted.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPage(item: StatusItem, description: String, player: Player, onError: () -> Unit) {
    KeepScreenOnWhilePlaying(player)
    val currentOnError by rememberUpdatedState(onError)
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                currentOnError()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        ContentFrame(
            player = player,
            modifier = Modifier.fillMaxSize(),
            // A TextureView moves with the page while swiping; a SurfaceView does not
            // follow view movement before Android 10 (D-025).
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            contentScale = ContentScale.Fit,
            // Until the first frame is decoded, the same frame the grid showed.
            shutter = {
                AsyncImage(
                    model = item.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            },
        )
        // The picture is an Android view, which TalkBack finds but cannot describe, and
        // semantics on ContentFrame do not reach it. This empty layer on top says what the
        // video shows; it has no touch handling, so taps still reach the picture. The
        // controls below keep their own labels.
        Box(
            modifier = Modifier
                .matchParentSize()
                .semantics(mergeDescendants = true) { contentDescription = description },
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayPauseButton(player)
                PositionText(player, modifier = Modifier.padding(horizontal = 4.dp))
                ProgressSlider(player, modifier = Modifier.weight(1f))
                DurationText(player, modifier = Modifier.padding(horizontal = 4.dp))
                MuteButton(player)
            }
        }
    }
}

/**
 * Keeps the screen on only while the video plays. This replaces Media3's wake lock,
 * whose permission the app removes (D-011).
 */
@Composable
private fun KeepScreenOnWhilePlaying(player: Player) {
    val view = LocalView.current
    DisposableEffect(player, view) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                view.keepScreenOn = isPlaying
            }
        }
        view.keepScreenOn = player.isPlaying
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            view.keepScreenOn = false
        }
    }
}
