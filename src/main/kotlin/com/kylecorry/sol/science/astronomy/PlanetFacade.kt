package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.science.astronomy.locators.Planet
import com.kylecorry.sol.science.astronomy.locators.PlanetLocator
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.science.astronomy.units.toUniversalTime
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import java.time.ZonedDateTime
import kotlinx.datetime.toKotlinTimeZone
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

internal object PlanetFacade {

    fun getPlanetPosition(
        planet: Planet,
        time: ZonedDateTime,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): CelestialObservation {
        return PlanetCalculationModel.position(
            planet, time.toInstant().toKotlinInstant(), location, withRefraction, withParallax
        )
    }

    fun getPlanetEvents(
        planet: Planet,
        date: ZonedDateTime,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): RiseSetTransitTimes {
        val times = PlanetCalculationModel.events(
            planet, date.toInstant().toKotlinInstant(), date.zone.toKotlinTimeZone(), location,
            withRefraction, withParallax
        )
        return RiseSetTransitTimes(
            times.rise?.toJavaInstant()?.atZone(date.zone), times.transit?.toJavaInstant()?.atZone(date.zone),
            times.set?.toJavaInstant()?.atZone(date.zone)
        )
    }
}
