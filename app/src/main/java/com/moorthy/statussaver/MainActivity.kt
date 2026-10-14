package com.moorthy.statussaver

import android.app.UiModeManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moorthy.statussaver.data.model.ThemeMode
import com.moorthy.statussaver.ui.KeepsakeNavHost
import com.moorthy.statussaver.ui.theme.KeepsakeTheme
import com.moorthy.statussaver.ui.theme.isDarkTheme

/** Single activity that hosts all Compose screens. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the system bars. Their icon colours are set again below once the
        // theme mode is known.
        enableEdgeToEdge()
        val settingsStore = (application as StatusSaverApp).settingsStore
        setContent {
            // Nothing is drawn for the few milliseconds the stored choice takes to read,
            // so the app never shows the wrong theme first (FR7).
            val themeMode: ThemeMode? by settingsStore.themeMode
                .collectAsStateWithLifecycle(initialValue = null)
            val resolvedThemeMode = themeMode ?: return@setContent
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                LaunchedEffect(resolvedThemeMode) {
                    tellSystemNightMode(resolvedThemeMode)
                }
            }
            val darkTheme = isDarkTheme(resolvedThemeMode, systemIsDark = isSystemInDarkTheme())
            // Light icons on a dark app and dark icons on a light one, also when the
            // choice differs from the phone's setting.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_NAVIGATION_SCRIM, DARK_NAVIGATION_SCRIM) { darkTheme },
                )
                onDispose {}
            }
            KeepsakeTheme(darkTheme = darkTheme) {
                KeepsakeNavHost()
            }
        }
    }

    /**
     * Android 12+ draws the launch screen before the app has read the choice. Telling the
     * system the app's night mode makes it draw that screen in the chosen theme at the
     * next start, instead of in the phone's (a white flash before a dark app). The system
     * keeps the value itself, and the app's configuration then reports the chosen mode,
     * which [isDarkTheme] reads the same way. Below Android 12 the launch window still
     * follows the phone.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun tellSystemNightMode(mode: ThemeMode) {
        val nightMode = when (mode) {
            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
        }
        getSystemService(UiModeManager::class.java).setApplicationNightMode(nightMode)
    }

    private companion object {
        // The scrims enableEdgeToEdge() uses by default behind three-button navigation.
        val LIGHT_NAVIGATION_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_NAVIGATION_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
