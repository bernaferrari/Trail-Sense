package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.tides.infrastructure.model.TideModelResult

/** Where tide tables that follow the built-in tide model get their harmonics, and the place they default to. */
interface TideHarmonicsSource {
    /** The model's harmonics nearest [location], or null when the model has none nearby. */
    suspend fun getHarmonics(location: Coordinate): TideModelResult?

    /** Where the person is, for tables without a location of their own. */
    val currentLocation: Coordinate
}
