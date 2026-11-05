package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kylecorry.trail_sense.shared.navigation.Route
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
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuideSettingsToolScreen
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
 * Renders the screen for a [Route]. The `when` is exhaustive, so a new route without a screen doesn't compile.
 * This keeps TrailSenseComposeDestinationHost.Content() small and the routing centralized.
 *
 * Behavior is 100% identical to the previous inlined when-expression (including how
 * `this` (the host) and lambdas are passed to screens that need controller callbacks,
 * and special destinations that use host wrappers like Notes(), Pedometer() etc.)
 */
@Composable
fun DestinationContent(
    route: Route,
    host: TrailSenseComposeDestinationHost,
    modifier: Modifier = Modifier
) {
    with(host) {
        when (route) {
            // --- Simple / no-arg screens (tools and misc) ---
            Route.Level -> LevelScreen()
            Route.Whistle -> WhistleScreen(
                onAudioUnavailable = { findNavigator().navigateUp() }
            )
            Route.LightMeter -> LightMeterScreen()
            Route.Convert -> ConvertScreen()
            Route.Ballistics -> BallisticsScreen()
            Route.SolarPanel -> SolarPanelScreen()
            Route.Lightning -> LightningScreen()
            Route.WhiteNoise -> WhiteNoiseScreen()
            Route.MetalDetector -> MetalDetectorToolScreen()
            Route.Declination -> DeclinationScreen()
            Route.Ruler -> RulerScreen()
            Route.TemperatureEstimation -> TemperatureEstimationToolScreen()

            // --- Guide / diagnostics / error ---
            is Route.Guide -> GuideScreen(
                name = route.name,
                resource = route.contents
            )
            Route.Experimentation -> ExperimentationContent()
            is Route.ErrorHandler -> ErrorHandler(route)
            Route.GuideList -> GuideList()
            Route.Diagnostics -> Diagnostics()

            // --- Settings screens (many defined as internal composables on host) ---
            Route.CellSignalSettings -> CellSignalSettings()
            Route.ClockSettings -> ClockSettings()
            Route.TideSettings -> TideSettings()
            Route.TurnBackSettings -> TurnBackSettings()
            Route.WaterBoilSettings -> WaterBoilSettings()
            Route.Licenses -> LicensesScreen()
            Route.About -> com.kylecorry.trail_sense.settings.ui.AboutToolScreen(this)
            Route.BeaconSettings -> BeaconSettings()
            Route.BubbleLevelSettings -> BubbleLevelSettings()
            Route.CameraSettings -> CameraSettings()
            Route.ErrorSettings -> ErrorSettings()
            Route.ClinometerSettings -> ClinometerSettings()
            Route.PrivacySettings -> PrivacySettings()
            Route.SensorSettings -> SensorSettings()
            Route.RulerSettings -> RulerSettings()
            Route.FlashlightSettings -> FlashlightSettings()
            Route.PowerSettings -> PowerSettings()
            Route.ToolsSettings -> ToolsSettings()
            Route.PluginsSettings -> PluginsSettings()
            Route.MapSettings -> MapSettings()
            Route.PhotoMapSettings -> PhotoMapSettings()
            Route.BackupSettings -> BackupSettings()
            Route.ExperimentalSettings -> ExperimentalSettings()
            Route.UnitSettings -> UnitSettings()
            Route.AugmentedRealitySettings -> AugmentedRealitySettings()
            Route.ThermometerSettings -> ThermometerSettingsToolScreen(this)
            Route.WeatherSettings -> WeatherSettingsToolScreen(this)
            Route.PedometerSettings -> PedometerSettingsToolScreen(this)
            Route.NavigationSettings -> NavigationSettingsToolScreen(this)
            Route.PathsSettings -> PathsSettingsToolScreen(this)
            Route.AstronomySettings -> AstronomySettingsToolScreen(this)

            // --- Map (special: controller callback + reactive) ---
            Route.Map -> MapToolScreen(
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
            Route.CliffHeight -> CliffHeightScreen(
                onOpenGuide = {
                    UserGuideUtils.showGuide(
                        requireContext(),
                        findNavigator(),
                        R.raw.guide_tool_cliff_height
                    )
                }
            )
            Route.Magnifier -> MagnifierScreen(
                onRequestCamera = ::requestCameraForMagnifier
            )
            Route.MirrorCamera -> MirrorCamera()
            Route.WaterPurification -> WaterPurification()
            is Route.PluginDetails -> PluginDetailsScreen(
                packageId = route.packageId,
                pluginSubsystem = PluginSubsystem.getInstance(requireContext()),
                onNavigateUp = {
                    tryOrNothing {
                        findNavigator().navigateUp()
                    }
                }
            )

            // --- Thin host wrappers (stateful, stay in host file) ---
            Route.Tools -> ToolsHome()
            Route.ScreenFlashlight -> ScreenFlashlight()
            Route.Notes -> Notes()
            is Route.NoteEditor -> NoteEditor(route)
            is Route.SurvivalGuideReader -> SurvivalGuideReader(route)
            is Route.SurvivalGuideList -> SurvivalGuideList()
            Route.Pedometer -> Pedometer()
            Route.Flashlight -> Flashlight()
            Route.Battery -> BatteryTool()

            // --- Tool screens that receive the host for callbacks / services ---
            Route.Triangulate -> TriangulateToolScreen(this)
            Route.Clock -> ClockToolScreen(this)
            Route.SignalFinder -> SignalFinderToolScreen(this)
            Route.TurnBack -> TurnBackToolScreen(this)
            Route.Settings -> SettingsToolScreen(this)
            Route.StrideLengthEstimation -> StrideLengthEstimationToolScreen(this)
            is Route.Clouds -> CloudToolScreen(this)
            is Route.CloudResults -> CloudResultsToolScreen(this)
            Route.Astronomy -> AstronomyToolScreen(this)
            is Route.AugmentedReality -> AugmentedRealityToolScreen(this)
            is Route.BeaconList -> BeaconListToolScreen(
                host = this,
                onController = { beaconListController = it }
            )
            is Route.BeaconDetails -> BeaconDetailsToolScreen(this)
            is Route.PlaceBeacon -> PlaceBeaconToolScreen(this)
            is Route.OfflineMapList -> OfflineMapListToolScreen(this)
            Route.ScanQr -> ScanQRToolScreen(this)
            is Route.ViewQr -> QRShareScreen(
                title = route.title,
                text = route.text
            )
            is Route.VectorMapView -> ViewVectorMapToolScreen(this)
            is Route.Navigation -> NavigatorToolScreen(this)
            is Route.PathOverview -> PathOverviewToolScreen(this)
            Route.Paths -> PathsToolScreen(this)
            Route.CalibrateAltimeter -> CalibrateAltimeterToolScreen(this)
            Route.CalibrateBarometer -> CalibrateBarometerToolScreen(this)
            Route.CalibrateCompass -> CalibrateCompassToolScreen(this)
            Route.CalibrateGps -> CalibrateGPSToolScreen(this)
            Route.SensorDetails -> SensorDetailsToolScreen(this)
            Route.Tides -> TidesToolScreen(this)
            Route.TideList -> TideListToolScreen(this)
            is Route.CreateTide -> CreateTideToolScreen(this)
            Route.Climate -> ClimateToolScreen(this)
            Route.PackList -> PackListToolScreen(this)
            is Route.PackItemList -> PackItemListToolScreen(this)
            is Route.CreatePackItem -> CreateItemToolScreen(this)
            Route.FieldGuide -> FieldGuideToolScreen(this)
            is Route.FieldGuidePage -> FieldGuidePageToolScreen(this)
            Route.FieldGuideSettings -> FieldGuideSettingsToolScreen(this)
            is Route.SightingList -> SightingListToolScreen(this)
            is Route.CreateFieldGuidePage -> CreateFieldGuidePageToolScreen(this)
            is Route.CreateFieldGuideSighting -> CreateFieldGuideSightingToolScreen(this)
            is Route.PhotoMaps -> PhotoMapsToolScreen(this)
            Route.Clinometer -> ClinometerToolScreen(
                host = this,
                onController = { clinometerLockController = it }
            )
            Route.Weather -> WeatherToolScreen(this)
            // These open the companion app (TrailSenseComposeDestinationHost.onStart) and draw nothing here.
            Route.LocalMessaging, Route.LocalTalk -> Unit
        }
    }
}
