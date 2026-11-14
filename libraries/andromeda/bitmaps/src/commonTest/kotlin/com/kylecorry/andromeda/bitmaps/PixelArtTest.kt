package com.kylecorry.andromeda.bitmaps

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PixelArtTest {
    @Test
    fun doublesSizeAndRetainsThePalette() {
        val image = PixelImage(3, 2, intArrayOf(-1, -1, -0x1000000, -1, -0x1000000, -0x1000000))
        val scaled = image.upscalePixelArt2x()
        assertEquals(6, scaled.width)
        assertEquals(4, scaled.height)
        assertTrue(scaled.pixels.all { it in image.pixels })
        assertTrue(PixelImage(1, 1, intArrayOf(0xff123456.toInt())).upscalePixelArt2x().pixels.all { it == 0xff123456.toInt() })
    }
}
