package com.kylecorry.sol.science.astronomy.locators

import com.kylecorry.sol.science.astronomy.moon.MoonPhase
import com.kylecorry.sol.science.astronomy.moon.MoonPhaseTimeCalculator
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.science.astronomy.units.EquatorialCoordinate
import com.kylecorry.sol.science.astronomy.units.UniversalTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Suppress("TooManyFunctions")
internal class Moon : JvmCelestialLocator {
    private val position = MoonPositionModel()
    private val phases = MoonPhaseModel()
    private fun UniversalTime.asInstant(): Instant = toInstant(ZoneOffset.UTC).toKotlinInstant()
    override fun getCoordinates(ut: UniversalTime): EquatorialCoordinate = position.getCoordinates(ut.asInstant())
    override fun getDistance(ut: UniversalTime): Distance = position.getDistance(ut.asInstant())
    fun getAngularDiameter(
        ut: UniversalTime, location: Coordinate = Coordinate.zero, distance: Distance? = null
    ): Double =
        phases.getAngularDiameter(ut.asInstant(), location, distance)
    fun getMeanAnomaly(ut: UniversalTime): Double = position.getMeanAnomaly(ut.asInstant())
    fun getPhase(ut: UniversalTime): MoonPhase = phases.getPhase(ut.asInstant())
    fun getNextMeanPhase(date: UniversalTime, moonTruePhase: MoonTruePhase): UniversalTime =
        LocalDateTime.ofInstant(
            phases.getNextMeanPhase(date.asInstant(), moonTruePhase).toJavaInstant(), ZoneOffset.UTC
        )
    fun getNextPhaseK(date: UniversalTime, moonTruePhase: MoonTruePhase): Double =
        phases.getNextPhaseK(date.asInstant(), moonTruePhase)
    fun getZenithAngleOfBrightLimb(date: UniversalTime, location: Coordinate): Float =
        phases.getZenithAngleOfBrightLimb(date.asInstant(), location)
    fun getPositionAngleOfBrightLimb(ut: UniversalTime): Double = phases.getPositionAngleOfBrightLimb(ut.asInstant())
    fun getEccentricity(julianCenturiesSinceJ2000: Double): Double = position.getEccentricity(julianCenturiesSinceJ2000)

    companion object {
        const val SYNODIC_MONTH_DAYS = MoonPhaseTimeCalculator.SYNODIC_MONTH_DAYS
        const val DIAMETER_IN_EARTH_RADII = 0.545
        const val RADIUS_IN_EARTH_RADII = DIAMETER_IN_EARTH_RADII / 2
    }
}
