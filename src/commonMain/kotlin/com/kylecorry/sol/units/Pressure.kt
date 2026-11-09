package com.kylecorry.sol.units

import kotlin.jvm.JvmInline

@JvmInline
public value class Pressure private constructor(private val measure: Measure) : Comparable<Pressure> {
    public val value: Float
        get() = measureValue(measure)

    public val units: PressureUnits
        get() = measureUnit<PressureUnits>(measure)

    public fun convertTo(toUnits: PressureUnits): Pressure {
        if (units == toUnits) {
            return this
        }
        val hpa = value * units.hpa
        val newPressure = hpa / toUnits.hpa
        return from(newPressure.toFloat(), toUnits)
    }

    public fun hpa(): Pressure {
        return convertTo(PressureUnits.Hpa)
    }

    override fun toString(): String {
        return "$value $units"
    }

    override fun compareTo(other: Pressure): Int {
        val hpa = hpa().value
        val otherHpa = other.hpa().value
        return hpa.compareTo(otherHpa)
    }

    public companion object {
        public fun hpa(pressure: Float): Pressure {
            return from(pressure, PressureUnits.Hpa)
        }

        public fun from(value: Float, unit: PressureUnits): Pressure {
            return Pressure(packMeasure(value, unit))
        }
    }
}
