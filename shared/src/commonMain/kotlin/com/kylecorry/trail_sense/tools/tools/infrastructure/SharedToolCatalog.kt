package com.kylecorry.trail_sense.tools.tools.infrastructure

import com.kylecorry.trail_sense.shared.device.DeviceProfile
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.tools.astronomy.AstronomyBroadcasts
import com.kylecorry.trail_sense.tools.paths.PathsToolServices
import com.kylecorry.trail_sense.tools.weather.WeatherBroadcasts
import kotlin.time.Duration

/** A tool service a shared screen can describe before the host has an implementation. */
private class UnavailableToolService(
    override val id: String,
    override val name: String
) : ToolService {
    override val isSupported = false
    override fun getFrequency(): Duration = Duration.ZERO
    override fun isRunning(): Boolean = false
    override fun isEnabled(): Boolean = false
    override fun isBlocked(): Boolean = true
    override suspend fun enable() = Unit
    override suspend fun disable() = Unit
    override suspend fun restart() = Unit
    override suspend fun stop() = Unit
}

/** Shared catalog for hosts that supply their platform capabilities. */
class SharedToolCatalogImpl(
    private val strings: AppStrings,
    private val device: DeviceProfile,
    private val hasMotion: Boolean = false,
    private val hasTorch: Boolean = false,
    private val hasScreenLight: Boolean = false,
    private val hasCamera: Boolean = false,
    private val platformServices: List<ToolService> = emptyList(),
    private val hasTurnBack: Boolean = false,
    private val hasOfflineMaps: Boolean = false
) : ToolCatalog {

    private val tools: List<SharedTool> by lazy {
        sharedTools(
            strings = strings,
            hasMotion = hasMotion,
            hasCompass = device.hasCompass,
            hasTorch = hasTorch,
            hasScreenLight = hasScreenLight,
            hasCamera = hasCamera,
            hasTurnBack = hasTurnBack,
            hasOfflineMaps = hasOfflineMaps
        )
    }

    private val services: Map<String, ToolService> by lazy {
        mapOf(
            WeatherBroadcasts.SERVICE_WEATHER_MONITOR to Res.string.weather_monitor,
            AstronomyBroadcasts.SERVICE_SUNSET_ALERTS to Res.string.sunset,
            AstronomyBroadcasts.SERVICE_SUNRISE_ALERTS to Res.string.sunrise,
            PathsToolServices.BACKTRACK to Res.string.paths
        ).mapValues { (id, label) -> UnavailableToolService(id, strings.get(label)) } +
            platformServices.associateBy(ToolService::id)
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
    hasTorch: Boolean = false,
    hasScreenLight: Boolean = false,
    hasCamera: Boolean = false,
    hasTurnBack: Boolean = false,
    hasOfflineMaps: Boolean = false
): List<SharedTool> = listOfNotNull(
    // Navigation & Location
    if (hasOfflineMaps) SharedTool(
        ToolIds.OFFLINE_MAPS, strings.get(Res.string.offline_maps), TrailIcons.PhotoMaps,
        Route.OfflineMapList(), ToolCategory.Location, additionalRoutes = listOf(Route.VectorMapView::class)
    ) else null,
    if (hasOfflineMaps) SharedTool(
        ToolIds.MAP, strings.get(Res.string.map), TrailIcons.Maps,
        Route.Map(), ToolCategory.Location
    ) else null,
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
    SharedTool(
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
    if (hasTurnBack) SharedTool(
        ToolIds.TURN_BACK, strings.get(Res.string.tool_turn_back), TrailIcons.ToolClock,
        Route.TurnBack, ToolCategory.Time, guideId = RawTexts.guide_tool_turn_back
    ) else null,
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
    ) else if (hasScreenLight) SharedTool(
        ToolIds.FLASHLIGHT, strings.get(Res.string.flashlight_title), TrailIcons.ScreenFlashlight,
        Route.ScreenFlashlight, ToolCategory.Signaling, guideId = RawTexts.guide_tool_flashlight,
        settingsRoute = Route.FlashlightSettings
    ) else null,
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
    ),
    if (hasCamera) SharedTool(
        ToolIds.MIRROR_CAMERA, strings.get(Res.string.mirror_camera), TrailIcons.MirrorCamera,
        Route.MirrorCamera, ToolCategory.Other, guideId = RawTexts.guide_tool_mirror_camera
    ) else null,
    SharedTool(
        ToolIds.QR_CODE_SCANNER, strings.get(Res.string.qr_code_scanner), TrailIcons.QrCode,
        Route.ScanQr, ToolCategory.Other, guideId = RawTexts.guide_tool_qr_code_scanner
    )
)
