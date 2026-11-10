package com.kylecorry.sol.science.geography.projections

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Coordinate

public interface IMapProjection {

    public fun toPixels(location: Coordinate): Vector2

    public fun toPixels(latitude: Double, longitude: Double): Vector2

    public fun toCoordinate(pixel: Vector2): Coordinate

}
