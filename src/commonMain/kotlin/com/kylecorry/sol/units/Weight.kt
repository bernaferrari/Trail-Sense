package com.kylecorry.sol.units

import kotlin.jvm.JvmInline

import kotlin.math.absoluteValue

@JvmInline
public value class Weight private constructor(private val measure: Measure) {
    public val value: Float
        get() = measureValue(measure)

    public val units: WeightUnits
        get() = measureUnit<WeightUnits>(measure)

    public fun convertTo(newUnits: WeightUnits): Weight {
        if (units == newUnits) {
            return this
        }
        val grams = value * units.grams
        return from((grams / newUnits.grams).toFloat(), newUnits)
    }

    public operator fun plus(other: Weight): Weight {
        val otherInUnits = other.convertTo(units)
        return from(value + otherInUnits.value, units)
    }

    public operator fun times(amount: Number): Weight {
        return from(value * amount.toFloat().absoluteValue, units)
    }

    override fun toString(): String {
        return "$value $units"
    }

    public companion object {
        public fun from(value: Float, units: WeightUnits): Weight {
            return Weight(packMeasure(value, units))
        }
    }
}
