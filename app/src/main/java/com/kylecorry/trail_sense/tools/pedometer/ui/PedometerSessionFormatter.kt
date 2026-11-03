package com.kylecorry.trail_sense.tools.pedometer.ui

import android.content.Context
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import java.time.Instant

internal class PedometerSessionFormatter(
    private val context: Context,
    private val paceCalculator: StrideLengthPaceCalculator,
    private val formatter: FormatService,
    private val prefs: UserPreferences
) {
    fun title(period: StepTrackingPeriod): String {
        val endTime = period.endTime ?: Instant.now()
        return formatter.formatTimeSpan(
            period.startTime.toZonedDateTime(),
            endTime.toZonedDateTime(),
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
            context.resources.getQuantityString(R.plurals.step_count, steps, steps),
            formatter.formatDistance(
                distance,
                Units.getDecimalPlaces(distance.units),
                false
            ),
            if (duration.seconds > 0) {
                formatter.formatSpeed(speed.value)
            } else {
                context.getString(R.string.dash)
            },
            formatter.formatDuration(duration, short = true),
            separator = FormatService.Separator.Dot
        )
    }
}
