package com.kylecorry.sol.science.astronomy.locators

import com.kylecorry.sol.science.astronomy.meteors.MeteorShower
import com.kylecorry.sol.science.astronomy.units.EquatorialCoordinate
import kotlin.time.Instant
import com.kylecorry.sol.units.Distance

internal class MeteorShowerPositionModel(private val shower: MeteorShower) : ICelestialLocator {
    override fun getCoordinates(ut: Instant): EquatorialCoordinate {
        return when (shower) {
            MeteorShower.Quadrantids -> EquatorialCoordinate(49.7, angle(15, 20, 0))
            MeteorShower.Lyrids -> EquatorialCoordinate(33.3, angle(18, 8, 0))
            MeteorShower.EtaAquariids -> EquatorialCoordinate(
                -1.0,
                angle(22, 32, 0)
            )

            MeteorShower.DeltaAquariids -> EquatorialCoordinate(
                -16.4,
                angle(22, 40, 0)
            )

            MeteorShower.Perseids -> EquatorialCoordinate(58.1, angle(3, 17, 0))
            MeteorShower.Orionids -> EquatorialCoordinate(15.8, angle(6, 25, 0))
            MeteorShower.Leonids -> EquatorialCoordinate(21.8, angle(10, 16, 0))
            MeteorShower.Geminids -> EquatorialCoordinate(32.4, angle(7, 33, 0))
            MeteorShower.Ursids -> EquatorialCoordinate(75.4, angle(14, 38, 0))
        }
    }

    override fun getDistance(ut: Instant): Distance? {
        return null
    }

    private fun angle(hours: Int, minutes: Int, seconds: Int): Double =
        (hours.toDouble() + minutes.toDouble() / 60.0 + seconds.toDouble() / 3600.0) * 15
}
