package com.kylecorry.sol.science.astronomy.locators

import com.kylecorry.sol.science.astronomy.units.EquatorialCoordinate
import com.kylecorry.sol.science.astronomy.units.UniversalTime
import com.kylecorry.sol.units.Distance
import java.time.ZoneOffset
import kotlin.time.toKotlinInstant

internal class PlanetLocator(planet: Planet) : JvmCelestialLocator {
    private val model = PlanetPositionModel(planet)
    override fun getCoordinates(ut: UniversalTime): EquatorialCoordinate = model.getCoordinates(ut.asInstant())
    override fun getDistance(ut: UniversalTime): Distance = model.getDistance(ut.asInstant())
    fun getAngularDiameter(ut: UniversalTime): Double = model.getAngularDiameter(ut.asInstant())
    fun getMagnitude(ut: UniversalTime): Double = model.getMagnitude(ut.asInstant())
    private fun UniversalTime.asInstant() = toInstant(ZoneOffset.UTC).toKotlinInstant()
}
