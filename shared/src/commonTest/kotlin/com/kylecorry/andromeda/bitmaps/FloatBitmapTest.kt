package com.kylecorry.andromeda.bitmaps

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FloatBitmapTest {
    @Test
    fun interpolatesAndFallsBackForMissingPixels() {
        val plane = FloatBitmap(4, 4, 1)
        for (y in 0..3) for (x in 0..3) plane.set(x, y, 0, y * 10f + x)
        assertEquals(16.5f, plane.upscale(7, 7).get(3, 3, 0), 0.0001f)
        val small = FloatBitmap(2, 2, 1)
        small.set(0, 0, 0, 0f)
        small.set(1, 0, 0, 1f)
        small.set(0, 1, 0, 10f)
        small.set(1, 1, 0, 11f)
        assertEquals(5.5f, small.upscale(3, 3).get(1, 1, 0), 0.0001f)
        small.set(0, 0, 0, Float.NaN)
        assertEquals(10f, small.upscale(1, 1, startX = 0f, startY = 0f).get(0, 0, 0))
        for (i in small.data.indices) small.data[i] = Float.NaN
        assertTrue(small.upscale(1, 1).get(0, 0, 0).isNaN())
    }
}
