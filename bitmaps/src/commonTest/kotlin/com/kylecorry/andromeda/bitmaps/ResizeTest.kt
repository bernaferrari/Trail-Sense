package com.kylecorry.andromeda.bitmaps

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ResizeTest {
    @Test
    fun supportsNearestAndSmoothInterpolation() {
        val image = PixelImage(2, 1, intArrayOf(0xff000000.toInt(), 0xffffffff.toInt()))
        assertContentEquals(intArrayOf(0xff000000.toInt(), 0xff000000.toInt(), 0xffffffff.toInt()), image.resize(3, 1).pixels)
        assertEquals(0xff7f7f7f.toInt(), image.resize(3, 1, ImageInterpolation.Bilinear).pixels[1])
        assertEquals(0xff808080.toInt(), image.resize(3, 1, ImageInterpolation.Bicubic).pixels[1])
        assertContentEquals(image.pixels, image.resize(2, 1, ImageInterpolation.Bicubic).pixels)
    }
}
