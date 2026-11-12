package com.kylecorry.trail_sense.tools.astronomy.ui.items

import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.astronomy_altitude
import com.kylecorry.trail_sense.shared.generated.resources.astronomy_altitude_peak
import com.kylecorry.trail_sense.shared.generated.resources.daylight
import com.kylecorry.trail_sense.shared.generated.resources.daylight_duration
import com.kylecorry.trail_sense.shared.generated.resources.direction
import com.kylecorry.trail_sense.shared.generated.resources.night
import com.kylecorry.trail_sense.shared.generated.resources.season
import com.kylecorry.trail_sense.shared.generated.resources.sun
import com.kylecorry.trail_sense.shared.generated.resources.sun_actual
import com.kylecorry.trail_sense.shared.generated.resources.sun_astronomical
import com.kylecorry.trail_sense.shared.generated.resources.sun_civil
import com.kylecorry.trail_sense.shared.generated.resources.sun_nautical
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.time.today
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.LocalDate

class SunListItemProducer : BaseAstroListItemProducer() {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate,
        declination: Float
    ): AstronomyListItem = onDefault {
        // At a glance
        val times = astronomyService.getSunTimes(location, prefs.astronomy.sunTimesMode, date)
        val daylight = astronomyService.getLengthOfDay(location, prefs.astronomy.sunTimesMode, date)

        // Advanced
        val actual = astronomyService.getSunTimes(location, SunTimesMode.Actual, date)
        val civil = astronomyService.getSunTimes(location, SunTimesMode.Civil, date)
        val nautical = astronomyService.getSunTimes(location, SunTimesMode.Nautical, date)
        val astronomical = astronomyService.getSunTimes(location, SunTimesMode.Astronomical, date)
        val peak = times.transit?.let {
            astronomyService.getSunAltitude(location, it)
        }
        val night = 1.days - daylight
        val season = astronomyService.getSeason(location, date)
        val azimuth = if (date == today()) DeclinationUtils.fromTrueNorthBearing(
            astronomyService.getSunAzimuth(location),
            declination
        ) else null
        val altitude =
            if (date == today()) astronomyService.getSunAltitude(location) else null

        list(
            1,
            strings.get(Res.string.sun),
            strings.get(Res.string.daylight_duration,
                formatter.formatDuration(daylight, false)
            ),
            icon(TrailIcons.SunRays),
            data = riseSetTransit(times)
        ) {
            val advancedData = listOf(
                strings.get(Res.string.sun_actual) to riseSetTransit(actual),
                strings.get(Res.string.sun_civil) to riseSetTransit(civil),
                strings.get(Res.string.sun_nautical) to riseSetTransit(nautical),
                strings.get(Res.string.sun_astronomical) to riseSetTransit(astronomical),
                strings.get(Res.string.astronomy_altitude_peak) to peak?.let { degrees(it) },
                strings.get(Res.string.daylight) to duration(daylight),
                strings.get(Res.string.night) to duration(night),
                strings.get(Res.string.season) to data(formatter.formatSeason(season)),
                strings.get(Res.string.astronomy_altitude) to altitude?.let { degrees(it) },
                strings.get(Res.string.direction) to azimuth?.let { degrees(it.value) }
            )

            showAdvancedData(strings.get(Res.string.sun), advancedData)
        }
    }


}
