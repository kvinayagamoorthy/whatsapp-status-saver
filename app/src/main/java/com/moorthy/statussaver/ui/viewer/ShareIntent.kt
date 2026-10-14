package com.moorthy.statussaver.ui.viewer

import android.content.ClipData
import android.content.Intent
import com.moorthy.statussaver.data.model.StatusItem

/**
 * The system share sheet for [item] (FR6), titled [title].
 *
 * The status is shared by its own SAF document URI, with a one-off read grant for the
 * app the user picks (design.md §5). The app may pass that grant on because the URI lies
 * inside the folder the user granted Keepsake, so no copy and no `FileProvider` is
 * needed. The grant ends when the receiving app is done with the URI.
 *
 * The URI is also set as the clip data: that is what carries the grant, and the share
 * sheet itself needs it to show a preview.
 */
fun shareIntent(item: StatusItem, title: String): Intent {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = item.mimeType
        putExtra(Intent.EXTRA_STREAM, item.uri)
        clipData = ClipData.newRawUri(item.name, item.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(send, title)
}
