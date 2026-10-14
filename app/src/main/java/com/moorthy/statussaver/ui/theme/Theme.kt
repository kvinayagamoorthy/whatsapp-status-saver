package com.moorthy.statussaver.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.moorthy.statussaver.data.model.ThemeMode

private val KeepsakeLightColorScheme = lightColorScheme(
    primary = TerracottaLightPrimary,
    onPrimary = TerracottaLightOnPrimary,
    primaryContainer = TerracottaLightPrimaryContainer,
    onPrimaryContainer = TerracottaLightOnPrimaryContainer,
    secondary = TerracottaLightSecondary,
    onSecondary = TerracottaLightOnSecondary,
    secondaryContainer = TerracottaLightSecondaryContainer,
    onSecondaryContainer = TerracottaLightOnSecondaryContainer,
    tertiary = TerracottaLightTertiary,
    onTertiary = TerracottaLightOnTertiary,
    background = TerracottaLightBackground,
    onBackground = TerracottaLightOnBackground,
    surface = TerracottaLightSurface,
    onSurface = TerracottaLightOnSurface,
)

private val KeepsakeDarkColorScheme = darkColorScheme(
    primary = TerracottaDarkPrimary,
    onPrimary = TerracottaDarkOnPrimary,
    primaryContainer = TerracottaDarkPrimaryContainer,
    onPrimaryContainer = TerracottaDarkOnPrimaryContainer,
    secondary = TerracottaDarkSecondary,
    onSecondary = TerracottaDarkOnSecondary,
    secondaryContainer = TerracottaDarkSecondaryContainer,
    onSecondaryContainer = TerracottaDarkOnSecondaryContainer,
    tertiary = TerracottaDarkTertiary,
    onTertiary = TerracottaDarkOnTertiary,
    background = TerracottaDarkBackground,
    onBackground = TerracottaDarkOnBackground,
    surface = TerracottaDarkSurface,
    onSurface = TerracottaDarkOnSurface,
)

/** Whether [mode] makes the app dark, given whether the phone is in dark mode now. */
fun isDarkTheme(mode: ThemeMode, systemIsDark: Boolean): Boolean = when (mode) {
    ThemeMode.SYSTEM -> systemIsDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * App-wide Material 3 theme, dark when [darkTheme] is true.
 *
 * Light or dark follows the system setting unless the user chose otherwise in settings
 * (FR7, `MainActivity`). On Android 12+ colors come from the user's wallpaper (dynamic
 * color); older versions use the Keepsake brand palette.
 */
@Composable
fun KeepsakeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) KeepsakeDarkColorScheme else KeepsakeLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
