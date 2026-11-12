package com.kylecorry.trail_sense.tools.weather.infrastructure

import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.time.decodeStoredLocalDate

import com.kylecorry.trail_sense.shared.time.nowLocal

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.luna.text.toIntCompat
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.FloatPreference
import com.kylecorry.trail_sense.shared.device.DeviceProfile
import com.kylecorry.sol.math.Range
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import kotlin.time.Duration
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class WeatherPreferences(
    private val cache: Preferences,
    private val device: DeviceProfile
) : IWeatherPreferences {

    override val hasBarometer: Boolean
        get() = device.hasBarometer

    override var shouldMonitorWeather: Boolean
        get() = device.hasBarometer && (cache.getBoolean(PreferenceKeys.monitorWeather)
            ?: false)
        set(value) {
            cache.putBoolean(PreferenceKeys.monitorWeather, value)
        }

    override var pressureSmoothing: Float
        get() {
            val raw = (cache.getInt(PreferenceKeys.barometerPressureSmoothing)
                ?: 150) / 1000f
            return raw * 100
        }
        set(value) {
            val scaled = (value * 10).coerceIn(0f, 1000f)
            cache.putInt(
                PreferenceKeys.barometerPressureSmoothing,
                scaled.toInt()
            )
        }

    override var weatherUpdateFrequency: Duration
        get() {
            return cache.getDuration(PreferenceKeys.weatherUpdateFrequency)
                ?: 30.minutes
        }
        set(value) {
            cache.putDuration(PreferenceKeys.weatherUpdateFrequency, value)
        }

    override val shouldShowDailyWeatherNotification: Boolean
        get() = cache.getBoolean(PreferenceKeys.dailyWeatherNotification)
            ?: true

    override val shouldShowPressureInNotification: Boolean
        get() = cache.getBoolean(
            PreferenceKeys.showPressureInNotification
        ) ?: false

    override val shouldShowTemperatureInNotification: Boolean
        get() = cache.getBoolean(
            PreferenceKeys.showTemperatureInNotification
        ) ?: true

    override val useSeaLevelPressure: Boolean
        get() = cache.getBoolean(PreferenceKeys.useSeaLevelPressure) ?: true

    override val seaLevelFactorInTemp: Boolean
        get() = cache.getBoolean(PreferenceKeys.adjustForTemperature) ?: false

    override var barometerOffset by FloatPreference(
        cache,
        PreferenceKeys.barometerOffset,
        0f
    )

    override val pressureHistory: Duration
        get() {
            val raw = cache.getString(PreferenceKeys.pressureHistory) ?: "48"
            return (raw.toLong()).hours
        }

    override val sendStormAlerts: Boolean
        get() = cache.getBoolean(PreferenceKeys.sendStormAlert) ?: true

    override val hourlyForecastChangeThreshold: Float
        get() {
            return when (cache.getString(PreferenceKeys.forecastSensitivity)) {
                "low" -> HPA_FORECAST_LOW
                "high" -> HPA_FORECAST_HIGH
                else -> HPA_FORECAST_MEDIUM
            }
        }

    override val stormAlertThreshold: Float
        get() {
            return when (cache.getString(PreferenceKeys.stormAlertSensitivity)) {
                "low" -> HPA_STORM_LOW
                "high" -> HPA_STORM_HIGH
                else -> HPA_STORM_MEDIUM
            }
        }

    override var dailyWeatherLastSent: LocalDate?
        get() {
            return decodeStoredLocalDate(cache.getString("daily_weather_last_sent_date"))
        }
        set(value) {
            if (value == null) {
                cache.remove("daily_weather_last_sent_date")
            } else {
                cache.putString("daily_weather_last_sent_date", value.toString())
            }
        }

    override val dailyWeatherIsForTomorrow: Boolean
        get() = dailyForecastTime >= LocalTime(16, 0)

    override var dailyForecastTime: LocalTime
        get() {
            val raw = (cache.getString(PreferenceKeys.dailyWeatherTime)
                ?: LocalTime(7, 0).toString())
            return LocalTime.parse(raw)
        }
        set(value) {
            cache.putString(PreferenceKeys.dailyWeatherTime, value.toString())
        }

    override val leftButton: Int
        get() {
            val id = cache.getString(PreferenceKeys.weatherQuickActionLeft)
                ?.toIntCompat()
            return id ?: (ToolIds.CLOUDS.toInt() + ToolIds.TOOL_QUICK_ACTION_OFFSET)
        }

    override val rightButton: Int
        get() {
            val id = cache.getString(PreferenceKeys.weatherQuickActionRight)
                ?.toIntCompat()
            return id ?: (ToolIds.TEMPERATURE_ESTIMATION.toInt() + ToolIds.TOOL_QUICK_ACTION_OFFSET)
        }

    override val showColoredNotificationIcon: Boolean
        get() = cache.getBoolean(PreferenceKeys.weatherShowDetailedIcon)
            ?: true

    override val useAlarmForStormAlert: Boolean
        get() {
            val hours = stormAlertAlarmHours ?: return false
            val now = nowLocal().time
            return hours.contains(now)
        }

    val showPressureForecast by BooleanPreference(
        cache,
        PreferenceKeys.showPressureForecast,
        false
    )

    private val useAlarmForStormAlertInternal by BooleanPreference(
        cache,
        PreferenceKeys.weatherUseAlarmForStormAlert,
        false
    )

    private val muteStormAlarmAtNight by BooleanPreference(
        cache,
        PreferenceKeys.weatherMuteStormAlarmAtNight,
        true
    )

    private val stormAlertAlarmHours: Range<LocalTime>?
        get() {
            if (!useAlarmForStormAlertInternal) {
                return null
            }

            return if (muteStormAlarmAtNight) {
                Range(LocalTime(8, 0), LocalTime(20, 0))
            } else {
                Range(LocalTime(0, 0), LocalTime(23, 59, 59, 999_999_999))
            }
        }

    companion object {
        const val HPA_FORECAST_LOW = 2.5f
        const val HPA_FORECAST_MEDIUM = 1.5f
        const val HPA_FORECAST_HIGH = 0.5f

        const val HPA_STORM_LOW = -6f
        const val HPA_STORM_MEDIUM = -4.5f
        const val HPA_STORM_HIGH = -3f
    }

}
