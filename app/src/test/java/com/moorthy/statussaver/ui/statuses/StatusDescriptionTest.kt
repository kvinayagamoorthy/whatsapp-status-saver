package com.moorthy.statussaver.ui.statuses

import android.net.Uri
import com.moorthy.statussaver.data.model.MediaType
import com.moorthy.statussaver.data.model.StatusItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StatusDescriptionTest {

    private val resources = RuntimeEnvironment.getApplication().resources

    @Test
    fun saysTheKindAndHowLongAgo() {
        val image = item(MediaType.IMAGE, arrivedMillis = NOW - 3 * HOUR)
        val video = item(MediaType.VIDEO, arrivedMillis = NOW - 5 * MINUTE)

        assertEquals("Image status, 3 hours ago", statusDescription(resources, image, NOW))
        assertEquals("Video status, 5 minutes ago", statusDescription(resources, video, NOW))
    }

    private fun item(type: MediaType, arrivedMillis: Long) = StatusItem(
        uri = Uri.parse("content://test/$arrivedMillis"),
        name = "status-$arrivedMillis",
        type = type,
        mimeType = if (type == MediaType.IMAGE) "image/jpeg" else "video/mp4",
        lastModifiedMillis = arrivedMillis,
        sizeBytes = 1,
    )

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
        const val NOW = 1_800_000_000_000L
    }
}
