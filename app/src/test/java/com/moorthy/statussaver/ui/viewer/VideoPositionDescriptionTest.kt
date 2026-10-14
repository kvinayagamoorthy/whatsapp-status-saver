package com.moorthy.statussaver.ui.viewer

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class VideoPositionDescriptionTest {

    private val resources = RuntimeEnvironment.getApplication().resources

    @Test
    fun saysPositionAndLengthInWholeSeconds() {
        assertEquals("00:07 of 00:30", videoPositionDescription(resources, 7_900, 30_000))
        assertEquals("01:05 of 02:00", videoPositionDescription(resources, 65_000, 120_000))
    }

    @Test
    fun saysOnlyThePositionWhileTheLengthIsUnknown() {
        assertEquals("00:00", videoPositionDescription(resources, 0, C.TIME_UNSET))
    }
}
