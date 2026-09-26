package com.crownos.connect.util

import com.crownos.connect.protocol.Edge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryTest {
    private val width = 1080
    private val height = 2400

    @Test
    fun theCursorEntersJustInsideTheFacingEdge() {
        val point = EdgeGeometry.entryPoint(Edge.Right.facing, EdgeGeometry.fractionOf(65_535), width, height)
        assertEquals(1080 * 0.005f, point.x, 0.01f)
        assertEquals(height - 1f, point.y, 0.01f)
        assertEquals(Edge.Left, Edge.Right.facing)
        assertEquals(Edge.Bottom, Edge.Top.facing)
    }

    @Test
    fun crossingOnlyCountsForTheEntryEdge() {
        assertTrue(EdgeGeometry.crossed(Edge.Left, ScreenPoint(-1f, 400f), width, height))
        assertFalse(EdgeGeometry.crossed(Edge.Left, ScreenPoint(width + 5f, 400f), width, height))
        assertTrue(EdgeGeometry.crossed(Edge.Bottom, ScreenPoint(10f, height.toFloat()), width, height))
    }

    @Test
    fun positionsRoundTripAlongAnEdge() {
        val fraction = EdgeGeometry.fractionAlong(Edge.Left, ScreenPoint(0f, (height - 1) / 2f), width, height)
        assertEquals(32_768, EdgeGeometry.positionOf(fraction))
        assertEquals(0, EdgeGeometry.positionOf(-3f))
        assertEquals(65_535, EdgeGeometry.positionOf(4f))
    }

    @Test
    fun letterboxedVideoMapsBackOntoTheScreen() {
        val box = Letterbox.fit(width, height, 1920, 1080)
        assertEquals(1080f, box.height, 0.01f)
        assertEquals(486f, box.width, 0.01f)
        val centre = box.toScreen(0.5f, 0.5f, 1920, 1080)
        assertEquals(0.5f, centre.x, 0.001f)
        assertEquals(0.5f, centre.y, 0.001f)
        val leftBar = box.toScreen(0.1f, 0.5f, 1920, 1080)
        assertEquals(0f, leftBar.x, 0.001f)
    }
}
