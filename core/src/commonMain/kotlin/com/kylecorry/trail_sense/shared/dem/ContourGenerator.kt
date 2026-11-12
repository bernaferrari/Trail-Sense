package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.luna.concurrency.Parallel
import com.kylecorry.sol.math.geometry.Geometry
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.shared.Executor
import com.kylecorry.sol.units.Coordinate

/** Contour lines (marching squares) through a grid of elevations. */
object ContourGenerator {

    /**
     * Contours every [interval] meters through the grid whose rows are [latitudes] and columns are [longitudes];
     * [elevationAt] gives the elevation of column x, row y. Longitudes may run past 180 (a grid crossing the
     * antimeridian); they're wrapped back.
     */
    suspend fun generate(
        latitudes: DoubleArray,
        longitudes: DoubleArray,
        interval: Float,
        executor: Executor,
        elevationAt: (x: Int, y: Int) -> Float
    ): List<Contour> {
        if (latitudes.isEmpty() || longitudes.isEmpty()) {
            return emptyList()
        }

        var minElevation = Float.POSITIVE_INFINITY
        var maxElevation = Float.NEGATIVE_INFINITY

        val grid = ArrayList<List<Pair<Coordinate, Float>>>(latitudes.size)
        for (y in latitudes.indices) {
            val row = ArrayList<Pair<Coordinate, Float>>(longitudes.size)
            val lat = latitudes[y]
            for (x in longitudes.indices) {
                val lon = Coordinate.toLongitude(longitudes[x])
                val value = elevationAt(x, y)
                if (value < minElevation) {
                    minElevation = value
                }
                if (value > maxElevation) {
                    maxElevation = value
                }
                row.add(Coordinate(lat, lon) to value)
            }
            grid.add(row)
        }

        val thresholds = Interpolation.getMultiplesBetween(minElevation, maxElevation, interval).toList()

        return Parallel.map(thresholds) { threshold ->
            val segments = Interpolation.getIsoline(
                grid,
                threshold,
                executor = executor,
                interpolator = ::lerpCoordinate
            )
            Contour(threshold, Geometry.getConnectedLines(segments.map { it.start to it.end }))
        }
    }

    private fun lerpCoordinate(percent: Float, a: Coordinate, b: Coordinate): Coordinate {
        val distance = a.distanceTo(b)
        val bearing = a.bearingTo(b)
        return a.plus(distance * percent.toDouble(), bearing)
    }
}
