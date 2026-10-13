package com.moorthy.statussaver.data

import com.moorthy.statussaver.data.model.StatusSourceKind

/**
 * Where WhatsApp and WhatsApp Business keep viewed statuses, and how to recognise such a
 * folder from the tree the user picked (design.md §3). Folder names live only here, so a
 * future WhatsApp move (risk R1) is a change in one file.
 */
object StatusFolders {

    private const val STATUSES_FOLDER_NAME = ".Statuses"

    // Folder names that only appear in WhatsApp Business paths: the package folder under
    // Android/media on Android 11+, and the app folder on both path styles.
    private val businessFolderNames = setOf("com.whatsapp.w4b", "WhatsApp Business")

    // The same for WhatsApp. These never match a Business path, because folder names
    // are compared whole.
    private val whatsAppFolderNames = setOf("com.whatsapp", "WhatsApp")

    /**
     * Tells which app's statuses folder a picked tree is, from its tree document id
     * (`DocumentsContract.getTreeDocumentId`), for example
     * `primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses`.
     *
     * Returns null when the tree is not a statuses folder of either app, which the
     * onboarding screen reports as a wrong folder. Any storage volume is accepted.
     */
    fun classify(treeDocumentId: String): StatusSourceKind? {
        val path = treeDocumentId.substringAfter(':')
        val folderNames = path.split('/')
        if (folderNames.last() != STATUSES_FOLDER_NAME) {
            return null
        }
        return when {
            folderNames.any { it in businessFolderNames } -> StatusSourceKind.BUSINESS
            folderNames.any { it in whatsAppFolderNames } -> StatusSourceKind.WHATSAPP
            else -> null
        }
    }
}
