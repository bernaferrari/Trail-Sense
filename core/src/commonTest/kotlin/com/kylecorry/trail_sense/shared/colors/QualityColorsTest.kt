package com.kylecorry.trail_sense.shared.colors

import com.kylecorry.andromeda.core.sensors.Quality
import kotlin.test.Test
import kotlin.test.assertEquals

class QualityColorsTest {

    @Test
    fun colorsEachQuality() {
        assertEquals(AppColor.Red.color, QualityColors.of(Quality.Poor))
        assertEquals(AppColor.Red.color, QualityColors.of(Quality.Unknown))
        assertEquals(AppColor.Yellow.color, QualityColors.of(Quality.Moderate))
        assertEquals(AppColor.Green.color, QualityColors.of(Quality.Good))
    }
}
