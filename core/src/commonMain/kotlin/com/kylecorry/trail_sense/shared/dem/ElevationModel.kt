package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate

/** The digital elevation model: the built-in one, or one the user loaded. */
interface ElevationModel {
    /** The elevation at [location]; hasData is false outside the model. */
    suspend fun getElevation(location: Coordinate): DEMElevation

    /** Contour lines every [interval] meters across [bounds], sampled every [resolution] degrees. */
    suspend fun getContourLines(bounds: CoordinateBounds, interval: Float, resolution: Double): List<Contour>

    /** Whether the user loaded their own model (instead of the built-in one). */
    fun isExternalModel(): Boolean

    /** Forget cached elevations, ex. after the model changed. */
    fun invalidateCache()
}
