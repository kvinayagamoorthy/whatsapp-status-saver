package com.moorthy.statussaver.ui.statuses

import android.net.Uri
import com.moorthy.statussaver.data.model.MediaType
import com.moorthy.statussaver.data.model.StatusItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The ViewModel gets `StatusRepository.load` as a function, so these tests hand it a fake
 * answer and check the screen state it turns into. Robolectric only provides `Uri`.
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
        val viewModel = StatusesViewModel { answer.await() }

        testScheduler.advanceUntilIdle()
        assertEquals(StatusesUiState.Loading, viewModel.uiState.value)

        answer.complete(listOf(item("a.jpg")))
        testScheduler.advanceUntilIdle()
        assertEquals(StatusesUiState.Content(listOf(item("a.jpg"))), viewModel.uiState.value)
    }

    @Test
    fun loadedStatusesAreShownInTheOrderGiven() = runTest(testDispatcher) {
        val items = listOf(item("new.jpg"), item("old.mp4", MediaType.VIDEO))
        val viewModel = StatusesViewModel { items }

        testScheduler.advanceUntilIdle()

        assertEquals(StatusesUiState.Content(items), viewModel.uiState.value)
    }

    @Test
    fun readableButEmptyFoldersShowEmpty() = runTest(testDispatcher) {
        val viewModel = StatusesViewModel { emptyList() }

        testScheduler.advanceUntilIdle()

        assertEquals(StatusesUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun noReadableFolderNeedsAccess() = runTest(testDispatcher) {
        val viewModel = StatusesViewModel { null }

        testScheduler.advanceUntilIdle()

        assertEquals(StatusesUiState.NeedsAccess, viewModel.uiState.value)
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

    private fun item(name: String, type: MediaType = MediaType.IMAGE) = StatusItem(
        uri = Uri.parse("content://test/$name"),
        name = name,
        type = type,
        mimeType = if (type == MediaType.IMAGE) "image/jpeg" else "video/mp4",
        lastModifiedMillis = 0,
        sizeBytes = 0,
    )
}
