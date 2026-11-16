package com.kylecorry.trail_sense.tools.tools.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.shared.device.DeviceProfile
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.tools.astronomy.AstronomyBroadcasts
import com.kylecorry.trail_sense.tools.paths.PathsBroadcasts
import com.kylecorry.trail_sense.tools.paths.PathsToolServices
import com.kylecorry.trail_sense.tools.weather.WeatherBroadcasts
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Shared in-process [ToolService] state tracker so ViewModels calling `ToolCatalog.getService(...)`
 * can inspect, toggle, and persist foreground/background tool switches across non-Android platforms.
 */
class SharedInProcessToolService(
    override val id: String,
    override val name: String,
    private val prefs: Preferences,
    private val prefKey: String,
    private val defaultFrequency: Duration = 15.minutes,
    private val frequencyProvider: () -> Duration = { defaultFrequency },
    private val stateChangedBroadcast: String? = null
) : ToolService {

    override fun getFrequency(): Duration = frequencyProvider()

    override fun isRunning(): Boolean = isEnabled()

    override fun isEnabled(): Boolean = prefs.getBoolean(prefKey) ?: false

    override fun isBlocked(): Boolean = false

    override suspend fun enable() {
        prefs.putBoolean(prefKey, true)
        stateChangedBroadcast?.let { ToolBroadcasts.broadcast(it) }
    }

    override suspend fun disable() {
        prefs.putBoolean(prefKey, false)
        stateChangedBroadcast?.let { ToolBroadcasts.broadcast(it) }
    }

    override suspend fun restart() {
        if (isEnabled()) {
            stateChangedBroadcast?.let { ToolBroadcasts.broadcast(it) }
        }
    }

    override suspend fun stop() {
        prefs.putBoolean(prefKey, false)
        stateChangedBroadcast?.let { ToolBroadcasts.broadcast(it) }
    }
}

/** Shared [ToolCatalog] implementation registering all multiplatform tools and in-process services. */
class SharedToolCatalogImpl(
    private val strings: AppStrings,
    private val device: DeviceProfile,
    private val prefs: Preferences,
    private val hasMotion: Boolean = false,
    private val hasTorch: Boolean = true
) : ToolCatalog {

    private val tools: List<SharedTool> by lazy {
        sharedTools(
            strings = strings,
            hasMotion = hasMotion,
            hasCompass = device.hasCompass,
            hasBarometer = device.hasBarometer,
            hasTorch = hasTorch
        )
    }

    private val services: Map<String, ToolService> by lazy {
        mapOf(
            WeatherBroadcasts.SERVICE_WEATHER_MONITOR to SharedInProcessToolService(
                id = WeatherBroadcasts.SERVICE_WEATHER_MONITOR,
                name = strings.get(Res.string.weather_monitor),
                prefs = prefs,
                prefKey = "pref_monitor_weather",
                defaultFrequency = 15.minutes,
                stateChangedBroadcast = WeatherBroadcasts.MONITOR_STATE_CHANGED
            ),
            AstronomyBroadcasts.SERVICE_SUNSET_ALERTS to SharedInProcessToolService(
                id = AstronomyBroadcasts.SERVICE_SUNSET_ALERTS,
                name = strings.get(Res.string.astronomy),
                prefs = prefs,
                prefKey = "pref_sunset_alerts",
                stateChangedBroadcast = AstronomyBroadcasts.SUNSET_ALERTS_STATE_CHANGED
            ),
            AstronomyBroadcasts.SERVICE_SUNRISE_ALERTS to SharedInProcessToolService(
                id = AstronomyBroadcasts.SERVICE_SUNRISE_ALERTS,
                name = strings.get(Res.string.astronomy),
                prefs = prefs,
                prefKey = "pref_sunrise_alerts",
                stateChangedBroadcast = AstronomyBroadcasts.SUNRISE_ALERTS_STATE_CHANGED
            ),
            PathsToolServices.BACKTRACK to SharedInProcessToolService(
                id = PathsToolServices.BACKTRACK,
                name = strings.get(Res.string.paths),
                prefs = prefs,
                prefKey = "pref_backtrack_enabled",
                defaultFrequency = 15.minutes,
                stateChangedBroadcast = PathsBroadcasts.BACKTRACK_STATE_CHANGED
            )
        )
    }

    override fun getTools(availableOnly: Boolean): List<ToolDescriptor> {
        return tools
    }

    override fun getTool(toolId: Long): ToolDescriptor? {
        return tools.firstOrNull { it.id == toolId }
    }

    override fun isToolAvailable(toolId: Long): Boolean {
        return tools.any { it.id == toolId }
    }

    override fun getService(serviceId: String): ToolService? {
        return services[serviceId]
    }
}

/** Tools whose screens and services are available in the shared app. */
fun sharedTools(
    strings: AppStrings,
    hasMotion: Boolean = false,
    hasCompass: Boolean = true,
    hasBarometer: Boolean = true,
    hasTorch: Boolean = true
): List<SharedTool> = listOfNotNull(
    // Navigation & Location
    SharedTool(
        ToolIds.NAVIGATION, strings.get(Res.string.navigation), TrailIcons.NavigationArrow,
        Route.Navigation(), ToolCategory.Location, guideId = RawTexts.guide_tool_navigation,
        settingsRoute = Route.NavigationSettings
    ),
    SharedTool(
        ToolIds.BEACONS, strings.get(Res.string.beacons), TrailIcons.Location,
        Route.BeaconList(), ToolCategory.Location, guideId = RawTexts.guide_tool_beacons,
        settingsRoute = Route.BeaconSettings,
        additionalRoutes = listOf(Route.PlaceBeacon::class, Route.BeaconDetails::class)
    ),
    SharedTool(
        ToolIds.PATHS, strings.get(Res.string.paths), TrailIcons.ToolBacktrack,
        Route.Paths, ToolCategory.Location, guideId = RawTexts.guide_tool_paths,
        settingsRoute = Route.PathsSettings,
        additionalRoutes = listOf(Route.PathOverview::class)
    ),
    SharedTool(
        ToolIds.TRIANGULATE_LOCATION, strings.get(Res.string.tool_triangulate_title), TrailIcons.ToolTriangulate,
        Route.Triangulate, ToolCategory.Location, guideId = RawTexts.guide_tool_triangulate_location
    ),

    // Weather & Nature
    if (hasBarometer) SharedTool(
        ToolIds.WEATHER, strings.get(Res.string.weather), TrailIcons.Weather,
        Route.Weather, ToolCategory.Weather, guideId = RawTexts.guide_tool_weather,
        settingsRoute = Route.WeatherSettings
    ) else SharedTool(
        ToolIds.WEATHER, strings.get(Res.string.weather), TrailIcons.Weather,
        Route.Weather, ToolCategory.Weather, guideId = RawTexts.guide_tool_weather,
        settingsRoute = Route.WeatherSettings
    ),
    SharedTool(
        ToolIds.CLIMATE, strings.get(Res.string.tool_climate), TrailIcons.TemperatureRange,
        Route.Climate, ToolCategory.Weather, guideId = RawTexts.guide_tool_climate
    ),
    SharedTool(
        ToolIds.CLOUDS, strings.get(Res.string.clouds), TrailIcons.ToolClouds,
        Route.Clouds(), ToolCategory.Weather, guideId = RawTexts.guide_tool_clouds,
        additionalRoutes = listOf(Route.CloudResults::class)
    ),
    SharedTool(
        ToolIds.LIGHTNING_STRIKE_DISTANCE, strings.get(Res.string.tool_lightning_title), TrailIcons.Flashlight,
        Route.Lightning, ToolCategory.Weather, description = strings.get(Res.string.tool_lightning_description),
        guideId = RawTexts.guide_tool_lightning_strike_distance
    ),
    SharedTool(
        ToolIds.TEMPERATURE_ESTIMATION, strings.get(Res.string.tool_temperature_estimation_title), TrailIcons.ThermometerColored,
        Route.TemperatureEstimation, ToolCategory.Weather, guideId = RawTexts.guide_tool_temperature_estimation
    ),

    // Time & Astronomy
    SharedTool(
        ToolIds.ASTRONOMY, strings.get(Res.string.astronomy), TrailIcons.Astronomy,
        Route.Astronomy, ToolCategory.Time, guideId = RawTexts.guide_tool_astronomy,
        settingsRoute = Route.AstronomySettings
    ),
    SharedTool(
        ToolIds.TIDES, strings.get(Res.string.tides), TrailIcons.LightRain,
        Route.Tides, ToolCategory.Time, guideId = RawTexts.guide_tool_tides,
        settingsRoute = Route.TideSettings,
        additionalRoutes = listOf(Route.TideList::class, Route.CreateTide::class)
    ),
    SharedTool(
        ToolIds.CLOCK, strings.get(Res.string.tool_clock_title), TrailIcons.ToolClock,
        Route.Clock, ToolCategory.Time, guideId = RawTexts.guide_tool_clock,
        settingsRoute = Route.ClockSettings
    ),
    SharedTool(
        ToolIds.WATER_BOIL_TIMER, strings.get(Res.string.water_boil_timer), TrailIcons.ToolBoil,
        Route.WaterPurification, ToolCategory.Time, guideId = RawTexts.guide_tool_water_boil_timer,
        settingsRoute = Route.WaterBoilSettings
    ),
    SharedTool(
        ToolIds.TURN_BACK, strings.get(Res.string.tool_turn_back), TrailIcons.Prev,
        Route.TurnBack, ToolCategory.Time, guideId = RawTexts.guide_tool_turn_back,
        settingsRoute = Route.TurnBackSettings
    ),

    // Distance & Angles
    SharedTool(
        ToolIds.PEDOMETER, strings.get(Res.string.pedometer), TrailIcons.Steps,
        Route.Pedometer, ToolCategory.Distance, guideId = RawTexts.guide_tool_pedometer,
        settingsRoute = Route.PedometerSettings,
        additionalRoutes = listOf(Route.StrideLengthEstimation::class)
    ),
    SharedTool(
        ToolIds.RULER, strings.get(Res.string.tool_ruler_title), TrailIcons.Ruler,
        Route.Ruler, ToolCategory.Distance, guideId = RawTexts.guide_tool_ruler,
        settingsRoute = Route.RulerSettings
    ),
    SharedTool(
        ToolIds.CLIFF_HEIGHT, strings.get(Res.string.tool_cliff_height_title), TrailIcons.ToolCliffHeight,
        Route.CliffHeight, ToolCategory.Distance, guideId = RawTexts.guide_tool_cliff_height
    ),
    if (hasMotion) SharedTool(
        ToolIds.BUBBLE_LEVEL, strings.get(Res.string.tool_bubble_level_title), TrailIcons.Level,
        Route.Level, ToolCategory.Angles, description = strings.get(Res.string.tool_bubble_level_summary),
        guideId = RawTexts.guide_tool_bubble_level, settingsRoute = Route.BubbleLevelSettings
    ) else null,
    if (hasMotion) SharedTool(
        ToolIds.CLINOMETER, strings.get(Res.string.clinometer_title), TrailIcons.Clinometer,
        Route.Clinometer, ToolCategory.Angles, guideId = RawTexts.guide_tool_clinometer,
        settingsRoute = Route.ClinometerSettings
    ) else null,
    if (hasCompass) SharedTool(
        ToolIds.DECLINATION, strings.get(Res.string.declination), TrailIcons.Declination,
        Route.Declination, ToolCategory.Angles, guideId = RawTexts.guide_tool_declination
    ) else null,
    if (hasCompass && hasMotion) SharedTool(
        ToolIds.SOLAR_PANEL_ALIGNER, strings.get(Res.string.tool_solar_panel_title), TrailIcons.ToolSolarPanel,
        Route.SolarPanel, ToolCategory.Power, guideId = RawTexts.guide_tool_solar_panel_aligner
    ) else null,

    // Signaling & Sound
    if (hasTorch) SharedTool(
        ToolIds.FLASHLIGHT, strings.get(Res.string.flashlight_title), TrailIcons.Flashlight,
        Route.Flashlight, ToolCategory.Signaling, guideId = RawTexts.guide_tool_flashlight,
        settingsRoute = Route.FlashlightSettings,
        additionalRoutes = listOf(Route.ScreenFlashlight::class)
    ) else SharedTool(
        ToolIds.FLASHLIGHT, strings.get(Res.string.flashlight_title), TrailIcons.ScreenFlashlight,
        Route.ScreenFlashlight, ToolCategory.Signaling, guideId = RawTexts.guide_tool_flashlight,
        settingsRoute = Route.FlashlightSettings
    ),
    SharedTool(
        ToolIds.WHISTLE, strings.get(Res.string.tool_whistle_title), TrailIcons.ToolWhistle,
        Route.Whistle, ToolCategory.Signaling, guideId = RawTexts.guide_tool_whistle
    ),
    SharedTool(
        ToolIds.WHITE_NOISE, strings.get(Res.string.tool_white_noise_title), TrailIcons.ToolWhiteNoise,
        Route.WhiteNoise, ToolCategory.Other, guideId = RawTexts.guide_tool_white_noise
    ),

    // Books & Reference
    SharedTool(
        ToolIds.SURVIVAL_GUIDE, strings.get(Res.string.survival_guide), TrailIcons.SurvivalGuide,
        Route.SurvivalGuideList(), ToolCategory.Books, guideId = RawTexts.guide_tool_survival_guide,
        additionalRoutes = listOf(Route.SurvivalGuideReader::class)
    ),
    SharedTool(
        ToolIds.FIELD_GUIDE, strings.get(Res.string.field_guide), TrailIcons.FieldGuide,
        Route.FieldGuide, ToolCategory.Books, guideId = RawTexts.guide_tool_field_guide,
        settingsRoute = Route.FieldGuideSettings,
        additionalRoutes = listOf(
            Route.FieldGuidePage::class,
            Route.SightingList::class,
            Route.CreateFieldGuideSighting::class
        )
    ),
    SharedTool(
        ToolIds.USER_GUIDE, strings.get(Res.string.tool_user_guide_title), TrailIcons.UserGuide,
        Route.GuideList, ToolCategory.Books, guideId = RawTexts.guide_tool_survival_guide,
        additionalRoutes = listOf(Route.Guide::class)
    ),

    // Utilities & Other
    SharedTool(
        ToolIds.NOTES, strings.get(Res.string.tool_notes_title), TrailIcons.ToolNotes,
        Route.Notes, ToolCategory.Other, guideId = RawTexts.guide_tool_notes,
        additionalRoutes = listOf(Route.NoteEditor::class)
    ),
    SharedTool(
        ToolIds.PACKING_LISTS, strings.get(Res.string.packing_lists), TrailIcons.ToolPack,
        Route.PackList, ToolCategory.Other, guideId = RawTexts.guide_tool_packing_lists,
        additionalRoutes = listOf(Route.PackItemList::class, Route.CreatePackItem::class)
    ),
    SharedTool(
        ToolIds.CONVERT, strings.get(Res.string.convert), TrailIcons.ToolDistanceConvert,
        Route.Convert, ToolCategory.Other, guideId = RawTexts.guide_tool_convert
    ),
    SharedTool(
        ToolIds.BALLISTICS, strings.get(Res.string.ballistics), TrailIcons.Ballistics,
        Route.Ballistics, ToolCategory.Other, guideId = RawTexts.guide_tool_ballistics
    ),
    if (hasCompass) SharedTool(
        ToolIds.METAL_DETECTOR, strings.get(Res.string.tool_metal_detector_title), TrailIcons.ToolMetalDetector,
        Route.MetalDetector, ToolCategory.Other, guideId = RawTexts.guide_tool_metal_detector
    ) else null,
    SharedTool(
        ToolIds.LIGHT_METER, strings.get(Res.string.tool_light_meter_title), TrailIcons.Flashlight,
        Route.LightMeter, ToolCategory.Other, guideId = RawTexts.guide_tool_light_meter
    ),
    SharedTool(
        ToolIds.DIAGNOSTICS, strings.get(Res.string.diagnostics), TrailIcons.Sensors,
        Route.Diagnostics, ToolCategory.Other, guideId = RawTexts.guide_tool_diagnostics
    )
)
