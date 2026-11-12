package com.kylecorry.trail_sense.shared.dem.colors

import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.colormaps.SampledColorMap
import com.kylecorry.trail_sense.shared.colors.ColorInts

class GrayscaleSlopeColorMap : SlopeColorMap {

    private val map = SampledColorMap(
        mapOf(
            5 / 90f to ColorInts.rgb(20, 20, 20),
            10 / 90f to ColorInts.rgb(127, 127, 127),
            25 / 90f to Colors.interpolate(ColorInts.rgb(127, 127, 127), ColorInts.White, 0.5f),
            1f to ColorInts.White
        )
    )

    override fun getSlopeColor(degrees: Float): Int {
        return map.getColor(degrees / 90f)
    }
}
