package com.kylecorry.sol.math.geometry

import kotlin.test.Test
import kotlin.test.assertEquals

internal class SizeTest {

    @Test
    fun getSize() {
        // Right angles
        getSize(100f, 200f, 0f, 100f, 200f)
        getSize(100f, 200f, 90f, 200f, 100f)
        getSize(100f, 200f, 180f, 100f, 200f)
        getSize(100f, 200f, 270f, 200f, 100f)
        // 45 degrees
        getSize(100f, 200f, 45f, 212.1320317678f, 212.1320317678f)
        getSize(100f, 200f, 135f, 212.1320317678f, 212.1320317678f)
        getSize(100f, 200f, 225f, 212.1320317678f, 212.1320317678f)
        getSize(100f, 200f, 315f, 212.1320317678f, 212.1320317678f)
        // 30 degrees
        getSize(100f, 200f, 30f, 186.6025433849f, 223.20508043f)
        getSize(100f, 200f, 60f, 223.20508043f, 186.6025433849f)
        getSize(100f, 200f, 120f, 223.20508043f, 186.6025433849f)
        getSize(100f, 200f, 150f, 186.6025433849f, 223.20508043f)
        getSize(100f, 200f, 210f, 186.6025433849f, 223.20508043f)
        getSize(100f, 200f, 240f, 223.20508043f, 186.6025433849f)
        getSize(100f, 200f, 300f, 223.20508043f, 186.6025433849f)
        getSize(100f, 200f, 330f, 186.6025433849f, 223.20508043f)
    }

    private fun getSize(
        width: Float,
        height: Float,
        rotation: Float,
        expectedWidth: Float,
        expectedHeight: Float
    ) {
        val size = Size(width, height).rotate(rotation)
        assertEquals(expectedWidth, size.width, 0.0001f)
        assertEquals(expectedHeight, size.height, 0.0001f)
    }
    
}