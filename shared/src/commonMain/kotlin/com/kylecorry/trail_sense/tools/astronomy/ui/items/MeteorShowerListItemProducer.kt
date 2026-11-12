package com.kylecorry.trail_sense.tools.astronomy.ui.items

import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.astronomy_altitude_peak
import com.kylecorry.trail_sense.shared.generated.resources.astronomy_direction_peak
import com.kylecorry.trail_sense.shared.generated.resources.meteor_shower
import com.kylecorry.trail_sense.shared.generated.resources.meteors_per_hour
import com.kylecorry.trail_sense.shared.generated.resources.name
import com.kylecorry.trail_sense.shared.generated.resources.rate
import com.kylecorry.trail_sense.shared.generated.resources.times
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.readableName
import kotlinx.datetime.LocalDate

class MeteorShowerListItemProducer : BaseAstroListItemProducer() {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate,
        declination: Float
    ): AstronomyListItem? = onDefault {
        val shower = astronomyService.getMeteorShower(location, date) ?: return@onDefault null

        // Advanced
        val peakAltitude = astronomyService.getMeteorShowerPeakAltitude(shower, location)
        val peakAzimuth = DeclinationUtils.fromTrueNorthBearing(
            astronomyService.getMeteorShowerPeakAzimuth(shower, location),
            declination
        )

        val times = times(shower.start, shower.peak, shower.end, date)
        val rate = strings.get(Res.string.meteors_per_hour, shower.shower.rate)

        list(
            3,
            strings.get(Res.string.meteor_shower),
            rate,
            icon(TrailIcons.Meteor, secondaryColor),
            data = times
        ) {
            val advancedData = listOf(
                strings.get(Res.string.times) to times,
                strings.get(Res.string.name) to data(shower.shower.readableName()),
                strings.get(Res.string.rate) to data(rate),
                strings.get(Res.string.astronomy_altitude_peak) to degrees(peakAltitude),
                strings.get(Res.string.astronomy_direction_peak) to direction(peakAzimuth)
            )

            showAdvancedData(strings.get(Res.string.meteor_shower), advancedData)
        }
    }


}
