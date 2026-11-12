@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.weather.ui

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.settings.ui.SettingsOption
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.notifications.NotificationChannelControl
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.CurrentWeatherAlerter

/** The weather settings with the app's quick actions, notification channel and CSV file picker. */
@Composable
fun WeatherSettingsToolScreen() {
    val context = LocalContext.current
    // MainActivity is the app's IntentResultRetriever, which the CSV export's file picker needs.
    val intentResultRetriever = LocalActivity.current as IntentResultRetriever
    val quickActionOptions = remember(context) {
        Tools.getQuickActions(context).map { SettingsOption(it.id.toString(), it.name) }
    }
    val notificationChannel = remember(context) {
        AndroidNotificationChannel(context, CurrentWeatherAlerter.WEATHER_CHANNEL_ID)
    }
    val csvExporter = remember(context, intentResultRetriever) {
        IOFactory().createCsvService(context, intentResultRetriever)
    }
    WeatherSettingsScreen(
        quickActionOptions = quickActionOptions,
        notificationChannel = notificationChannel,
        csvExporter = csvExporter
    )
}

private class AndroidNotificationChannel(
    private val context: Context,
    private val channelId: String
) : NotificationChannelControl {
    override fun isBlocked(): Boolean = Notify.isChannelBlocked(context, channelId)

    override fun openSettings() {
        context.startActivity(Intents.notificationSettings(context, channelId))
    }
}
