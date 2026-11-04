package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.weather.domain.isHigh
import com.kylecorry.trail_sense.tools.weather.domain.isLow

class PressureSystemWeatherField(private val pressure: Pressure?) : WeatherField {
    override fun getListItem(context: Context): WeatherListItem? {
        pressure ?: return null
        val name: String
        val description: String
        val icon: androidx.compose.ui.graphics.vector.ImageVector
        if (pressure.isHigh()) {
            name = context.getString(R.string.high_pressure)
            description = context.getString(R.string.high_pressure_system_description)
            icon = TrailIcons.HighPressureSystem
        } else if (pressure.isLow()) {
            name = context.getString(R.string.low_pressure)
            description = context.getString(R.string.low_pressure_system_description)
            icon = TrailIcons.LowPressureSystem
        } else {
            return null
        }

        return WeatherListItem(
            id = 3,
            title = context.getString(R.string.pressure_system),
            icon = icon,
            trailingText = name
        ) {
            Alerts.dialog(context, name, description, cancelText = null)
        }
    }
}
