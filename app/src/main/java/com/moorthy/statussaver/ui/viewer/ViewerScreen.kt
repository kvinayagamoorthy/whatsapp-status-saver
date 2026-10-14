package com.moorthy.statussaver.ui.viewer

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
import com.moorthy.statussaver.ui.rememberSaveWithPermission
import com.moorthy.statussaver.ui.statuses.SaveNoticeEffect
import com.moorthy.statussaver.ui.statuses.StatusesUiState
import com.moorthy.statussaver.ui.statuses.StatusesViewModel
import kotlinx.coroutines.launch

/**
 * Full-screen viewer (FR3): swipes through the items of one tab, starting at the one the
 * user tapped.
 *
 * [viewModel] is the status list's own ViewModel, so [startIndex] points into the same
 * list the user tapped in. That list does not reload while the viewer is open, because
 * the list reloads only when its own screen resumes (D-021, D-023). [onClose] returns to
 * the list, which then shows why there is nothing to view.
 *
 * A status that fails to load is checked against its folder: if WhatsApp has deleted it
 * meanwhile, the viewer closes and the list says it is no longer available (T3.4).
 *
 * The Save button at the top keeps the current status in the gallery (FR4) and shows a
 * check mark once it is there (FR5).
 */
@Composable
fun ViewerScreen(
    tab: MediaType,
    startIndex: Int,
    viewModel: StatusesViewModel,
    onClose: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedNames by viewModel.savedNames.collectAsStateWithLifecycle()
    val savingNames by viewModel.savingNames.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    SaveNoticeEffect(viewModel, snackbarHostState)
    val needsPermissionMessage = stringResource(R.string.save_needs_permission)
    val saveWithPermission = rememberSaveWithPermission(
        onSave = viewModel::save,
        onDenied = {
            snackbarScope.launch {
                snackbarHostState.showSnackbar(needsPermissionMessage)
            }
        },
    )

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
                    ViewerPager(
                        tab = tab,
                        items = items,
                        startIndex = startIndex,
                        savedNames = savedNames,
                        savingNames = savingNames,
                        onSave = saveWithPermission,
                        onCheckVanished = viewModel::confirmVanished,
                        onClose = onClose,
                    )
                }
            }
            StatusesUiState.Empty, StatusesUiState.NeedsAccess -> LaunchedEffect(Unit) {
                onClose()
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                // Above the video controls, which sit at the bottom of video pages.
                .padding(bottom = if (tab == MediaType.VIDEO) VIDEO_CONTROLS_HEIGHT else 0.dp),
        )
    }
}

/** Room left below the snackbar for the video page's control bar. */
private val VIDEO_CONTROLS_HEIGHT = 64.dp

@Composable
private fun ViewerPager(
    tab: MediaType,
    items: List<StatusItem>,
    startIndex: Int,
    savedNames: Set<String>,
    savingNames: Set<String>,
    onSave: (StatusItem) -> Unit,
    onCheckVanished: suspend (StatusItem) -> Boolean,
    onClose: () -> Unit,
) {
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
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { page -> items[page].uri.toString() },
        ) { page ->
            val item = items[page]
            // Saved, so a page that failed before a rotation is checked again after it.
            var failed by rememberSaveable(item.uri) { mutableStateOf(false) }
            val onError = { failed = true }
            when {
                failed -> CannotShowMessage()
                item.type == MediaType.IMAGE -> ZoomableImage(item = item, onError = onError)
                videoPlayer != null && page == pagerState.settledPage ->
                    VideoPage(item = item, player = videoPlayer.player, onError = onError)
                else -> VideoPreview(item)
            }
            // Only the page the user is looking at may close the viewer, not a neighbour
            // that is drawn while swiping.
            if (failed && page == pagerState.settledPage) {
                LaunchedEffect(item.uri) {
                    if (onCheckVanished(item)) {
                        onClose()
                    }
                }
            }
        }
        // Acts on the page in the middle of the screen, also while swiping.
        val currentItem = items[pagerState.currentPage]
        SaveButton(
            isSaved = currentItem.name in savedNames,
            isSaving = currentItem.name in savingNames,
            onClick = { onSave(currentItem) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp),
        )
    }
}

/**
 * Saves the current status, or shows a check mark when it is already saved. Tapping the
 * check mark saves again, which only says "Already saved" (I4). On a dark scrim, since
 * statuses can have any colours behind it.
 */
@Composable
private fun SaveButton(
    isSaved: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        enabled = !isSaving,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.45f),
            contentColor = Color.White,
            disabledContainerColor = Color.Black.copy(alpha = 0.45f),
            disabledContentColor = Color.White,
        ),
        modifier = modifier,
    ) {
        when {
            isSaving -> {
                val savingDescription = stringResource(R.string.viewer_saving)
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(20.dp)
                        .semantics { contentDescription = savingDescription },
                )
            }
            isSaved -> Icon(
                painter = painterResource(R.drawable.ic_saved),
                contentDescription = stringResource(R.string.viewer_saved),
            )
            else -> Icon(
                painter = painterResource(R.drawable.ic_save),
                contentDescription = stringResource(R.string.viewer_save),
            )
        }
    }
}

/** A status whose file is still there but cannot be decoded. */
@Composable
private fun CannotShowMessage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.viewer_cannot_show),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
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
private fun ZoomableImage(item: StatusItem, onError: () -> Unit) {
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
        onError = { onError() },
        modifier = Modifier
            .fillMaxSize()
            .zoomable(zoomState),
    )
}
