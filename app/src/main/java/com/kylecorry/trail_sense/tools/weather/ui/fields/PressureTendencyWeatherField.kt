package com.kylecorry.trail_sense.tools.weather.ui.fields

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.weather.ui.PressureCharacteristicImageMapper

class PressureTendencyWeatherField(private val tendency: PressureTendency) : WeatherField {
    override fun getListItem(context: Context): WeatherListItem {
        val formatter = FormatService.getInstance(context)
        val units = UserPreferences(context).pressureUnits
        val color = Resources.androidTextColorSecondary(context)
        val value = context.getString(
            R.string.pressure_tendency_format_2, formatter.formatPressure(
                Pressure.hpa(tendency.amount).convertTo(units),
                // One decimal more than readings only when readings have none (hPa); inHg's two are plenty
                // ("0.000 in" was noise).
                maxOf(1, Units.getDecimalPlaces(units))
            )
        )
        val icon = PressureCharacteristicImageMapper().getImageResource(tendency.characteristic)

        return WeatherListItem(
            id = 2,
            title = context.getString(R.string.pressure_tendency),
            icon = icon,
            iconTint = color,
            trailingText = value
        )
    }
}
