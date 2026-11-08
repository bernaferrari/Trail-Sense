package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.math.arithmetic.Arithmetic.wrap
import com.kylecorry.sol.science.astronomy.locators.SunModel
import com.kylecorry.sol.science.astronomy.units.EclipticCoordinate
import com.kylecorry.sol.science.astronomy.units.JulianDay
import com.kylecorry.sol.science.shared.Season
import com.kylecorry.sol.units.Coordinate
import kotlin.time.Instant

internal object SeasonCalculator {
    private val sun = SunModel()

    fun getSeason(location: Coordinate, time: Instant): Season {
        val coordinates = sun.getCoordinates(time)
        val longitude = EclipticCoordinate.fromEquatorial(coordinates, JulianDay.centuries(time)).eclipticLongitude
        val sl = wrap(longitude.toFloat(), 0f, 360f)
        return when {
            sl >= OrbitalPosition.WinterSolstice.solarLongitude ->
                if (location.isNorthernHemisphere) Season.Winter else Season.Summer
            sl >= OrbitalPosition.AutumnalEquinox.solarLongitude ->
                if (location.isNorthernHemisphere) Season.Fall else Season.Spring
            sl >= OrbitalPosition.SummerSolstice.solarLongitude ->
                if (location.isNorthernHemisphere) Season.Summer else Season.Winter
            else -> if (location.isNorthernHemisphere) Season.Spring else Season.Fall
        }
    }
}
