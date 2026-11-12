package com.kylecorry.trail_sense.tools.navigation.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.luna.text.toIntCompat
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.FloatPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.settings.infrastructure.ICompassStylePreferences
import com.kylecorry.trail_sense.shared.device.DeviceProfile
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds

class NavigationPreferences(
    private val cache: Preferences,
    private val device: DeviceProfile
) : ICompassStylePreferences {

    private var _showCalibrationOnNavigateDialog by BooleanPreference(
        cache,
        PreferenceKeys.showCalibrateOnNavigateDialog,
        true
    )

    var showCalibrationOnNavigateDialog: Boolean
        get() = device.hasCompass && _showCalibrationOnNavigateDialog
        set(value) {
            _showCalibrationOnNavigateDialog = value
        }

    private val screenLockMode by StringEnumPreference(
        cache,
        PreferenceKeys.navigationLockScreenMode,
        ScreenLockMode.entries.associateBy { it.id },
        ScreenLockMode.Never
    )

    val keepScreenUnlockedWhileNavigating: Boolean
        get() = screenLockMode != ScreenLockMode.Never

    val keepScreenUnlockedWhileOpen: Boolean
        get() = screenLockMode == ScreenLockMode.WhileOpen

    /** The dial's waves: 12 (one per 30°) or 8 (one per 45°). */
    val dialLobes: Int
        get() = cache.getString(PreferenceKeys.navigationDialLobes)?.toIntOrNull()?.takeIf { it == 8 || it == 12 } ?: 12

    override val useLinearCompass: Boolean
        get() = cache.getBoolean(PreferenceKeys.showLinearCompass) ?: false

    val showMultipleBeacons: Boolean
        get() = !device.hasCompass || cache.getBoolean(PreferenceKeys.displayMultiBeacons) ?: true

    val showNearbyBeaconsOnlyOnLinearCompass by BooleanPreference(
        cache,
        PreferenceKeys.nearbyLinearOnly,
        false
    )

    val numberOfVisibleBeacons: Int
        get() {
            val raw = cache.getString(PreferenceKeys.numVisibleBeacons) ?: "10"
            return raw.toIntOrNull() ?: 10
        }

    override val showDialTicksWhenNoCompass by BooleanPreference(
        cache,
        PreferenceKeys.showDialTicksWhenNoCompass,
        false
    )

    val highDetailMode by BooleanPreference(
        cache,
        PreferenceKeys.navigationHighDetailMode,
        false
    )

    var maxBeaconDistance: Float
        get() {
            val raw =
                cache.getString(PreferenceKeys.maxBeaconDistance) ?: "0.5"
            return Distance.kilometers(raw.toFloatCompat() ?: 0.5f)
                .meters()
                .value
                .coerceIn(1f, 25000000f)
        }
        set(value) {
            val meters = Distance.meters(value.coerceIn(1f, 25000000f))
            cache.putString(
                PreferenceKeys.maxBeaconDistance,
                meters.convertTo(DistanceUnits.Kilometers).value.toString()
            )
        }

    var radarCompassScale: Float by FloatPreference(
        cache,
        "cache_radar_compass_state_scale",
        2f
    )

    val leftButton: Int
        get() {
            val id = cache.getString(PreferenceKeys.navigationQuickActionLeft)
                ?.toIntCompat()
            return id ?: (ToolIds.PATHS.toInt() + ToolIds.TOOL_QUICK_ACTION_OFFSET)
        }

    val rightButton: Int
        get() {
            val id = cache.getString(PreferenceKeys.navigationQuickActionRight)
                ?.toIntCompat()
            return id ?: (ToolIds.OFFLINE_MAPS.toInt() + ToolIds.TOOL_QUICK_ACTION_OFFSET)
        }

    var speedometerMode by StringEnumPreference(
        cache,
        PreferenceKeys.navigationSpeedometerType,
        mapOf(
            "average" to SpeedometerMode.Backtrack,
            "instant_pedometer" to SpeedometerMode.CurrentPace,
            "average_pedometer" to SpeedometerMode.AveragePace,
            "instant" to SpeedometerMode.GPS
        ),
        SpeedometerMode.GPS
    )

    val showAzimuthIndicator by BooleanPreference(
        cache,
        PreferenceKeys.navigationShowAzimuthIndicator,
        true
    )

    var lockBearingToLocation by BooleanPreference(
        cache,
        PreferenceKeys.lockBearingToLocation,
        false
    )

    var bearingDistance: Distance
        get() {
            val raw = cache.getString(PreferenceKeys.bearingDistance) ?: "5.0"
            return Distance.meters(
                Distance.kilometers(raw.toFloatCompat() ?: 5.0f)
                    .meters()
                    .value
                    .coerceIn(1f, Destination.Bearing.maxBearingDistance.value)
            )
        }
        set(value) {
            val km = value.meters().value
                .coerceIn(1f, Destination.Bearing.maxBearingDistance.value)
                .let { Distance.meters(it).convertTo(DistanceUnits.Kilometers).value }
            cache.putString(
                PreferenceKeys.bearingDistance,
                km.toString()
            )
        }

    enum class ScreenLockMode(val id: String) {
        Never("never"),
        WhileNavigating("navigating"),
        WhileOpen("open")
    }

    enum class SpeedometerMode {
        Backtrack,
        GPS,
        CurrentPace,
        AveragePace
    }
}
