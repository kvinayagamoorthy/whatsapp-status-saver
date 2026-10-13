package com.moorthy.statussaver.data

import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import com.moorthy.statussaver.data.model.StatusSourceKind

/**
 * Where WhatsApp and WhatsApp Business keep viewed statuses, and how to recognise such a
 * folder from the tree the user picked (design.md §3). Folder names live only here, so a
 * future WhatsApp move (risk R1) is a change in one file.
 */
object StatusFolders {

    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    private const val PRIMARY_VOLUME = "primary"
    private const val STATUSES_FOLDER_NAME = ".Statuses"

    private const val WHATSAPP_PACKAGE = "com.whatsapp"
    private const val WHATSAPP_APP_FOLDER = "WhatsApp"
    private const val BUSINESS_PACKAGE = "com.whatsapp.w4b"
    private const val BUSINESS_APP_FOLDER = "WhatsApp Business"

    // Folder names that only appear in WhatsApp Business paths: the package folder under
    // Android/media on Android 11+, and the app folder on both path styles.
    private val businessFolderNames = setOf(BUSINESS_PACKAGE, BUSINESS_APP_FOLDER)

    // The same for WhatsApp. These never match a Business path, because folder names
    // are compared whole.
    private val whatsAppFolderNames = setOf(WHATSAPP_PACKAGE, WHATSAPP_APP_FOLDER)

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

    /**
     * Document to open the folder picker at (`EXTRA_INITIAL_URI`), so the user only has
     * to confirm. Some pickers ignore it (risk R3); the user can still browse there.
     */
    fun initialUri(kind: StatusSourceKind): Uri =
        DocumentsContract.buildDocumentUri(
            EXTERNAL_STORAGE_AUTHORITY,
            initialDocumentId(kind, Build.VERSION.SDK_INT),
        )

    /** Folder path shown to the user for [kind], e.g. `WhatsApp/Media/.Statuses`. */
    fun expectedPath(kind: StatusSourceKind): String =
        initialDocumentId(kind, Build.VERSION.SDK_INT).substringAfter(':')

    /**
     * Document id of the statuses folder [kind] most likely uses on a phone running
     * [sdkInt]. From Android 11, scoped storage moved WhatsApp's media under
     * `Android/media/<package>/`; before that it sat at the storage root.
     */
    internal fun initialDocumentId(kind: StatusSourceKind, sdkInt: Int): String {
        val (packageName, appFolder) = when (kind) {
            StatusSourceKind.WHATSAPP -> WHATSAPP_PACKAGE to WHATSAPP_APP_FOLDER
            StatusSourceKind.BUSINESS -> BUSINESS_PACKAGE to BUSINESS_APP_FOLDER
        }
        val appRoot = if (sdkInt >= Build.VERSION_CODES.R) {
            "Android/media/$packageName/$appFolder"
        } else {
            appFolder
        }
        return "$PRIMARY_VOLUME:$appRoot/Media/$STATUSES_FOLDER_NAME"
    }
}
