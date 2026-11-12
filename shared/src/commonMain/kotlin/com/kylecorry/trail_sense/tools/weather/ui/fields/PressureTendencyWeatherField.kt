package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.weather.ui.PressureCharacteristicImageMapper
import androidx.compose.ui.graphics.toArgb
import com.kylecorry.trail_sense.shared.views.TrailSenseColors

class PressureTendencyWeatherField(private val tendency: PressureTendency) : WeatherField {
    override fun getListItem(): WeatherListItem {
        val formatter = FormatService.getInstance()
        val units = getAppService<UserPreferences>().pressureUnits
        val color = TrailSenseColors.current.onSurfaceVariant.toArgb()
        val value = getAppService<AppStrings>().get(Res.string.pressure_tendency_format_2, formatter.formatPressure(
                Pressure.hpa(tendency.amount).convertTo(units),
                // One decimal more than readings only when readings have none (hPa); inHg's two are plenty
                // ("0.000 in" was noise).
                maxOf(1, Units.getDecimalPlaces(units))
            )
        )
        val icon = PressureCharacteristicImageMapper().getImageResource(tendency.characteristic)

        return WeatherListItem(
            id = 2,
            title = getAppService<AppStrings>().get(Res.string.pressure_tendency),
            icon = icon,
            iconTint = color,
            trailingText = value
        )
    }
}
