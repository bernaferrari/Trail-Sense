package com.kylecorry.sol.science.astronomy.locators

import com.kylecorry.sol.science.astronomy.units.EquatorialCoordinate
import com.kylecorry.sol.science.astronomy.units.UniversalTime
import com.kylecorry.sol.units.Distance
import java.time.ZoneOffset
import kotlin.time.toKotlinInstant

internal class Sun : JvmCelestialLocator {
    private val model = SunModel()
    override fun getCoordinates(ut: UniversalTime): EquatorialCoordinate = model.getCoordinates(ut.asInstant())
    override fun getDistance(ut: UniversalTime): Distance = model.getDistance(ut.asInstant())
    fun getAngularDiameter(ut: UniversalTime, distance: Distance? = null): Double =
        model.getAngularDiameter(ut.asInstant(), distance)
    fun getMeanAnomaly(ut: UniversalTime): Double = model.getMeanAnomaly(ut.asInstant())
    private fun UniversalTime.asInstant() = toInstant(ZoneOffset.UTC).toKotlinInstant()
}
