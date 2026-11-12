package com.kylecorry.trail_sense.tools.paths.domain.factories

import com.kylecorry.andromeda.core.rangeOrNull
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.scales.ContinuousColorScale
import com.kylecorry.trail_sense.shared.scales.IColorScale
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.waypointcolors.AltitudePointColoringStrategy
import com.kylecorry.trail_sense.tools.paths.domain.waypointcolors.IPointColoringStrategy

class AltitudePointDisplayFactory : IPointDisplayFactory {
    override fun createColoringStrategy(path: List<PathPoint>): IPointColoringStrategy {
        val altitudeRange = getAltitudeRange(path)
        return AltitudePointColoringStrategy(
            altitudeRange,
            createColorScale(path)
        )
    }

    override fun createColorScale(path: List<PathPoint>): IColorScale {
        return ContinuousColorScale(AppColor.DarkBlue.color, AppColor.Red.color)
    }

    override fun createLabelMap(path: List<PathPoint>): Map<Float, String> {
        val range = getAltitudeRange(path)
        val units = getAppService<UserPreferences>().baseDistanceUnits
        val formatService = FormatService.getInstance()
        val min = Distance.meters(range.start).convertTo(units)
        val max = Distance.meters(range.endInclusive).convertTo(units)

        // TODO: Make these at the beginning and end
        return mapOf(
            0.167f to formatService.formatDistance(min, Units.getDecimalPlaces(units), false),
            0.833f to formatService.formatDistance(max, Units.getDecimalPlaces(units), false),
        )
    }

    private fun getAltitudeRange(path: List<PathPoint>): ClosedRange<Float> {
        return path.mapNotNull { it.elevation }.rangeOrNull() ?: 0f..0f
    }
}
