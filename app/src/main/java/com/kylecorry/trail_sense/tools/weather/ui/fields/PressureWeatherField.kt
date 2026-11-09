package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences

class PressureWeatherField(private val pressure: Pressure?) : WeatherField {
    override fun getListItem(context: Context): WeatherListItem? {
        pressure ?: return null
        val formatter = FormatService.getInstance(context)
        val units = UserPreferences(context).pressureUnits
        val color = Resources.androidTextColorSecondary(context)
        val value = formatter.formatPressure(
            pressure.convertTo(units),
            Units.getDecimalPlaces(units)
        )

        return WeatherListItem(
            id = 1,
            title = context.getString(R.string.pressure),
            icon = TrailIcons.Barometer,
            iconTint = color,
            trailingText = value
        )
    }
}
