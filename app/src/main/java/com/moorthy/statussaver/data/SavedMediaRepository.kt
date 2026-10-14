package com.moorthy.statussaver.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.moorthy.statussaver.data.model.MediaType
import com.moorthy.statussaver.data.model.StatusItem
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Keeps copies of statuses in the phone's shared gallery (FR4, design.md §4): images in
 * `Pictures/Keepsake`, videos in `Movies/Keepsake`, each under the status's own file
 * name (I3). It only reads from the statuses folders, never writes there (I1).
 */
class SavedMediaRepository(private val context: Context) {

    private val contentResolver = context.contentResolver

    /**
     * Copies [item] into the gallery. Returns true once the copy is complete and visible
     * to gallery apps, false when it could not be made, for example because WhatsApp
     * deleted the status in the meantime, the storage is full, or (Android 8-9) the
     * storage permission has not been granted.
     *
     * Gallery apps never see a half-written file, and nothing is left behind when the
     * copy fails.
     */
    suspend fun save(item: StatusItem): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMediaStore(item)
        } else {
            saveToPublicFolder(item)
        }
    }

    /**
     * Android 10+: the gallery row is marked pending while the bytes are copied and
     * removed again if the copy fails. No permission is needed (D-027).
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStore(item: StatusItem): Boolean {
        val pending = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, item.name)
            put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath(item.type))
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val savedUri = contentResolver.insert(collection(item.type), pending)
            ?: return false
        return copyOrCleanUp(
            copy = {
                val output = contentResolver.openOutputStream(savedUri)
                    ?: throw IOException("Cannot write $savedUri")
                output.use {
                    copy(from = item.uri, to = output)
                }
                val done = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                contentResolver.update(savedUri, done, null, null)
            },
            cleanUp = {
                contentResolver.delete(savedUri, null, null)
            },
        )
    }

    /**
     * Android 8-9: writes the file into the public Pictures or Movies folder, which needs
     * the storage permission, then asks the media scanner to add it to the gallery
     * (D-028). The bytes go into a hidden `.part` file first and are renamed once
     * complete, so a failed copy never leaves a broken file under the status's name.
     */
    @Suppress("DEPRECATION") // The public folders are the API for Android 8-9.
    private suspend fun saveToPublicFolder(item: StatusItem): Boolean {
        val folder = File(
            Environment.getExternalStoragePublicDirectory(publicDirectory(item.type)),
            FOLDER_NAME,
        )
        val target = File(folder, item.name)
        val partial = File(folder, ".${item.name}.part")
        val copied = copyOrCleanUp(
            copy = {
                // mkdirs() also fails without the storage permission.
                if (!folder.isDirectory && !folder.mkdirs()) {
                    throw IOException("Cannot create $folder")
                }
                FileOutputStream(partial).use { output ->
                    copy(from = item.uri, to = output)
                }
                if (!partial.renameTo(target)) {
                    throw IOException("Cannot rename $partial to $target")
                }
            },
            cleanUp = {
                partial.delete()
            },
        )
        if (copied) {
            scan(target, item.mimeType)
        }
        return copied
    }

    /**
     * Runs [copy] and returns true, or runs [cleanUp] and returns false when the copy
     * fails in one of the ways a save can fail on a phone.
     */
    private inline fun copyOrCleanUp(copy: () -> Unit, cleanUp: () -> Unit): Boolean {
        try {
            copy()
            return true
        } catch (e: IOException) {
            // Includes a status file that no longer exists, a full storage, and on
            // Android 8-9 a missing storage permission.
            cleanUp()
            return false
        } catch (e: SecurityException) {
            // The statuses folder grant was lost.
            cleanUp()
            return false
        } catch (e: IllegalArgumentException) {
            // How Android's external storage provider reports some missing documents
            // (seen in T3.4 for queries).
            cleanUp()
            return false
        }
    }

    private fun copy(from: Uri, to: OutputStream) {
        val input = contentResolver.openInputStream(from)
            ?: throw IOException("Cannot read $from")
        input.use {
            input.copyTo(to)
        }
    }

    /**
     * Adds [file] to the gallery and waits until the scanner is done, so the saved copy
     * is visible to gallery apps (and to MediaStore queries) when [save] returns.
     */
    private suspend fun scan(file: File, mimeType: String) {
        suspendCancellableCoroutine { continuation ->
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(mimeType),
            ) { _, _ ->
                continuation.resume(Unit)
            }
        }
    }

    companion object {
        /** The folder inside Pictures and Movies that holds everything the app saves. */
        const val FOLDER_NAME = "Keepsake"

        /** The shared top-level folder for [type]: Pictures or Movies. */
        fun publicDirectory(type: MediaType): String = when (type) {
            MediaType.IMAGE -> Environment.DIRECTORY_PICTURES
            MediaType.VIDEO -> Environment.DIRECTORY_MOVIES
        }

        /** Where a saved item ends up, relative to the storage root. */
        fun relativePath(type: MediaType): String = "${publicDirectory(type)}/$FOLDER_NAME/"

        @RequiresApi(Build.VERSION_CODES.Q)
        fun collection(type: MediaType): Uri = when (type) {
            MediaType.IMAGE -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            MediaType.VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
    }
}
