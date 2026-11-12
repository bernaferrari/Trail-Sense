package com.kylecorry.trail_sense.shared.dem.colors

import com.kylecorry.andromeda.core.ui.colormaps.RgbInterpolationColorMap
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.trail_sense.shared.colors.ColorInts

class GrayscaleElevationColorMap : RgbInterpolationColorMap(
    arrayOf(
        ColorInts.rgb(75, 75, 75),
        ColorInts.rgb(250, 250, 250),
    )
), ElevationColorMap {
    override fun getElevationColor(meters: Float): Int {
        val min = 0f
        val max = 3000f
        return getColor(Interpolation.norm(meters, min, max, shouldClamp = true))
    }
}
