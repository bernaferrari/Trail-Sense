package com.kylecorry.sol.units

import kotlin.jvm.JvmInline

import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.sinDegrees
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.science.geography.formatting.CoordinateFormat
import com.kylecorry.sol.science.geography.formatting.DecimalDegreesCoordinateFormat
import com.kylecorry.sol.science.geography.formatting.DegreesDecimalMinutesCoordinateFormat
import com.kylecorry.sol.science.geography.formatting.DegreesMinutesSecondsCoordinateFormat
import com.kylecorry.sol.science.geography.formatting.MGRSCoordinateFormat
import com.kylecorry.sol.science.geography.formatting.OSGBCoordinateFormat
import com.kylecorry.sol.science.geography.formatting.USNGCoordinateFormat
import com.kylecorry.sol.science.geography.formatting.UTMCoordinateFormat
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * A geographic coordinate with precision down to about 1cm (7 decimal places).
 * Latitude is clamped to the range [-90, 90] and longitude is wrapped to [-180, 180].
 * If unclamped or more precise values are needeed, use the [DoubleCoordinate] class instead.
 */
@JvmInline
public value class Coordinate internal constructor(public val packed: Long) : ICoordinate {

    public constructor(latitude: Double, longitude: Double) : this(
        pack(latitude.coerceIn(-90.0, 90.0), toLongitude(longitude))
    )

    override val latitude: Double
        get() = unpackLatitude(packed)

    override val longitude: Double
        get() = unpackLongitude(packed)

    public val isNorthernHemisphere: Boolean
        get() = latitude >= 0

    public val antipode: Coordinate
        get() = Coordinate(-latitude, toLongitude(longitude + 180))

    override fun toString(): String {
        return "$latitude, $longitude"
    }

    public fun copy(latitude: Double = this.latitude, longitude: Double = this.longitude): Coordinate {
        return Coordinate(latitude, longitude)
    }

    public operator fun component1(): Double {
        return latitude
    }

    public operator fun component2(): Double {
        return longitude
    }

    public fun distanceTo(other: Coordinate, highAccuracy: Boolean = true): Float {
        return if (highAccuracy) {
            Geography.vincenty(this, other)[0]
        } else {
            Geography.haversine(this, other, EARTH_AVERAGE_RADIUS)[0]
        }
    }

    public fun plus(distance: Distance, bearing: Bearing): Coordinate {
        return plus(distance.meters().value.toDouble(), bearing)
    }

    public fun plus(meters: Double, bearing: Bearing): Coordinate {
        // Adapted from https://www.movable-type.co.uk/scripts/latlong.html
        val radius = EARTH_AVERAGE_RADIUS
        val newLat = asin(
            sinDegrees(latitude) * cos(meters / radius) +
                    cosDegrees(latitude) * sin(meters / radius) * cosDegrees(bearing.value.toDouble())
        ).toDegrees()

        val newLng = longitude + atan2(
            sinDegrees(bearing.value.toDouble()) * sin(meters / radius) * cosDegrees(latitude),
            cos(meters / radius) - sinDegrees(latitude) * sinDegrees(newLat)
        ).toDegrees()

        val normalLng = (newLng + 540) % 360 - 180

        return Coordinate(newLat, normalLng)
    }

    /**
     * Get the bearing to the other coordinate (using True North)
     */
    public fun bearingTo(other: Coordinate, highAccuracy: Boolean = true): Bearing {
        return if (highAccuracy) {
            Bearing.from(Geography.vincenty(this, other)[1])
        } else {
            Bearing.from(Geography.haversine(this, other, EARTH_AVERAGE_RADIUS)[1])
        }
    }

    public fun toCartesian(): Vector3 {
        val x = cosDegrees(latitude) * cosDegrees(longitude)
        val y = cosDegrees(latitude) * sinDegrees(longitude)
        val z = sinDegrees(latitude)

        return Vector3(x.toFloat(), y.toFloat(), z.toFloat())
    }

    /**
     * Get the angle between two coordinates (through the center of the Earth)
     */
    public fun degreesBetween(other: Coordinate): Float {
        val value =
            sinDegrees(latitude) * sinDegrees(other.latitude) +
                    cosDegrees(latitude) * cosDegrees(other.latitude) *
                    cosDegrees(longitude - other.longitude)
        return acos(value.coerceIn(-1.0, 1.0)).toDegrees().toFloat().absoluteValue
    }

    public companion object {

        public val zero: Coordinate = Coordinate(0.0, 0.0)
        private const val EARTH_AVERAGE_RADIUS = 6371.2e3
        private const val LONGITUDE_BITS = 32
        private const val LONGITUDE_MASK = 0xFFFFFFFFL
        private const val COORDINATE_SCALE = 1e7

        public fun toLongitude(degrees: Double): Double {
            return Arithmetic.wrap(degrees, -180.0, 180.0)
        }

        public fun parse(
            location: String,
            formats: List<CoordinateFormat> = defaultFormats
        ): Coordinate? {
            return formats.firstNotNullOfOrNull { it.parse(location) }
        }

        internal fun isValidLongitude(longitude: Double): Boolean {
            return longitude.absoluteValue <= 180
        }

        internal fun isValidLatitude(latitude: Double): Boolean {
            return latitude.absoluteValue <= 90
        }

        internal fun pack(latitude: Double, longitude: Double): Long {
            val lat = (latitude * COORDINATE_SCALE).roundToLong().toInt()
            val lon = (longitude * COORDINATE_SCALE).roundToLong().toInt()
            return (lat.toLong() shl LONGITUDE_BITS) or (lon.toLong() and LONGITUDE_MASK)
        }

        internal fun unpackLatitude(packed: Long): Double {
            return (packed shr LONGITUDE_BITS).toInt() / COORDINATE_SCALE
        }

        internal fun unpackLongitude(packed: Long): Double {
            return (packed and LONGITUDE_MASK).toInt() / COORDINATE_SCALE
        }

        private val defaultFormats = listOf(
            DecimalDegreesCoordinateFormat(),
            DegreesDecimalMinutesCoordinateFormat(),
            DegreesMinutesSecondsCoordinateFormat(),
            UTMCoordinateFormat(),
            MGRSCoordinateFormat(),
            USNGCoordinateFormat(),
            OSGBCoordinateFormat()
        )

    }
}
