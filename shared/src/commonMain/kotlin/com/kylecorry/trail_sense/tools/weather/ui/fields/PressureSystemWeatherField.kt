package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.tools.weather.domain.isHigh
import com.kylecorry.trail_sense.tools.weather.domain.isLow

class PressureSystemWeatherField(private val pressure: Pressure?) : WeatherField {
    override fun getListItem(): WeatherListItem? {
        pressure ?: return null
        val name: String
        val description: String
        val icon: androidx.compose.ui.graphics.vector.ImageVector
        if (pressure.isHigh()) {
            name = getAppService<AppStrings>().get(Res.string.high_pressure)
            description = getAppService<AppStrings>().get(Res.string.high_pressure_system_description)
            icon = TrailIcons.HighPressureSystem
        } else if (pressure.isLow()) {
            name = getAppService<AppStrings>().get(Res.string.low_pressure)
            description = getAppService<AppStrings>().get(Res.string.low_pressure_system_description)
            icon = TrailIcons.LowPressureSystem
        } else {
            return null
        }

        return WeatherListItem(
            id = 3,
            title = getAppService<AppStrings>().get(Res.string.pressure_system),
            icon = icon,
            trailingText = name
        ) {
            Alerts.dialog(AppHost.current(), name, description, cancelText = null)
        }
    }
}
