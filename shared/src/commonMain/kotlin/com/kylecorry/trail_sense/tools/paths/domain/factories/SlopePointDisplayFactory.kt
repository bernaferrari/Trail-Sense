package com.kylecorry.trail_sense.tools.paths.domain.factories

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.scales.DiscreteColorScale
import com.kylecorry.trail_sense.shared.scales.IColorScale
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.waypointcolors.IPointColoringStrategy
import com.kylecorry.trail_sense.tools.paths.domain.waypointcolors.SlopePointColoringStrategy

class SlopePointDisplayFactory : IPointDisplayFactory {
    override fun createColoringStrategy(path: List<PathPoint>): IPointColoringStrategy {
        return SlopePointColoringStrategy(
            createColorScale(path)
        )
    }

    override fun createColorScale(path: List<PathPoint>): IColorScale {
        return DiscreteColorScale(
            listOf(
                AppColor.Green.color,
                AppColor.Yellow.color,
                AppColor.Red.color
            )
        )
    }

    override fun createLabelMap(path: List<PathPoint>): Map<Float, String> {
        return mapOf(
            0.167f to getAppService<AppStrings>().get(Res.string.path_slope_flat),
            0.5f to getAppService<AppStrings>().get(Res.string.path_slope_moderate),
            0.833f to getAppService<AppStrings>().get(Res.string.path_slope_steep),
        )
    }
}
