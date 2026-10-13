package com.moorthy.statussaver.ui.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Zoom arithmetic on a 1000 x 2000 px screen. A portrait image of the same shape fills
 * it; a square image is letterboxed with 500 px bands above and below.
 */
class ZoomStateTest {

    @Test
    fun startsFitted() {
        val state = zoomState(content = PORTRAIT)

        assertEquals(1f, state.scale, DELTA)
        assertOffset(Offset.Zero, state.offset)
        assertFalse(state.isZoomed)
    }

    @Test
    fun pinchKeepsThePointUnderTheFingersInPlace() {
        val state = zoomState(content = PORTRAIT)
        val fingers = Offset(750f, 1000f)

        state.transform(zoomChange = 2f, centroid = fingers, pan = Offset.Zero)

        assertEquals(2f, state.scale, DELTA)
        assertTrue(state.isZoomed)
        // The image point that was at x = 750 is drawn at center + offset + 2 * 250.
        assertEquals(750f, 500f + state.offset.x + 2f * 250f, DELTA)
        assertEquals(0f, state.offset.y, DELTA)
    }

    @Test
    fun zoomStaysBetweenFittedAndFourTimes() {
        val state = zoomState(content = PORTRAIT)

        state.transform(zoomChange = 10f, centroid = CENTER, pan = Offset.Zero)
        assertEquals(ZoomState.MAX_SCALE, state.scale, DELTA)

        state.transform(zoomChange = 0.01f, centroid = Offset(900f, 100f), pan = Offset.Zero)
        assertEquals(1f, state.scale, DELTA)
        assertOffset(Offset.Zero, state.offset)
    }

    @Test
    fun pinchEndingJustAboveFittedReturnsToFitted() {
        val state = zoomState(content = PORTRAIT)
        // Seen on the emulator: pinching back in left the scale at 1.000000119.
        state.transform(zoomChange = 1.000000119f, centroid = CENTER, pan = Offset.Zero)
        assertTrue(state.isZoomed)

        state.endGesture()

        assertFalse(state.isZoomed)
        assertOffset(Offset.Zero, state.offset)
    }

    @Test
    fun pinchEndingClearlyZoomedStaysZoomed() {
        val state = zoomState(content = PORTRAIT)
        state.transform(zoomChange = 1.5f, centroid = Offset(800f, 400f), pan = Offset.Zero)
        val offsetBefore = state.offset

        state.endGesture()

        assertEquals(1.5f, state.scale, DELTA)
        assertOffset(offsetBefore, state.offset)
    }

    @Test
    fun panStopsAtTheImageEdges() {
        val state = zoomState(content = PORTRAIT)
        state.transform(zoomChange = 2f, centroid = CENTER, pan = Offset.Zero)

        state.transform(zoomChange = 1f, centroid = CENTER, pan = Offset(10_000f, -10_000f))

        // At scale 2 the image is 2000 x 4000, so it can move 500 and 1000 px each way.
        assertOffset(Offset(500f, -1000f), state.offset)
    }

    @Test
    fun letterboxedImageCannotBePannedIntoItsBands() {
        val state = zoomState(content = SQUARE)
        state.transform(zoomChange = 2f, centroid = CENTER, pan = Offset.Zero)

        state.transform(zoomChange = 1f, centroid = CENTER, pan = Offset(300f, 800f))

        // At scale 2 the square is exactly as tall as the screen: no vertical room.
        assertOffset(Offset(300f, 0f), state.offset)
    }

    @Test
    fun doubleTapZoomsInAtTheTapAndAgainReturnsToFitted() {
        val state = zoomState(content = PORTRAIT)

        state.toggleZoom(Offset(750f, 1000f))
        assertEquals(ZoomState.DOUBLE_TAP_SCALE, state.scale, DELTA)
        assertEquals(750f, 500f + state.offset.x + ZoomState.DOUBLE_TAP_SCALE * 250f, DELTA)

        state.toggleZoom(Offset(100f, 100f))
        assertEquals(1f, state.scale, DELTA)
        assertOffset(Offset.Zero, state.offset)
    }

    @Test
    fun doubleTapInALetterboxBandStillShowsOnlyTheImage() {
        val state = zoomState(content = SQUARE)

        state.toggleZoom(Offset(500f, 0f))

        // The square is 2500 px tall at scale 2.5, so it may move at most 250 px.
        assertOffset(Offset(0f, 250f), state.offset)
    }

    @Test
    fun imageSizeArrivingLateLimitsAnEarlierPan() {
        val state = zoomState(content = Size.Unspecified)
        state.transform(zoomChange = 2f, centroid = CENTER, pan = Offset(0f, 800f))
        assertOffset(Offset(0f, 800f), state.offset)

        state.setContentSize(SQUARE)

        assertOffset(Offset(0f, 0f), state.offset)
    }

    // Compares by value: Offset equality tells 0.0 and -0.0 apart.
    private fun assertOffset(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, DELTA)
        assertEquals(expected.y, actual.y, DELTA)
    }

    private fun zoomState(content: Size) = ZoomState().apply {
        setContainerSize(SCREEN)
        setContentSize(content)
    }

    private companion object {
        const val DELTA = 0.001f
        val SCREEN = Size(1000f, 2000f)
        val CENTER = Offset(500f, 1000f)
        val PORTRAIT = Size(500f, 1000f)
        val SQUARE = Size(640f, 640f)
    }
}
