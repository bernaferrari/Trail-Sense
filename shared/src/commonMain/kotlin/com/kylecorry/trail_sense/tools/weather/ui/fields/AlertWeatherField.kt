package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.weather.domain.WeatherAlert

class AlertWeatherField(private val alerts: List<WeatherAlert>) : WeatherField {
    override fun getListItem(): WeatherListItem? {
        if (alerts.isEmpty()) {
            return null
        }

        val title = getAppService<AppStrings>().get(Res.string.alerts)
        // The row names the alert itself ("Cold"), and under it the first thing to do; "Alerts" over "Cold" said
        // nothing. The dialog keeps the full advice.
        val rowTitle = alerts.joinToString(" · ") { formatWeatherAlert(it) }
        val description = if (alerts.size == 1) {
            formatWeatherAlertDescription(alerts.first()).substringBefore(". ").trimEnd('.') + "."
        } else {
            getAppService<AppStrings>().get(Res.string.weather_alerts_tap_for_advice)
        }
        val moreDescription =
            alerts.joinToString("\n\n") { formatWeatherAlertDescription(it) }

        return WeatherListItem(
            id = 6293,
            title = rowTitle,
            icon = TrailIcons.Alert,
            iconTint = AppColor.Yellow.color,
            trailingText = description
        ) {
            Alerts.dialog(AppHost.current(),
                title,
                moreDescription,
                cancelText = null
            )
        }
    }

    private fun formatWeatherAlert(alert: WeatherAlert): String {
        return when (alert) {
            WeatherAlert.Storm -> getAppService<AppStrings>().get(Res.string.weather_storm)
            WeatherAlert.Hot -> getAppService<AppStrings>().get(Res.string.hot)
            WeatherAlert.Cold -> getAppService<AppStrings>().get(Res.string.cold)
        }
    }

    private fun formatWeatherAlertDescription(alert: WeatherAlert): String {
        return when (alert) {
            WeatherAlert.Storm -> getAppService<AppStrings>().get(Res.string.weather_alert_storm_description)
            WeatherAlert.Hot -> getAppService<AppStrings>().get(Res.string.weather_alert_hot_description)
            WeatherAlert.Cold -> getAppService<AppStrings>().get(Res.string.weather_alert_cold_description)
        }
    }
}
