package com.kylecorry.sol.units

import kotlin.jvm.JvmInline

@JvmInline
public value class Distance private constructor(private val measure: Measure) : Comparable<Distance> {
    public val value: Float
        get() = measureValue(measure)

    public val units: DistanceUnits
        get() = measureUnit<DistanceUnits>(measure)


    public fun convertTo(newUnits: DistanceUnits): Distance {
        val m = value * units.meters
        val newDistance = m / newUnits.meters
        return from(newDistance.toFloat(), newUnits)
    }

    public fun meters(): Distance {
        return convertTo(DistanceUnits.Meters)
    }

    public operator fun times(multiplier: Float): Distance {
        return from(value * multiplier, units)
    }

    override fun toString(): String {
        return "$value $units"
    }

    public companion object {

        public fun meters(distance: Float): Distance {
            return from(distance, DistanceUnits.Meters)
        }

        public fun feet(distance: Float): Distance {
            return from(distance, DistanceUnits.Feet)
        }

        public fun centimeters(distance: Float): Distance {
            return from(distance, DistanceUnits.Centimeters)
        }

        public fun kilometers(distance: Float): Distance {
            return from(distance, DistanceUnits.Kilometers)
        }

        public fun miles(distance: Float): Distance {
            return from(distance, DistanceUnits.Miles)
        }

        public fun nauticalMiles(distance: Float): Distance {
            return from(distance, DistanceUnits.NauticalMiles)
        }

        public fun yards(distance: Float): Distance {
            return from(distance, DistanceUnits.Yards)
        }

        public fun from(value: Float, unit: DistanceUnits): Distance {
            return Distance(packMeasure(value, unit))
        }

    }

    override fun compareTo(other: Distance): Int {
        val meters = meters().value
        val otherMeters = other.meters().value
        return meters.compareTo(otherMeters)
    }
}
