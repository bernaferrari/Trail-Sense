package com.kylecorry.trail_sense.shared.navigation

import android.os.Bundle
import androidx.annotation.IdRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.*

/**
 * One screen the app can show: its id (the key every caller navigates with), a label for logs, the screen's default
 * arguments (always including which composable draws it, [ARG_DESTINATION]) and the old action ids that lead from it
 * to another screen.
 */
class TrailSenseDestination(
    @IdRes val id: Int,
    val label: String,
    val actions: Map<Int, Int> = emptyMap(),
    private val defaults: Bundle.() -> Unit = {}
) {
    fun arguments(overrides: Bundle?): Bundle = Bundle().apply {
        defaults()
        if (overrides != null) putAll(overrides)
    }
}

/** Every screen in the app, looked up by id. Navigation 3 draws them; this is only the table of what exists. */
object TrailSenseDestinations {

    private fun destination(
        @IdRes id: Int,
        label: String,
        actions: Map<Int, Int> = emptyMap(),
        defaults: Bundle.() -> Unit = {}
    ) = TrailSenseDestination(id, label, actions, defaults)

    val all: Map<Int, TrailSenseDestination> by lazy {
        listOf(
        destination(R.id.action_navigation, "NavigationScreen", actions = mapOf(R.id.action_navigatorFragment_to_calibrateGPSFragment to R.id.calibrateGPSFragment)) {
            putString(ARG_DESTINATION, DESTINATION_NAVIGATION)
            putLong("destination", 0L)
        },
        destination(R.id.action_astronomy, "AstronomyScreen", actions = mapOf(R.id.action_astronomyFragment_to_calibrateGPSFragment to R.id.calibrateGPSFragment)) {
            putString(ARG_DESTINATION, DESTINATION_ASTRONOMY)
        },
        destination(R.id.action_experimental_tools, "ToolsScreen") {
            putString(ARG_DESTINATION, DESTINATION_TOOLS)
        },
        destination(R.id.action_weather, "WeatherScreen") {
            putString(ARG_DESTINATION, DESTINATION_WEATHER)
        },
        destination(R.id.mapFragment, "MapScreen") {
            putString(ARG_DESTINATION, DESTINATION_MAP)
        },
        destination(R.id.mapSettingsFragment, "MapSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_MAP_SETTINGS)
        },
        destination(R.id.offlineMapViewFragment, "ViewVectorMapScreen") {
            putString(ARG_DESTINATION, DESTINATION_VECTOR_MAP_VIEW)
        },
        destination(R.id.action_settings, "SettingsScreen", actions = mapOf(R.id.action_settings_to_sensor_settings to R.id.sensorSettingsFragment, R.id.action_settings_to_diagnostics to R.id.diagnosticsFragment, R.id.action_settings_to_error_settings to R.id.errorSettingsFragment, R.id.action_settings_to_experimental_settings to R.id.experimentalSettingsFragment, R.id.action_settings_to_plugins_settings to R.id.pluginsSettingsFragment, R.id.action_settings_to_unit_settings to R.id.unitSettingsFragment, R.id.action_settings_to_power_settings to R.id.powerSettingsFragment, R.id.action_settings_to_privacy_settings to R.id.privacySettingsFragment, R.id.action_settings_to_backup_settings to R.id.backupSettingsFragment, R.id.action_settings_to_about to R.id.aboutFragment, R.id.action_action_settings_to_licenseFragment to R.id.licenseFragment)) {
            putString(ARG_DESTINATION, DESTINATION_SETTINGS)
        },
        destination(R.id.experimentationFragment, "ExperimentationScreen") {
            putString(ARG_DESTINATION, DESTINATION_EXPERIMENTATION)
        },
        destination(R.id.fieldGuideFragment, "FieldGuideScreen") {
            putString(ARG_DESTINATION, DESTINATION_FIELD_GUIDE)
        },
        destination(R.id.fieldGuidePageFragment, "FieldGuidePageScreen") {
            putString(ARG_DESTINATION, DESTINATION_FIELD_GUIDE_PAGE)
            putLong("page_id", 0L)
        },
        destination(R.id.fieldGuideSettingsFragment, "FieldGuideSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_FIELD_GUIDE_SETTINGS)
        },
        destination(R.id.createFieldGuideSightingFragment, "CreateFieldGuideSightingScreen") {
            putString(ARG_DESTINATION, DESTINATION_CREATE_FIELD_GUIDE_SIGHTING)
            putLong("sighting_id", 0L)
        },
        destination(R.id.sightingListFragment, "SightingListScreen") {
            putString(ARG_DESTINATION, DESTINATION_SIGHTING_LIST)
        },
        destination(R.id.createFieldGuidePageFragment, "CreateFieldGuidePageScreen") {
            putString(ARG_DESTINATION, DESTINATION_CREATE_FIELD_GUIDE_PAGE)
            putLong("page_id", 0L)
            putLong("classification_id", 0L)
        },
        destination(R.id.signalFinderFragment, "SignalFinderScreen") {
            putString(ARG_DESTINATION, DESTINATION_SIGNAL_FINDER)
        },
        destination(R.id.toolDeclinationFragment, "DeclinationScreen") {
            putString(ARG_DESTINATION, DESTINATION_DECLINATION)
        },
        destination(R.id.beacon_list, "BeaconListScreen", actions = mapOf(R.id.action_beaconListFragment_to_placeBeaconFragment to R.id.placeBeaconFragment, R.id.action_beacon_list_to_beaconDetailsFragment to R.id.beaconDetailsFragment)) {
            putString(ARG_DESTINATION, DESTINATION_BEACON_LIST)
        },
        destination(R.id.placeBeaconFragment, "PlaceBeaconScreen") {
            putString(ARG_DESTINATION, DESTINATION_PLACE_BEACON)
            putLong("edit_beacon", 0L)
            putLong("initial_group", 0L)
        },
        destination(R.id.clinometerFragment, "ClinometerScreen") {
            putString(ARG_DESTINATION, DESTINATION_CLINOMETER)
        },
        destination(R.id.calibrateAltimeterFragment, "CalibrateAltimeterScreen") {
            putString(ARG_DESTINATION, DESTINATION_CALIBRATE_ALTIMETER)
        },
        destination(R.id.calibrateBarometerFragment, "CalibrateBarometerScreen") {
            putString(ARG_DESTINATION, DESTINATION_CALIBRATE_BAROMETER)
        },
        destination(R.id.calibrateCompassFragment, "CalibrateCompassScreen") {
            putString(ARG_DESTINATION, DESTINATION_CALIBRATE_COMPASS)
        },
        destination(R.id.calibrateGPSFragment, "CalibrateGPSScreen") {
            putString(ARG_DESTINATION, DESTINATION_CALIBRATE_GPS)
        },
        destination(R.id.aboutFragment, "AboutScreen") {
            putString(ARG_DESTINATION, DESTINATION_ABOUT)
        },
        destination(R.id.licenseFragment, "LicenseScreen") {
            putString(ARG_DESTINATION, DESTINATION_LICENSES)
        },
        destination(R.id.weatherSettingsFragment, "WeatherSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_WEATHER_SETTINGS)
        },
        destination(R.id.navigationSettingsFragment, "NavigationSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_NAVIGATION_SETTINGS)
        },
        destination(R.id.pathsSettingsFragment, "PathsSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_PATHS_SETTINGS)
        },
        destination(R.id.beaconsSettingsFragment, "BeaconSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_BEACON_SETTINGS)
        },
        destination(R.id.pluginsSettingsFragment, "PluginsSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_PLUGINS_SETTINGS)
        },
        destination(R.id.pluginDetailsFragment, "PluginDetailsScreen") {
            putString(ARG_DESTINATION, DESTINATION_PLUGIN_DETAILS)
        },
        destination(R.id.astronomySettingsFragment, "AstronomySettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_ASTRONOMY_SETTINGS)
        },
        destination(R.id.guideListFragment, "GuideListScreen", actions = mapOf(R.id.action_guideListFragment_to_guideFragment to R.id.guideFragment)) {
            putString(ARG_DESTINATION, DESTINATION_GUIDE_LIST)
        },
        destination(R.id.guideFragment, "GuideScreen") {
            putString(ARG_DESTINATION, DESTINATION_GUIDE)
        },
        destination(R.id.viewQrFragment, "QRShareScreen") {
            putString(ARG_DESTINATION, DESTINATION_VIEW_QR)
            putString("qr_title", "")
            putString("qr_text", "")
        },
        destination(R.id.levelFragment, "LevelScreen") {
            putString(ARG_DESTINATION, DESTINATION_LEVEL)
        },
        destination(R.id.beaconDetailsFragment, "BeaconDetailsScreen", actions = mapOf(R.id.action_beacon_details_to_beacon_edit to R.id.placeBeaconFragment)) {
            putString(ARG_DESTINATION, DESTINATION_BEACON_DETAILS)
        },
        destination(R.id.packItemListFragment, "PackItemListScreen", actions = mapOf(R.id.action_action_inventory_to_createItemFragment to R.id.createItemFragment)) {
            putString(ARG_DESTINATION, DESTINATION_PACK_ITEM_LIST)
            putLong("pack_id", 0L)
        },
        destination(R.id.createItemFragment, "CreateItemScreen") {
            putString(ARG_DESTINATION, DESTINATION_CREATE_PACK_ITEM)
            putLong("edit_item_id", 0L)
            putLong("pack_id", 0L)
        },
        destination(R.id.toolConvertFragment, "ConvertScreen") {
            putString(ARG_DESTINATION, DESTINATION_CONVERT)
        },
        destination(R.id.toolCliffHeightFragment, "ToolCliffHeightScreen") {
            putString(ARG_DESTINATION, DESTINATION_CLIFF_HEIGHT)
        },
        destination(R.id.toolWhistleFragment, "ToolWhistleScreen") {
            putString(ARG_DESTINATION, DESTINATION_WHISTLE)
        },
        destination(R.id.fragmentToolSolarPanel, "ScreenToolSolarPanel") {
            putString(ARG_DESTINATION, DESTINATION_SOLAR_PANEL)
        },
        destination(R.id.waterPurificationFragment, "WaterPurificationScreen") {
            putString(ARG_DESTINATION, DESTINATION_WATER_PURIFICATION)
        },
        destination(R.id.fragmentToolFlashlight, "ScreenToolFlashlight", actions = mapOf(R.id.action_flashlight_to_screen_flashlight to R.id.fragmentToolScreenFlashlight)) {
            putString(ARG_DESTINATION, DESTINATION_FLASHLIGHT)
        },
        destination(R.id.toolClockFragment, "ClockScreen") {
            putString(ARG_DESTINATION, DESTINATION_CLOCK)
        },
        destination(R.id.fragmentToolLightning, "ScreenToolLightning") {
            putString(ARG_DESTINATION, DESTINATION_LIGHTNING)
        },
        destination(R.id.rulerFragment, "RulerScreen") {
            putString(ARG_DESTINATION, DESTINATION_RULER)
        },
        destination(R.id.fragmentToolBattery, "ScreenToolBattery", actions = mapOf(R.id.action_settings_to_power_settings to R.id.powerSettingsFragment)) {
            putString(ARG_DESTINATION, DESTINATION_BATTERY)
        },
        destination(R.id.fragmentToolTriangulate, "ScreenToolTriangulate") {
            putString(ARG_DESTINATION, DESTINATION_TRIANGULATE)
        },
        destination(R.id.fragmentToolBallistics, "ScreenToolBallistics") {
            putString(ARG_DESTINATION, DESTINATION_BALLISTICS)
        },
        destination(R.id.fragmentToolMetalDetector, "ScreenToolMetalDetector") {
            putString(ARG_DESTINATION, DESTINATION_METAL_DETECTOR)
        },
        destination(R.id.fragmentToolSurvivalGuideList, "ScreenToolSurvivalGuideList") {
            putString(ARG_DESTINATION, DESTINATION_SURVIVAL_GUIDE_LIST)
        },
        destination(R.id.fragmentToolSurvivalGuideReader, "ScreenToolSurvivalGuideReader") {
            putString(ARG_DESTINATION, DESTINATION_SURVIVAL_GUIDE_READER)
            putInt("header_index", 0)
        },
        destination(R.id.fragmentToolNotes, "ScreenToolNotes", actions = mapOf(R.id.action_fragmentToolNotes_to_fragmentToolNotesCreate to R.id.fragmentToolNotesCreate)) {
            putString(ARG_DESTINATION, DESTINATION_NOTES)
        },
        destination(R.id.fragmentToolNotesCreate, "ScreenToolNotesCreate") {
            putString(ARG_DESTINATION, DESTINATION_NOTE_EDITOR)
            putLong("edit_note_id", 0L)
        },
        destination(R.id.fragmentToolWhiteNoise, "ScreenToolWhiteNoise") {
            putString(ARG_DESTINATION, DESTINATION_WHITE_NOISE)
        },
        destination(R.id.fragmentBacktrack, "PathsScreen", actions = mapOf(R.id.action_fragmentBacktrack_to_action_navigation to R.id.action_navigation, R.id.action_backtrack_to_path to R.id.pathDetailsFragment)) {
            putString(ARG_DESTINATION, DESTINATION_PATHS)
        },
        destination(R.id.cloudFragment, "CloudScreen", actions = mapOf(R.id.action_cloud_to_cloud_picker to R.id.cloudResultsFragment)) {
            putString(ARG_DESTINATION, DESTINATION_CLOUDS)
            putBoolean("open_scanner", false)
        },
        destination(R.id.sensorDetailsFragment, "SensorDetailsScreen") {
            putString(ARG_DESTINATION, DESTINATION_SENSOR_DETAILS)
        },
        destination(R.id.toolLightFragment, "ToolLightScreen") {
            putString(ARG_DESTINATION, DESTINATION_LIGHT_METER)
        },
        destination(R.id.tidesFragment, "TidesScreen", actions = mapOf(R.id.action_tides_to_tideList to R.id.tideListFragment)) {
            putString(ARG_DESTINATION, DESTINATION_TIDES)
        },
        destination(R.id.fragmentToolScreenFlashlight, "ScreenToolScreenFlashlight") {
            putString(ARG_DESTINATION, DESTINATION_SCREEN_FLASHLIGHT)
        },
        destination(R.id.fragmentToolPedometer, "ScreenToolSpeedometer") {
            putString(ARG_DESTINATION, DESTINATION_PEDOMETER)
        },
        destination(R.id.fragmentTurnBackSettings, "TurnBackSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_TURN_BACK_SETTINGS)
        },
        destination(R.id.fragmentWaterBoilTimerSettings, "WaterBoilTimerSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_WATER_BOIL_SETTINGS)
        },
        destination(R.id.calibrateOdometerFragment, "CalibrateOdometerScreen", actions = mapOf(R.id.action_calibrate_pedometer_to_estimate_stride_length to R.id.fragmentStrideLengthEstimation)) {
            putString(ARG_DESTINATION, DESTINATION_PEDOMETER_SETTINGS)
        },
        destination(R.id.createTideFragment, "CreateTideScreen") {
            putString(ARG_DESTINATION, DESTINATION_CREATE_TIDE)
            putLong("edit_tide_id", 0L)
        },
        destination(R.id.tideListFragment, "TideListScreen", actions = mapOf(R.id.action_tideList_to_createTide to R.id.createTideFragment)) {
            putString(ARG_DESTINATION, DESTINATION_TIDE_LIST)
        },
        destination(R.id.photoMapsFragment, "PhotoMapsScreen", actions = mapOf(R.id.action_maps_to_path to R.id.pathDetailsFragment)) {
            putString(ARG_DESTINATION, DESTINATION_PHOTO_MAPS)
            putLong("mapId", 0L)
            putBoolean("autoLockLocation", false)
        },
        destination(R.id.mapListFragment, "MapListScreen", actions = mapOf(R.id.action_mapList_to_maps to R.id.photoMapsFragment)) {
            putString(ARG_DESTINATION, DESTINATION_OFFLINE_MAP_LIST)
        },
        destination(R.id.flashlightSettingsFragment, "FlashlightSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_FLASHLIGHT_SETTINGS)
        },
        destination(R.id.thermometerSettingsFragment, "ThermometerSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_THERMOMETER_SETTINGS)
        },
        destination(R.id.cellSignalSettingsFragment, "CellSignalSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_CELL_SIGNAL_SETTINGS)
        },
        destination(R.id.bubbleLevelSettingsFragment, "BubbleLevelSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_BUBBLE_LEVEL_SETTINGS)
        },
        destination(R.id.clockSettingsFragment, "ClockSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_CLOCK_SETTINGS)
        },
        destination(R.id.privacySettingsFragment, "PrivacySettingsScreen", actions = mapOf(R.id.action_privacy_settings_to_gps_calibration to R.id.calibrateGPSFragment)) {
            putString(ARG_DESTINATION, DESTINATION_PRIVACY_SETTINGS)
        },
        destination(R.id.backupSettingsFragment, "BackupSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_BACKUP_SETTINGS)
        },
        destination(R.id.powerSettingsFragment, "PowerSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_POWER_SETTINGS)
        },
        destination(R.id.unitSettingsFragment, "UnitSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_UNIT_SETTINGS)
        },
        destination(R.id.experimentalSettingsFragment, "ExperimentalSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_EXPERIMENTAL_SETTINGS)
        },
        destination(R.id.toolRulerSettingsFragment, "RulerSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_RULER_SETTINGS)
        },
        destination(R.id.sensorSettingsFragment, "SensorSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_SENSOR_SETTINGS)
        },
        destination(R.id.photoMapSettingsFragment, "PhotoMapSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_PHOTO_MAP_SETTINGS)
        },
        destination(R.id.temperatureEstimationFragment, "TemperatureEstimationScreen") {
            putString(ARG_DESTINATION, DESTINATION_TEMPERATURE_ESTIMATION)
        },
        destination(R.id.packListFragment, "PackListScreen", actions = mapOf(R.id.action_pack_to_pack_items to R.id.packItemListFragment)) {
            putString(ARG_DESTINATION, DESTINATION_PACK_LIST)
        },
        destination(R.id.diagnosticsFragment, "DiagnosticsScreen", actions = mapOf(R.id.action_diagnostics_to_sensor_details to R.id.sensorDetailsFragment)) {
            putString(ARG_DESTINATION, DESTINATION_DIAGNOSTICS)
        },
        destination(R.id.pathDetailsFragment, "PathOverviewScreen") {
            putString(ARG_DESTINATION, DESTINATION_PATH_OVERVIEW)
            putLong("destination", 0L)
            putLong("path_id", 0L)
        },
        destination(R.id.tideSettingsFragment, "TideSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_TIDE_SETTINGS)
        },
        destination(R.id.clinometerSettingsFragment, "ClinometerSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_CLINOMETER_SETTINGS)
        },
        destination(R.id.augmentedRealitySettingsFragment, "AugmentedRealitySettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_AUGMENTED_REALITY_SETTINGS)
        },
        destination(R.id.cameraSettingsFragment, "CameraSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_CAMERA_SETTINGS)
        },
        destination(R.id.errorSettingsFragment, "ErrorSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_ERROR_SETTINGS)
        },
        destination(R.id.scanQrFragment, "ScanQRScreen") {
            putString(ARG_DESTINATION, DESTINATION_SCAN_QR)
        },
        destination(R.id.augmentedRealityFragment, "AugmentedRealityScreen") {
            putString(ARG_DESTINATION, DESTINATION_AUGMENTED_REALITY)
            putLong("mode", 1L)
            putBoolean("camera_enabled", true)
        },
        destination(R.id.fragmentStrideLengthEstimation, "StrideLengthEstimationScreen") {
            putString(ARG_DESTINATION, DESTINATION_STRIDE_LENGTH_ESTIMATION)
        },
        destination(R.id.cloudResultsFragment, "CloudResultsScreen") {
            putString(ARG_DESTINATION, DESTINATION_CLOUD_RESULTS)
        },
        destination(R.id.climateFragment, "ClimateScreen") {
            putString(ARG_DESTINATION, DESTINATION_CLIMATE)
        },
        destination(R.id.mirrorCameraFragment, "ToolMirrorCameraScreen") {
            putString(ARG_DESTINATION, DESTINATION_MIRROR_CAMERA)
        },
        destination(R.id.magnifierFragment, "ToolMagnifierScreen") {
            putString(ARG_DESTINATION, DESTINATION_MAGNIFIER)
        },
        destination(R.id.turnBackFragment, "TurnBackScreen") {
            putString(ARG_DESTINATION, DESTINATION_TURN_BACK)
        },
        destination(R.id.fragmentLocalMessaging, "ScreenToolLocalMessaging") {
            putString(ARG_DESTINATION, DESTINATION_LOCAL_MESSAGING)
        },
        destination(R.id.fragmentLocalTalk, "ScreenToolLocalTalk") {
            putString(ARG_DESTINATION, DESTINATION_LOCAL_TALK)
        },
        destination(R.id.toolsSettingsFragment, "ToolsSettingsScreen") {
            putString(ARG_DESTINATION, DESTINATION_TOOLS_SETTINGS)
        },
        destination(R.id.fragmentToolErrorHandler, "ErrorHandlerScreen") {
            putString(ARG_DESTINATION, DESTINATION_ERROR_HANDLER)
            putString("error", "")
        }
        ).associateBy { it.id }
    }

    operator fun get(@IdRes id: Int): TrailSenseDestination? = all[id]
}
