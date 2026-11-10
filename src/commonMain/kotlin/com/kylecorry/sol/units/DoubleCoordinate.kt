package com.kylecorry.sol.units


public data class DoubleCoordinate(
    override val latitude: Double,
    override val longitude: Double
) : ICoordinate {

    override fun toString(): String {
        return "$latitude, $longitude"
    }

    public fun toCoordinate(): Coordinate {
        return Coordinate(latitude, longitude)
    }


    public companion object {
        public val zero: DoubleCoordinate = DoubleCoordinate(0.0, 0.0)

        public fun from(coordinate: Coordinate): DoubleCoordinate {
            return DoubleCoordinate(coordinate.latitude, coordinate.longitude)
        }

        public fun constrained(latitude: Double, longitude: Double): DoubleCoordinate {
            return DoubleCoordinate(latitude.coerceIn(-90.0, 90.0), Coordinate.toLongitude(longitude))
        }
    }
}
