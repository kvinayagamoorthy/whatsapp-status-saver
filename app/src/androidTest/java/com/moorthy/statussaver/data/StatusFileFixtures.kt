package com.moorthy.statussaver.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.moorthy.statussaver.data.model.MediaType
import com.moorthy.statussaver.data.model.StatusItem
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Writes a status file into the app's cache and returns it as a [StatusItem] read
 * through a `file://` URI. Saving only needs a readable URI, and SAF folder grants
 * cannot be set up from a test.
 */
fun statusFile(context: Context, name: String, type: MediaType, mimeType: String, bytes: ByteArray): StatusItem {
    val file = File(context.cacheDir, name)
    file.writeBytes(bytes)
    return StatusItem(
        uri = Uri.fromFile(file),
        name = name,
        type = type,
        mimeType = mimeType,
        lastModifiedMillis = file.lastModified(),
        sizeBytes = file.length(),
    )
}

/** A small valid JPEG, so the media scanner and MediaStore see a real image. */
fun jpegBytes(): ByteArray {
    val bitmap = Bitmap.createBitmap(64, 128, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.rgb(200, 90, 60))
    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
    return output.toByteArray()
}

/**
 * A one-second MP4 from the test APK's assets. It has to be a real video: the media
 * scanner on Android 8-9 leaves anything it cannot parse out of the video collection.
 */
fun videoBytes(): ByteArray =
    InstrumentationRegistry.getInstrumentation().context.assets
        .open("status_video.mp4")
        .use { input -> input.readBytes() }
