package com.moorthy.statussaver.data

import android.content.ContentResolver
import android.database.Cursor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import com.moorthy.statussaver.data.model.MediaType
import com.moorthy.statussaver.data.model.StatusItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lists the statuses currently in every granted statuses folder (design.md §3, steps 4
 * to 6). Read only: it never writes to or deletes from those folders (I1).
 */
class StatusRepository(
    private val statusSourceStore: StatusSourceStore,
    private val contentResolver: ContentResolver,
) {

    /**
     * Images and videos from all still-readable sources, newest first. Sources whose
     * grant was lost are skipped.
     *
     * Returns null when no source is readable at all, so callers can tell "the user must
     * grant a folder again" apart from "the folders are empty" (an empty list).
     */
    suspend fun load(): List<StatusItem>? = withContext(Dispatchers.IO) {
        val sources = statusSourceStore.validSources()
        if (sources.isEmpty()) {
            return@withContext null
        }
        sources
            .flatMap { source -> queryChildren(source) }
            .sortedWith(newestFirst)
    }

    /**
     * Lists one folder with a single query. Asking for every column in one go is much
     * faster than `DocumentFile.listFiles()` plus one call per file on large folders.
     */
    private fun queryChildren(source: StatusSource): List<StatusItem> {
        val treeDocumentId = DocumentsContract.getTreeDocumentId(source.treeUri)
        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(source.treeUri, treeDocumentId)
        val cursor = contentResolver.query(childrenUri, projection, null, null, null)
            ?: return emptyList()
        return cursor.use {
            buildList {
                while (cursor.moveToNext()) {
                    cursor.toStatusItem(source)?.let { item -> add(item) }
                }
            }
        }
    }

    /**
     * Maps one row of [projection] to a [StatusItem], or null when the row is not an image
     * or video. This drops `.nomedia`, sub-folders and any other file WhatsApp keeps there.
     */
    private fun Cursor.toStatusItem(source: StatusSource): StatusItem? {
        val name = getString(NAME_COLUMN) ?: return null
        val mimeType = getString(MIME_TYPE_COLUMN) ?: return null
        val type = when {
            mimeType.startsWith("image/") -> MediaType.IMAGE
            mimeType.startsWith("video/") -> MediaType.VIDEO
            else -> return null
        }
        return StatusItem(
            uri = DocumentsContract.buildDocumentUriUsingTree(
                source.treeUri,
                getString(DOCUMENT_ID_COLUMN),
            ),
            name = name,
            type = type,
            mimeType = mimeType,
            // Providers may leave these empty; zero sorts the item last and shows no size.
            lastModifiedMillis = if (isNull(LAST_MODIFIED_COLUMN)) 0 else getLong(LAST_MODIFIED_COLUMN),
            sizeBytes = if (isNull(SIZE_COLUMN)) 0 else getLong(SIZE_COLUMN),
        )
    }

    private companion object {
        val projection = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_SIZE,
        )
        const val DOCUMENT_ID_COLUMN = 0
        const val NAME_COLUMN = 1
        const val MIME_TYPE_COLUMN = 2
        const val LAST_MODIFIED_COLUMN = 3
        const val SIZE_COLUMN = 4

        // Same-time items keep a fixed order by name, so the grid does not reshuffle
        // between refreshes.
        val newestFirst = compareByDescending<StatusItem> { it.lastModifiedMillis }
            .thenBy { it.name }
    }
}
