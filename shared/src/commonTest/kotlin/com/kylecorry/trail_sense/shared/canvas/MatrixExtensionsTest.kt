package com.kylecorry.trail_sense.shared.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MatrixExtensionsTest {

    private fun assertMaps(matrix: Matrix, x: Float, y: Float, ex: Float, ey: Float) {
        val p = matrix.map(Offset(x, y))
        assertEquals(ex, p.x, 0.01f, "x of ($x, $y)")
        assertEquals(ey, p.y, 0.01f, "y of ($x, $y)")
    }

    @Test
    fun mapsEachCornerToItsTarget() {
        val src = floatArrayOf(0f, 0f, 256f, 0f, 0f, 256f, 256f, 256f)
        // A rotated, skewed quadrilateral with perspective
        val dst = floatArrayOf(10f, 20f, 300f, 5f, -15f, 280f, 330f, 350f)
        val matrix = Matrix()
        assertTrue(matrix.setPolyToPoly(src, dst))
        for (i in 0 until 4) {
            assertMaps(matrix, src[2 * i], src[2 * i + 1], dst[2 * i], dst[2 * i + 1])
        }
    }

    @Test
    fun anAffineTargetMapsTheCenterToTheCenter() {
        val src = floatArrayOf(0f, 0f, 10f, 0f, 0f, 10f, 10f, 10f)
        val dst = floatArrayOf(100f, 100f, 120f, 100f, 100f, 140f, 120f, 140f)
        val matrix = Matrix()
        matrix.setPolyToPoly(src, dst)
        assertMaps(matrix, 5f, 5f, 110f, 120f)
        assertEquals(2f, matrix.scaleX, 0.001f)
    }

    @Test
    fun mapsPointsInPlace() {
        val matrix = Matrix()
        matrix.setPolyToPoly(
            floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f),
            floatArrayOf(5f, 5f, 6f, 5f, 5f, 6f, 6f, 6f)
        )
        val points = floatArrayOf(0.5f, 0.5f, 1f, 0f)
        matrix.mapPoints(points)
        assertEquals(listOf(5.5f, 5.5f, 6f, 5f), points.map { kotlin.math.round(it * 100) / 100 })
    }

    @Test
    fun degenerateCornersAreRejected() {
        val matrix = Matrix()
        assertFalse(
            matrix.setPolyToPoly(
                floatArrayOf(0f, 0f, 1f, 1f, 2f, 2f, 3f, 3f),
                floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)
            )
        )
        assertMaps(matrix, 3f, 4f, 3f, 4f)
    }
}
