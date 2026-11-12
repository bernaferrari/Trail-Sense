package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.weather.domain.TemperatureThresholds
import androidx.compose.ui.graphics.toArgb
import com.kylecorry.trail_sense.shared.views.TrailSenseColors

class HistoricTemperatureRangeWeatherField(
    private val low: Temperature?,
    private val high: Temperature?,
    private val onClick: () -> Unit
) : WeatherField {
    override fun getListItem(): WeatherListItem? {
        low ?: return null
        high ?: return null
        val formatter = FormatService.getInstance()
        val units = getAppService<UserPreferences>().temperatureUnits
        val lowValue = formatter.formatTemperature(
            low.convertTo(units)
        )
        val highValue = formatter.formatTemperature(
            high.convertTo(units)
        )

        val color = when {
            low.value <= TemperatureThresholds.COLD -> {
                AppColor.Blue.color
            }

            high.value >= TemperatureThresholds.HOT -> {
                AppColor.Red.color
            }

            else -> {
                TrailSenseColors.current.onSurfaceVariant.toArgb()
            }
        }

        return WeatherListItem(
            id = 9,
            title = getAppService<AppStrings>().get(Res.string.temperature_high_low),
            subtitle = getAppService<AppStrings>().get(Res.string.historic_temperature_years, 30),
            icon = TrailIcons.TemperatureRange,
            iconTint = color,
            trailingText = getAppService<AppStrings>().get(Res.string.slash_separated_pair, highValue, lowValue)
        ) {
            onClick()
        }
    }
}
