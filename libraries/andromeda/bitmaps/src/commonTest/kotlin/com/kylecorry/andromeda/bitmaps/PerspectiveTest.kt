package com.kylecorry.andromeda.bitmaps

import com.kylecorry.andromeda.core.units.PixelBounds
import com.kylecorry.andromeda.core.units.PixelCoordinate
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class PerspectiveTest {
    @Test
    fun rectangularBoundsPreservePixelsAndDegenerateBoundsFail() {
        val image = PixelImage(2, 2, intArrayOf(-1, -0x10000, -0xff0100, -0xffff01))
        val bounds = PixelBounds(PixelCoordinate(0f, 0f), PixelCoordinate(2f, 0f), PixelCoordinate(0f, 2f), PixelCoordinate(2f, 2f))
        assertContentEquals(image.pixels, image.correctPerspective(bounds).pixels)
        assertFailsWith<IllegalArgumentException> { image.correctPerspective(bounds.copy(bottomRight = bounds.topLeft)) }
    }
}
