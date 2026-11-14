package com.kylecorry.andromeda.bitmaps

import kotlin.test.Test
import kotlin.test.assertEquals

class BlendTest {
    @Test
    fun compositesStraightAlphaAndSupportsBitwiseXor() {
        val destination = PixelImage(1, 1, intArrayOf(0xff0000ff.toInt()))
        val source = PixelImage(1, 1, intArrayOf(0x80ff0000.toInt()))
        assertEquals(0xff80007f.toInt(), destination.blend(source).pixels[0])
        assertEquals(source.pixels[0], destination.blend(source, BlendingMode.SRC).pixels[0])
        assertEquals(source.pixels[0] xor destination.pixels[0], destination.blend(source, BlendingMode.XOR).pixels[0])
    }
}
