package com.kylecorry.trail_sense.tools.weather.infrastructure

import kotlin.time.Duration.Companion.seconds
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import com.kylecorry.andromeda.background.AlwaysOnTaskScheduler
import com.kylecorry.andromeda.background.TaskSchedulerFactory
import com.kylecorry.andromeda.background.services.ForegroundInfo
import com.kylecorry.andromeda.background.services.IntervalService
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.tryStartForegroundOrNotify
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.permissions.canStartLocationForegroundService
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.CurrentWeatherAlerter
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

class WeatherMonitorService :
    IntervalService(wakelockDuration = 60.seconds, useOneTimeWorkers = true) {

    private val prefs by lazy { UserPreferences(applicationContext) }

    @SuppressLint("InlinedApi")
    override fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            WEATHER_NOTIFICATION_ID,
            CurrentWeatherAlerter.getDefaultNotification(applicationContext),
            if (!Permissions.canStartLocationForegroundService(applicationContext)) {
                listOf(ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                // This shouldn't be needed, but I was seeing several crashes on Android 14 around security exceptions (might be a known bug in Android), so I'm adding it to experiment
                listOf(
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            }
        )
    }

    override val uniqueId: Int
        get() = 2387092

    override val period: Duration
        get() = prefs.weather.weatherUpdateFrequency

    override suspend fun doWork() {
        WeatherSubsystem.getInstance(applicationContext).updateWeather()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        getAppService<Logger>().info(TAG, "Started (period: $period, restarted by system: ${intent == null})")
        isRunning = true
        return tryStartForegroundOrNotify {
            super.onStartCommand(intent, flags, startId)
        }
    }

    override fun onDestroy() {
        getAppService<Logger>().info(TAG, "Stopped")
        isRunning = false
        stopService(true)
        super.onDestroy()
    }

    companion object {

        const val WEATHER_NOTIFICATION_ID = 1
        private const val TAG = "WeatherMonitorService"

        var isRunning = false
            private set

        fun start(context: Context) {
            scheduler(context).start()
        }

        fun stop(context: Context) {
            scheduler(context).cancel()
        }

        private fun scheduler(context: Context): AlwaysOnTaskScheduler {
            return TaskSchedulerFactory(context).alwaysOn(
                WeatherMonitorService::class.java,
                foreground = true
            )
        }

    }
}
