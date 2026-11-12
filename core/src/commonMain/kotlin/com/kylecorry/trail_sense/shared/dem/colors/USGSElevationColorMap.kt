package com.kylecorry.trail_sense.shared.dem.colors

import com.kylecorry.andromeda.core.ui.colormaps.SampledColorMap
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.trail_sense.shared.colors.ColorInts

/*
<copying>
<authors>
<author href="https://www.usgs.gov/">
<name>U.S. Geological Survey</name>
</author>
<author>
<name>R. Langford</name>
</author>
</authors>
<license>
<informal> Creative Commons Attribution-NonCommercial 3.0 unported </informal>
<year>2021</year>
<text href="https://creativecommons.org/licenses/by-nc/3.0/"/>
</license>
<src>
<format>QGIS file</format>
</src>
<distribute>
<qgis distribute="noncomm" license="ccnc"/>
</distribute>
</copying>
 */
class USGSElevationColorMap : SampledColorMap(
    mapOf(
        0f to ColorInts.rgb(127, 159, 101),
        0.048f to ColorInts.rgb(130, 176, 112),
        0.095f to ColorInts.rgb(160, 194, 124),
        0.143f to ColorInts.rgb(185, 214, 124),
        0.190f to ColorInts.rgb(207, 224, 156),
        0.238f to ColorInts.rgb(223, 233, 168),
        0.286f to ColorInts.rgb(241, 238, 166),
        0.333f to ColorInts.rgb(237, 223, 159),
        0.381f to ColorInts.rgb(240, 210, 141),
        0.429f to ColorInts.rgb(230, 188, 138),
        0.476f to ColorInts.rgb(216, 165, 133),
        0.524f to ColorInts.rgb(197, 149, 135),
        0.571f to ColorInts.rgb(217, 165, 156),
        0.619f to ColorInts.rgb(227, 183, 177),
        0.667f to ColorInts.rgb(223, 192, 191),
        0.714f to ColorInts.rgb(239, 205, 217),
        0.762f to ColorInts.rgb(244, 215, 225),
        0.810f to ColorInts.rgb(243, 223, 233),
        0.857f to ColorInts.rgb(248, 227, 232),
        0.905f to ColorInts.rgb(248, 233, 238),
        0.952f to ColorInts.rgb(246, 240, 245),
        1f to ColorInts.rgb(255, 255, 255)
    )
), ElevationColorMap {
    override fun getElevationColor(meters: Float): Int {
        val low = 0f
        val high = 3048f
        return getColor(Interpolation.norm(meters, low, high, shouldClamp = true))
    }
}
