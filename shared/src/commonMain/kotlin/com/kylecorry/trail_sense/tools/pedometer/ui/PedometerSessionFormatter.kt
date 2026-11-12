package com.kylecorry.trail_sense.tools.pedometer.ui

import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import kotlin.time.Clock

class PedometerSessionFormatter(
    private val paceCalculator: StrideLengthPaceCalculator,
    private val formatter: FormatService,
    private val prefs: UserPreferences
) {
    fun title(period: StepTrackingPeriod): String {
        val endTime = period.endTime ?: Clock.System.now()
        return formatter.formatTimeSpan(
            period.startTime,
            endTime,
            relative = true
        )
    }

    fun subtitle(period: StepTrackingPeriod): String {
        val duration = period.getDuration(prefs.pedometer.averagePaceTimeMode)
        val distance = paceCalculator.distance(period.steps)
            .convertTo(prefs.baseDistanceUnits)
            .toRelativeDistance()
        val speed = paceCalculator.speed(period.steps, duration)
            .convertTo(DistanceUnits.Meters, TimeUnits.Seconds)
        val steps = period.steps.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return formatter.join(
            getAppService<AppStrings>().plural(Res.plurals.step_count, steps, steps),
            formatter.formatDistance(
                distance,
                Units.getDecimalPlaces(distance.units),
                false
            ),
            if (duration.inWholeSeconds > 0) {
                formatter.formatSpeed(speed.value)
            } else {
                getAppService<AppStrings>().get(Res.string.dash)
            },
            formatter.formatDuration(duration, short = true),
            separator = FormatService.Separator.Dot
        )
    }
}
