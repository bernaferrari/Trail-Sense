package com.kylecorry.trail_sense.shared

import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.IntEnumPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.andromeda.preferences.getIntArray
import com.kylecorry.andromeda.preferences.getLongArray
import com.kylecorry.andromeda.preferences.putIntArray
import com.kylecorry.andromeda.preferences.putLongArray
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.main.MAX_BOTTOM_NAVIGATION_ITEMS
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.alerts.NotificationGroupBehavior
import com.kylecorry.trail_sense.shared.device.DeviceProfile
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.settings.backup.BackupPreferences
import com.kylecorry.trail_sense.settings.infrastructure.AltimeterPreferences
import com.kylecorry.trail_sense.settings.infrastructure.AugmentedRealityPreferences
import com.kylecorry.trail_sense.settings.infrastructure.BubbleLevelPreferences
import com.kylecorry.trail_sense.settings.infrastructure.CameraPreferences
import com.kylecorry.trail_sense.settings.infrastructure.CellSignalPreferences
import com.kylecorry.trail_sense.settings.infrastructure.ClinometerPreferences
import com.kylecorry.trail_sense.settings.infrastructure.ClockPreferences
import com.kylecorry.trail_sense.settings.infrastructure.CompassPreferences
import com.kylecorry.trail_sense.settings.infrastructure.DebugPreferences
import com.kylecorry.trail_sense.settings.infrastructure.ErrorPreferences
import com.kylecorry.trail_sense.settings.infrastructure.FlashlightPreferenceRepo
import com.kylecorry.trail_sense.settings.infrastructure.GPSPreferences
import com.kylecorry.trail_sense.settings.infrastructure.IDeclinationPreferences
import com.kylecorry.trail_sense.settings.infrastructure.MetalDetectorPreferences
import com.kylecorry.trail_sense.settings.infrastructure.PackPreferences
import com.kylecorry.trail_sense.settings.infrastructure.PedometerPreferences
import com.kylecorry.trail_sense.settings.infrastructure.PowerPreferences
import com.kylecorry.trail_sense.settings.infrastructure.PrivacyPreferences
import com.kylecorry.trail_sense.settings.infrastructure.ThermometerPreferences
import com.kylecorry.trail_sense.settings.infrastructure.TidePreferences
import com.kylecorry.trail_sense.shared.sharing.MapSite
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.AstronomyPreferences
import com.kylecorry.trail_sense.tools.ballistics.infrastructure.BallisticsPreferences
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPreferences
import com.kylecorry.trail_sense.tools.climate.infrastructure.ClimatePreferenceRepo
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuidePreferences
import com.kylecorry.trail_sense.tools.map.infrastructure.MapPreferences
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.PhotoMapPreferences
import com.kylecorry.trail_sense.tools.paths.infrastructure.PathsPreferences
import com.kylecorry.trail_sense.tools.ruler.infrastructure.RulerPreferences
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.tools.ui.sort.ToolSortType
import com.kylecorry.trail_sense.tools.turn_back.infrastructure.TurnBackPreferences
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterBoilTimerPreferences
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherPreferences

/**
 * Every setting in the app, grouped by tool.
 * @param preferences the store every group reads from and writes to
 * @param device the hardware and locale facts some defaults and availability checks depend on
 */
class UserPreferences(
    val preferences: Preferences,
    val device: DeviceProfile
) : IDeclinationPreferences {

    private val cache = preferences

    val navigation by lazy { NavigationPreferences(cache, device) }
    val beacons by lazy { BeaconPreferences(cache) }
    val photoMaps by lazy { PhotoMapPreferences(cache) }
    val map by lazy { MapPreferences(cache) }
    val weather by lazy { WeatherPreferences(cache, device) }
    val astronomy by lazy { AstronomyPreferences(cache) }
    val ruler by lazy { RulerPreferences(cache) }
    val flashlight by lazy { FlashlightPreferenceRepo(cache) }
    val cellSignal by lazy { CellSignalPreferences(cache) }
    val metalDetector by lazy { MetalDetectorPreferences(cache, device) }
    val privacy by lazy { PrivacyPreferences(cache) }
    val tides by lazy { TidePreferences(cache) }
    val power by lazy { PowerPreferences(cache) }
    val packs by lazy { PackPreferences(cache) }
    val clinometer by lazy { ClinometerPreferences(cache) }
    val errors by lazy { ErrorPreferences(cache) }
    val pedometer by lazy { PedometerPreferences(cache) }
    val thermometer by lazy { ThermometerPreferences(cache) }
    val compass by lazy { CompassPreferences(cache, device) }
    val clock by lazy { ClockPreferences(cache) }
    val camera by lazy { CameraPreferences(cache) }
    val altimeter by lazy { AltimeterPreferences(cache) }
    val augmentedReality by lazy { AugmentedRealityPreferences(cache) { baseDistanceUnits } }
    val backup by lazy { BackupPreferences(cache) }
    val ballistics by lazy { BallisticsPreferences(cache) }
    val climate by lazy { ClimatePreferenceRepo(cache) }
    val waterBoilTimer by lazy { WaterBoilTimerPreferences(cache) }
    val turnBack by lazy { TurnBackPreferences(cache) }
    val bubbleLevel by lazy { BubbleLevelPreferences(cache) }
    val fieldGuide by lazy { FieldGuidePreferences(cache) }
    val gps by lazy { GPSPreferences(cache) }
    val debug by lazy { DebugPreferences(cache) }
    val paths by lazy { PathsPreferences(cache, gps) }

    private val isMetricPreferred = device.isMetricPreferred

    val useWidgetIconCompatibilityMode by BooleanPreference(
        cache,
        PreferenceKeys.widgetIconCompatibility,
        false
    )

    val useLegacyFilePicker by BooleanPreference(
        cache,
        PreferenceKeys.useLegacyFilePicker,
        false
    )

    val useLargeTileCache by BooleanPreference(
        cache,
        PreferenceKeys.largeTileCache,
        false
    )

    var isCliffHeightEnabled by BooleanPreference(
        cache,
        PreferenceKeys.cliffHeightEnabled,
        false
    )

    val arePluginsEnabled by BooleanPreference(
        cache,
        PreferenceKeys.pluginsEnabled,
        false
    )

    val distanceUnits by StringEnumPreference(
        cache,
        PreferenceKeys.distanceUnits,
        mapOf(
            "meters" to DistanceUnits.Meters,
            "feet_miles" to DistanceUnits.Feet
        ),
        if (isMetricPreferred) DistanceUnits.Meters else DistanceUnits.Feet,
        saveDefault = true
    )

    val weightUnits by StringEnumPreference(
        cache,
        PreferenceKeys.weightUnits,
        mapOf(
            "kg" to WeightUnits.Kilograms,
            "lbs" to WeightUnits.Pounds,
            "stone" to WeightUnits.Stone
        ),
        if (isMetricPreferred) WeightUnits.Kilograms else WeightUnits.Pounds,
        saveDefault = true
    )

    val baseDistanceUnits: com.kylecorry.sol.units.DistanceUnits
        get() = if (distanceUnits == DistanceUnits.Meters) {
            com.kylecorry.sol.units.DistanceUnits.Meters
        } else {
            com.kylecorry.sol.units.DistanceUnits.Feet
        }

    val pressureUnits by StringEnumPreference(
        cache,
        PreferenceKeys.pressureUnits,
        mapOf(
            "hpa" to PressureUnits.Hpa,
            "in" to PressureUnits.Inhg,
            "mbar" to PressureUnits.Mbar,
            "psi" to PressureUnits.Psi,
            "mm" to PressureUnits.MmHg
        ),
        if (isMetricPreferred) PressureUnits.Hpa else PressureUnits.Inhg,
        saveDefault = true
    )

    val temperatureUnits by StringEnumPreference(
        cache,
        PreferenceKeys.temperatureUnits,
        mapOf(
            "c" to TemperatureUnits.Celsius,
            "f" to TemperatureUnits.Fahrenheit
        ),
        if (isMetricPreferred) TemperatureUnits.Celsius else TemperatureUnits.Fahrenheit,
        saveDefault = true
    )

    val coordinateFormat by StringEnumPreference(
        cache,
        PreferenceKeys.coordinateFormat,
        mapOf(
            "dd" to BuiltInCoordinateFormat.DecimalDegrees,
            "ddm" to BuiltInCoordinateFormat.DegreesDecimalMinutes,
            "dms" to BuiltInCoordinateFormat.DegreesMinutesSeconds,
            "utm" to BuiltInCoordinateFormat.UTM,
            "mgrs" to BuiltInCoordinateFormat.MGRS,
            "usng" to BuiltInCoordinateFormat.USNG,
            "osng" to BuiltInCoordinateFormat.OSGB
        ),
        BuiltInCoordinateFormat.DecimalDegrees
    )

    val use24HourTime by BooleanPreference(
        cache,
        PreferenceKeys.use24Hour,
        device.uses24HourClock,
        saveDefault = true
    )

    val addLeadingZeroToTime by BooleanPreference(
        cache,
        PreferenceKeys.includeLeadingZero,
        false
    )

    var lastTheme: Theme by StringEnumPreference(
        cache,
        "pref_last_theme",
        mapOf(
            "light" to Theme.Light,
            "dark" to Theme.Dark,
            "black" to Theme.Black,
            "sunrise_sunset" to Theme.SunriseSunset,
            "night" to Theme.Night,
            "system" to Theme.System,
            "system_black" to Theme.SystemBlack
        ),
        Theme.System
    )

    private var _theme: Theme by StringEnumPreference(
        cache,
        PreferenceKeys.theme,
        mapOf(
            "light" to Theme.Light,
            "dark" to Theme.Dark,
            "black" to Theme.Black,
            "sunrise_sunset" to Theme.SunriseSunset,
            "night" to Theme.Night,
            "system" to Theme.System,
            "system_black" to Theme.SystemBlack
        ),
        Theme.System
    )

    var theme: Theme
        get() {
            if (isLowPowerModeOn) {
                return Theme.Black
            }

            return _theme
        }
        set(value) {
            _theme = value
        }

    private val _useDynamicColorsOnCompass by BooleanPreference(
        cache,
        PreferenceKeys.useDynamicColorsOnCompass,
        false
    )

    val useDynamicColorsOnCompass: Boolean
        get() = device.hasDynamicColors && _useDynamicColorsOnCompass

    var useCompactMode by BooleanPreference(
        cache,
        PreferenceKeys.useCompactMode,
        false
    )

    var useShowAllBottomNavigationLabels by BooleanPreference(
        preferences = cache,
        PreferenceKeys.useBottomNavigationLabels,
        defaultValue = true
    )

    val nightModeFullscreen by BooleanPreference(
        cache,
        PreferenceKeys.nightModeFullscreen,
        false
    )

    // Calibration

    override var useAutoDeclination: Boolean
        get() = cache.getBoolean(PreferenceKeys.autoDeclination) ?: true
        set(value) = cache.putBoolean(PreferenceKeys.autoDeclination, value)

    override var declinationOverride: Float
        get() = (cache.getString(PreferenceKeys.declinationOverride)
            ?: "0.0").toFloatCompat() ?: 0.0f
        set(value) = cache.putString(
            PreferenceKeys.declinationOverride,
            value.toString()
        )

    var altitudeOverride: Float
        get() {
            val raw = cache.getString(PreferenceKeys.altitudeOverride) ?: "0.0"
            return raw.toFloatCompat() ?: 0.0f
        }
        set(value) = cache.putString(PreferenceKeys.altitudeOverride, value.toString())

    val altimeterMode: AltimeterMode
        get() {
            var raw = cache.getString(PreferenceKeys.altimeterCalibrationMode)

            if (raw == null) {
                if (useAutoAltitude && weather.hasBarometer) {
                    raw = "gps_barometer"
                } else if (useAutoAltitude) {
                    raw = "gps"
                }
            }

            return when (raw) {
                "gps" -> AltimeterMode.GPS
                "gps_barometer" -> AltimeterMode.GPSBarometer
                "barometer" -> AltimeterMode.Barometer
                "dem" -> AltimeterMode.DigitalElevationModel
                "dem_barometer" -> AltimeterMode.DigitalElevationModelBarometer
                else -> AltimeterMode.Override
            }

        }

    var altimeterSamples: Int by IntPreference(
        cache,
        PreferenceKeys.altimeterAccuracy,
        4
    )

    var seaLevelPressureOverride: Float
        get() = cache.getFloat(
            PreferenceKeys.seaLevelPressureOverride
        ) ?: STANDARD_SEA_LEVEL_PRESSURE
        set(value) = cache.putFloat(
            PreferenceKeys.seaLevelPressureOverride,
            value
        )

    private var useAutoAltitude: Boolean
        get() = cache.getBoolean(PreferenceKeys.autoAltitude) ?: true
        set(value) = cache.putBoolean(PreferenceKeys.autoAltitude, value)

    val odometerDistanceThreshold: Distance
        get() = Distance.meters(15f)

    var isLowPowerModeOn: Boolean
        get() = cache.getBoolean(PreferenceKeys.lowPowerMode) ?: false
        set(value) = cache.putBoolean(PreferenceKeys.lowPowerMode, value)

    val lowPowerModeDisablesWeather: Boolean
        get() = cache.getBoolean(PreferenceKeys.lowPowerModeWeather) ?: true

    val lowPowerModeDisablesBacktrack: Boolean
        get() = cache.getBoolean(PreferenceKeys.lowPowerModeBacktrack) ?: true

    val mapSite: MapSite by StringEnumPreference(
        cache, PreferenceKeys.mapUrlSource, mapOf(
            "apple" to MapSite.Apple,
            "bing" to MapSite.Bing,
            "caltopo" to MapSite.Caltopo,
            "google" to MapSite.Google,
            "osm" to MapSite.OSM
        ), MapSite.OSM
    )

    var toolSort: ToolSortType by IntEnumPreference(
        cache,
        PreferenceKeys.toolSort,
        mapOf(
            1 to ToolSortType.Name,
            2 to ToolSortType.Category
        ),
        ToolSortType.Category
    )

    var toolQuickActions: List<Int>
        get() {
            return cache.getIntArray(PreferenceKeys.toolQuickActions)
                ?: listOf(
                    ToolIds.QUICK_ACTION_FLASHLIGHT,
                    ToolIds.QUICK_ACTION_WHISTLE,
                    ToolIds.QUICK_ACTION_LOW_POWER_MODE,
                )
        }
        set(value) {
            cache.putIntArray(
                PreferenceKeys.toolQuickActions,
                value
            )
        }

    var bottomNavigationTools: List<Long>
        get() {
            val maxLength = MAX_BOTTOM_NAVIGATION_ITEMS - 1
            return (cache.getIntArray(PreferenceKeys.bottomNavigationTools)
                ?.map { it.toLong() }
                // Four tabs with Tools (the family's bar): the three most used, everything else one tap away.
                ?: listOf(
                    ToolIds.NAVIGATION,
                    ToolIds.MAP,
                    ToolIds.WEATHER
                ))
                .padRight(maxLength, 0)
                .subList(0, maxLength)
        }
        set(value) {
            cache.putIntArray(
                PreferenceKeys.bottomNavigationTools,
                value.map { it.toInt() }
            )
        }

    var toolPinnedIds: List<Long>
        get() {
            // Navigation and Weather already have bottom-bar tabs, so the pins start with the next most loved
            // tools instead of repeating them.
            return cache.getLongArray(PreferenceKeys.pinnedTools) ?: listOf(
                ToolIds.BEACONS,
                ToolIds.PATHS,
                ToolIds.ASTRONOMY,
                ToolIds.USER_GUIDE
            )
        }
        set(value) {
            cache.putLongArray(PreferenceKeys.pinnedTools, value)
        }

    /** The widgets chosen for the Tools screen, or null when the user hasn't chosen (meaning every widget). */
    var toolWidgets: List<String>?
        get() = cache.getString(PreferenceKeys.toolWidgets)?.split(",")
        set(value) {
            if (value == null) {
                cache.remove(PreferenceKeys.toolWidgets)
            } else {
                cache.putString(PreferenceKeys.toolWidgets, value.joinToString(","))
            }
        }

    val notificationGroupingBehavior by StringEnumPreference(
        cache,
        PreferenceKeys.notificationGroupBehavior,
        mapOf(
            "1" to NotificationGroupBehavior.System,
            "2" to NotificationGroupBehavior.UngroupAll,
            "3" to NotificationGroupBehavior.UngroupHigh
        ),
        NotificationGroupBehavior.System
    )

    private companion object {
        /** Standard atmosphere at sea level, hPa. */
        const val STANDARD_SEA_LEVEL_PRESSURE = 1013.25f
    }

    enum class DistanceUnits {
        Meters, Feet
    }

    enum class Theme {
        Light, Dark, Black, System, SunriseSunset, Night, SystemBlack
    }

    enum class AltimeterMode(val usesDem: Boolean) {
        GPS(false),
        GPSBarometer(false),
        Barometer(false),
        DigitalElevationModel(true),
        DigitalElevationModelBarometer(true),
        Override(false)
    }

}
