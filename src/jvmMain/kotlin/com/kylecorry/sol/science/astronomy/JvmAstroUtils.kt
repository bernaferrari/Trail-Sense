package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.science.astronomy.locators.ICelestialLocator
import com.kylecorry.sol.science.astronomy.locators.JvmCelestialLocator
import com.kylecorry.sol.science.astronomy.units.EquatorialCoordinate
import com.kylecorry.sol.science.astronomy.units.HorizonCoordinate
import com.kylecorry.sol.science.astronomy.units.UniversalTime
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

internal object JvmAstroUtils {
    private fun UniversalTime.asInstant(): Instant = toInstant(ZoneOffset.UTC).toKotlinInstant()
    private fun Instant.asUniversalTime(): UniversalTime = LocalDateTime.ofInstant(toJavaInstant(), ZoneOffset.UTC)
    private fun JvmCelestialLocator.asCommon(): ICelestialLocator = object : ICelestialLocator {
        override fun getCoordinates(ut: Instant): EquatorialCoordinate = this@asCommon.getCoordinates(ut.asUniversalTime())
        override fun getDistance(ut: Instant): Distance? = this@asCommon.getDistance(ut.asUniversalTime())
    }

    internal fun commonLocator(locator: JvmCelestialLocator): ICelestialLocator = locator.asCommon()

    fun getAltitude(
        locator: JvmCelestialLocator, ut: UniversalTime, location: Coordinate,
        withRefraction: Boolean = false, withParallax: Boolean = false
    ): Float = AstroUtils.getAltitude(locator.asCommon(), ut.asInstant(), location, withRefraction, withParallax)

    fun getAltitude(
        coordinate: EquatorialCoordinate, ut: UniversalTime, location: Coordinate,
        withRefraction: Boolean = false, distanceToBody: Distance? = null
    ): Float = AstroUtils.getAltitude(coordinate, ut.asInstant(), location, withRefraction, distanceToBody)

    fun getAzimuth(
        locator: JvmCelestialLocator, ut: UniversalTime, location: Coordinate, withParallax: Boolean = false
    ): Bearing = AstroUtils.getAzimuth(locator.asCommon(), ut.asInstant(), location, withParallax)

    fun getLocation(
        locator: JvmCelestialLocator, ut: UniversalTime, location: Coordinate,
        withRefraction: Boolean = false, withParallax: Boolean = false, distance: Distance? = null
    ): HorizonCoordinate = AstroUtils.getLocation(locator.asCommon(), ut.asInstant(), location, withRefraction, withParallax, distance)

    fun getParallacticAngle(locator: JvmCelestialLocator, ut: UniversalTime, location: Coordinate): Float =
        AstroUtils.getParallacticAngle(locator.asCommon(), ut.asInstant(), location)
}
