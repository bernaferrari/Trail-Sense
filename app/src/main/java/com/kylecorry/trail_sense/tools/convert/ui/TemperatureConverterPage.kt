package com.kylecorry.trail_sense.tools.convert.ui

import android.content.Context
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService

class TemperatureConverterPage(private val context: Context) :
    SimpleConverterPage<TemperatureUnits>(TemperatureUnits.Celsius, TemperatureUnits.Fahrenheit) {

    private val formatService by lazy { FormatService.getInstance(context) }

    override fun getUnitName(unit: TemperatureUnits): String {
        return when (unit) {
            TemperatureUnits.Fahrenheit -> context.getString(R.string.fahrenheit)
            TemperatureUnits.Celsius -> context.getString(R.string.celsius)
        }
    }

    override val units: List<TemperatureUnits> = TemperatureUnits.values().toList()

    override fun convertValue(amount: Float, from: TemperatureUnits, to: TemperatureUnits): Float {
        return Temperature.from(amount, from).convertTo(to).value
    }

    override fun format(amount: Float, unit: TemperatureUnits): String {
        return formatService.formatTemperature(Temperature.from(amount, unit), 4, false)
    }

}
