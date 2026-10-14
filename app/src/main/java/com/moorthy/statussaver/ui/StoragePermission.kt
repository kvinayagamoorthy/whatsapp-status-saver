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

/**
 * Returns a function that runs a save action once saving is allowed: Android 8-9 write
 * into the shared Pictures and Movies folders, which needs the storage permission
 * (D-028), so the action waits for the user's answer to the permission dialog there.
 * The dialog also grants READ_EXTERNAL_STORAGE, which Android adds to the request. On
 * Android 10+ the action runs straight away.
 *
 * [onDenied] is called when the user refuses, or has refused for good and the dialog no
 * longer appears.
 */
@Composable
fun rememberStoragePermissionGate(onDenied: () -> Unit): (saveAction: () -> Unit) -> Unit {
    val context = LocalContext.current
    val currentOnDenied by rememberUpdatedState(onDenied)
    // The action waiting for the user's answer. Lost if the screen rotates while the
    // dialog is open; the user then taps Save again.
    var waitingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val action = waitingAction
        waitingAction = null
        if (!granted) {
            currentOnDenied()
        } else if (action != null) {
            action()
        }
    }
    return remember(context, launcher) {
        { saveAction ->
            val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED
            if (needsPermission) {
                waitingAction = saveAction
                launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                saveAction()
            }
        }
    }
}
