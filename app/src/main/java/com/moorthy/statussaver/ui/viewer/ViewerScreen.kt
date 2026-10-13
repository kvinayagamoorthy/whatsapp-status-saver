package com.moorthy.statussaver.ui.viewer

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size as CoilSize
import com.moorthy.statussaver.R
import com.moorthy.statussaver.data.model.MediaType
import com.moorthy.statussaver.data.model.StatusItem
import com.moorthy.statussaver.ui.statuses.StatusesUiState
import com.moorthy.statussaver.ui.statuses.StatusesViewModel

/**
 * Full-screen viewer (FR3): swipes through the items of one tab, starting at the one the
 * user tapped.
 *
 * [viewModel] is the status list's own ViewModel, so [startIndex] points into the same
 * list the user tapped in. That list does not reload while the viewer is open, because
 * the list reloads only when its own screen resumes (D-021, D-023). [onClose] returns to
 * the list, which then shows why there is nothing to view.
 */
@Composable
fun ViewerScreen(
    tab: MediaType,
    startIndex: Int,
    viewModel: StatusesViewModel,
    onClose: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (val state = uiState) {
            // Only after the app was restarted in the viewer: the list was never loaded.
            StatusesUiState.Loading -> {
                LaunchedEffect(Unit) {
                    viewModel.reload()
                }
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is StatusesUiState.Content -> {
                val items = remember(state, tab) { state.itemsOf(tab) }
                if (items.isEmpty()) {
                    LaunchedEffect(Unit) {
                        onClose()
                    }
                } else {
                    ViewerPager(tab = tab, items = items, startIndex = startIndex)
                }
            }
            StatusesUiState.Empty, StatusesUiState.NeedsAccess -> LaunchedEffect(Unit) {
                onClose()
            }
        }
    }
}

@Composable
private fun ViewerPager(tab: MediaType, items: List<StatusItem>, startIndex: Int) {
    // The pager state is saved, so the current page survives rotation.
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, items.lastIndex)) {
        items.size
    }
    // The tab never changes while the viewer is open, so only the Videos tab ever makes
    // a player, and it has exactly one (T3.3).
    val videoPlayer = if (tab == MediaType.VIDEO) {
        viewModel<VideoPlayerViewModel>(factory = VideoPlayerViewModel.Factory)
    } else {
        null
    }
    if (videoPlayer != null) {
        // The video plays once its page has settled, not while it slides past.
        LaunchedEffect(videoPlayer, pagerState.settledPage) {
            items.getOrNull(pagerState.settledPage)?.let { item -> videoPlayer.show(item.uri) }
        }
        val activity = LocalActivity.current
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
            // A rotation also stops the screen briefly; the video keeps playing then.
            if (activity?.isChangingConfigurations != true) {
                videoPlayer.pause()
            }
        }
    }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        key = { page -> items[page].uri.toString() },
    ) { page ->
        val item = items[page]
        when {
            item.type == MediaType.IMAGE -> ZoomableImage(item)
            videoPlayer != null && page == pagerState.settledPage ->
                VideoPage(item = item, player = videoPlayer.player)
            else -> VideoPreview(item)
        }
    }
}

/** A video page that is not playing: its first frame with a play badge. */
@Composable
private fun VideoPreview(item: StatusItem) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        Icon(
            painter = painterResource(R.drawable.ic_play_badge),
            contentDescription = stringResource(R.string.statuses_video_badge),
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                .padding(12.dp),
        )
    }
}

/**
 * The whole image fitted to the screen, zoomable (T3.2). Its zoom is forgotten when the
 * page leaves the pager or the screen rotates, so each visit starts fitted.
 */
@Composable
private fun ZoomableImage(item: StatusItem) {
    val context = LocalContext.current
    val zoomState = remember(item.uri) { ZoomState() }
    // Decoded at full resolution rather than screen size, so zooming in shows real
    // detail. Status images are at most a few megapixels.
    val request = remember(item.uri) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(CoilSize.ORIGINAL)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        onSuccess = { success -> zoomState.setContentSize(success.painter.intrinsicSize) },
        modifier = Modifier
            .fillMaxSize()
            .zoomable(zoomState),
    )
}
