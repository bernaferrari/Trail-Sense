package com.kylecorry.trail_sense.shared.colors

import kotlin.test.Test
import kotlin.test.assertEquals

class ColorIntsTest {

    @Test
    fun packsChannelsAsArgb() {
        assertEquals(0xFF698599.toInt(), ColorInts.rgb(0x69, 0x85, 0x99))
        assertEquals(0x80FF0000.toInt(), ColorInts.argb(0x80, 255, 0, 0))
        assertEquals(ColorInts.White, ColorInts.rgb(255, 255, 255))
        assertEquals(ColorInts.Black, ColorInts.rgb(0, 0, 0))
        assertEquals(ColorInts.Gray, ColorInts.rgb(0x88, 0x88, 0x88))
    }

    @Test
    fun blendsEachChannelLikeColorUtils() {
        val red = ColorInts.rgb(255, 0, 0)
        val green = ColorInts.rgb(0, 255, 0)
        assertEquals(red, ColorInts.blend(red, green, 0f))
        assertEquals(green, ColorInts.blend(red, green, 1f))
        // Each channel is truncated, so half of 255 is 127.
        assertEquals(0xFF7F7F7F.toInt(), ColorInts.blend(ColorInts.Black, ColorInts.White, 0.5f))
        // Alpha mixes too: a quarter of 255 is 63.
        assertEquals(ColorInts.argb(63, 63, 0, 0), ColorInts.blend(red, ColorInts.Transparent, 0.75f))
    }
}
