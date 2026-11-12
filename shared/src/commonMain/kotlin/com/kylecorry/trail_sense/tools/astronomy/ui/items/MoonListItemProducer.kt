package com.kylecorry.trail_sense.tools.astronomy.ui.items

import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.astronomy_altitude
import com.kylecorry.trail_sense.shared.generated.resources.astronomy_altitude_peak
import com.kylecorry.trail_sense.shared.generated.resources.direction
import com.kylecorry.trail_sense.shared.generated.resources.illumination
import com.kylecorry.trail_sense.shared.generated.resources.moon
import com.kylecorry.trail_sense.shared.generated.resources.moon_phase
import com.kylecorry.trail_sense.shared.generated.resources.supermoon
import com.kylecorry.trail_sense.shared.generated.resources.times
import com.kylecorry.trail_sense.shared.time.today
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

class MoonListItemProducer : BaseAstroListItemProducer() {

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
            strings.get(Res.string.moon),
            percent(formatter.formatMoonPhase(phase.phase), phase.illumination),
            icon(
                MoonPhaseImageMapper().getPhaseImage(
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
                strings.get(Res.string.times) to riseSetTransit(times),
                strings.get(Res.string.moon_phase) to data(formatter.formatMoonPhase(phase.phase)),
                strings.get(Res.string.illumination) to percent(phase.illumination),
                strings.get(Res.string.astronomy_altitude_peak) to peak?.let { degrees(it) },
                strings.get(Res.string.supermoon) to data(
                    formatter.formatBooleanYesNo(
                        isSuperMoon
                    )
                ),
                strings.get(Res.string.astronomy_altitude) to altitude?.let { degrees(it) },
                strings.get(Res.string.direction) to azimuth?.let { degrees(it.value) }
            )

            showAdvancedData(strings.get(Res.string.moon), advancedData)
        }
    }


}
