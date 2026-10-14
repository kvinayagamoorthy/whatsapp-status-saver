package com.moorthy.statussaver.ui.statuses

import android.content.res.Resources
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import com.moorthy.statussaver.ui.rememberStoragePermissionGate
import kotlinx.coroutines.launch

/**
 * Status list (FR2): Images and Videos tabs over a grid of thumbnails, newest first.
 * [onNeedsAccess] is called when no granted folder is readable any more, so the user can
 * grant one again (FR8). [onOpenItem] opens the viewer at an item of a tab's list (FR3).
 *
 * A long press starts selection mode (FR4): taps then select or unselect tiles, the top
 * bar counts them and offers "Save selected", and Back leaves the mode.
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
    // A save started in the viewer can end after the user went back to the list.
    SaveNoticeEffect(viewModel, snackbarHostState)
    val savedNames by viewModel.savedNames.collectAsStateWithLifecycle()
    val selectedUris by viewModel.selectedUris.collectAsStateWithLifecycle()
    val isSelecting = selectedUris.isNotEmpty()
    BackHandler(enabled = isSelecting) {
        viewModel.clearSelection()
    }
    val needsPermissionMessage = stringResource(R.string.save_needs_permission)
    val withStoragePermission = rememberStoragePermissionGate(
        onDenied = {
            snackbarScope.launch {
                snackbarHostState.showSnackbar(needsPermissionMessage)
            }
        },
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (isSelecting) {
                SelectionTopBar(
                    count = selectedUris.size,
                    onClear = viewModel::clearSelection,
                    onSave = { withStoragePermission { viewModel.saveSelected() } },
                )
            } else {
                TopAppBar(title = { Text(stringResource(R.string.app_name)) })
            }
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
                                savedNames = savedNames,
                                selectedUris = selectedUris,
                                onTap = { index, item ->
                                    if (isSelecting) {
                                        viewModel.toggleSelection(item)
                                    } else {
                                        onOpenItem(selectedTab, index)
                                    }
                                },
                                onLongPress = viewModel::toggleSelection,
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

/**
 * Tells the user how a save ended (FR4), on whichever screen is in front: the viewer
 * where the user tapped Save, or the list if they went back before the copy was done.
 */
@Composable
fun SaveNoticeEffect(viewModel: StatusesViewModel, snackbarHostState: SnackbarHostState) {
    val saveNotice by viewModel.saveNotice.collectAsStateWithLifecycle()
    val snackbarScope = rememberCoroutineScope()
    val resources = LocalResources.current
    LaunchedEffect(saveNotice) {
        val summary = saveNotice ?: return@LaunchedEffect
        // As with the vanished notice: marked first, shown in the screen's own scope.
        viewModel.saveNoticeShown()
        snackbarScope.launch {
            // A newer result replaces the one on screen instead of queueing behind it.
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(saveMessage(resources, summary))
        }
    }
}

/**
 * The message for a save action: for one status, what happened to it; for several, one
 * sentence per outcome that occurred, e.g. "3 statuses saved to your gallery. 1 was
 * already saved."
 */
private fun saveMessage(resources: Resources, summary: SaveSummary): String {
    if (summary.total == 1) {
        return resources.getString(
            when {
                summary.saved == 1 -> R.string.save_done
                summary.alreadySaved == 1 -> R.string.save_already
                else -> R.string.save_failed
            },
        )
    }
    val parts = listOf(
        R.plurals.save_summary_saved to summary.saved,
        R.plurals.save_summary_already to summary.alreadySaved,
        R.plurals.save_summary_failed to summary.failed,
    )
    return parts
        .filter { (_, count) -> count > 0 }
        .joinToString(" ") { (pluralsRes, count) -> resources.getQuantityString(pluralsRes, count, count) }
}

/** Top bar in selection mode: leave the mode, how many are selected, save them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(count: Int, onClear: () -> Unit, onSave: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClear) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.statuses_clear_selection),
                )
            }
        },
        title = { Text(pluralStringResource(R.plurals.statuses_selected_count, count, count)) },
        actions = {
            IconButton(onClick = onSave) {
                Icon(
                    painter = painterResource(R.drawable.ic_save),
                    contentDescription = stringResource(R.string.statuses_save_selected),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}

@Composable
private fun StatusGrid(
    items: List<StatusItem>,
    savedNames: Set<String>,
    selectedUris: Set<Uri>,
    onTap: (index: Int, item: StatusItem) -> Unit,
    onLongPress: (StatusItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp),
    ) {
        // The URI, not the name, is the key: both apps' folders could hold the same name.
        itemsIndexed(items, key = { _, item -> item.uri.toString() }) { index, item ->
            StatusTile(
                item = item,
                isSaved = item.name in savedNames,
                isSelecting = selectedUris.isNotEmpty(),
                isSelected = item.uri in selectedUris,
                onTap = { onTap(index, item) },
                onLongPress = { onLongPress(item) },
            )
        }
    }
}

@Composable
private fun StatusTile(
    item: StatusItem,
    isSaved: Boolean,
    isSelecting: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val selectionState = if (isSelected) {
        stringResource(R.string.statuses_selected)
    } else {
        stringResource(R.string.statuses_not_selected)
    }
    Box(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary)
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                onClickLabel = if (isSelecting) {
                    stringResource(R.string.statuses_select)
                } else {
                    stringResource(R.string.statuses_open)
                },
                onClick = onTap,
                onLongClickLabel = stringResource(R.string.statuses_select),
                onLongClick = onLongPress,
            )
            .then(
                if (isSelecting) {
                    Modifier.semantics {
                        selected = isSelected
                        stateDescription = selectionState
                    }
                } else {
                    Modifier
                },
            ),
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
        if (isSaved) {
            SavedBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            )
        }
        if (isSelecting) {
            SelectionMark(
                isSelected = isSelected,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
            )
        }
    }
}

/** In selection mode: an empty ring on unselected tiles, a filled check on selected ones. */
@Composable
private fun SelectionMark(isSelected: Boolean, modifier: Modifier = Modifier) {
    if (isSelected) {
        Icon(
            painter = painterResource(R.drawable.ic_saved),
            // The tile's own state description says it is selected.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .padding(3.dp),
        )
    } else {
        Box(
            modifier = modifier
                .size(24.dp)
                .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                .border(2.dp, Color.White, CircleShape),
        )
    }
}

/** Check mark on a tile whose status is already in the gallery (FR5). */
@Composable
private fun SavedBadge(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_saved),
        contentDescription = stringResource(R.string.statuses_saved_badge),
        tint = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .size(22.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .padding(3.dp),
    )
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
