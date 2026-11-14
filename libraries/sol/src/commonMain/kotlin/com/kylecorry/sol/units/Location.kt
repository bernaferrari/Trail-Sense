package com.kylecorry.sol.units

import com.kylecorry.sol.science.geology.Geology
import kotlin.math.hypot

/**
 * A location on Earth
 * @property coordinate The geographic coordinate
 * @property elevation The elevation (reference point is unspecified)
 */
public class Location(public val coordinate: Coordinate, public val elevation: Distance) {
    public fun distanceTo(other: Location, highAccuracy: Boolean = true): Float {
        val horizontal = horizontalDistanceTo(other, highAccuracy)
        val vertical = elevation.meters().value - other.elevation.meters().value
        return hypot(horizontal, vertical)
    }

    public fun inclinationTo(other: Location): Float {
        val distance = horizontalDistanceTo(other)
        val vertical = verticalDistanceTo(other)
        return Geology.getInclination(Distance.meters(distance), Distance.meters(vertical))
    }

    public fun horizontalDistanceTo(other: Location, highAccuracy: Boolean = true): Float {
        return coordinate.distanceTo(other.coordinate, highAccuracy)
    }

    public fun verticalDistanceTo(other: Location): Float {
        return other.elevation.meters().value - elevation.meters().value
    }

    public fun bearingTo(other: Location, highAccuracy: Boolean = true): Bearing {
        return coordinate.bearingTo(other.coordinate, highAccuracy)
    }
}
