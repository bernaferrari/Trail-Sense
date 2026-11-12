package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource
import com.kylecorry.trail_sense.tools.weather.domain.TemperatureThresholds
import androidx.compose.ui.graphics.toArgb
import com.kylecorry.trail_sense.shared.views.TrailSenseColors

// TODO: Build chart into this
class TemperatureWeatherField(
    private val temperature: Temperature?,
    private val onClick: () -> Unit
) : WeatherField {
    override fun getListItem(): WeatherListItem? {
        temperature ?: return null
        val formatter = FormatService.getInstance()
        val prefs = getAppService<UserPreferences>()
        val units = prefs.temperatureUnits
        val source = prefs.thermometer.source
        val value = formatter.formatTemperature(
            temperature.convertTo(units)
        )

        val color = when {
            temperature.value <= TemperatureThresholds.COLD -> {
                AppColor.Blue.color
            }

            temperature.value >= TemperatureThresholds.HOT -> {
                AppColor.Red.color
            }

            else -> {
                TrailSenseColors.current.onSurfaceVariant.toArgb()
            }
        }

        return WeatherListItem(
            id = 5,
            title = getAppService<AppStrings>().get(Res.string.temperature),
            subtitle = when (source) {
                ThermometerSource.Historic -> getAppService<AppStrings>().get(Res.string.historic_temperature_years,
                    30
                )

                ThermometerSource.Sensor -> getAppService<AppStrings>().get(Res.string.sensor)
            },
            icon = TrailIcons.Thermometer,
            iconTint = color,
            trailingText = value
        ) {
            onClick()
        }
    }
}
