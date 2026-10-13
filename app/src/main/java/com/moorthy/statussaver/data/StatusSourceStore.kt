package com.moorthy.statussaver.data

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.moorthy.statussaver.data.model.StatusSourceKind
import kotlinx.coroutines.flow.first

/** One granted statuses folder, identified by the SAF tree URI the user picked. */
data class StatusSource(
    val kind: StatusSourceKind,
    val treeUri: Uri,
)

/**
 * Remembers which statuses folder the user granted for each [StatusSourceKind].
 *
 * The stored URI alone is not enough: the system can drop a grant (for example when the
 * folder's owner app is reinstalled), so a source only counts while the system still
 * holds a persisted read permission for its tree (design.md §3, step 6).
 */
class StatusSourceStore(
    private val dataStore: DataStore<Preferences>,
    private val contentResolver: ContentResolver,
) {

    /** Sources that are stored and still readable, in [StatusSourceKind] order. */
    suspend fun validSources(): List<StatusSource> {
        val preferences = dataStore.data.first()
        val grantedTrees = grantedTreeUris()
        return StatusSourceKind.entries.mapNotNull { kind ->
            val storedUri = preferences[treeUriKey(kind)] ?: return@mapNotNull null
            val treeUri = storedUri.toUri()
            if (treeUri in grantedTrees) {
                StatusSource(kind, treeUri)
            } else {
                null
            }
        }
    }

    /**
     * Keeps read access to [treeUri] across restarts and records it as the folder for
     * [kind]. The caller must have validated the tree first (T1.3).
     *
     * A folder picked earlier for the same kind is replaced and its grant released, so
     * re-picking does not pile up persisted permissions.
     */
    suspend fun grant(kind: StatusSourceKind, treeUri: Uri) {
        contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        val previousUri = dataStore.data.first()[treeUriKey(kind)]?.toUri()
        dataStore.edit { preferences ->
            preferences[treeUriKey(kind)] = treeUri.toString()
        }
        // Releasing a grant the system no longer holds throws, so check it first.
        if (previousUri != null && previousUri != treeUri && previousUri in grantedTreeUris()) {
            contentResolver.releasePersistableUriPermission(
                previousUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    private fun grantedTreeUris(): Set<Uri> =
        contentResolver.persistedUriPermissions
            .filter { permission -> permission.isReadPermission }
            .map { permission -> permission.uri }
            .toSet()

    // The key includes the enum name, so renaming a StatusSourceKind entry loses the
    // stored folder for that kind and sends the user back to onboarding.
    private fun treeUriKey(kind: StatusSourceKind): Preferences.Key<String> =
        stringPreferencesKey("tree_uri_${kind.name}")
}
