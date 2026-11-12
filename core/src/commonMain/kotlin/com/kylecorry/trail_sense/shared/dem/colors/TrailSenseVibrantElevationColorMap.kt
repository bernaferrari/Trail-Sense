package com.kylecorry.trail_sense.shared.dem.colors

import com.kylecorry.andromeda.core.ui.colormaps.RgbInterpolationColorMap
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.colors.ColorInts

class TrailSenseVibrantElevationColorMap : RgbInterpolationColorMap(
    arrayOf(
        AppColor.Green.color,
        AppColor.Yellow.color,
        AppColor.Orange.color,
        AppColor.Red.color,
        AppColor.Purple.color,
        ColorInts.White
    )
), ElevationColorMap {
    override fun getElevationColor(meters: Float): Int {
        val min = 0f
        val max = 6000f
        return getColor(Interpolation.norm(meters, min, max, shouldClamp = true))
    }
}
