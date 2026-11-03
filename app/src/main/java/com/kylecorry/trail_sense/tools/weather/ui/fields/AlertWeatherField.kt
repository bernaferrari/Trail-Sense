package com.kylecorry.trail_sense.tools.weather.ui.fields

import android.content.Context
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.weather.domain.WeatherAlert

class AlertWeatherField(private val alerts: List<WeatherAlert>) : WeatherField {
    override fun getListItem(context: Context): WeatherListItem? {
        if (alerts.isEmpty()) {
            return null
        }

        val title = context.getString(R.string.alerts)
        val description = alerts.joinToString("\n") { formatWeatherAlert(context, it) }
        val moreDescription =
            alerts.joinToString("\n\n") { formatWeatherAlertDescription(context, it) }

        return WeatherListItem(
            id = 6293,
            title = title,
            icon = R.drawable.ic_alert,
            iconTint = AppColor.Yellow.color,
            trailingText = description
        ) {
            Alerts.dialog(
                context,
                title,
                moreDescription,
                cancelText = null
            )
        }
    }

    private fun formatWeatherAlert(context: Context, alert: WeatherAlert): String {
        return when (alert) {
            WeatherAlert.Storm -> context.getString(R.string.weather_storm)
            WeatherAlert.Hot -> context.getString(R.string.hot)
            WeatherAlert.Cold -> context.getString(R.string.cold)
        }
    }

    private fun formatWeatherAlertDescription(context: Context, alert: WeatherAlert): String {
        return when (alert) {
            WeatherAlert.Storm -> context.getString(R.string.weather_alert_storm_description)
            WeatherAlert.Hot -> context.getString(R.string.weather_alert_hot_description)
            WeatherAlert.Cold -> context.getString(R.string.weather_alert_cold_description)
        }
    }
}
