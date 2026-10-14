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
import com.moorthy.statussaver.data.model.StatusItem
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
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
    }

    @Test
    fun imageIsSavedCompleteUnderPicturesKeepsakeAndShownInTheGallery() = runBlocking {
        val bytes = jpegBytes()
        val item = statusFile("IMG-$runId.jpg", MediaType.IMAGE, "image/jpeg", bytes)

        val saved = repository.save(item)

        assertTrue(saved)
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

        assertTrue(saved)
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

        assertFalse(saved)
        assertFalse("a file was left behind", savedFile(MediaType.IMAGE, item.name).exists())
        assertFalse("a partial file was left behind", partialFile(MediaType.IMAGE, item.name).exists())
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
        val STORAGE_PERMISSIONS = listOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
        )
    }
}
