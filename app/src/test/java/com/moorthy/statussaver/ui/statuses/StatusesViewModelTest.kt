package com.moorthy.statussaver.ui.statuses

import android.net.Uri
import com.moorthy.statussaver.data.model.MediaType
import com.moorthy.statussaver.data.model.SaveResult
import com.moorthy.statussaver.data.model.StatusItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The ViewModel gets `StatusRepository.load` as a function, so these tests hand it a fake
 * answer and check the screen state it turns into. Robolectric only provides `Uri`.
 * Loads start from `reload`, which the screen calls each time it resumes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StatusesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsLoadingUntilTheFoldersAreRead() = runTest(testDispatcher) {
        val answer = CompletableDeferred<List<StatusItem>?>()
        val viewModel = statusesViewModel { answer.await() }

        viewModel.reload()
        testScheduler.advanceUntilIdle()
        assertEquals(StatusesUiState.Loading, viewModel.uiState.value)

        answer.complete(listOf(item("a.jpg")))
        testScheduler.advanceUntilIdle()
        assertEquals(StatusesUiState.Content(listOf(item("a.jpg"))), viewModel.uiState.value)
    }

    @Test
    fun loadedStatusesAreShownInTheOrderGiven() = runTest(testDispatcher) {
        val items = listOf(item("new.jpg"), item("old.mp4", MediaType.VIDEO))
        val viewModel = statusesViewModel { items }

        viewModel.reload()
        testScheduler.advanceUntilIdle()

        assertEquals(StatusesUiState.Content(items), viewModel.uiState.value)
    }

    @Test
    fun readableButEmptyFoldersShowEmpty() = runTest(testDispatcher) {
        val viewModel = statusesViewModel { emptyList() }

        viewModel.reload()
        testScheduler.advanceUntilIdle()

        assertEquals(StatusesUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun noReadableFolderNeedsAccess() = runTest(testDispatcher) {
        val viewModel = statusesViewModel { null }

        viewModel.reload()
        testScheduler.advanceUntilIdle()

        assertEquals(StatusesUiState.NeedsAccess, viewModel.uiState.value)
    }

    @Test
    fun nothingIsLoadedBeforeTheFirstReload() = runTest(testDispatcher) {
        var loads = 0
        val viewModel = statusesViewModel {
            loads++
            emptyList()
        }

        testScheduler.advanceUntilIdle()

        assertEquals(0, loads)
        assertEquals(StatusesUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun reloadShowsStatusesAddedSinceTheLastLoad() = runTest(testDispatcher) {
        var folder = listOf(item("first.jpg"))
        val viewModel = statusesViewModel { folder }
        viewModel.reload()
        testScheduler.advanceUntilIdle()

        folder = listOf(item("second.jpg"), item("first.jpg"))
        viewModel.reload()
        testScheduler.advanceUntilIdle()

        assertEquals(StatusesUiState.Content(folder), viewModel.uiState.value)
    }

    @Test
    fun reloadWhileALoadIsRunningDoesNotStartAnother() = runTest(testDispatcher) {
        var loads = 0
        val answer = CompletableDeferred<List<StatusItem>?>()
        val viewModel = statusesViewModel {
            loads++
            answer.await()
        }

        viewModel.reload()
        testScheduler.advanceUntilIdle()
        viewModel.reload()
        viewModel.refresh()
        testScheduler.advanceUntilIdle()

        assertEquals(1, loads)
    }

    @Test
    fun quietReloadNeverShowsTheRefreshIndicator() = runTest(testDispatcher) {
        val answer = CompletableDeferred<List<StatusItem>?>()
        val viewModel = statusesViewModel { answer.await() }

        viewModel.reload()
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun refreshShowsTheIndicatorUntilTheLoadFinishes() = runTest(testDispatcher) {
        val answer = CompletableDeferred<List<StatusItem>?>()
        val viewModel = statusesViewModel { answer.await() }

        viewModel.refresh()
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.isRefreshing.value)

        answer.complete(emptyList())
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.isRefreshing.value)
    }

    // A resume reload can already be running when the user pulls; the indicator must
    // still turn off when that load finishes.
    @Test
    fun refreshDuringAQuietReloadEndsWithThatLoad() = runTest(testDispatcher) {
        val answer = CompletableDeferred<List<StatusItem>?>()
        val viewModel = statusesViewModel { answer.await() }
        viewModel.reload()
        testScheduler.advanceUntilIdle()

        viewModel.refresh()
        assertTrue(viewModel.isRefreshing.value)

        answer.complete(listOf(item("a.jpg")))
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.isRefreshing.value)
        assertEquals(StatusesUiState.Content(listOf(item("a.jpg"))), viewModel.uiState.value)
    }

    @Test
    fun eachTabGetsItsOwnTypeInNewestFirstOrder() {
        val content = StatusesUiState.Content(
            listOf(
                item("new.mp4", MediaType.VIDEO),
                item("new.jpg"),
                item("old.mp4", MediaType.VIDEO),
                item("old.jpg"),
            ),
        )

        assertEquals(listOf(item("new.jpg"), item("old.jpg")), content.itemsOf(MediaType.IMAGE))
        assertEquals(
            listOf(item("new.mp4", MediaType.VIDEO), item("old.mp4", MediaType.VIDEO)),
            content.itemsOf(MediaType.VIDEO),
        )
    }

    @Test
    fun aStatusThatIsGoneIsReportedOnceOnTheList() = runTest(testDispatcher) {
        val viewModel = statusesViewModel(
            statusStillExists = { false },
            loadStatuses = { listOf(item("a.jpg")) },
        )

        assertTrue(viewModel.confirmVanished(item("a.jpg")))
        assertTrue(viewModel.showVanishedNotice.value)

        viewModel.vanishedNoticeShown()
        assertFalse(viewModel.showVanishedNotice.value)
    }

    @Test
    fun aStatusThatIsStillThereIsNotReportedAsGone() = runTest(testDispatcher) {
        val checked = mutableListOf<StatusItem>()
        val viewModel = statusesViewModel(
            statusStillExists = { status ->
                checked += status
                true
            },
            loadStatuses = { listOf(item("a.jpg")) },
        )

        assertFalse(viewModel.confirmVanished(item("a.jpg")))

        assertFalse(viewModel.showVanishedNotice.value)
        assertEquals(listOf(item("a.jpg")), checked)
    }

    /** A ViewModel whose statuses all still exist; most tests are about loading. */
    @Test
    fun eachReloadAlsoRereadsTheSavedNames() = runTest(testDispatcher) {
        var refreshes = 0
        val viewModel = statusesViewModel(
            loadStatuses = { listOf(item("a.jpg")) },
            refreshSavedNames = { refreshes++ },
        )

        viewModel.reload()
        testScheduler.advanceUntilIdle()
        viewModel.reload()
        testScheduler.advanceUntilIdle()

        assertEquals(2, refreshes)
    }

    @Test
    fun saveReportsItsResultUntilShown() = runTest(testDispatcher) {
        val viewModel = statusesViewModel(saveStatus = { SaveResult.ALREADY_SAVED })

        viewModel.save(item("a.jpg"))
        testScheduler.advanceUntilIdle()
        assertEquals(SaveSummary(alreadySaved = 1), viewModel.saveNotice.value)

        viewModel.saveNoticeShown()
        assertNull(viewModel.saveNotice.value)
    }

    @Test
    fun aSecondTapWhileSavingIsIgnored() = runTest(testDispatcher) {
        val copyDone = CompletableDeferred<SaveResult>()
        val saved = mutableListOf<String>()
        val viewModel = statusesViewModel(
            saveStatus = { item ->
                saved += item.name
                copyDone.await()
            },
        )

        viewModel.save(item("a.mp4", MediaType.VIDEO))
        testScheduler.advanceUntilIdle()
        viewModel.save(item("a.mp4", MediaType.VIDEO))
        testScheduler.advanceUntilIdle()
        assertEquals(setOf("a.mp4"), viewModel.savingNames.value)
        assertNull(viewModel.saveNotice.value)

        copyDone.complete(SaveResult.SAVED)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("a.mp4"), saved)
        assertEquals(emptySet<String>(), viewModel.savingNames.value)
        assertEquals(SaveSummary(saved = 1), viewModel.saveNotice.value)
    }

    @Test
    fun anotherStatusCanBeSavedWhileOneIsSaving() = runTest(testDispatcher) {
        val firstDone = CompletableDeferred<SaveResult>()
        val saved = mutableListOf<String>()
        val viewModel = statusesViewModel(
            saveStatus = { item ->
                saved += item.name
                if (item.name == "a.jpg") firstDone.await() else SaveResult.SAVED
            },
        )

        viewModel.save(item("a.jpg"))
        viewModel.save(item("b.jpg"))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("a.jpg", "b.jpg"), saved)
        firstDone.complete(SaveResult.FAILED)
        testScheduler.advanceUntilIdle()
        assertEquals(SaveSummary(failed = 1), viewModel.saveNotice.value)
    }

    @Test
    fun longPressedItemsAreSelectedAndATapUnselects() = runTest(testDispatcher) {
        val viewModel = statusesViewModel { listOf(item("a.jpg"), item("b.jpg")) }

        viewModel.toggleSelection(item("a.jpg"))
        viewModel.toggleSelection(item("b.jpg"))
        assertEquals(setOf(item("a.jpg").uri, item("b.jpg").uri), viewModel.selectedUris.value)

        viewModel.toggleSelection(item("a.jpg"))
        assertEquals(setOf(item("b.jpg").uri), viewModel.selectedUris.value)

        viewModel.clearSelection()
        assertEquals(emptySet<Uri>(), viewModel.selectedUris.value)
    }

    @Test
    fun saveSelectedSavesTheSelectedItemsInListOrderAndCountsTheResults() = runTest(testDispatcher) {
        val list = listOf(item("a.jpg"), item("b.jpg"), item("c.mp4", MediaType.VIDEO), item("d.jpg"))
        val results = mapOf(
            "a.jpg" to SaveResult.SAVED,
            "c.mp4" to SaveResult.ALREADY_SAVED,
            "d.jpg" to SaveResult.FAILED,
        )
        val saved = mutableListOf<String>()
        val viewModel = statusesViewModel(
            saveStatus = { item ->
                saved += item.name
                results.getValue(item.name)
            },
            loadStatuses = { list },
        )
        viewModel.reload()
        testScheduler.advanceUntilIdle()
        // Selected out of order; b.jpg is not selected.
        viewModel.toggleSelection(item("d.jpg"))
        viewModel.toggleSelection(item("a.jpg"))
        viewModel.toggleSelection(item("c.mp4", MediaType.VIDEO))

        viewModel.saveSelected()
        assertEquals(emptySet<Uri>(), viewModel.selectedUris.value)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("a.jpg", "c.mp4", "d.jpg"), saved)
        assertEquals(SaveSummary(saved = 1, alreadySaved = 1, failed = 1), viewModel.saveNotice.value)
        assertEquals(emptySet<String>(), viewModel.savingNames.value)
    }

    @Test
    fun itemsGoneAfterAReloadLeaveTheSelection() = runTest(testDispatcher) {
        var list = listOf(item("a.jpg"), item("b.jpg"))
        val viewModel = statusesViewModel { list }
        viewModel.reload()
        testScheduler.advanceUntilIdle()
        viewModel.toggleSelection(item("a.jpg"))
        viewModel.toggleSelection(item("b.jpg"))

        // WhatsApp deleted a.jpg.
        list = listOf(item("b.jpg"))
        viewModel.reload()
        testScheduler.advanceUntilIdle()

        assertEquals(setOf(item("b.jpg").uri), viewModel.selectedUris.value)
    }

    @Test
    fun eachSelectedItemShowsAsSavingUntilItsOwnCopyIsDone() = runTest(testDispatcher) {
        val secondDone = CompletableDeferred<SaveResult>()
        val viewModel = statusesViewModel(
            saveStatus = { item -> if (item.name == "b.jpg") secondDone.await() else SaveResult.SAVED },
            loadStatuses = { listOf(item("a.jpg"), item("b.jpg")) },
        )
        viewModel.reload()
        testScheduler.advanceUntilIdle()
        viewModel.toggleSelection(item("a.jpg"))
        viewModel.toggleSelection(item("b.jpg"))

        viewModel.saveSelected()
        assertEquals(setOf("a.jpg", "b.jpg"), viewModel.savingNames.value)
        testScheduler.advanceUntilIdle()
        // a.jpg is done, b.jpg is still being copied: one notice comes only at the end.
        assertEquals(setOf("b.jpg"), viewModel.savingNames.value)
        assertNull(viewModel.saveNotice.value)

        secondDone.complete(SaveResult.SAVED)
        testScheduler.advanceUntilIdle()
        assertEquals(SaveSummary(saved = 2), viewModel.saveNotice.value)
    }

    /** [loadStatuses] comes last, so most tests pass it as a trailing lambda. */
    private fun statusesViewModel(
        statusStillExists: suspend (StatusItem) -> Boolean = { true },
        refreshSavedNames: suspend () -> Unit = {},
        saveStatus: suspend (StatusItem) -> SaveResult = { SaveResult.SAVED },
        loadStatuses: suspend () -> List<StatusItem>? = { emptyList() },
    ) = StatusesViewModel(
        loadStatuses = loadStatuses,
        statusStillExists = statusStillExists,
        savedNames = MutableStateFlow(emptySet()),
        refreshSavedNames = refreshSavedNames,
        saveStatus = saveStatus,
    )

    private fun item(name: String, type: MediaType = MediaType.IMAGE) = StatusItem(
        uri = Uri.parse("content://test/$name"),
        name = name,
        type = type,
        mimeType = if (type == MediaType.IMAGE) "image/jpeg" else "video/mp4",
        lastModifiedMillis = 0,
        sizeBytes = 0,
    )
}
