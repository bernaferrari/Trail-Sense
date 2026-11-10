package com.kylecorry.trail_sense.shared.views.compose

import android.content.Context
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.icons.trail.Altitude
import com.kylecorry.trail_sense.shared.icons.trail.CompassIcon
import com.kylecorry.trail_sense.shared.icons.trail.Ruler
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons

/**
 * How far, which way, and how much higher or lower [to] is from [from], for the map long-press sheet.
 * The elevation change is left out when either elevation is unknown.
 */
fun locationDetailStats(
    context: Context,
    formatter: FormatService,
    baseUnits: DistanceUnits,
    from: Coordinate,
    fromElevation: Distance?,
    to: Coordinate,
    toElevation: Distance?
): List<TrailSenseStat> {
    val distance = Distance.meters(from.distanceTo(to)).convertTo(baseUnits).toRelativeDistance()
    val azimuth = from.bearingTo(to).value
    val stats = mutableListOf(
        TrailSenseStat(
            label = context.getString(R.string.from_you),
            value = formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units), strict = false),
            icon = TrailIcons.Ruler
        ),
        TrailSenseStat(
            label = context.getString(R.string.direction),
            value = formatter.formatDegrees(azimuth, replace360 = true) + " " +
                    formatter.formatDirection(CompassDirection.nearest(azimuth)),
            icon = TrailIcons.CompassIcon
        )
    )
    if (fromElevation != null && toElevation != null) {
        val change = toElevation.meters().value - fromElevation.meters().value
        val sign = if (change > 0) context.getString(R.string.increase) else ""
        stats += TrailSenseStat(
            label = context.getString(R.string.elevation_change),
            value = context.getString(
                R.string.elevation_diff_format,
                sign,
                formatter.formatElevation(Distance.meters(change))
            ),
            icon = TrailIcons.Altitude
        )
    }
    return stats
}
