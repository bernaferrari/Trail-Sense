package com.kylecorry.sol.units

import kotlin.jvm.JvmInline

@JvmInline
public value class Volume private constructor(private val measure: Measure) {
    public val value: Float
        get() = measureValue(measure)

    public val units: VolumeUnits
        get() = measureUnit<VolumeUnits>(measure)

    public fun convertTo(newUnits: VolumeUnits): Volume {
        if (units == newUnits) {
            return this
        }
        val l = value * units.liters
        return from((l / newUnits.liters).toFloat(), newUnits)
    }

    override fun toString(): String {
        return "$value $units"
    }

    public companion object {
        public fun from(value: Float, unit: VolumeUnits): Volume {
            return Volume(packMeasure(value, unit))
        }
    }
}
