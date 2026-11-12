package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import androidx.compose.ui.graphics.toArgb
import com.kylecorry.trail_sense.shared.views.TrailSenseColors

class HistoricTemperatureRangeWeatherField(
    private val low: Temperature?,
    private val high: Temperature?,
    private val onClick: () -> Unit
) : WeatherField {
    override fun getListItem(context: Context): WeatherListItem? {
        low ?: return null
        high ?: return null
        val formatter = FormatService.getInstance(context)
        val units = UserPreferences(context).temperatureUnits
        val lowValue = formatter.formatTemperature(
            low.convertTo(units)
        )
        val highValue = formatter.formatTemperature(
            high.convertTo(units)
        )

        val color = when {
            low.value <= WeatherSubsystem.COLD -> {
                AppColor.Blue.color
            }

            high.value >= WeatherSubsystem.HOT -> {
                AppColor.Red.color
            }

            else -> {
                TrailSenseColors.current.onSurfaceVariant.toArgb()
            }
        }

        return WeatherListItem(
            id = 9,
            title = context.getString(R.string.temperature_high_low),
            subtitle = context.getString(R.string.historic_temperature_years, 30),
            icon = TrailIcons.TemperatureRange,
            iconTint = color,
            trailingText = context.getString(R.string.slash_separated_pair, highValue, lowValue)
        ) {
            onClick()
        }
    }
}
