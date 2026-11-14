package com.kylecorry.sol.science.geology

import com.kylecorry.sol.math.arithmetic.Arithmetic.square
import kotlin.math.sqrt

public data class ReferenceEllipsoid(val semiMajorAxis: Double, val semiMinorAxis: Double) {

    val a: Double = semiMajorAxis
    val b: Double = semiMinorAxis
    val flattening: Double = (a - b) / a
    val inverseFlattening: Double = 1 / flattening
    val squaredEccentricity: Double = (square(a) - square(b)) / (square(a))
    val eccentricity: Double = sqrt(squaredEccentricity)

    public companion object {

        public val wgs84: ReferenceEllipsoid = ReferenceEllipsoid(6378137.0, 6356752.3142)
        public val sphere: ReferenceEllipsoid = ReferenceEllipsoid(6371000.0, 6371000.0)

        public fun new(semiMajorAxis: Double, flattening: Double): ReferenceEllipsoid {
            val semiMinorAxis = -(flattening * semiMajorAxis - semiMajorAxis)
            return ReferenceEllipsoid(semiMajorAxis, semiMinorAxis)
        }
    }

}
