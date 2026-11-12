package com.kylecorry.trail_sense.tools.map.ui

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapCameraTest {

    private fun camera(): MapCamera {
        return MapCamera(density = 2f).apply {
            setSize(1000, 2000)
            center = Coordinate(42.0, -72.0)
            resolutionPixels = 10f
        }
    }

    @Test
    fun centerIsInTheMiddleOfTheCanvas() {
        val camera = camera()
        val pixel = camera.toPixel(camera.center)
        assertEquals(500f, pixel.x, 0.01f)
        assertEquals(1000f, pixel.y, 0.01f)
    }

    @Test
    fun pixelsAndCoordinatesRoundTrip() {
        val camera = camera()
        val coordinate = camera.toCoordinate(PixelCoordinate(120f, 1800f))
        val pixel = camera.toPixel(coordinate)
        assertEquals(120f, pixel.x, 0.1f)
        assertEquals(1800f, pixel.y, 0.1f)
    }

    @Test
    fun resolutionIsInDensityPixels() {
        val camera = camera()
        assertEquals(10f, camera.resolutionPixels, 0.001f)
        assertEquals(20f, camera.resolution, 0.001f)
    }

    @Test
    fun zoomingKeepsTheFocusInPlace() {
        val camera = camera()
        val focus = PixelCoordinate(200f, 300f)
        val before = camera.toCoordinate(focus)

        camera.zoomWithFocus(2f, focus)

        assertEquals(5f, camera.resolutionPixels, 0.01f)
        val after = camera.toPixel(before)
        assertEquals(focus.x, after.x, 0.5f)
        assertEquals(focus.y, after.y, 0.5f)
    }

    @Test
    fun panningFollowsTheFinger() {
        val camera = camera()
        val start = camera.center
        // Dragging content to the right looks further west; dragging it down looks further north.
        camera.pan(100f, 100f)
        assertTrue(camera.center.longitude < start.longitude)
        assertTrue(camera.center.latitude > start.latitude)
    }

    @Test
    fun panningFollowsTheFingerOnARotatedMap() {
        val camera = camera()
        camera.azimuth = 90f
        val start = camera.center
        // Facing east, a finger moving down the screen drags the map toward the west edge of the view.
        camera.pan(0f, 100f)
        assertTrue(camera.center.longitude > start.longitude)
        assertEquals(start.latitude, camera.center.latitude, 0.0001)
    }

    @Test
    fun unrotatedTurnsScreenPixelsBackOntoTheMap() {
        val camera = camera()
        camera.azimuth = 90f
        val pixel = camera.unrotated(PixelCoordinate(500f, 900f))
        assertEquals(600f, pixel.x, 0.01f)
        assertEquals(1000f, pixel.y, 0.01f)
    }

    @Test
    fun theCenterStaysInsideTheConstraints() {
        val camera = camera()
        camera.constraintBounds = CoordinateBounds(43.0, -71.0, 41.0, -73.0)
        camera.center = Coordinate(50.0, -60.0)
        assertEquals(Coordinate(43.0, -71.0), camera.center)
    }

    @Test
    fun fittingShowsTheWholeArea() {
        val camera = camera()
        val bounds = CoordinateBounds(42.1, -71.9, 41.9, -72.1)
        camera.fitIntoView(bounds, paddingFactor = 1f)
        listOf(bounds.northEast, bounds.southWest, bounds.northWest, bounds.southEast).forEach {
            val pixel = camera.toPixel(it)
            assertTrue(pixel.x in -1f..1001f && pixel.y in -1f..2001f, "$it at $pixel")
        }
        // And fills it: one side touches the canvas edges
        val west = camera.toPixel(bounds.northWest).x
        val east = camera.toPixel(bounds.northEast).x
        assertEquals(1000f, east - west, 5f)
    }

    @Test
    fun scaleChangesAreReported() {
        val camera = camera()
        var reported: Float? = null
        camera.onScaleChange = { reported = it }
        camera.zoom(2f)
        assertEquals(5f, reported!!, 0.01f)
    }
}
