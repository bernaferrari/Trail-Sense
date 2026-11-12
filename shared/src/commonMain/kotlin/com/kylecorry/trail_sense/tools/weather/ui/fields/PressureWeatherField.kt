package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import androidx.compose.ui.graphics.toArgb
import com.kylecorry.trail_sense.shared.views.TrailSenseColors

class PressureWeatherField(private val pressure: Pressure?) : WeatherField {
    override fun getListItem(): WeatherListItem? {
        pressure ?: return null
        val formatter = FormatService.getInstance()
        val units = getAppService<UserPreferences>().pressureUnits
        val color = TrailSenseColors.current.onSurfaceVariant.toArgb()
        val value = formatter.formatPressure(
            pressure.convertTo(units),
            Units.getDecimalPlaces(units)
        )

        return WeatherListItem(
            id = 1,
            title = getAppService<AppStrings>().get(Res.string.pressure),
            icon = TrailIcons.Barometer,
            iconTint = color,
            trailingText = value
        )
    }
}
