package com.kylecorry.andromeda.bitmaps

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class QuantizationTest {
    @Test fun handlesIndependentChannelsAndDithering() {
        val image = PixelImage(2, 1, intArrayOf(0x801080f0.toInt(), 0xff1080f0.toInt()))
        assertEquals(2, image.histogram(ColorChannel.Red)[16])
        assertContentEquals(intArrayOf(0x80808080.toInt(), 0xff808080.toInt()), image.grayscale(average = true).pixels)
        assertContentEquals(intArrayOf(0xff00aaff.toInt(), 0xff00aaff.toInt()), image.quantize(2, 4, 2, 2).pixels)
        val gray = PixelImage(2, 1, intArrayOf(0xff808080.toInt(), 0xff808080.toInt()))
        assertContentEquals(intArrayOf(0xffffffff.toInt(), 0xff000000.toInt()), gray.quantize(2, 2, 2, dither = true).pixels)
    }
}
