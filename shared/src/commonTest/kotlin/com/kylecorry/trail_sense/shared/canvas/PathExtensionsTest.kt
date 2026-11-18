package com.kylecorry.trail_sense.shared.canvas

import kotlin.test.BeforeTest
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PathExtensionsTest {
    @BeforeTest
    fun setUpGraphics() {
        prepareTestGraphics()
    }


    @Test
    fun straightSegmentsCoverEveryPoint() {
        val path = Path()
        path.drawLines(floatArrayOf(0f, 0f, 10f, 0f, 10f, 0f, 10f, 20f))
        assertEquals(Rect(0f, 0f, 10f, 20f), path.getBounds())
    }

    @Test
    fun aGapStartsANewContour() {
        val path = Path()
        path.drawLines(floatArrayOf(0f, 0f, 10f, 0f, 50f, 50f, 60f, 60f))
        assertEquals(Rect(0f, 0f, 60f, 60f), path.getBounds())
    }

    @Test
    fun smoothCurvesStayWithinTheSegmentsYRange() {
        // The control points used to take an x as a y, which flung curves far off the line
        val path = Path()
        path.drawLines(floatArrayOf(100f, 0f, 200f, 5f, 200f, 5f, 300f, 0f), smooth = true)
        val bounds = path.getBounds()
        assertTrue(bounds.top >= 0f && bounds.bottom <= 5f, "Bounds were $bounds")
        assertEquals(100f, bounds.left)
        assertEquals(300f, bounds.right)
    }
}
