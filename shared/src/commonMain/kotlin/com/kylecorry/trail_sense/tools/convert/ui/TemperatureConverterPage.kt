package com.kylecorry.trail_sense.tools.convert.ui

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.shared.FormatService

class TemperatureConverterPage :
    SimpleConverterPage<TemperatureUnits>(TemperatureUnits.Celsius, TemperatureUnits.Fahrenheit) {

    private val formatService by lazy { FormatService.getInstance() }

    override fun getUnitName(unit: TemperatureUnits): String {
        return when (unit) {
            TemperatureUnits.Fahrenheit -> getAppService<AppStrings>().get(Res.string.fahrenheit)
            TemperatureUnits.Celsius -> getAppService<AppStrings>().get(Res.string.celsius)
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
