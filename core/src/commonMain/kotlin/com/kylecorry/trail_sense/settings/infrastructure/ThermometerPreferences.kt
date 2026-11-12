package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.sensors.thermometer.ITemperatureCalibrator
import com.kylecorry.trail_sense.shared.sensors.thermometer.RangeTemperatureCalibrator
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource

class ThermometerPreferences(preferences: Preferences) : PreferenceRepo(preferences), IThermometerPreferences {
    override val source by StringEnumPreference(
        cache, PreferenceKeys.thermometerSource,
        mapOf(
            "historic" to ThermometerSource.Historic,
            "sensor" to ThermometerSource.Sensor
        ),
        ThermometerSource.Historic
    )

    override var smoothing: Float
        get() {
            return (cache.getInt(PreferenceKeys.temperatureSmoothing)
                ?: 0) / 1000f
        }
        set(value) {
            val scaled = (value * 1000).coerceIn(0f, 1000f)
            cache.putInt(
                PreferenceKeys.temperatureSmoothing,
                scaled.toInt()
            )
        }

    override var minBatteryTemperature: Float
        get() = cache.getString(PreferenceKeys.minUncalibratedTempC)
            ?.toFloatCompat() ?: 0f
        set(value) {
            cache.putString(
                PreferenceKeys.minUncalibratedTempC,
                value.toString()
            )
        }

    override var minActualTemperature: Float
        get() = cache.getString(PreferenceKeys.minCalibratedTempC)
            ?.toFloatCompat() ?: 0f
        set(value) {
            cache.putString(
                PreferenceKeys.minCalibratedTempC,
                value.toString()
            )
        }

    override var maxBatteryTemperature: Float
        get() = cache.getString(PreferenceKeys.maxUncalibratedTempC)
            ?.toFloatCompat() ?: 100f
        set(value) {
            cache.putString(
                PreferenceKeys.maxUncalibratedTempC,
                value.toString()
            )
        }

    override var maxActualTemperature: Float
        get() = cache.getString(PreferenceKeys.maxCalibratedTempC)
            ?.toFloatCompat() ?: 100f
        set(value) {
            cache.putString(
                PreferenceKeys.maxCalibratedTempC,
                value.toString()
            )
        }

    override var minBatteryTemperatureF: Float
        get() = cache.getString(PreferenceKeys.minUncalibratedTempF)
            ?.toFloatCompat() ?: 32f
        set(value) {
            cache.putString(
                PreferenceKeys.minUncalibratedTempF,
                value.toString()
            )
        }

    override var minActualTemperatureF: Float
        get() = cache.getString(PreferenceKeys.minCalibratedTempF)
            ?.toFloatCompat() ?: 32f
        set(value) {
            cache.putString(
                PreferenceKeys.minCalibratedTempF,
                value.toString()
            )
        }

    override var maxBatteryTemperatureF: Float
        get() = cache.getString(PreferenceKeys.maxUncalibratedTempF)
            ?.toFloatCompat() ?: 212f
        set(value) {
            cache.putString(
                PreferenceKeys.maxUncalibratedTempF,
                value.toString()
            )
        }

    override var maxActualTemperatureF: Float
        get() = cache.getString(PreferenceKeys.maxCalibratedTempF)
            ?.toFloatCompat() ?: 212f
        set(value) {
            cache.putString(
                PreferenceKeys.maxCalibratedTempF,
                value.toString()
            )
        }
    override val calibrator: ITemperatureCalibrator
        get() = RangeTemperatureCalibrator(
            minBatteryTemperature,
            maxBatteryTemperature,
            minActualTemperature,
            maxActualTemperature
        )

    override fun resetThermometerCalibration() {
        minActualTemperature = 0f
        minActualTemperatureF = 32f
        maxActualTemperature = 100f
        maxActualTemperatureF = 212f
        minBatteryTemperature = 0f
        minBatteryTemperatureF = 32f
        maxBatteryTemperature = 100f
        maxBatteryTemperatureF = 212f
    }
}
