package com.kylecorry.trail_sense.tools.astronomy.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import kotlin.test.Test
import kotlin.test.assertEquals

class MoonPhaseImageMapperTest {

    private val mapper = MoonPhaseImageMapper()

    private fun ImageBitmap.at(x: Int, y: Int): Color = toPixelMap()[x, y]

    @Test
    fun fullMoonIsLit() {
        val image = mapper.getPhaseImage(180f, 40, 40, plain = true)
        assertEquals(Color.White, image.at(20, 20))
        assertEquals(Color.White, image.at(8, 20))
        assertEquals(Color.White, image.at(32, 20))
    }

    @Test
    fun newMoonIsInShadow() {
        val image = mapper.getPhaseImage(0f, 40, 40, plain = true)
        assertEquals(Color.Black, image.at(20, 20))
        assertEquals(Color.Black, image.at(8, 20))
    }

    @Test
    fun firstQuarterIsLitOnTheRight() {
        val image = mapper.getPhaseImage(90f, 40, 40, plain = true)
        assertEquals(Color.Black, image.at(8, 20))
        assertEquals(Color.White, image.at(32, 20))
    }

    @Test
    fun lastQuarterIsLitOnTheLeft() {
        val image = mapper.getPhaseImage(270f, 40, 40, plain = true)
        assertEquals(Color.White, image.at(8, 20))
        assertEquals(Color.Black, image.at(32, 20))
    }

    @Test
    fun cornersOutsideTheDiscStayClear() {
        val image = mapper.getPhaseImage(180f, 40, 40, plain = true)
        assertEquals(0f, image.at(0, 0).alpha)
    }
}
