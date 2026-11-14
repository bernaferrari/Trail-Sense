package com.kylecorry.sol.units

import kotlin.jvm.JvmInline

@JvmInline
public value class Energy private constructor(private val measure: Measure) {
    public val value: Float
        get() = measureValue(measure)

    public val units: EnergyUnits
        get() = measureUnit<EnergyUnits>(measure)

    public fun convertTo(newUnits: EnergyUnits): Energy {
        if (units == newUnits) {
            return this
        }
        val joules = value * units.joules
        return from((joules / newUnits.joules).toFloat(), newUnits)
    }

    override fun toString(): String {
        return "$value $units"
    }

    public fun joules(): Energy {
        return convertTo(EnergyUnits.Joules)
    }

    public companion object {
        public fun from(value: Float, units: EnergyUnits): Energy {
            return Energy(packMeasure(value, units))
        }
    }
}
