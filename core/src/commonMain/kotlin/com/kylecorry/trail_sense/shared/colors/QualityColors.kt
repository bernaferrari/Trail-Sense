package com.kylecorry.trail_sense.shared.colors

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.core.sensors.Quality

/** The color a sensor or signal quality is drawn in: red when poor or unknown, yellow when moderate, green when good. */
object QualityColors {
    @ColorInt
    fun of(quality: Quality): Int {
        return when (quality) {
            Quality.Poor, Quality.Unknown -> AppColor.Red.color
            Quality.Moderate -> AppColor.Yellow.color
            Quality.Good -> AppColor.Green.color
        }
    }
}
