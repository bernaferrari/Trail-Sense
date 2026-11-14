package com.kylecorry.andromeda.bitmaps

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ColorOperationsTest {
    @Test
    fun thresholdAndToleranceRespectChannels() {
        val image = PixelImage(2, 1, intArrayOf(0xff102030.toInt(), 0xffc08040.toInt()))
        assertContentEquals(intArrayOf(0xff002030.toInt(), 0xffff8040.toInt()), image.threshold(64f, true, ColorChannel.Red).pixels)
        assertContentEquals(intArrayOf(0xff000000.toInt(), 0xffc08040.toInt()), image.threshold(64f, false).pixels)
        assertEquals(0xff000000.toInt(), image.replaceColor(0xff102030.toInt(), 0xff000000.toInt(), interpolate = true).pixels[0])
        assertEquals(0xff000000.toInt(), image.replaceColor(0xff102031.toInt(), 0xff000000.toInt(), tolerance = 1f).pixels[0])
    }
}
