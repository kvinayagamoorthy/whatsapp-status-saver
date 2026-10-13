package com.moorthy.statussaver.data

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.moorthy.statussaver.data.model.MediaType
import com.moorthy.statussaver.data.model.StatusItem
import com.moorthy.statussaver.data.model.StatusSourceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Runs the repository against a fake external storage documents provider. The fake only
 * answers the columns the repository asks for, so a missing projection column shows up
 * as a dropped or wrong item.
 */
@RunWith(RobolectricTestRunner::class)
class StatusRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var contentResolver: ContentResolver
    private lateinit var statusSourceStore: StatusSourceStore
    private lateinit var repository: StatusRepository

    @Before
    fun setUp() {
        FakeDocumentsProvider.folders.clear()
        FakeDocumentsProvider.permissionLost = false
        Robolectric.setupContentProvider(FakeDocumentsProvider::class.java, AUTHORITY)
        val dataStore = PreferenceDataStoreFactory.create(scope = dataStoreScope) {
            temporaryFolder.newFile("sources.preferences_pb")
        }
        contentResolver = RuntimeEnvironment.getApplication().contentResolver
        statusSourceStore = StatusSourceStore(dataStore, contentResolver)
        repository = StatusRepository(statusSourceStore, contentResolver)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun noGrantedSourceGivesNull() = runBlocking {
        FakeDocumentsProvider.folders[WHATSAPP_TREE_ID] = listOf(image("a.jpg", modified = 1))

        assertNull(repository.load())
    }

    @Test
    fun everySourceWithItsPermissionLostGivesNull() = runBlocking {
        FakeDocumentsProvider.folders[WHATSAPP_TREE_ID] = listOf(image("a.jpg", modified = 1))
        statusSourceStore.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)

        contentResolver.releasePersistableUriPermission(
            WHATSAPP_TREE,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )

        assertNull(repository.load())
    }

    @Test
    fun grantedButEmptyFolderGivesAnEmptyList() = runBlocking {
        FakeDocumentsProvider.folders[WHATSAPP_TREE_ID] = emptyList()
        statusSourceStore.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)

        assertEquals(emptyList<StatusItem>(), repository.load())
    }

    @Test
    fun imageAndVideoRowsBecomeItemsWithAllTheirDetails() = runBlocking {
        FakeDocumentsProvider.folders[WHATSAPP_TREE_ID] = listOf(
            row("$WHATSAPP_TREE_ID/a.jpg", "a.jpg", "image/jpeg", 2_000, 150),
            row("$WHATSAPP_TREE_ID/b.mp4", "b.mp4", "video/mp4", 1_000, 9_000),
        )
        statusSourceStore.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)

        assertEquals(
            listOf(
                StatusItem(
                    uri = DocumentsContract.buildDocumentUriUsingTree(
                        WHATSAPP_TREE,
                        "$WHATSAPP_TREE_ID/a.jpg",
                    ),
                    name = "a.jpg",
                    type = MediaType.IMAGE,
                    mimeType = "image/jpeg",
                    lastModifiedMillis = 2_000,
                    sizeBytes = 150,
                ),
                StatusItem(
                    uri = DocumentsContract.buildDocumentUriUsingTree(
                        WHATSAPP_TREE,
                        "$WHATSAPP_TREE_ID/b.mp4",
                    ),
                    name = "b.mp4",
                    type = MediaType.VIDEO,
                    mimeType = "video/mp4",
                    lastModifiedMillis = 1_000,
                    sizeBytes = 9_000,
                ),
            ),
            repository.load(),
        )
    }

    @Test
    fun nomediaFoldersAndOtherFilesAreDropped() = runBlocking {
        FakeDocumentsProvider.folders[WHATSAPP_TREE_ID] = listOf(
            image("a.jpg", modified = 1),
            row("$WHATSAPP_TREE_ID/.nomedia", ".nomedia", "application/octet-stream", 1, 0),
            row("$WHATSAPP_TREE_ID/old", "old", Document.MIME_TYPE_DIR, 1, 0),
            row("$WHATSAPP_TREE_ID/notes.txt", "notes.txt", "text/plain", 1, 0),
            row("$WHATSAPP_TREE_ID/unknown", "unknown", null, 1, 0),
        )
        statusSourceStore.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)

        assertEquals(listOf("a.jpg"), repository.load()!!.map { it.name })
    }

    @Test
    fun bothSourcesAreMergedNewestFirst() = runBlocking {
        FakeDocumentsProvider.folders[WHATSAPP_TREE_ID] = listOf(
            image("w-old.jpg", modified = 100),
            image("w-new.jpg", modified = 300),
        )
        FakeDocumentsProvider.folders[BUSINESS_TREE_ID] = listOf(
            image("b-mid.jpg", modified = 200, treeId = BUSINESS_TREE_ID),
        )
        statusSourceStore.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)
        statusSourceStore.grant(StatusSourceKind.BUSINESS, BUSINESS_TREE)

        assertEquals(
            listOf("w-new.jpg", "b-mid.jpg", "w-old.jpg"),
            repository.load()!!.map { it.name },
        )
    }

    @Test
    fun itemsWithTheSameTimeAreOrderedByName() = runBlocking {
        FakeDocumentsProvider.folders[WHATSAPP_TREE_ID] = listOf(
            image("c.jpg", modified = 5),
            image("a.jpg", modified = 5),
            image("b.jpg", modified = 5),
        )
        statusSourceStore.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)

        assertEquals(listOf("a.jpg", "b.jpg", "c.jpg"), repository.load()!!.map { it.name })
    }

    @Test
    fun sourceWhosePermissionWasLostIsSkipped() = runBlocking {
        FakeDocumentsProvider.folders[WHATSAPP_TREE_ID] = listOf(image("w.jpg", modified = 1))
        FakeDocumentsProvider.folders[BUSINESS_TREE_ID] =
            listOf(image("b.jpg", modified = 1, treeId = BUSINESS_TREE_ID))
        statusSourceStore.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)
        statusSourceStore.grant(StatusSourceKind.BUSINESS, BUSINESS_TREE)

        contentResolver.releasePersistableUriPermission(
            WHATSAPP_TREE,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )

        assertEquals(listOf("b.jpg"), repository.load()!!.map { it.name })
    }

    // On a device, the provider answers null for a granted folder that was deleted, for
    // example when WhatsApp is uninstalled while the grant stays (checked in T2.1).
    @Test
    fun grantedFolderThatNoLongerExistsGivesNoItemsFromIt() = runBlocking {
        FakeDocumentsProvider.folders[BUSINESS_TREE_ID] =
            listOf(image("b.jpg", modified = 1, treeId = BUSINESS_TREE_ID))
        statusSourceStore.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)
        statusSourceStore.grant(StatusSourceKind.BUSINESS, BUSINESS_TREE)

        assertEquals(listOf("b.jpg"), repository.load()!!.map { it.name })
    }

    @Test
    fun missingTimeAndSizeReadAsZeroAndSortLast() = runBlocking {
        FakeDocumentsProvider.folders[WHATSAPP_TREE_ID] = listOf(
            row("$WHATSAPP_TREE_ID/undated.jpg", "undated.jpg", "image/jpeg", null, null),
            image("dated.jpg", modified = 1),
        )
        statusSourceStore.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)

        val items = repository.load()!!

        assertEquals(listOf("dated.jpg", "undated.jpg"), items.map { it.name })
        assertEquals(0L, items.last().lastModifiedMillis)
        assertEquals(0L, items.last().sizeBytes)
    }

    @Test
    fun aListedFileIsStillThereUntilItIsDeleted() = runBlocking {
        FakeDocumentsProvider.folders[WHATSAPP_TREE_ID] = listOf(
            image("a.jpg", modified = 2),
            image("b.jpg", modified = 1),
        )
        statusSourceStore.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)
        val (a, b) = repository.load()!!

        FakeDocumentsProvider.folders[WHATSAPP_TREE_ID] = listOf(image("b.jpg", modified = 1))

        assertFalse(repository.stillExists(a))
        assertTrue(repository.stillExists(b))
    }

    @Test
    fun aFileWhoseWholeFolderWasDeletedIsGone() = runBlocking {
        FakeDocumentsProvider.folders[WHATSAPP_TREE_ID] = listOf(image("a.jpg", modified = 1))
        statusSourceStore.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)
        val item = repository.load()!!.single()

        FakeDocumentsProvider.folders.remove(WHATSAPP_TREE_ID)

        assertFalse(repository.stillExists(item))
    }

    @Test
    fun aFileInAFolderWhosePermissionWasLostCountsAsGone() = runBlocking {
        FakeDocumentsProvider.folders[WHATSAPP_TREE_ID] = listOf(image("a.jpg", modified = 1))
        statusSourceStore.grant(StatusSourceKind.WHATSAPP, WHATSAPP_TREE)
        val item = repository.load()!!.single()
        FakeDocumentsProvider.permissionLost = true

        assertFalse(repository.stillExists(item))
    }

    /**
     * Stands in for Android's external storage documents provider. Children queries get
     * the rows stored for the tree being listed; a query for one document gets its row,
     * or an IllegalArgumentException when it is missing, as the real provider does
     * (checked in T3.4).
     */
    class FakeDocumentsProvider : ContentProvider() {

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<String>?,
            selection: String?,
            selectionArgs: Array<String>?,
            sortOrder: String?,
        ): Cursor? {
            if (permissionLost) {
                throw SecurityException("No permission for $uri")
            }
            val treeRows = folders[DocumentsContract.getTreeDocumentId(uri)] ?: return null
            val rows = if (uri.lastPathSegment == "children") {
                treeRows
            } else {
                val documentId = DocumentsContract.getDocumentId(uri)
                val row = treeRows.find { row -> row[Document.COLUMN_DOCUMENT_ID] == documentId }
                    ?: throw IllegalArgumentException("Missing file for $documentId")
                listOf(row)
            }
            val columns = requireNotNull(projection)
            val cursor = MatrixCursor(columns)
            for (row in rows) {
                cursor.addRow(columns.map { column -> row[column] })
            }
            return cursor
        }

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? =
            throw UnsupportedOperationException()

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int =
            throw UnsupportedOperationException()

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<String>?,
        ): Int = throw UnsupportedOperationException()

        companion object {
            /** Rows of each folder, keyed by the tree document id. */
            val folders = mutableMapOf<String, List<Map<String, Any?>>>()

            /** Makes every query fail as it does once the folder grant is gone. */
            var permissionLost = false
        }
    }

    private fun image(
        name: String,
        modified: Long,
        treeId: String = WHATSAPP_TREE_ID,
    ): Map<String, Any?> = row("$treeId/$name", name, "image/jpeg", modified, 100)

    private fun row(
        documentId: String,
        name: String,
        mimeType: String?,
        lastModified: Long?,
        size: Long?,
    ): Map<String, Any?> = mapOf(
        Document.COLUMN_DOCUMENT_ID to documentId,
        Document.COLUMN_DISPLAY_NAME to name,
        Document.COLUMN_MIME_TYPE to mimeType,
        Document.COLUMN_LAST_MODIFIED to lastModified,
        Document.COLUMN_SIZE to size,
    )

    private companion object {
        const val AUTHORITY = "com.android.externalstorage.documents"
        const val WHATSAPP_TREE_ID = "primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
        const val BUSINESS_TREE_ID =
            "primary:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"
        val WHATSAPP_TREE: Uri = DocumentsContract.buildTreeDocumentUri(AUTHORITY, WHATSAPP_TREE_ID)
        val BUSINESS_TREE: Uri = DocumentsContract.buildTreeDocumentUri(AUTHORITY, BUSINESS_TREE_ID)
    }
}
