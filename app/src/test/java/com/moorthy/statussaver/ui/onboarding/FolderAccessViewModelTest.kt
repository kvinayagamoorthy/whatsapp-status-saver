package com.moorthy.statussaver.ui.onboarding

import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.moorthy.statussaver.data.StatusSourceStore
import com.moorthy.statussaver.data.model.StatusSourceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Runs the ViewModel against a real [StatusSourceStore], so a revoke is checked where it
 * matters: in the persisted permissions Robolectric's ContentResolver keeps.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FolderAccessViewModelTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val contentResolver = RuntimeEnvironment.getApplication().contentResolver
    private lateinit var store: StatusSourceStore

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val dataStore = PreferenceDataStoreFactory.create(scope = dataStoreScope) {
            temporaryFolder.newFile("sources.preferences_pb")
        }
        store = StatusSourceStore(dataStore, contentResolver)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun revokingOneAppKeepsTheOtherAndGivesItsPermissionBack() = runBlocking {
        store.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)
        store.grant(StatusSourceKind.BUSINESS, BUSINESS_TREE)
        val viewModel = FolderAccessViewModel(store)
        awaitGranted(viewModel, setOf(StatusSourceKind.WHATSAPP, StatusSourceKind.BUSINESS))

        viewModel.revoke(StatusSourceKind.WHATSAPP)

        awaitGranted(viewModel, setOf(StatusSourceKind.BUSINESS))
        assertEquals(
            listOf(BUSINESS_TREE),
            contentResolver.persistedUriPermissions.map { permission -> permission.uri },
        )
    }

    private suspend fun awaitGranted(viewModel: FolderAccessViewModel, kinds: Set<StatusSourceKind>) {
        // Fails rather than waiting forever if the state never gets there.
        withTimeout(WAIT_MILLIS) {
            viewModel.uiState.first { state -> state.grantedKinds == kinds }
        }
    }

    private companion object {
        const val WAIT_MILLIS = 5_000L

        val WHATSAPP_TREE: Uri = Uri.parse(
            "content://com.android.externalstorage.documents/tree/" +
                "primary%3AAndroid%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses",
        )
        val BUSINESS_TREE: Uri = Uri.parse(
            "content://com.android.externalstorage.documents/tree/" +
                "primary%3AAndroid%2Fmedia%2Fcom.whatsapp.w4b%2FWhatsApp%20Business%2FMedia%2F.Statuses",
        )
    }
}
