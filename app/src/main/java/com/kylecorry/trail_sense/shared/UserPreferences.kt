package com.kylecorry.trail_sense.shared

import android.content.Context
import android.hardware.SensorManager
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.andromeda.preferences.BooleanPreference
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
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.shared.views.TrailSenseColorStyle
import com.kylecorry.trail_sense.shared.views.TrailSensePalette
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
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
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
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.sort.ToolSortType
import com.kylecorry.trail_sense.tools.turn_back.infrastructure.TurnBackPreferences
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterBoilTimerPreferences
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherPreferences
import com.kylecorry.trail_sense.main.theme.isDynamicColorAvailable

class UserPreferences(ctx: Context) : IDeclinationPreferences {

    private val context = ctx.applicationContext
    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }

    val navigation by lazy { NavigationPreferences(context) }
    val beacons by lazy { BeaconPreferences(context) }
    val photoMaps by lazy { PhotoMapPreferences(context) }
    val map by lazy { MapPreferences(context) }
    val weather by lazy { WeatherPreferences(context) }
    val astronomy by lazy { AstronomyPreferences(context) }
    val ruler by lazy { RulerPreferences(context) }
    val flashlight by lazy { FlashlightPreferenceRepo(context) }
    val cellSignal by lazy { CellSignalPreferences(context) }
    val metalDetector by lazy { MetalDetectorPreferences(context) }
    val privacy by lazy { PrivacyPreferences(context) }
    val tides by lazy { TidePreferences(context) }
    val power by lazy { PowerPreferences(context) }
    val packs by lazy { PackPreferences(context) }
    val clinometer by lazy { ClinometerPreferences(context) }
    val errors by lazy { ErrorPreferences(context) }
    val pedometer by lazy { PedometerPreferences(context) }
    val thermometer by lazy { ThermometerPreferences(context) }
    val compass by lazy { CompassPreferences(context) }
    val clock by lazy { ClockPreferences(context) }
    val camera by lazy { CameraPreferences(context) }
    val altimeter by lazy { AltimeterPreferences(context) }
    val augmentedReality by lazy { AugmentedRealityPreferences(context) }
    val backup by lazy { BackupPreferences(context) }
    val ballistics by lazy { BallisticsPreferences(context) }
    val climate by lazy { ClimatePreferenceRepo(context) }
    val waterBoilTimer by lazy { WaterBoilTimerPreferences(context) }
    val turnBack by lazy { TurnBackPreferences(context) }
    val bubbleLevel by lazy { BubbleLevelPreferences(context) }
    val fieldGuide by lazy { FieldGuidePreferences(context) }
    val gps by lazy { GPSPreferences(context) }
    val debug by lazy { DebugPreferences(context) }
    val paths by lazy { PathsPreferences(context) }

    private val isMetricPreferred = Resources.isMetricPreferred(context)

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
        Resources.uses24HourClock(context),
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

    private var _useDynamicColors by BooleanPreference(
        cache,
        PreferenceKeys.useDynamicColors,
        true
    )

    /**
     * The app's colors: the wallpaper's (where Android has them; the default) or one of the family's seeds. Installs
     * that turned dynamic colors off before the palette existed keep Trail Sense's own orange.
     */
    var palette: TrailSensePalette
        get() {
            val stored = cache.getString(PreferenceKeys.themePalette)
            val palette = when {
                stored != null -> TrailSensePalette.from(stored)
                _useDynamicColors -> TrailSensePalette.Wallpaper
                else -> TrailSensePalette.Default
            }
            return if (palette == TrailSensePalette.Wallpaper && !isDynamicColorAvailable()) {
                TrailSensePalette.Default
            } else {
                palette
            }
        }
        set(value) {
            cache.putString(PreferenceKeys.themePalette, value.id)
            _useDynamicColors = value == TrailSensePalette.Wallpaper
        }

    /** How the app's own colors become a palette; wallpaper colors are used as Android gives them. */
    var colorStyle: TrailSenseColorStyle
        get() = TrailSenseColorStyle.from(cache.getString(PreferenceKeys.colorStyle))
        set(value) = cache.putString(PreferenceKeys.colorStyle, value.id)

    val useDynamicColors: Boolean
        get() = palette == TrailSensePalette.Wallpaper

    private val _useDynamicColorsOnCompass by BooleanPreference(
        cache,
        PreferenceKeys.useDynamicColorsOnCompass,
        false
    )

    val useDynamicColorsOnCompass: Boolean
        get() = isDynamicColorAvailable() && _useDynamicColorsOnCompass

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
        ) ?: SensorManager.PRESSURE_STANDARD_ATMOSPHERE
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
                    Tools.QUICK_ACTION_FLASHLIGHT,
                    Tools.QUICK_ACTION_WHISTLE,
                    Tools.QUICK_ACTION_LOW_POWER_MODE,
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
                    Tools.NAVIGATION,
                    Tools.MAP,
                    Tools.WEATHER
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
                Tools.BEACONS,
                Tools.PATHS,
                Tools.ASTRONOMY,
                Tools.USER_GUIDE
            )
        }
        set(value) {
            cache.putLongArray(PreferenceKeys.pinnedTools, value)
        }

    var toolWidgets: List<String>
        get() {
            val value = cache.getString(PreferenceKeys.toolWidgets)?.split(",")
            if (value != null) {
                return value
            }

            // The user hasn't made a selection, so default to all widgets
            return Tools.getTools(context)
                .flatMap { it.widgets }
                .filter { it.isEnabled(context) && it.canPlaceInApp }
                .map { it.id }
                .distinct()
        }
        set(value) {
            cache.putString(PreferenceKeys.toolWidgets, value.joinToString(","))
        }

    val notificationGroupingBehavior by StringEnumPreference(
        cache,
        PreferenceKeys.notificationGroupBehavior,
        mapOf(
            "1" to NotificationSubsystem.GroupBehavior.System,
            "2" to NotificationSubsystem.GroupBehavior.UngroupAll,
            "3" to NotificationSubsystem.GroupBehavior.UngroupHigh
        ),
        NotificationSubsystem.GroupBehavior.System
    )

    private fun getString(id: Int): String {
        return context.getString(id)
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
