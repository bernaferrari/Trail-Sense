package com.kylecorry.sol.science.astronomy.locators

import com.kylecorry.sol.science.astronomy.stars.Star
import com.kylecorry.sol.science.astronomy.units.EquatorialCoordinate
import com.kylecorry.sol.science.astronomy.units.UniversalTime
import com.kylecorry.sol.units.Distance
import java.time.ZoneOffset
import kotlin.time.toKotlinInstant

internal class StarLocator(star: Star) : JvmCelestialLocator {
    private val model = StarPositionModel(star)
    override fun getCoordinates(ut: UniversalTime): EquatorialCoordinate =
        model.getCoordinates(ut.toInstant(ZoneOffset.UTC).toKotlinInstant())
    override fun getDistance(ut: UniversalTime): Distance? = null
}
