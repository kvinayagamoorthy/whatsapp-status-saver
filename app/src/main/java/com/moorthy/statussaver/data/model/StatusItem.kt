package com.moorthy.statussaver.data.model

import android.net.Uri

/**
 * One media file currently in a granted statuses folder.
 *
 * WhatsApp deletes the file about 24 hours after the status was posted, so an item can
 * vanish between listing and use.
 */
data class StatusItem(
    /** SAF document URI of the file, readable through the folder grant. */
    val uri: Uri,
    /**
     * File name as WhatsApp wrote it. This is the item's identity (design.md I3): a saved
     * copy in the gallery is matched by this name.
     */
    val name: String,
    val type: MediaType,
    /** MIME type reported by the documents provider, used when saving and sharing. */
    val mimeType: String,
    val lastModifiedMillis: Long,
    val sizeBytes: Long,
)
