package com.moorthy.statussaver.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the Android 8-9 path, which writes into the public Pictures and Movies folders
 * and asks the media scanner to add the file to the gallery (D-028). The status files
 * are plain files in the app's cache ([statusFile]).
 *
 * The storage permission is granted before each test, as the user would at the first
 * save. On the API 28 emulator a grant given to the already running test process did not
 * reach that process, so the first run after the permission was revoked (or the app
 * reinstalled) fails; the next run passes. Every file and gallery row a test creates is
 * deleted again afterwards.
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(maxSdkVersion = Build.VERSION_CODES.P)
class LegacySavedMediaRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val contentResolver: ContentResolver = context.contentResolver
    private val repository = SavedMediaRepository(context)

    // Unique per run, so a file left behind by an aborted run cannot satisfy a test.
    private val runId = System.currentTimeMillis()
    private val createdFiles = mutableListOf<Pair<MediaType, String>>()

    /**
     * Grants what the permission dialog grants. Android adds READ_EXTERNAL_STORAGE to any
     * app that declares WRITE, and the dialog grants both; with WRITE alone the app still
     * gets no access to the shared folders (checked on the API 28 emulator).
     */
    @Before
    fun grantStoragePermission() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        for (permission in STORAGE_PERMISSIONS) {
            // The command has finished once its output is closed.
            uiAutomation.executeShellCommand("pm grant ${context.packageName} $permission").close()
        }
    }

    @After
    fun deleteSavedFiles() {
        for ((type, name) in createdFiles) {
            val file = savedFile(type, name)
            contentResolver.delete(
                collection(type),
                "${MediaStore.MediaColumns.DATA} = ?",
                arrayOf(file.absolutePath),
            )
            file.delete()
            partialFile(type, name).delete()
        }
        deleteStatusFiles(context, runId)
    }

    @Test
    fun imageIsSavedCompleteUnderPicturesKeepsakeAndShownInTheGallery() = runBlocking {
        val bytes = jpegBytes()
        val item = statusFile("IMG-$runId.jpg", MediaType.IMAGE, "image/jpeg", bytes)

        val saved = repository.save(item)

        assertEquals(SaveResult.SAVED, saved)
        val file = savedFile(MediaType.IMAGE, item.name)
        assertTrue("no file at $file", file.isFile)
        assertArrayEquals(bytes, file.readBytes())
        assertTrue("not in the gallery", isInGallery(MediaType.IMAGE, file))
    }

    @Test
    fun videoIsSavedUnderMoviesKeepsakeAndShownInTheGallery() = runBlocking {
        val bytes = videoBytes()
        val item = statusFile("VID-$runId.mp4", MediaType.VIDEO, "video/mp4", bytes)

        val saved = repository.save(item)

        assertEquals(SaveResult.SAVED, saved)
        val file = savedFile(MediaType.VIDEO, item.name)
        assertTrue("no file at $file", file.isFile)
        assertArrayEquals(bytes, file.readBytes())
        assertTrue("not in the gallery", isInGallery(MediaType.VIDEO, file))
    }

    @Test
    fun savingLeavesTheStatusFileAsItWas() = runBlocking {
        val bytes = jpegBytes()
        val item = statusFile("IMG-$runId-source.jpg", MediaType.IMAGE, "image/jpeg", bytes)

        repository.save(item)

        assertArrayEquals(bytes, File(item.uri.path!!).readBytes())
    }

    @Test
    fun statusDeletedBeforeSavingLeavesNothingBehind() = runBlocking {
        val item = statusFile("IMG-$runId-gone.jpg", MediaType.IMAGE, "image/jpeg", jpegBytes())
        File(item.uri.path!!).delete()

        val saved = repository.save(item)

        assertEquals(SaveResult.FAILED, saved)
        assertFalse("a file was left behind", savedFile(MediaType.IMAGE, item.name).exists())
        assertFalse("a partial file was left behind", partialFile(MediaType.IMAGE, item.name).exists())
    }

    @Test
    fun savingTheSameStatusTwiceWritesItOnce() = runBlocking {
        val item = statusFile("IMG-$runId-twice.jpg", MediaType.IMAGE, "image/jpeg", jpegBytes())
        repository.save(item)
        val file = savedFile(MediaType.IMAGE, item.name)
        // Marks the first copy, so a second write would be noticed.
        file.setLastModified(MARKED_TIME)

        val second = repository.save(item)

        assertEquals(SaveResult.ALREADY_SAVED, second)
        assertEquals(MARKED_TIME, file.lastModified())
    }

    @Test
    fun twoSavesOfTheSameStatusAtOnceWriteItOnce() = runBlocking {
        // A large file keeps the first copy busy while the second save looks for it.
        val item = statusFile("VID-$runId-tap.mp4", MediaType.VIDEO, "video/mp4", largeBytes())

        val results = List(2) {
            async(Dispatchers.Default) { repository.save(item) }
        }.awaitAll()

        assertEquals(setOf(SaveResult.SAVED, SaveResult.ALREADY_SAVED), results.toSet())
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
        // A copy still being written is not saved yet.
        partialFile(MediaType.IMAGE, "IMG-$runId-writing.jpg").writeBytes(jpegBytes())
        createdFiles += MediaType.IMAGE to "IMG-$runId-writing.jpg"
        // As after an app restart: a fresh repository knows nothing until it looks.
        val restarted = SavedMediaRepository(context)

        restarted.refreshSavedNames()

        val names = restarted.savedNames.value
        assertTrue(image.name in names)
        assertTrue(video.name in names)
        assertFalse(names.any { name -> name.contains("writing") })
    }

    @Test
    fun copyDeletedInTheGalleryCanBeSavedAgain() = runBlocking {
        val item = statusFile("IMG-$runId-deleted.jpg", MediaType.IMAGE, "image/jpeg", jpegBytes())
        repository.save(item)
        // The user deletes the copy in a gallery app.
        savedFile(MediaType.IMAGE, item.name).delete()

        repository.refreshSavedNames()
        assertFalse(item.name in repository.savedNames.value)
        assertEquals(SaveResult.SAVED, repository.save(item))
    }

    private fun statusFile(name: String, type: MediaType, mimeType: String, bytes: ByteArray): StatusItem {
        createdFiles += type to name
        return statusFile(context, name, type, mimeType, bytes)
    }

    /** Spelled out here rather than taken from the repository, so a wrong folder is noticed. */
    @Suppress("DEPRECATION")
    private fun keepsakeFolder(type: MediaType): File {
        val sharedFolder = when (type) {
            MediaType.IMAGE -> Environment.DIRECTORY_PICTURES
            MediaType.VIDEO -> Environment.DIRECTORY_MOVIES
        }
        return File(Environment.getExternalStoragePublicDirectory(sharedFolder), "Keepsake")
    }

    private fun savedFile(type: MediaType, name: String) = File(keepsakeFolder(type), name)

    private fun partialFile(type: MediaType, name: String) = File(keepsakeFolder(type), ".$name.part")

    private fun collection(type: MediaType): Uri = when (type) {
        MediaType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    /** Whether gallery apps can find [file]: MediaStore has a row for its path. */
    @Suppress("DEPRECATION")
    private fun isInGallery(type: MediaType, file: File): Boolean =
        contentResolver.query(
            collection(type),
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DATA} = ?",
            arrayOf(file.absolutePath),
            null,
        )?.use { cursor -> cursor.count > 0 } ?: false

    private companion object {
        const val MARKED_TIME = 1_000_000_000_000L

        val STORAGE_PERMISSIONS = listOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
        )
    }
}
