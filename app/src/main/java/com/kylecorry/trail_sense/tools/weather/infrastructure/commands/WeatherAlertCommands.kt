package com.kylecorry.trail_sense.tools.weather.infrastructure.commands

import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.IWeatherRepo
import android.content.Context
import com.kylecorry.andromeda.core.time.SystemZonedDateTimeProvider
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.shared.preferences.PreferencesFlag
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.CurrentWeatherAlerter
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.DailyWeatherAlerter
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.StormAlerter
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.IWeatherSubsystem
import kotlin.time.Duration.Companion.seconds

/** Wires the shared weather alert commands to their Android notifications, sensors, and storage. */
object WeatherAlertCommands {

    fun storm(context: Context): StormAlertCommand {
        val prefs = PreferencesSubsystem.getInstance(context).preferences
        return StormAlertCommand(
            PreferencesFlag(prefs, PreferenceKeys.justSentAlert),
            UserPreferences(context).weather,
            StormAlerter(context)
        )
    }

    fun current(context: Context): CurrentWeatherAlertCommand {
        val prefs = UserPreferences(context)
        return CurrentWeatherAlertCommand(
            prefs.weather,
            CurrentWeatherAlerter(
                context,
                FormatService.getInstance(context),
                prefs.pressureUnits,
                prefs.temperatureUnits,
                prefs.weather
            )
        )
    }

    fun daily(context: Context): DailyWeatherAlertCommand {
        val prefs = UserPreferences(context).weather
        return DailyWeatherAlertCommand(
            prefs,
            DailyWeatherAlerter(context, FormatService.getInstance(context), prefs),
            SystemZonedDateTimeProvider()
        )
    }

    fun monitor(context: Context): MonitorWeatherCommand {
        return MonitorWeatherCommand(
            getAppService<IWeatherRepo>(),
            WeatherObserver(context, 10.seconds),
            getAppService<IWeatherSubsystem>(),
            SendWeatherAlertsCommand(context)
        )
    }
}
