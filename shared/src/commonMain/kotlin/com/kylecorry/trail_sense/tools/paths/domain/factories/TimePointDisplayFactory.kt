package com.kylecorry.trail_sense.tools.paths.domain.factories

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.andromeda.core.rangeOrNull
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.colors.ColorInts
import com.kylecorry.trail_sense.shared.scales.ContinuousColorScale
import com.kylecorry.trail_sense.shared.scales.IColorScale
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.waypointcolors.IPointColoringStrategy
import com.kylecorry.trail_sense.tools.paths.domain.waypointcolors.TimePointColoringStrategy
import kotlin.time.Clock

class TimePointDisplayFactory : IPointDisplayFactory {
    override fun createColoringStrategy(path: List<PathPoint>): IPointColoringStrategy {
        val timeRange = path.mapNotNull { it.time }.rangeOrNull() ?: Clock.System.now().let { it..it }
        return TimePointColoringStrategy(
            timeRange,
            createColorScale(path)
        )
    }

    override fun createColorScale(path: List<PathPoint>): IColorScale {
        return ContinuousColorScale(ColorInts.White, AppColor.DarkBlue.color)
    }

    override fun createLabelMap(path: List<PathPoint>): Map<Float, String> {
        return mapOf(
            0.167f to getAppService<AppStrings>().get(Res.string.old),
            0.833f to getAppService<AppStrings>().get(Res.string.new_text),
        )
    }
}
