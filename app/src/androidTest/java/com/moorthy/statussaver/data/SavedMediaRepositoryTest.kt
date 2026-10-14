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
import com.moorthy.statussaver.data.model.SaveResult
import com.moorthy.statussaver.data.model.StatusItem
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    // Every status name in this class contains it.
    private val runId = System.currentTimeMillis()

    /** Also removes rows MediaStore renamed to avoid a clash, such as `IMG-1 (1).jpg`. */
    @After
    fun deleteSavedRows() {
        for (type in MediaType.entries) {
            for (row in rowsNamedLike(type, "%$runId%")) {
                contentResolver.delete(row, null, null)
            }
        }
        deleteStatusFiles(context, runId)
    }

    @Test
    fun imageIsSavedCompleteUnderPicturesKeepsakeWithItsOwnName() = runBlocking {
        val bytes = jpegBytes()
        val item = statusFile("IMG-$runId.jpg", MediaType.IMAGE, "image/jpeg", bytes)

        val saved = repository.save(item)

        assertEquals(SaveResult.SAVED, saved)
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

        assertEquals(SaveResult.SAVED, saved)
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

        assertEquals(SaveResult.FAILED, saved)
        assertNull("a gallery row was left behind", savedRow(MediaType.IMAGE, item.name))
    }

    @Test
    fun savingTheSameStatusTwiceKeepsOneCopy() = runBlocking {
        val item = statusFile("IMG-$runId-twice.jpg", MediaType.IMAGE, "image/jpeg", jpegBytes())

        val first = repository.save(item)
        val second = repository.save(item)

        assertEquals(SaveResult.SAVED, first)
        assertEquals(SaveResult.ALREADY_SAVED, second)
        assertEquals(1, rowsNamedLike(MediaType.IMAGE, "IMG-$runId-twice%").size)
    }

    @Test
    fun twoSavesOfTheSameStatusAtOnceKeepOneCopy() = runBlocking {
        // A large file keeps the first copy busy while the second save looks for it.
        val item = statusFile("VID-$runId-tap.mp4", MediaType.VIDEO, "video/mp4", largeBytes())

        val results = List(2) {
            async(Dispatchers.Default) { repository.save(item) }
        }.awaitAll()

        assertEquals(setOf(SaveResult.SAVED, SaveResult.ALREADY_SAVED), results.toSet())
        assertEquals(1, rowsNamedLike(MediaType.VIDEO, "VID-$runId-tap%").size)
    }

    @Test
    fun anotherStatusIsSavedEvenWhenOneIsAlreadySaved() = runBlocking {
        val first = statusFile("IMG-$runId-first.jpg", MediaType.IMAGE, "image/jpeg", jpegBytes())
        val second = statusFile("IMG-$runId-second.jpg", MediaType.IMAGE, "image/jpeg", jpegBytes())
        repository.save(first)

        val result = repository.save(second)

        assertEquals(SaveResult.SAVED, result)
    }

    @Test
    fun savedNamesListsCopiesAlreadyInTheGallery() = runBlocking {
        val image = statusFile("IMG-$runId-listed.jpg", MediaType.IMAGE, "image/jpeg", jpegBytes())
        val video = statusFile("VID-$runId-listed.mp4", MediaType.VIDEO, "video/mp4", videoBytes())
        repository.save(image)
        repository.save(video)
        // As after an app restart: a fresh repository knows nothing until it looks.
        val restarted = SavedMediaRepository(context)

        restarted.refreshSavedNames()

        val names = restarted.savedNames.value
        assertTrue(image.name in names)
        assertTrue(video.name in names)
        assertFalse("IMG-$runId-never-saved.jpg" in names)
    }

    @Test
    fun savedNamesFollowSavesWithoutARefresh() = runBlocking {
        val item = statusFile("IMG-$runId-follow.jpg", MediaType.IMAGE, "image/jpeg", jpegBytes())
        repository.refreshSavedNames()
        assertFalse(item.name in repository.savedNames.value)

        repository.save(item)

        assertTrue(item.name in repository.savedNames.value)
    }

    @Test
    fun statusFoundAlreadySavedIsListedEvenBeforeARefresh() = runBlocking {
        val item = statusFile("IMG-$runId-early.jpg", MediaType.IMAGE, "image/jpeg", jpegBytes())
        repository.save(item)
        // After an app restart, the user taps Save before the saved names were read.
        val restarted = SavedMediaRepository(context)

        val result = restarted.save(item)

        assertEquals(SaveResult.ALREADY_SAVED, result)
        assertTrue(item.name in restarted.savedNames.value)
    }

    @Test
    fun failedSaveIsNotListedAsSaved() = runBlocking {
        val item = statusFile("IMG-$runId-failed.jpg", MediaType.IMAGE, "image/jpeg", jpegBytes())
        File(item.uri.path!!).delete()

        repository.save(item)
        assertFalse(item.name in repository.savedNames.value)
        repository.refreshSavedNames()
        assertFalse(item.name in repository.savedNames.value)
    }

    @Test
    fun copyDeletedInTheGalleryCanBeSavedAgain() = runBlocking {
        val item = statusFile("IMG-$runId-deleted.jpg", MediaType.IMAGE, "image/jpeg", jpegBytes())
        repository.save(item)
        val row = savedRow(MediaType.IMAGE, item.name)!!
        // The user deletes the copy in a gallery app.
        contentResolver.delete(row.uri, null, null)

        repository.refreshSavedNames()
        assertFalse(item.name in repository.savedNames.value)
        assertEquals(SaveResult.SAVED, repository.save(item))
    }

    private fun statusFile(name: String, type: MediaType, mimeType: String, bytes: ByteArray): StatusItem =
        statusFile(context, name, type, mimeType, bytes)

    /** URIs of this app's rows in [type]'s collection whose name matches [pattern], pending rows included. */
    private fun rowsNamedLike(type: MediaType, pattern: String): List<Uri> {
        val collection = SavedMediaRepository.collection(type)
        val query = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?")
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(pattern))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
            }
        }
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        return contentResolver.query(includingPending(collection), projection, query, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(ContentUris.withAppendedId(collection, cursor.getLong(0)))
                }
            }
        }.orEmpty()
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
