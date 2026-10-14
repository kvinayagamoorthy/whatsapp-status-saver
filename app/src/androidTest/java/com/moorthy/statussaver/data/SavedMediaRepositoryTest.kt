package com.moorthy.statussaver.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.moorthy.statussaver.data.model.MediaType
import com.moorthy.statussaver.data.model.StatusItem
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Saves into the device's real MediaStore. The status files are plain files in the
 * app's cache ([statusFile]).
 *
 * Covers the MediaStore path of Android 10+; [LegacySavedMediaRepositoryTest] covers
 * Android 8-9. Queries include pending rows, so a file left pending is noticed. Every
 * gallery row a test creates is deleted again afterwards.
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
class SavedMediaRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val contentResolver: ContentResolver = context.contentResolver
    private val repository = SavedMediaRepository(context)

    // Unique per run, so a row left behind by an aborted run cannot satisfy a test.
    private val runId = System.currentTimeMillis()
    private val createdNames = mutableListOf<String>()

    @After
    fun deleteSavedRows() {
        for (type in MediaType.entries) {
            for (name in createdNames) {
                savedRow(type, name)?.let { row -> contentResolver.delete(row.uri, null, null) }
            }
        }
    }

    @Test
    fun imageIsSavedCompleteUnderPicturesKeepsakeWithItsOwnName() = runBlocking {
        val bytes = jpegBytes()
        val item = statusFile("IMG-$runId.jpg", MediaType.IMAGE, "image/jpeg", bytes)

        val saved = repository.save(item)

        assertTrue(saved)
        val row = savedRow(MediaType.IMAGE, item.name)
        assertNotNull("no gallery row named ${item.name}", row)
        assertEquals("Pictures/Keepsake/", row!!.relativePath)
        assertFalse("still pending", row.isPending)
        assertArrayEquals(bytes, read(row.uri))
    }

    @Test
    fun videoIsSavedUnderMoviesKeepsake() = runBlocking {
        val bytes = videoBytes()
        val item = statusFile("VID-$runId.mp4", MediaType.VIDEO, "video/mp4", bytes)

        val saved = repository.save(item)

        assertTrue(saved)
        val row = savedRow(MediaType.VIDEO, item.name)
        assertNotNull("no gallery row named ${item.name}", row)
        assertEquals("Movies/Keepsake/", row!!.relativePath)
        assertFalse("still pending", row.isPending)
        assertArrayEquals(bytes, read(row.uri))
    }

    @Test
    fun savingLeavesTheStatusFileAsItWas() = runBlocking {
        val bytes = jpegBytes()
        val item = statusFile("IMG-$runId-source.jpg", MediaType.IMAGE, "image/jpeg", bytes)

        repository.save(item)

        assertArrayEquals(bytes, read(item.uri))
    }

    @Test
    fun statusDeletedBeforeSavingLeavesNothingInTheGallery() = runBlocking {
        val item = statusFile("IMG-$runId-gone.jpg", MediaType.IMAGE, "image/jpeg", jpegBytes())
        File(item.uri.path!!).delete()

        val saved = repository.save(item)

        assertFalse(saved)
        assertNull("a gallery row was left behind", savedRow(MediaType.IMAGE, item.name))
    }

    private fun statusFile(name: String, type: MediaType, mimeType: String, bytes: ByteArray): StatusItem {
        createdNames += name
        return statusFile(context, name, type, mimeType, bytes)
    }

    private fun read(uri: Uri): ByteArray =
        contentResolver.openInputStream(uri)!!.use { input -> input.readBytes() }

    private class SavedRow(val uri: Uri, val relativePath: String, val isPending: Boolean)

    /** The app's own row named [name] in [type]'s collection, pending rows included. */
    private fun savedRow(type: MediaType, name: String): SavedRow? {
        val collection = SavedMediaRepository.collection(type)
        val query = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.MediaColumns.DISPLAY_NAME} = ?")
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(name))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
            }
        }
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.IS_PENDING,
        )
        return contentResolver.query(includingPending(collection), projection, query, null)?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return@use null
            }
            SavedRow(
                uri = ContentUris.withAppendedId(collection, cursor.getLong(0)),
                relativePath = cursor.getString(1),
                isPending = cursor.getInt(2) == 1,
            )
        }
    }

    /** Android 10 has no query argument for pending rows; it marks the URI instead. */
    @Suppress("DEPRECATION")
    private fun includingPending(collection: Uri): Uri =
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            MediaStore.setIncludePending(collection)
        } else {
            collection
        }
}
