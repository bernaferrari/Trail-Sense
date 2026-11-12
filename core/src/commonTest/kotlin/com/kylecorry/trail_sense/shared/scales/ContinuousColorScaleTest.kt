package com.kylecorry.trail_sense.shared.scales

import com.kylecorry.trail_sense.shared.colors.ColorInts
import kotlin.test.Test
import kotlin.test.assertEquals

class ContinuousColorScaleTest {

    @Test
    fun blendsFromStartToEnd() {
        val scale = ContinuousColorScale(ColorInts.Black, ColorInts.White)
        assertEquals(ColorInts.Black, scale.getColor(0f))
        assertEquals(ColorInts.White, scale.getColor(1f))
        assertEquals(ColorInts.rgb(63, 63, 63), scale.getColor(0.25f))
    }
}
