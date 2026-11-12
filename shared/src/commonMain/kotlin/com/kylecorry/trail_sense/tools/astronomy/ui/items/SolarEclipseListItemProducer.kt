package com.kylecorry.trail_sense.tools.astronomy.ui.items

import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.astronomy_altitude_peak
import com.kylecorry.trail_sense.shared.generated.resources.astronomy_direction_peak
import com.kylecorry.trail_sense.shared.generated.resources.duration
import com.kylecorry.trail_sense.shared.generated.resources.magnitude
import com.kylecorry.trail_sense.shared.generated.resources.obscuration
import com.kylecorry.trail_sense.shared.generated.resources.solar_eclipse
import com.kylecorry.trail_sense.shared.generated.resources.times
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.tools.astronomy.ui.format.EclipseFormatter
import kotlinx.datetime.LocalDate

class SolarEclipseListItemProducer : BaseAstroListItemProducer() {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate,
        declination: Float
    ): AstronomyListItem? = onDefault {
        val eclipse = astronomyService.getSolarEclipse(location, date) ?: return@onDefault null

        // Advanced
        val peakAltitude = astronomyService.getSunAltitude(location, eclipse.peak)
        val peakAzimuth = DeclinationUtils.fromTrueNorthBearing(
            astronomyService.getSunAzimuth(location, eclipse.peak),
            declination
        )

        val times = times(eclipse.start, eclipse.peak, eclipse.end, date)

        list(
            5,
            strings.get(Res.string.solar_eclipse),
            EclipseFormatter.type(eclipse),
            icon(
                if (eclipse.isTotal) {
                    TrailIcons.TotalSolarEclipse
                } else {
                    TrailIcons.PartialSolarEclipse
                }
            ),
            data = times
        ) {
            val advancedData = listOf(
                strings.get(Res.string.times) to times,
                strings.get(Res.string.duration) to duration(eclipse.duration),
                strings.get(Res.string.obscuration) to data(
                    EclipseFormatter.type(eclipse)
                ),
                strings.get(Res.string.magnitude) to decimal(eclipse.magnitude, 2),
                strings.get(Res.string.astronomy_altitude_peak) to degrees(peakAltitude),
                strings.get(Res.string.astronomy_direction_peak) to direction(peakAzimuth)
            )

            showAdvancedData(strings.get(Res.string.solar_eclipse), advancedData)
        }
    }


}
