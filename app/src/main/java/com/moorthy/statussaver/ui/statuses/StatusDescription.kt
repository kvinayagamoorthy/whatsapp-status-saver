package com.moorthy.statussaver.ui.statuses

import android.content.res.Resources
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.moorthy.statussaver.R
import com.moorthy.statussaver.data.model.MediaType
import com.moorthy.statussaver.data.model.StatusItem

/**
 * What TalkBack says for a status (NFR6): its kind and how long ago it arrived, such as
 * "Image status, 3 hours ago". File names mean nothing to a listener, and the time is
 * what tells statuses apart, since all are less than a day old.
 */
fun statusDescription(resources: Resources, item: StatusItem, nowMillis: Long): String {
    val age = DateUtils.getRelativeTimeSpanString(
        item.lastModifiedMillis,
        nowMillis,
        DateUtils.MINUTE_IN_MILLIS,
    )
    val format = when (item.type) {
        MediaType.IMAGE -> R.string.status_description_image
        MediaType.VIDEO -> R.string.status_description_video
    }
    return resources.getString(format, age)
}

@Composable
fun statusDescription(item: StatusItem): String =
    statusDescription(LocalResources.current, item, System.currentTimeMillis())
