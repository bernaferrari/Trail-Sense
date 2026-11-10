package com.kylecorry.trail_sense.tools.astronomy.ui.items

import com.kylecorry.trail_sense.shared.time.today

import android.content.Context
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.TimeZone

class MoonListItemProducer(context: Context) : BaseAstroListItemProducer(context) {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate,
        declination: Float
    ): AstronomyListItem = onDefault {
        // At a glance
        val times = astronomyService.getMoonTimes(location, date)
        val phase = if (date == today()) {
            astronomyService.getCurrentMoonPhase()
        } else {
            astronomyService.getMoonPhase(date)
        }
        val tilt = if (date == today()) {
            astronomyService.getMoonTilt(location)
        } else {
            astronomyService.getMoonTilt(
                location,
                date.atTime(12, 0).toInstant(TimeZone.currentSystemDefault()),
                useNearestTransit = true
            )
        }

        // Advanced
        val isSuperMoon = astronomyService.isSuperMoon(date)
        val peak = times.transit?.let { astronomyService.getMoonAltitude(location, it) }
        val azimuth =
            if (date == today()) DeclinationUtils.fromTrueNorthBearing(
                astronomyService.getMoonAzimuth(location),
                declination
            ) else null
        val altitude =
            if (date == today()) astronomyService.getMoonAltitude(location) else null

        list(
            2,
            context.getString(R.string.moon),
            percent(formatter.formatMoonPhase(phase.phase), phase.illumination),
            icon(
                MoonPhaseImageMapper(context).getPhaseImage(
                    phase.phaseAngle,
                    imageSize,
                    imageSize,
                    tilt,
                    plain = true
                )
            ),
            data = riseSetTransit(times)
        ) {
            val advancedData = listOf(
                context.getString(R.string.times) to riseSetTransit(times),
                context.getString(R.string.moon_phase) to data(formatter.formatMoonPhase(phase.phase)),
                context.getString(R.string.illumination) to percent(phase.illumination),
                context.getString(R.string.astronomy_altitude_peak) to peak?.let { degrees(it) },
                context.getString(R.string.supermoon) to data(
                    formatter.formatBooleanYesNo(
                        isSuperMoon
                    )
                ),
                context.getString(R.string.astronomy_altitude) to altitude?.let { degrees(it) },
                context.getString(R.string.direction) to azimuth?.let { degrees(it.value) }
            )

            showAdvancedData(context.getString(R.string.moon), advancedData)
        }
    }


}
