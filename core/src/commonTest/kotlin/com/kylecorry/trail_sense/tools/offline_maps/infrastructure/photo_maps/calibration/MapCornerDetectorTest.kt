package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration

import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PixelBounds
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapCornerDetectorTest {

    @Test
    fun detectsMapCorners() {
        val pixels = rectangle(150, 112, 22, 17, 128, 95)

        val bounds = assertNotNull(MapCornerDetector().detect(pixels, 150, 112))

        assertCorners(bounds, 22f, 17f, 128f, 95f)
    }

    @Test
    fun scalesCornersBackToTheOriginalImage() {
        val pixels = rectangle(150, 112, 22, 17, 128, 95)

        val bounds = assertNotNull(
            MapCornerDetector().detect(pixels, 150, 112, originalWidth = 300, originalHeight = 224)
        )

        assertCorners(bounds, 44f, 34f, 256f, 190f)
    }

    @Test
    fun rejectsUniformImage() {
        assertNull(MapCornerDetector().detect(IntArray(100 * 100) { WHITE }, 100, 100))
    }

    @Test
    fun rejectsTinyImage() {
        val detector = MapCornerDetector()
        assertNull(detector.detectionSize(9, 100))
        assertNull(detector.detect(IntArray(9 * 100) { WHITE }, 9, 100))
    }

    @Test
    fun detectionSizeFitsTheMaxDimension() {
        val detector = MapCornerDetector(maxDimension = 150)
        assertEquals(150 to 113, detector.detectionSize(400, 300))
        assertEquals(100 to 80, detector.detectionSize(100, 80))
    }

    private fun rectangle(width: Int, height: Int, left: Int, top: Int, right: Int, bottom: Int): IntArray {
        return IntArray(width * height) { i ->
            val x = i % width
            val y = i / width
            if (x in left..right && y in top..bottom) WHITE else BLACK
        }
    }

    private fun assertCorners(bounds: PixelBounds, left: Float, top: Float, right: Float, bottom: Float) {
        assertNear(left, top, bounds.topLeft.x, bounds.topLeft.y)
        assertNear(right, top, bounds.topRight.x, bounds.topRight.y)
        assertNear(left, bottom, bounds.bottomLeft.x, bounds.bottomLeft.y)
        assertNear(right, bottom, bounds.bottomRight.x, bounds.bottomRight.y)
    }

    private fun assertNear(expectedX: Float, expectedY: Float, actualX: Float, actualY: Float) {
        assertTrue(abs(expectedX - actualX) <= TOLERANCE, "x: expected $expectedX, was $actualX")
        assertTrue(abs(expectedY - actualY) <= TOLERANCE, "y: expected $expectedY, was $actualY")
    }

    private companion object {
        const val BLACK = 0xFF000000.toInt()
        const val WHITE = 0xFFFFFFFF.toInt()
        const val TOLERANCE = 5f
    }
}
