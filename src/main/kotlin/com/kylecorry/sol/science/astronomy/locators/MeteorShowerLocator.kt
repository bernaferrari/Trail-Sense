package com.kylecorry.sol.science.astronomy.locators

import com.kylecorry.sol.science.astronomy.meteors.MeteorShower
import com.kylecorry.sol.science.astronomy.units.EquatorialCoordinate
import com.kylecorry.sol.science.astronomy.units.UniversalTime
import com.kylecorry.sol.units.Distance
import java.time.ZoneOffset
import kotlin.time.toKotlinInstant

internal class MeteorShowerLocator(shower: MeteorShower) : JvmCelestialLocator {
    private val model = MeteorShowerPositionModel(shower)
    override fun getCoordinates(ut: UniversalTime): EquatorialCoordinate =
        model.getCoordinates(ut.toInstant(ZoneOffset.UTC).toKotlinInstant())
    override fun getDistance(ut: UniversalTime): Distance? = null
}
