package com.kylecorry.sol.science.geography.projections

import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.MathExtensions.toRadians
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.arithmetic.Arithmetic.clamp
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.sinDegrees
import com.kylecorry.sol.units.Coordinate
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.sinh

public class MercatorProjection(private val scale: Float = 1f) : IMapProjection {
    override fun toCoordinate(pixel: Vector2): Coordinate {
        // Double math keeps the inverse projection precise near the poles on every platform
        val longitude = (pixel.x.toDouble() / scale).toDegrees()
        val latitude = atan(sinh(pixel.y.toDouble() / scale)).toDegrees()
        return Coordinate(
            clamp(latitude, -90.0, 90.0),
            Coordinate.toLongitude(longitude)
        )
    }

    override fun toPixels(location: Coordinate): Vector2 {
        return toPixels(location.latitude, location.longitude)
    }

    override fun toPixels(latitude: Double, longitude: Double): Vector2 {
        val x = scale * longitude.toRadians()
        val sinLat = sinDegrees(latitude)
        val y = scale * 0.5 * ln((1 + sinLat) / (1 - sinLat))
        return Vector2(x.toFloat(), y.toFloat())
    }

    public companion object {
        public fun getScaleForLatitude(latitude: Double): Float {
            return cosDegrees(latitude).toFloat()
        }

        public const val MAX_LATITUDE: Double = 85.05112878
        public const val MIN_LATITUDE: Double = -85.05112878
    }

}
