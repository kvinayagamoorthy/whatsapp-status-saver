package com.moorthy.statussaver.ui.viewer

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
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
                    ViewerPager(items = items, startIndex = startIndex)
                }
            }
            StatusesUiState.Empty, StatusesUiState.NeedsAccess -> LaunchedEffect(Unit) {
                onClose()
            }
        }
    }
}

@Composable
private fun ViewerPager(items: List<StatusItem>, startIndex: Int) {
    // The pager state is saved, so the current page survives rotation.
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, items.lastIndex)) {
        items.size
    }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        key = { page -> items[page].uri.toString() },
    ) { page ->
        ViewerPage(items[page])
    }
}

@Composable
private fun ViewerPage(item: StatusItem) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Whole image fitted to the screen; zoom arrives in T3.2.
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        // A video shows its first frame until the player arrives in T3.3.
        if (item.type == MediaType.VIDEO) {
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
}
