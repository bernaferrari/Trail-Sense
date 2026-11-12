package com.kylecorry.trail_sense.tools.astronomy.infrastructure

import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.time.decodeStoredLocalDate

import android.content.Context
import com.kylecorry.luna.text.toIntCompat
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class AstronomyPreferences(private val context: Context) {

    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }

    val sunTimesMode: SunTimesMode
        get() {
            return when (cache.getString(PreferenceKeys.sunTimeMode)) {
                "civil" -> SunTimesMode.Civil
                "nautical" -> SunTimesMode.Nautical
                "astronomical" -> SunTimesMode.Astronomical
                else -> SunTimesMode.Actual
            }
        }

    val centerSunAndMoon: Boolean
        get() {
            return cache.getBoolean(PreferenceKeys.centerSunAndMoon) ?: false
        }

    private val sunMoonCompassMode: String
        get() = cache.getString(PreferenceKeys.showSunMoonCompass) ?: "when_up"

    val showOnCompass: Boolean
        get() = sunMoonCompassMode.let { it == "always" || it == "when_up" }

    val showOnCompassWhenDown: Boolean
        get() = sunMoonCompassMode == "always"

    var showAstronomyBands by BooleanPreference(
        cache,
        PreferenceKeys.showAstronomyBands,
        false
    )

    var sendSunsetAlerts by BooleanPreference(
        cache,
        PreferenceKeys.sunsetAlerts,
        false
    )

    var sendSunriseAlerts by BooleanPreference(
        cache,
        PreferenceKeys.sunriseAlerts,
        false
    )

    val sendAstronomyAlerts: Boolean
        get() {
            return sendLunarEclipseAlerts || sendMeteorShowerAlerts || sendSolarEclipseAlerts
        }

    // TODO: Let the user set this
    var astronomyAlertTime: LocalTime = LocalTime(10, 0)

    var sendLunarEclipseAlerts by BooleanPreference(
        cache,
        PreferenceKeys.sendLunarEclipseAlerts,
        false
    )

    var sendSolarEclipseAlerts by BooleanPreference(
        cache,
        PreferenceKeys.sendSolarEclipseAlerts,
        false
    )

    var sendMeteorShowerAlerts by BooleanPreference(
        cache,
        PreferenceKeys.sendMeteorShowerAlerts,
        false
    )

    val sunsetAlertMinutesBefore: Long
        get() {
            return (cache.getString(PreferenceKeys.sunsetAlertTime)
                ?: "60").toLong()
        }
    val sunriseAlertMinutesBefore: Long
        get() {
            return (cache.getString(PreferenceKeys.sunriseAlertTime)
                ?: "0").toLong()
        }

    val sunsetAlertLastSent: LocalDate?
        get() {
            return decodeStoredLocalDate(cache.getString(PreferenceKeys.sunsetAlertLastSentDate))
        }
    val sunriseAlertLastSent: LocalDate?
        get() {
            return decodeStoredLocalDate(cache.getString(PreferenceKeys.sunriseAlertLastSentDate))
        }

    fun setSunsetAlertLastSentDate(date: LocalDate) {
        cache.putString(
            PreferenceKeys.sunsetAlertLastSentDate,
            date.toString()
        )
    }

    fun setSunriseAlertLastSentDate(date: LocalDate) {
        cache.putString(
            PreferenceKeys.sunriseAlertLastSentDate,
            date.toString()
        )
    }

    val leftButton: Int
        get() {
            val id = cache.getString(PreferenceKeys.astronomyQuickActionLeft)
                ?.toIntCompat()
            return id ?: ToolIds.QUICK_ACTION_FLASHLIGHT
        }

    val rightButton: Int
        get() {
            val id = cache.getString(PreferenceKeys.astronomyQuickActionRight)
                ?.toIntCompat()
            return id ?: ToolIds.QUICK_ACTION_SUNSET_ALERT
        }

    val startCameraIn3DView by BooleanPreference(
        cache,
        PreferenceKeys.startCameraIn3dView,
        true
    )

    // Alarms
    val useAlarmForSunsetAlert by BooleanPreference(
        cache,
        PreferenceKeys.astronomyUseAlarmForSunsetAlert,
        false
    )

    val useAlarmForSunriseAlert by BooleanPreference(
        cache,
        PreferenceKeys.astronomyUseAlarmForSunriseAlert,
        false
    )

}
