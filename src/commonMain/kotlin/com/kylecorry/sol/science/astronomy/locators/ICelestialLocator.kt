package com.kylecorry.sol.science.astronomy.locators

import com.kylecorry.sol.science.astronomy.units.EquatorialCoordinate
import com.kylecorry.sol.units.Distance
import kotlin.time.Instant

internal interface ICelestialLocator {
    fun getCoordinates(ut: Instant): EquatorialCoordinate
    fun getDistance(ut: Instant): Distance?
}
