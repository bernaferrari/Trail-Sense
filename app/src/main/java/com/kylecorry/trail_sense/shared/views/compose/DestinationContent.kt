package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.settings.ui.PluginDetailsScreen
import com.kylecorry.trail_sense.settings.ui.PathsSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.CalibrateAltimeterToolScreen
import com.kylecorry.trail_sense.settings.ui.CalibrateBarometerToolScreen
import com.kylecorry.trail_sense.settings.ui.CalibrateCompassToolScreen
import com.kylecorry.trail_sense.settings.ui.CalibrateGPSToolScreen
import com.kylecorry.trail_sense.settings.ui.SettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.ThermometerSettingsToolScreen
import com.kylecorry.trail_sense.tools.WhistleScreen
import com.kylecorry.trail_sense.tools.ballistics.ui.BallisticsScreen
import com.kylecorry.trail_sense.tools.astronomy.ui.AstronomySettingsToolScreen
import com.kylecorry.trail_sense.tools.astronomy.ui.AstronomyToolScreen
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityToolScreen
import com.kylecorry.trail_sense.tools.beacons.ui.BeaconDetailsToolScreen
import com.kylecorry.trail_sense.tools.beacons.ui.PlaceBeaconToolScreen
import com.kylecorry.trail_sense.tools.beacons.ui.list.BeaconListToolScreen
import com.kylecorry.trail_sense.tools.cliffheight.ui.CliffHeightScreen
import com.kylecorry.trail_sense.tools.clouds.ui.CloudResultsToolScreen
import com.kylecorry.trail_sense.tools.clouds.ui.CloudToolScreen
import com.kylecorry.trail_sense.tools.clinometer.ui.ClinometerToolScreen
import com.kylecorry.trail_sense.tools.climate.ui.ClimateToolScreen
import com.kylecorry.trail_sense.tools.clock.ui.ClockToolScreen
import com.kylecorry.trail_sense.tools.convert.ui.ConvertScreen
import com.kylecorry.trail_sense.tools.declination.ui.DeclinationScreen
import com.kylecorry.trail_sense.tools.experimentation.ExperimentationContent
import com.kylecorry.trail_sense.tools.field_guide.ui.CreateFieldGuidePageToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.CreateFieldGuideSightingToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuidePageToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuideToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.SightingListToolScreen
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.guide.ui.GuideScreen
import com.kylecorry.trail_sense.tools.level.ui.LevelScreen
import com.kylecorry.trail_sense.tools.light.ui.LightMeterScreen
import com.kylecorry.trail_sense.tools.lightning.ui.LightningScreen
import com.kylecorry.trail_sense.tools.map.ui.MapToolScreen
import com.kylecorry.trail_sense.tools.magnifier.ui.MagnifierScreen
import com.kylecorry.trail_sense.tools.metaldetector.ui.MetalDetectorToolScreen
import com.kylecorry.trail_sense.tools.navigation.ui.NavigationSettingsToolScreen
import com.kylecorry.trail_sense.tools.navigation.ui.NavigatorToolScreen
import com.kylecorry.trail_sense.tools.offline_maps.ui.OfflineMapListToolScreen
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.PhotoMapsToolScreen
import com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.ViewVectorMapToolScreen
import com.kylecorry.trail_sense.tools.packs.ui.CreateItemToolScreen
import com.kylecorry.trail_sense.tools.packs.ui.PackItemListToolScreen
import com.kylecorry.trail_sense.tools.packs.ui.PackListToolScreen
import com.kylecorry.trail_sense.tools.paths.ui.PathOverviewToolScreen
import com.kylecorry.trail_sense.tools.paths.ui.PathsToolScreen
import com.kylecorry.trail_sense.tools.pedometer.ui.PedometerSettingsToolScreen
import com.kylecorry.trail_sense.tools.pedometer.ui.StrideLengthEstimationToolScreen
import com.kylecorry.trail_sense.tools.permits.ui.PermitsScreen
import com.kylecorry.trail_sense.tools.qr.ui.QRShareScreen
import com.kylecorry.trail_sense.tools.qr.ui.ScanQRToolScreen
import com.kylecorry.trail_sense.tools.ruler.ui.RulerScreen
import com.kylecorry.trail_sense.tools.signal_finder.ui.SignalFinderToolScreen
import com.kylecorry.trail_sense.tools.solarpanel.ui.SolarPanelScreen
import com.kylecorry.trail_sense.tools.sensors.ui.SensorDetailsToolScreen
import com.kylecorry.trail_sense.tools.tides.ui.CreateTideToolScreen
import com.kylecorry.trail_sense.tools.tides.ui.TideListToolScreen
import com.kylecorry.trail_sense.tools.tides.ui.TidesToolScreen
import com.kylecorry.trail_sense.tools.temperature_estimation.ui.TemperatureEstimationToolScreen
import com.kylecorry.trail_sense.tools.tools.ui.ToolsScreen
import com.kylecorry.trail_sense.tools.triangulate.ui.TriangulateToolScreen
import com.kylecorry.trail_sense.tools.turn_back.ui.TurnBackToolScreen
import com.kylecorry.trail_sense.tools.weather.ui.WeatherSettingsToolScreen
import com.kylecorry.trail_sense.tools.weather.ui.WeatherToolScreen
import com.kylecorry.trail_sense.tools.whitenoise.ui.WhiteNoiseScreen

/**
 * Extracted destination dispatch logic.
 * Renders the appropriate screen composable based on the destination string argument.
 * This keeps TrailSenseComposeDestinationHost.Content() small and the routing centralized.
 *
 * Behavior is 100% identical to the previous inlined when-expression (including how
 * `this` (the host) and lambdas are passed to screens that need controller callbacks,
 * and special destinations that use host wrappers like Notes(), Pedometer() etc.)
 */
@Composable
fun DestinationContent(
    destination: String?,
    host: TrailSenseComposeDestinationHost,
    modifier: Modifier = Modifier
) {
    with(host) {
        when (destination) {
            // --- Simple / no-arg screens (tools and misc) ---
            DESTINATION_LEVEL -> LevelScreen()
            DESTINATION_WHISTLE -> WhistleScreen()
            DESTINATION_LIGHT_METER -> LightMeterScreen()
            DESTINATION_CONVERT -> ConvertScreen()
            DESTINATION_BALLISTICS -> BallisticsScreen()
            DESTINATION_PERMITS -> PermitsScreen()
            DESTINATION_SOLAR_PANEL -> SolarPanelScreen()
            DESTINATION_LIGHTNING -> LightningScreen()
            DESTINATION_WHITE_NOISE -> WhiteNoiseScreen()
            DESTINATION_METAL_DETECTOR -> MetalDetectorToolScreen()
            DESTINATION_DECLINATION -> DeclinationScreen()
            DESTINATION_RULER -> RulerScreen()
            DESTINATION_TEMPERATURE_ESTIMATION -> TemperatureEstimationToolScreen()

            // --- Guide / diagnostics / error ---
            DESTINATION_GUIDE -> GuideScreen(
                name = requireArguments().getString("guide_name", ""),
                resource = requireArguments().getInt("guide_contents")
            )
            DESTINATION_EXPERIMENTATION -> ExperimentationContent()
            DESTINATION_ERROR_HANDLER -> ErrorHandler()
            DESTINATION_GUIDE_LIST -> GuideList()
            DESTINATION_DIAGNOSTICS -> Diagnostics()

            // --- Settings screens (many defined as internal composables on host) ---
            DESTINATION_CELL_SIGNAL_SETTINGS -> CellSignalSettings()
            DESTINATION_CLOCK_SETTINGS -> ClockSettings()
            DESTINATION_TIDE_SETTINGS -> TideSettings()
            DESTINATION_TURN_BACK_SETTINGS -> TurnBackSettings()
            DESTINATION_WATER_BOIL_SETTINGS -> WaterBoilSettings()
            DESTINATION_LICENSES -> LicensesScreen()
            DESTINATION_BEACON_SETTINGS -> BeaconSettings()
            DESTINATION_BUBBLE_LEVEL_SETTINGS -> BubbleLevelSettings()
            DESTINATION_CAMERA_SETTINGS -> CameraSettings()
            DESTINATION_ERROR_SETTINGS -> ErrorSettings()
            DESTINATION_CLINOMETER_SETTINGS -> ClinometerSettings()
            DESTINATION_PRIVACY_SETTINGS -> PrivacySettings()
            DESTINATION_SENSOR_SETTINGS -> SensorSettings()
            DESTINATION_RULER_SETTINGS -> RulerSettings()
            DESTINATION_FLASHLIGHT_SETTINGS -> FlashlightSettings()
            DESTINATION_POWER_SETTINGS -> PowerSettings()
            DESTINATION_TOOLS_SETTINGS -> ToolsSettings()
            DESTINATION_PLUGINS_SETTINGS -> PluginsSettings()
            DESTINATION_MAP_SETTINGS -> MapSettings()
            DESTINATION_PHOTO_MAP_SETTINGS -> PhotoMapSettings()
            DESTINATION_BACKUP_SETTINGS -> BackupSettings()
            DESTINATION_THEME_SETTINGS -> ThemeSettings()
            DESTINATION_EXPERIMENTAL_SETTINGS -> ExperimentalSettings()
            DESTINATION_UNIT_SETTINGS -> UnitSettings()
            DESTINATION_AUGMENTED_REALITY_SETTINGS -> AugmentedRealitySettings()
            DESTINATION_THERMOMETER_SETTINGS -> ThermometerSettingsToolScreen(this)
            DESTINATION_WEATHER_SETTINGS -> WeatherSettingsToolScreen(this)
            DESTINATION_PEDOMETER_SETTINGS -> PedometerSettingsToolScreen(this)
            DESTINATION_NAVIGATION_SETTINGS -> NavigationSettingsToolScreen(this)
            DESTINATION_PATHS_SETTINGS -> PathsSettingsToolScreen(this)
            DESTINATION_ASTRONOMY_SETTINGS -> AstronomySettingsToolScreen(this)

            // --- Map (special: controller callback + reactive) ---
            DESTINATION_MAP -> MapToolScreen(
                component = reactiveComponent,
                host = this,
                onController = {
                    mapController = it
                    if (it != null) {
                        onReactiveUpdate()
                    }
                }
            )

            // --- Special init destinations with lambdas (still dispatch here for visual) ---
            DESTINATION_CLIFF_HEIGHT -> CliffHeightScreen(
                onOpenGuide = {
                    UserGuideUtils.showGuide(
                        requireContext(),
                        findNavController(),
                        R.raw.guide_tool_cliff_height
                    )
                }
            )
            DESTINATION_MAGNIFIER -> MagnifierScreen(
                onRequestCamera = ::requestCameraForMagnifier
            )
            DESTINATION_MIRROR_CAMERA -> MirrorCamera()
            DESTINATION_WATER_PURIFICATION -> WaterPurification()
            DESTINATION_PLUGIN_DETAILS -> PluginDetailsScreen(
                packageId = requireArguments().getString("package_id").orEmpty(),
                pluginSubsystem = PluginSubsystem.getInstance(requireContext()),
                onNavigateUp = {
                    tryOrNothing {
                        findNavController().navigateUp()
                    }
                }
            )

            // --- Thin host wrappers (stateful, stay in host file) ---
            DESTINATION_TOOLS -> ToolsHome()
            DESTINATION_SCREEN_FLASHLIGHT -> ScreenFlashlight()
            DESTINATION_NOTES -> Notes()
            DESTINATION_NOTE_EDITOR -> NoteEditor()
            DESTINATION_SURVIVAL_GUIDE_READER -> SurvivalGuideReader()
            DESTINATION_SURVIVAL_GUIDE_LIST -> SurvivalGuideList()
            DESTINATION_PEDOMETER -> Pedometer()
            DESTINATION_FLASHLIGHT -> Flashlight()
            DESTINATION_BATTERY -> BatteryTool()

            // --- Tool screens that receive the host for callbacks / services ---
            DESTINATION_TRIANGULATE -> TriangulateToolScreen(this)
            DESTINATION_CLOCK -> ClockToolScreen(this)
            DESTINATION_SIGNAL_FINDER -> SignalFinderToolScreen(this)
            DESTINATION_TURN_BACK -> TurnBackToolScreen(this)
            DESTINATION_SETTINGS -> SettingsToolScreen(this)
            DESTINATION_STRIDE_LENGTH_ESTIMATION -> StrideLengthEstimationToolScreen(this)
            DESTINATION_CLOUDS -> CloudToolScreen(this)
            DESTINATION_CLOUD_RESULTS -> CloudResultsToolScreen(this)
            DESTINATION_ASTRONOMY -> AstronomyToolScreen(this)
            DESTINATION_AUGMENTED_REALITY -> AugmentedRealityToolScreen(this)
            DESTINATION_BEACON_LIST -> BeaconListToolScreen(
                host = this,
                onController = { beaconListController = it }
            )
            DESTINATION_BEACON_DETAILS -> BeaconDetailsToolScreen(this)
            DESTINATION_PLACE_BEACON -> PlaceBeaconToolScreen(this)
            DESTINATION_OFFLINE_MAP_LIST -> OfflineMapListToolScreen(this)
            DESTINATION_SCAN_QR -> ScanQRToolScreen(this)
            DESTINATION_VIEW_QR -> QRShareScreen(
                title = requireArguments().getString("qr_title").orEmpty(),
                text = requireArguments().getString("qr_text").orEmpty()
            )
            DESTINATION_VECTOR_MAP_VIEW -> ViewVectorMapToolScreen(this)
            DESTINATION_NAVIGATION -> NavigatorToolScreen(this)
            DESTINATION_PATH_OVERVIEW -> PathOverviewToolScreen(this)
            DESTINATION_PATHS -> PathsToolScreen(this)
            DESTINATION_CALIBRATE_ALTIMETER -> CalibrateAltimeterToolScreen(this)
            DESTINATION_CALIBRATE_BAROMETER -> CalibrateBarometerToolScreen(this)
            DESTINATION_CALIBRATE_COMPASS -> CalibrateCompassToolScreen(this)
            DESTINATION_CALIBRATE_GPS -> CalibrateGPSToolScreen(this)
            DESTINATION_SENSOR_DETAILS -> SensorDetailsToolScreen(this)
            DESTINATION_TIDES -> TidesToolScreen(this)
            DESTINATION_TIDE_LIST -> TideListToolScreen(this)
            DESTINATION_CREATE_TIDE -> CreateTideToolScreen(this)
            DESTINATION_CLIMATE -> ClimateToolScreen(this)
            DESTINATION_PACK_LIST -> PackListToolScreen(this)
            DESTINATION_PACK_ITEM_LIST -> PackItemListToolScreen(this)
            DESTINATION_CREATE_PACK_ITEM -> CreateItemToolScreen(this)
            DESTINATION_FIELD_GUIDE -> FieldGuideToolScreen(this)
            DESTINATION_FIELD_GUIDE_PAGE -> FieldGuidePageToolScreen(this)
            DESTINATION_SIGHTING_LIST -> SightingListToolScreen(this)
            DESTINATION_CREATE_FIELD_GUIDE_PAGE -> CreateFieldGuidePageToolScreen(this)
            DESTINATION_CREATE_FIELD_GUIDE_SIGHTING -> CreateFieldGuideSightingToolScreen(this)
            DESTINATION_PHOTO_MAPS -> PhotoMapsToolScreen(this)
            DESTINATION_CLINOMETER -> ClinometerToolScreen(
                host = this,
                onController = { clinometerLockController = it }
            )
            DESTINATION_WEATHER -> WeatherToolScreen(this)
            // Unknown destination: emit nothing (matches prior behavior; no else branch)
        }
    }
}
