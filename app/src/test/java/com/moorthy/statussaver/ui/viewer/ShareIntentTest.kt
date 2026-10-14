package com.moorthy.statussaver.ui.viewer

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import com.moorthy.statussaver.data.model.MediaType
import com.moorthy.statussaver.data.model.StatusItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShareIntentTest {

    private val video = StatusItem(
        uri = Uri.parse(
            "content://com.android.externalstorage.documents/tree/primary%3AWhatsApp/document/" +
                "primary%3AWhatsApp%2FMedia%2F.Statuses%2FVID-1.mp4",
        ),
        name = "VID-1.mp4",
        type = MediaType.VIDEO,
        mimeType = "video/mp4",
        lastModifiedMillis = 0,
        sizeBytes = 1,
    )

    @Test
    fun opensTheShareSheetWithTheGivenTitle() {
        val chooser = shareIntent(video, "Share")

        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertEquals("Share", chooser.getCharSequenceExtra(Intent.EXTRA_TITLE))
    }

    @Test
    fun sendsTheStatusItselfWithItsType() {
        val send = sentIntent(shareIntent(video, "Share"))

        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("video/mp4", send.type)
        assertEquals(video.uri, IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri::class.java))
    }

    /** Without the grant on the clip data the receiving app cannot open the URI. */
    @Test
    fun grantsTheReceivingAppReadAccessToTheStatus() {
        val chooser = shareIntent(video, "Share")
        val send = sentIntent(chooser)

        for (intent in listOf(chooser, send)) {
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertNotNull(intent.clipData)
            val clip = intent.clipData!!
            assertEquals(video.uri, clip.getItemAt(0).uri)
        }
    }

    private fun sentIntent(chooser: Intent): Intent =
        IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent::class.java)!!
}
