package com.kylecorry.sol.units

import kotlin.jvm.JvmInline

@JvmInline
public value class Speed private constructor(private val measure: Measure) {
    public val value: Float
        get() = measureValue(measure)
    public val distanceUnits: DistanceUnits
        get() = measureUnit1<DistanceUnits>(measure)
    public val timeUnits: TimeUnits
        get() = measureUnit2<TimeUnits>(measure)

    public fun convertTo(newDistanceUnits: DistanceUnits, newTimeUnits: TimeUnits): Speed {
        val distance = Distance.from(value, distanceUnits).convertTo(newDistanceUnits).value
        val newSpeed = (distance / timeUnits.seconds) * newTimeUnits.seconds
        return from(newSpeed.toFloat(), newDistanceUnits, newTimeUnits)
    }

    override fun toString(): String {
        return "$value $distanceUnits / $timeUnits"
    }

    public companion object {
        public fun from(value: Float, distanceUnits: DistanceUnits, timeUnits: TimeUnits): Speed {
            return Speed(packMeasureMultiUnit(value, distanceUnits, timeUnits))
        }
    }

}
