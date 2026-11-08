package com.kylecorry.andromeda.bitmaps

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class RotateTest {
    @Test
    fun rotatesRectangularImageClockwise() {
        val image = PixelImage(2, 3, intArrayOf(1, 2, 3, 4, 5, 6))
        val result = image.rotate(90f)
        assertEquals(3, result.width)
        assertEquals(2, result.height)
        assertContentEquals(intArrayOf(5, 3, 1, 6, 4, 2), result.pixels)
        assertContentEquals(image.pixels, image.rotate(360f).pixels)
    }
}
