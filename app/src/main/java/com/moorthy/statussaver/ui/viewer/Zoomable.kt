package com.moorthy.statussaver.ui.viewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import kotlin.math.max
import kotlin.math.min

/**
 * Zoom and pan of one image in the viewer (FR3): pinch to zoom around the fingers,
 * double-tap to zoom in at the tap or back out. The image is shown fitted (scale 1) and
 * can be panned only as far as its edges, so no empty space is pulled into view beyond
 * the letterbox it already has.
 *
 * [offset] is the image's shift from the center of the container, in pixels.
 */
class ZoomState {

    var scale by mutableFloatStateOf(1f)
        private set

    var offset by mutableStateOf(Offset.Zero)
        private set

    /** True while the image is zoomed in; swipes then pan it instead of changing page. */
    val isZoomed: Boolean
        get() = scale > 1f

    private var containerSize = Size.Zero

    /** Size of the image itself, which only matters for its aspect ratio. */
    private var contentSize = Size.Unspecified

    fun setContainerSize(size: Size) {
        containerSize = size
        offset = clamp(offset, scale)
    }

    fun setContentSize(size: Size) {
        contentSize = size
        offset = clamp(offset, scale)
    }

    /**
     * Applies one step of a pinch or drag: zooms by [zoomChange] keeping the point under
     * [centroid] (container coordinates) in place, then moves by [pan].
     */
    fun transform(zoomChange: Float, centroid: Offset, pan: Offset) {
        val newScale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
        val fromCenter = centroid - containerCenter()
        val appliedZoom = newScale / scale
        val zoomedOffset = fromCenter - (fromCenter - offset) * appliedZoom
        scale = newScale
        offset = clamp(zoomedOffset + pan, newScale)
    }

    /**
     * Ends a pinch or drag. A pinch rarely lands exactly back on scale 1 (the per-event
     * zoom factors multiply to values such as 1.0000001), and anything above 1 counts as
     * zoomed and blocks paging, so a scale this close to fitted becomes fitted.
     */
    fun endGesture() {
        if (scale < SNAP_TO_FITTED_BELOW) {
            scale = MIN_SCALE
            offset = Offset.Zero
        }
    }

    /** Double-tap: zooms in at [tap] when fitted, otherwise returns to fitted. */
    fun toggleZoom(tap: Offset) {
        if (isZoomed) {
            scale = MIN_SCALE
            offset = Offset.Zero
        } else {
            val fromCenter = tap - containerCenter()
            scale = DOUBLE_TAP_SCALE
            offset = clamp(fromCenter * (1f - DOUBLE_TAP_SCALE), DOUBLE_TAP_SCALE)
        }
    }

    private fun containerCenter() = Offset(containerSize.width / 2f, containerSize.height / 2f)

    /** Keeps the image's edges at or beyond the container's edges on each axis. */
    private fun clamp(offset: Offset, scale: Float): Offset {
        val fitted = fittedSize()
        val maxX = max(0f, (fitted.width * scale - containerSize.width) / 2f)
        val maxY = max(0f, (fitted.height * scale - containerSize.height) / 2f)
        return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
    }

    /** The image's size at scale 1, fitted inside the container (ContentScale.Fit). */
    private fun fittedSize(): Size {
        if (contentSize.isUnspecified || contentSize.width <= 0f || contentSize.height <= 0f) {
            return containerSize
        }
        val fit = min(
            containerSize.width / contentSize.width,
            containerSize.height / contentSize.height,
        )
        return Size(contentSize.width * fit, contentSize.height * fit)
    }

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 4f
        const val DOUBLE_TAP_SCALE = 2.5f
        const val SNAP_TO_FITTED_BELOW = 1.05f
    }
}

/**
 * Applies [state] to this element and feeds it the user's gestures. One-finger drags
 * are left to the pager while the image is fitted, so swiping still changes page; while
 * zoomed they pan the image and the pager stays put (D-024).
 */
fun Modifier.zoomable(state: ZoomState): Modifier = this
    .onSizeChanged { size -> state.setContainerSize(size.toSize()) }
    .pointerInput(state) {
        detectTapGestures(onDoubleTap = { tap -> state.toggleZoom(tap) })
    }
    .pointerInput(state) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                val fingers = event.changes.count { change -> change.pressed }
                // No centroid once the last finger has lifted.
                val centroid = event.calculateCentroid(useCurrent = true)
                if ((fingers > 1 || state.isZoomed) && centroid.isSpecified) {
                    state.transform(
                        zoomChange = event.calculateZoom(),
                        centroid = centroid,
                        pan = event.calculatePan(),
                    )
                    // Consumed moves are ignored by the pager's own drag handling.
                    event.changes.forEach { change ->
                        if (change.positionChanged()) {
                            change.consume()
                        }
                    }
                }
            } while (event.changes.any { change -> change.pressed })
            state.endGesture()
        }
    }
    .graphicsLayer {
        scaleX = state.scale
        scaleY = state.scale
        translationX = state.offset.x
        translationY = state.offset.y
    }
