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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
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
 * the viewer is mostly used to decide what to keep.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPage(item: StatusItem, player: Player) {
    KeepScreenOnWhilePlaying(player)
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
