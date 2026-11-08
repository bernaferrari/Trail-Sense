package com.kylecorry.andromeda.bitmaps

import com.kylecorry.sol.science.geology.CoordinateBounds
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ScaleAndTilesTest {
    @Test
    fun samplesGeographicTilesAcrossTheAntimeridian() {
        val image = PixelImage(4, 1, intArrayOf(1, 2, 3, 4))
        val imageBounds = CoordinateBounds(north = 10.0, east = -170.0, south = 0.0, west = 170.0)
        val tileBounds = CoordinateBounds(north = 10.0, east = -170.0, south = 0.0, west = 180.0)
        assertContentEquals(intArrayOf(3, 4), image.cropTile(imageBounds, tileBounds, 2, 1, ImageInterpolation.Nearest).pixels)
        assertEquals(2, image.resizeToFit(2, 3).width)
        assertEquals(1, image.resizeToFit(2, 3).height)
    }
}
