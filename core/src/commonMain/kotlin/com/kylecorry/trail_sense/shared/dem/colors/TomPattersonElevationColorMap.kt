package com.kylecorry.trail_sense.shared.dem.colors

import com.kylecorry.andromeda.core.ui.colormaps.SampledColorMap
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.trail_sense.shared.colors.ColorInts

// http://seaviewsensing.com/pub/cpt-city/tp/index.html
class TomPattersonElevationColorMap : SampledColorMap(
    mapOf(
        0f / 8000f to ColorInts.rgb(105, 152, 133),
        50f / 8000f to ColorInts.rgb(118, 169, 146),
        200f / 8000f to ColorInts.rgb(131, 181, 155),
        600f / 8000f to ColorInts.rgb(165, 192, 167),
        1000f / 8000f to ColorInts.rgb(211, 201, 179),
        2000f / 8000f to ColorInts.rgb(212, 184, 164),
        3000f / 8000f to ColorInts.rgb(212, 192, 181),
        4000f / 8000f to ColorInts.rgb(214, 209, 206),
        5000f / 8000f to ColorInts.rgb(222, 221, 222),
        6000f / 8000f to ColorInts.rgb(238, 238, 238),
        7000f / 8000f to ColorInts.rgb(246, 247, 246),
        1f to ColorInts.rgb(255, 255, 255)
    )
), ElevationColorMap {
    override fun getElevationColor(meters: Float): Int {
        val min = 0f
        val max = 8000f
        return getColor(Interpolation.norm(meters, min, max, shouldClamp = true))
    }
}
