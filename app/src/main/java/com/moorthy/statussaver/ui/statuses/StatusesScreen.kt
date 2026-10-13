package com.moorthy.statussaver.ui.statuses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.moorthy.statussaver.R
import com.moorthy.statussaver.data.model.MediaType
import com.moorthy.statussaver.data.model.StatusItem
import kotlinx.coroutines.launch

/**
 * Status list (FR2): Images and Videos tabs over a grid of thumbnails, newest first.
 * [onNeedsAccess] is called when no granted folder is readable any more, so the user can
 * grant one again (FR8). [onOpenItem] opens the viewer at an item of a tab's list (FR3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusesScreen(
    onNeedsAccess: () -> Unit,
    onOpenItem: (tab: MediaType, index: Int) -> Unit,
    viewModel: StatusesViewModel = viewModel(factory = StatusesViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    // Also fires when the screen first appears, so this is the first load too.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.reload()
    }
    if (uiState == StatusesUiState.NeedsAccess) {
        LaunchedEffect(Unit) {
            onNeedsAccess()
        }
    }
    // The tab is a MediaType, so it survives rotation and later names the viewer's tab.
    var selectedTab by rememberSaveable { mutableStateOf(MediaType.IMAGE) }
    val showVanishedNotice by viewModel.showVanishedNotice.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val vanishedMessage = stringResource(R.string.statuses_vanished)
    LaunchedEffect(showVanishedNotice) {
        if (showVanishedNotice) {
            // Marked first, so the notice is not shown a second time after a rotation.
            // That ends this effect, so the snackbar runs in the screen's own scope.
            viewModel.vanishedNoticeShown()
            snackbarScope.launch {
                snackbarHostState.showSnackbar(vanishedMessage)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                for (tab in MediaType.entries) {
                    Tab(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        text = { Text(stringResource(tab.titleRes)) },
                    )
                }
            }
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val state = uiState) {
                    StatusesUiState.Loading -> CenteredBox {
                        CircularProgressIndicator()
                    }
                    is StatusesUiState.Content -> {
                        val tabItems = remember(state, selectedTab) { state.itemsOf(selectedTab) }
                        if (tabItems.isEmpty()) {
                            CenteredMessage(stringResource(selectedTab.noItemsRes))
                        } else {
                            StatusGrid(
                                items = tabItems,
                                onOpenItem = { index -> onOpenItem(selectedTab, index) },
                            )
                        }
                    }
                    StatusesUiState.Empty -> CenteredMessage(stringResource(R.string.statuses_empty))
                    // The screen is about to be replaced by onboarding.
                    StatusesUiState.NeedsAccess -> Unit
                }
            }
        }
    }
}

@Composable
private fun StatusGrid(items: List<StatusItem>, onOpenItem: (index: Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp),
    ) {
        // The URI, not the name, is the key: both apps' folders could hold the same name.
        itemsIndexed(items, key = { _, item -> item.uri.toString() }) { index, item ->
            StatusTile(item = item, onOpen = { onOpenItem(index) })
        }
    }
}

@Composable
private fun StatusTile(item: StatusItem, onOpen: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClickLabel = stringResource(R.string.statuses_open), onClick = onOpen),
    ) {
        // Coil decodes off the main thread at tile size and caches the result (NFR3).
        // Tiles get their content descriptions in the accessibility pass (T5.4).
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (item.type == MediaType.VIDEO) {
            Icon(
                painter = painterResource(R.drawable.ic_play_badge),
                contentDescription = stringResource(R.string.statuses_video_badge),
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    CenteredBox {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

/** Scrollable even when its content fits, so pull-to-refresh also works on messages. */
@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

private val MediaType.titleRes: Int
    get() = when (this) {
        MediaType.IMAGE -> R.string.statuses_tab_images
        MediaType.VIDEO -> R.string.statuses_tab_videos
    }

private val MediaType.noItemsRes: Int
    get() = when (this) {
        MediaType.IMAGE -> R.string.statuses_no_images
        MediaType.VIDEO -> R.string.statuses_no_videos
    }
