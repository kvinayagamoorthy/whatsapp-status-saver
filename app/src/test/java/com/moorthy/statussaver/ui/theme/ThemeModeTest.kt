package com.moorthy.statussaver.ui.theme

import com.moorthy.statussaver.data.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun systemFollowsThePhone() {
        assertEquals(true, isDarkTheme(ThemeMode.SYSTEM, systemIsDark = true))
        assertEquals(false, isDarkTheme(ThemeMode.SYSTEM, systemIsDark = false))
    }

    @Test
    fun lightAndDarkIgnoreThePhone() {
        for (systemIsDark in listOf(true, false)) {
            assertEquals(false, isDarkTheme(ThemeMode.LIGHT, systemIsDark))
            assertEquals(true, isDarkTheme(ThemeMode.DARK, systemIsDark))
        }
    }
}
