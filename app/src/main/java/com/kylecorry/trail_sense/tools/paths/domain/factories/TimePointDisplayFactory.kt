package com.kylecorry.trail_sense.tools.paths.domain.factories

import kotlin.time.Clock

import android.content.Context
import android.graphics.Color
import android.util.Range
import com.kylecorry.trail_sense.shared.andromeda_temp.core.rangeOrNull
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.scales.ContinuousColorScale
import com.kylecorry.trail_sense.shared.scales.IColorScale
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.waypointcolors.IPointColoringStrategy
import com.kylecorry.trail_sense.tools.paths.domain.waypointcolors.TimePointColoringStrategy

class TimePointDisplayFactory(private val context: Context) : IPointDisplayFactory {
    override fun createColoringStrategy(path: List<PathPoint>): IPointColoringStrategy {
        val timeRange = path.mapNotNull { it.time }.rangeOrNull() ?: Range(
            Clock.System.now(),
            Clock.System.now()
        )
        return TimePointColoringStrategy(
            timeRange,
            createColorScale(path)
        )
    }

    override fun createColorScale(path: List<PathPoint>): IColorScale {
        return ContinuousColorScale(Color.WHITE, AppColor.DarkBlue.color)
    }

    override fun createLabelMap(path: List<PathPoint>): Map<Float, String> {
        return mapOf(
            0.167f to context.getString(R.string.old),
            0.833f to context.getString(R.string.new_text),
        )
    }
}
