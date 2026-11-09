package com.kylecorry.andromeda.bitmaps

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FloatImageTest {
    @Test
    fun interpolatesAndFallsBackForMissingData() {
        val pixels = floatArrayOf(0f, 2f, 4f, 6f)
        val resized = interpolateFloatImage(pixels, 2, 2, 1, 3, 3)
        assertEquals(3f, resized[4])
        assertEquals(6f, resized[8])
        val missing = interpolateFloatImage(floatArrayOf(Float.NaN, 2f, Float.NaN, Float.NaN), 2, 2, 1, 1, 1)
        assertContentEquals(floatArrayOf(2f), missing)
        assertTrue(interpolateFloatImage(floatArrayOf(Float.NaN), 1, 1, 1, 1, 1)[0].isNaN())
    }

    @Test
    fun countsWeightedChannels() {
        val image = PixelImage(2, 1, intArrayOf(0xffff0000.toInt(), 0xff0000ff.toInt()))
        val histogram = image.histogramDot(red = 1f, green = 0f, blue = 0f)
        assertEquals(1, histogram[0])
        assertEquals(1, histogram[255])
    }
}
