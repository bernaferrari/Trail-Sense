package com.kylecorry.sol.units

import kotlin.jvm.JvmInline

import com.kylecorry.sol.math.Range

@JvmInline
public value class Temperature private constructor(private val measure: Measure) : Comparable<Temperature> {
    public val value: Float
        get() = measureValue(measure)

    public val units: TemperatureUnits
        get() = measureUnit<TemperatureUnits>(measure)

    public fun convertTo(toUnits: TemperatureUnits): Temperature {
        if (units == toUnits) {
            return this
        }

        val c = when (units) {
            TemperatureUnits.Celsius -> value.toDouble()
            TemperatureUnits.Fahrenheit -> (value - 32) * 5 / 9.0
        }

        val newTemp = when (toUnits) {
            TemperatureUnits.Celsius -> c
            TemperatureUnits.Fahrenheit -> (c * 9 / 5.0) + 32
        }

        return from(newTemp.toFloat(), toUnits)
    }

    public fun celsius(): Temperature {
        return convertTo(TemperatureUnits.Celsius)
    }

    override fun toString(): String {
        return "$value $units"
    }

    override fun compareTo(other: Temperature): Int {
        return celsius().value.compareTo(other.celsius().value)
    }

    public companion object {
        public val zero: Temperature = celsius(0f)

        public val ABSOLUTE_ZERO: Temperature = celsius(-273.15f)

        public fun from(value: Float, unit: TemperatureUnits): Temperature {
            return Temperature(packMeasure(value, unit))
        }

        public fun celsius(temperature: Float): Temperature {
            return from(temperature, TemperatureUnits.Celsius)
        }

        public fun fahrenheit(temperature: Float): Temperature {
            return from(temperature, TemperatureUnits.Fahrenheit)
        }
    }
}

public fun Range<Temperature>.average(): Temperature {
    val startC = start.celsius().value
    val endC = end.celsius().value
    val averageC = (startC + endC) / 2
    return Temperature.celsius(averageC).convertTo(start.units)
}
