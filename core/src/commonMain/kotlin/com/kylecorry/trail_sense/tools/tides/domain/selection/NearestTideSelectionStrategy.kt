package com.kylecorry.trail_sense.tools.tides.domain.selection

import kotlinx.coroutines.withContext

import kotlinx.coroutines.IO

import kotlinx.coroutines.Dispatchers

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideEstimator

class NearestTideSelectionStrategy(
    private val locationProvider: () -> Coordinate
) : ITideSelectionStrategy {
    override suspend fun getTide(tides: List<TideTable>): TideTable? = withContext(Dispatchers.IO) {
        val tidesWithLocation = tides.filter { it.location != null }
        val nearest = tidesWithLocation.minByOrNull { it.location!!.distanceTo(locationProvider()) }
            ?: return@withContext null

        val autoTide =
            tides.firstOrNull { it.estimator == TideEstimator.TideModel && it.location == null }
                ?: return@withContext nearest

        // If the nearest tide is too far away, use the auto tide instead
        val maxDistance = Distance.kilometers(50f).meters().value
        if (nearest.location!!.distanceTo(locationProvider()) > maxDistance) {
            autoTide
        } else {
            nearest
        }
    }
}
