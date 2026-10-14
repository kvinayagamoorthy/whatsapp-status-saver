package com.moorthy.statussaver.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.moorthy.statussaver.data.model.StatusItem

/**
 * Returns a save action that asks for the storage permission first where saving needs
 * it: Android 8-9 write into the shared Pictures and Movies folders (D-028). The dialog
 * also grants READ_EXTERNAL_STORAGE, which Android adds to the request. On Android 10+
 * the action calls [onSave] straight away.
 *
 * [onDenied] is called when the user refuses, or has refused for good and the dialog no
 * longer appears.
 */
@Composable
fun rememberSaveWithPermission(
    onSave: (StatusItem) -> Unit,
    onDenied: () -> Unit,
): (StatusItem) -> Unit {
    val context = LocalContext.current
    val currentOnSave by rememberUpdatedState(onSave)
    val currentOnDenied by rememberUpdatedState(onDenied)
    // The item waiting for the user's answer. Lost if the screen rotates while the
    // dialog is open; the user then taps Save again.
    var waitingItem by remember { mutableStateOf<StatusItem?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val item = waitingItem
        waitingItem = null
        if (!granted) {
            currentOnDenied()
        } else if (item != null) {
            currentOnSave(item)
        }
    }
    return remember(context, launcher) {
        { item ->
            val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED
            if (needsPermission) {
                waitingItem = item
                launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                currentOnSave(item)
            }
        }
    }
}
