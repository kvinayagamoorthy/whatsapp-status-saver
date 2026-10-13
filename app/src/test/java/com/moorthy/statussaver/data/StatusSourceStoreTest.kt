package com.moorthy.statussaver.data

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.moorthy.statussaver.data.model.StatusSourceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
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
 * Robolectric's ContentResolver keeps a real list of persisted URI permissions, so these
 * tests exercise the same calls the store makes on a device.
 */
@RunWith(RobolectricTestRunner::class)
class StatusSourceStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var contentResolver: ContentResolver
    private lateinit var store: StatusSourceStore

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(scope = dataStoreScope) {
            temporaryFolder.newFile("sources.preferences_pb")
        }
        contentResolver = RuntimeEnvironment.getApplication().contentResolver
        store = StatusSourceStore(dataStore, contentResolver)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun noSourcesBeforeAnyGrant() = runBlocking {
        assertEquals(emptyList<StatusSource>(), store.validSources())
    }

    @Test
    fun grantedFolderIsAValidSource() = runBlocking {
        store.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)

        assertEquals(
            listOf(StatusSource(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)),
            store.validSources(),
        )
    }

    @Test
    fun bothAppsCanBeGrantedAndAreListedInKindOrder() = runBlocking {
        store.grant(StatusSourceKind.BUSINESS, BUSINESS_TREE)
        store.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)

        assertEquals(
            listOf(
                StatusSource(StatusSourceKind.WHATSAPP, WHATSAPP_TREE),
                StatusSource(StatusSourceKind.BUSINESS, BUSINESS_TREE),
            ),
            store.validSources(),
        )
    }

    @Test
    fun storedFolderWhosePermissionWasLostIsNotValid() = runBlocking {
        store.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)
        store.grant(StatusSourceKind.BUSINESS, BUSINESS_TREE)

        loseGrant(WHATSAPP_TREE)

        assertEquals(
            listOf(StatusSource(StatusSourceKind.BUSINESS, BUSINESS_TREE)),
            store.validSources(),
        )
    }

    @Test
    fun repickingAFolderReplacesItAndReleasesTheOldGrant() = runBlocking {
        store.grant(StatusSourceKind.WHATSAPP, LEGACY_WHATSAPP_TREE)

        store.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)

        assertEquals(
            listOf(StatusSource(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)),
            store.validSources(),
        )
        assertEquals(listOf(WHATSAPP_TREE), persistedTreeUris())
    }

    // A device throws SecurityException when releasing a grant it no longer holds, which
    // the store guards against. Robolectric does not throw there, so this test only shows
    // that the new folder wins; the guard itself is checked on a device in T1.5.
    @Test
    fun repickingAfterTheOldGrantWasLostStillSucceeds() = runBlocking {
        store.grant(StatusSourceKind.WHATSAPP, LEGACY_WHATSAPP_TREE)
        loseGrant(LEGACY_WHATSAPP_TREE)

        store.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)

        assertEquals(
            listOf(StatusSource(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)),
            store.validSources(),
        )
    }

    @Test
    fun repickingTheSameFolderKeepsItsGrant() = runBlocking {
        store.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)

        store.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)

        assertEquals(
            listOf(StatusSource(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)),
            store.validSources(),
        )
        assertEquals(listOf(WHATSAPP_TREE), persistedTreeUris())
    }

    /** What the system does when it drops a grant behind the app's back. */
    private fun loseGrant(treeUri: Uri) {
        contentResolver.releasePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }

    private fun persistedTreeUris(): List<Uri> =
        contentResolver.persistedUriPermissions.map { permission -> permission.uri }

    private companion object {
        val WHATSAPP_TREE: Uri = Uri.parse(
            "content://com.android.externalstorage.documents/tree/" +
                "primary%3AAndroid%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses",
        )
        val LEGACY_WHATSAPP_TREE: Uri = Uri.parse(
            "content://com.android.externalstorage.documents/tree/" +
                "primary%3AWhatsApp%2FMedia%2F.Statuses",
        )
        val BUSINESS_TREE: Uri = Uri.parse(
            "content://com.android.externalstorage.documents/tree/" +
                "primary%3AAndroid%2Fmedia%2Fcom.whatsapp.w4b%2FWhatsApp%20Business%2FMedia%2F.Statuses",
        )
    }
}
