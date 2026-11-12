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
}
