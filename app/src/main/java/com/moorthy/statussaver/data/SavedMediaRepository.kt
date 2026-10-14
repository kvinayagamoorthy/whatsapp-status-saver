package com.moorthy.statussaver.data

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.moorthy.statussaver.data.model.MediaType
import com.moorthy.statussaver.data.model.StatusItem
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Keeps copies of statuses in the phone's shared gallery (FR4, design.md §4): images in
 * `Pictures/Keepsake`, videos in `Movies/Keepsake`, each under the status's own file
 * name (I3). It only reads from the statuses folders, never writes there (I1).
 */
class SavedMediaRepository(private val contentResolver: ContentResolver) {

    /**
     * Copies [item] into the gallery. Returns true once the copy is complete, false when
     * it could not be made, for example because WhatsApp deleted the status in the
     * meantime or the storage is full.
     *
     * The gallery row is marked pending while the bytes are copied, so gallery apps never
     * show a half-written file, and it is removed again if the copy fails.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun save(item: StatusItem): Boolean = withContext(Dispatchers.IO) {
        val pending = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, item.name)
            put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath(item.type))
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val savedUri = contentResolver.insert(collection(item.type), pending)
            ?: return@withContext false
        try {
            copy(from = item.uri, to = savedUri)
            val done = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            contentResolver.update(savedUri, done, null, null)
            true
        } catch (e: IOException) {
            // Includes a status file that no longer exists, and a full storage.
            contentResolver.delete(savedUri, null, null)
            false
        } catch (e: SecurityException) {
            // The statuses folder grant was lost.
            contentResolver.delete(savedUri, null, null)
            false
        } catch (e: IllegalArgumentException) {
            // How Android's external storage provider reports some missing documents
            // (seen in T3.4 for queries).
            contentResolver.delete(savedUri, null, null)
            false
        }
    }

    private fun copy(from: Uri, to: Uri) {
        val input = contentResolver.openInputStream(from)
            ?: throw IOException("Cannot read $from")
        input.use {
            val output = contentResolver.openOutputStream(to)
                ?: throw IOException("Cannot write $to")
            output.use {
                input.copyTo(output)
            }
        }
    }

    companion object {
        /** The folder inside Pictures and Movies that holds everything the app saves. */
        const val FOLDER_NAME = "Keepsake"

        /** Where MediaStore puts a saved item, relative to the storage root. */
        fun relativePath(type: MediaType): String = when (type) {
            MediaType.IMAGE -> "${Environment.DIRECTORY_PICTURES}/$FOLDER_NAME/"
            MediaType.VIDEO -> "${Environment.DIRECTORY_MOVIES}/$FOLDER_NAME/"
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        fun collection(type: MediaType): Uri = when (type) {
            MediaType.IMAGE -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            MediaType.VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
    }
}
