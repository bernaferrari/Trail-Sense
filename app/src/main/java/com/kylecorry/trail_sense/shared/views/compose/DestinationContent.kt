package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.trail_sense.main.errors.ErrorHandlerToolScreen
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.settings.ui.AboutToolScreen
import com.kylecorry.trail_sense.settings.ui.AugmentedRealitySettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.BackupSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.BeaconSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.BubbleLevelSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.CalibrateAltimeterToolScreen
import com.kylecorry.trail_sense.settings.ui.CalibrateBarometerToolScreen
import com.kylecorry.trail_sense.settings.ui.CalibrateCompassToolScreen
import com.kylecorry.trail_sense.settings.ui.CalibrateGPSToolScreen
import com.kylecorry.trail_sense.settings.ui.CameraSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.CellSignalSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.ClinometerSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.ClockSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.ErrorSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.ExperimentalSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.FlashlightSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.LicensesToolScreen
import com.kylecorry.trail_sense.settings.ui.MapSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.PathsSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.PhotoMapSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.PluginDetailsScreen
import com.kylecorry.trail_sense.settings.ui.PluginsSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.PowerSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.PrivacySettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.RulerSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.SensorSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.SettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.ThermometerSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.TideSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.ToolsSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.TurnBackSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.UnitSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.WaterBoilSettingsToolScreen
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.tools.WhistleScreen
import com.kylecorry.trail_sense.tools.astronomy.ui.AstronomySettingsToolScreen
import com.kylecorry.trail_sense.tools.astronomy.ui.AstronomyToolScreen
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityToolScreen
import com.kylecorry.trail_sense.tools.ballistics.ui.BallisticsScreen
import com.kylecorry.trail_sense.tools.battery.ui.BatteryToolScreen
import com.kylecorry.trail_sense.tools.beacons.ui.BeaconDetailsToolScreen
import com.kylecorry.trail_sense.tools.beacons.ui.PlaceBeaconToolScreen
import com.kylecorry.trail_sense.tools.beacons.ui.list.BeaconListToolScreen
import com.kylecorry.trail_sense.tools.cliffheight.ui.CliffHeightToolScreen
import com.kylecorry.trail_sense.tools.climate.ui.ClimateToolScreen
import com.kylecorry.trail_sense.tools.clinometer.ui.ClinometerToolScreen
import com.kylecorry.trail_sense.tools.clock.ui.ClockToolScreen
import com.kylecorry.trail_sense.tools.clouds.ui.CloudResultsToolScreen
import com.kylecorry.trail_sense.tools.clouds.ui.CloudToolScreen
import com.kylecorry.trail_sense.tools.comms.TrailSenseCommsToolScreen
import com.kylecorry.trail_sense.tools.convert.ui.ConvertScreen
import com.kylecorry.trail_sense.tools.declination.ui.DeclinationScreen
import com.kylecorry.trail_sense.tools.diagnostics.ui.DiagnosticsToolScreen
import com.kylecorry.trail_sense.tools.experimentation.ExperimentationContent
import com.kylecorry.trail_sense.tools.field_guide.ui.CreateFieldGuidePageToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.CreateFieldGuideSightingToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuidePageToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuideSettingsToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuideToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.SightingListToolScreen
import com.kylecorry.trail_sense.tools.flashlight.ui.FlashlightToolScreen
import com.kylecorry.trail_sense.tools.flashlight.ui.ScreenFlashlightToolScreen
import com.kylecorry.trail_sense.tools.guide.ui.GuideListToolScreen
import com.kylecorry.trail_sense.tools.guide.ui.GuideScreen
import com.kylecorry.trail_sense.tools.level.ui.LevelScreen
import com.kylecorry.trail_sense.tools.light.ui.LightMeterScreen
import com.kylecorry.trail_sense.tools.lightning.ui.LightningScreen
import com.kylecorry.trail_sense.tools.magnifier.ui.MagnifierToolScreen
import com.kylecorry.trail_sense.tools.map.ui.MapToolScreen
import com.kylecorry.trail_sense.tools.metaldetector.ui.MetalDetectorToolScreen
import com.kylecorry.trail_sense.tools.mirror.ui.MirrorCameraToolScreen
import com.kylecorry.trail_sense.tools.navigation.ui.NavigationSettingsToolScreen
import com.kylecorry.trail_sense.tools.navigation.ui.NavigatorToolScreen
import com.kylecorry.trail_sense.tools.notes.ui.NoteEditorToolScreen
import com.kylecorry.trail_sense.tools.notes.ui.NotesToolScreen
import com.kylecorry.trail_sense.tools.offline_maps.ui.OfflineMapListToolScreen
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.PhotoMapsToolScreen
import com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.ViewVectorMapToolScreen
import com.kylecorry.trail_sense.tools.packs.ui.CreateItemToolScreen
import com.kylecorry.trail_sense.tools.packs.ui.PackItemListToolScreen
import com.kylecorry.trail_sense.tools.packs.ui.PackListToolScreen
import com.kylecorry.trail_sense.tools.paths.ui.PathOverviewToolScreen
import com.kylecorry.trail_sense.tools.paths.ui.PathsToolScreen
import com.kylecorry.trail_sense.tools.pedometer.ui.PedometerSettingsToolScreen
import com.kylecorry.trail_sense.tools.pedometer.ui.PedometerToolScreen
import com.kylecorry.trail_sense.tools.pedometer.ui.StrideLengthEstimationToolScreen
import com.kylecorry.trail_sense.tools.qr.ui.QRShareScreen
import com.kylecorry.trail_sense.tools.qr.ui.ScanQRToolScreen
import com.kylecorry.trail_sense.tools.ruler.ui.RulerScreen
import com.kylecorry.trail_sense.tools.sensors.ui.SensorDetailsToolScreen
import com.kylecorry.trail_sense.tools.signal_finder.ui.SignalFinderToolScreen
import com.kylecorry.trail_sense.tools.solarpanel.ui.SolarPanelScreen
import com.kylecorry.trail_sense.tools.survival_guide.ui.SurvivalGuideListToolScreen
import com.kylecorry.trail_sense.tools.survival_guide.ui.SurvivalGuideReaderToolScreen
import com.kylecorry.trail_sense.tools.temperature_estimation.ui.TemperatureEstimationToolScreen
import com.kylecorry.trail_sense.tools.tides.ui.CreateTideToolScreen
import com.kylecorry.trail_sense.tools.tides.ui.TideListToolScreen
import com.kylecorry.trail_sense.tools.tides.ui.TidesToolScreen
import com.kylecorry.trail_sense.tools.tools.ui.ToolsHomeScreen
import com.kylecorry.trail_sense.tools.triangulate.ui.TriangulateToolScreen
import com.kylecorry.trail_sense.tools.turn_back.ui.TurnBackToolScreen
import com.kylecorry.trail_sense.tools.waterpurification.ui.WaterPurificationToolScreen
import com.kylecorry.trail_sense.tools.weather.ui.WeatherSettingsToolScreen
import com.kylecorry.trail_sense.tools.weather.ui.WeatherToolScreen
import com.kylecorry.trail_sense.tools.whitenoise.ui.WhiteNoiseScreen

/**
 * Renders the screen for a [Route]. The `when` is exhaustive, so a new route without a screen doesn't compile.
 * Screens get the host as their [TrailSenseDestinationHost]; those the activity reaches while they are shown (volume
 * buttons, reselecting a tab, refreshing beacons) offer it a controller through [TrailSenseDestinationHost.screenController].
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
            is Route.ErrorHandler -> ErrorHandlerToolScreen(this, route)
            Route.GuideList -> GuideListToolScreen(this)
            Route.Diagnostics -> DiagnosticsToolScreen(this)

            // --- Settings screens ---
            Route.CellSignalSettings -> CellSignalSettingsToolScreen(this)
            Route.ClockSettings -> ClockSettingsToolScreen(this)
            Route.TideSettings -> TideSettingsToolScreen(this)
            Route.TurnBackSettings -> TurnBackSettingsToolScreen(this)
            Route.WaterBoilSettings -> WaterBoilSettingsToolScreen(this)
            Route.Licenses -> LicensesToolScreen(this)
            Route.About -> AboutToolScreen(this)
            Route.BeaconSettings -> BeaconSettingsToolScreen(this)
            Route.BubbleLevelSettings -> BubbleLevelSettingsToolScreen(this)
            Route.CameraSettings -> CameraSettingsToolScreen(this)
            Route.ErrorSettings -> ErrorSettingsToolScreen(this)
            Route.ClinometerSettings -> ClinometerSettingsToolScreen(this)
            Route.PrivacySettings -> PrivacySettingsToolScreen(this)
            Route.SensorSettings -> SensorSettingsToolScreen(this)
            Route.RulerSettings -> RulerSettingsToolScreen(this)
            Route.FlashlightSettings -> FlashlightSettingsToolScreen(this)
            Route.PowerSettings -> PowerSettingsToolScreen(this)
            Route.ToolsSettings -> ToolsSettingsToolScreen(this)
            Route.PluginsSettings -> PluginsSettingsToolScreen(this)
            Route.MapSettings -> MapSettingsToolScreen(this)
            Route.PhotoMapSettings -> PhotoMapSettingsToolScreen(this)
            Route.BackupSettings -> BackupSettingsToolScreen(this)
            Route.ExperimentalSettings -> ExperimentalSettingsToolScreen(this)
            Route.UnitSettings -> UnitSettingsToolScreen(this)
            Route.AugmentedRealitySettings -> AugmentedRealitySettingsToolScreen(this)
            Route.ThermometerSettings -> ThermometerSettingsToolScreen(this)
            Route.WeatherSettings -> WeatherSettingsToolScreen(this)
            Route.PedometerSettings -> PedometerSettingsToolScreen(this)
            Route.NavigationSettings -> NavigationSettingsToolScreen()
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

            // --- Tools with a disclaimer, camera permission or plugin details ---
            Route.CliffHeight -> CliffHeightToolScreen(this)
            Route.Magnifier -> MagnifierToolScreen(this)
            Route.MirrorCamera -> MirrorCameraToolScreen(this)
            Route.WaterPurification -> WaterPurificationToolScreen(this)
            is Route.PluginDetails -> PluginDetailsScreen(
                packageId = route.packageId,
                pluginSubsystem = PluginSubsystem.getInstance(requireContext()),
                onNavigateUp = {
                    tryOrNothing {
                        findNavigator().navigateUp()
                    }
                }
            )

            // --- Tools with their own state holder ---
            Route.Tools -> ToolsHomeScreen(this)
            Route.ScreenFlashlight -> ScreenFlashlightToolScreen(this)
            Route.Notes -> NotesToolScreen(this)
            is Route.NoteEditor -> NoteEditorToolScreen(this, route)
            is Route.SurvivalGuideReader -> SurvivalGuideReaderToolScreen(this, route)
            is Route.SurvivalGuideList -> SurvivalGuideListToolScreen(this, route)
            Route.Pedometer -> PedometerToolScreen(this)
            Route.Flashlight -> FlashlightToolScreen(this)
            Route.Battery -> BatteryToolScreen(this)

            // --- Tool screens that receive the host for callbacks / services ---
            Route.Triangulate -> TriangulateToolScreen(this)
            Route.Clock -> ClockToolScreen(this)
            Route.SignalFinder -> SignalFinderToolScreen(this)
            Route.TurnBack -> TurnBackToolScreen()
            Route.Settings -> SettingsToolScreen(this)
            Route.StrideLengthEstimation -> StrideLengthEstimationToolScreen(this)
            is Route.Clouds -> CloudToolScreen(this)
            is Route.CloudResults -> CloudResultsToolScreen(this)
            Route.Astronomy -> AstronomyToolScreen(this)
            is Route.AugmentedReality -> AugmentedRealityToolScreen(this)
            is Route.BeaconList -> BeaconListToolScreen(
                route = route,
                onController = { screenController = it }
            )
            is Route.BeaconDetails -> BeaconDetailsToolScreen(route)
            is Route.PlaceBeacon -> PlaceBeaconToolScreen(route)
            is Route.OfflineMapList -> OfflineMapListToolScreen(this)
            Route.ScanQr -> ScanQRToolScreen(this)
            is Route.ViewQr -> QRShareScreen(
                title = route.title,
                text = route.text
            )
            is Route.VectorMapView -> ViewVectorMapToolScreen(this)
            is Route.Navigation -> NavigatorToolScreen(route, quickActionHost = this)
            is Route.PathOverview -> PathOverviewToolScreen(route)
            Route.Paths -> PathsToolScreen()
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
            is Route.CreatePackItem -> CreateItemToolScreen(route)
            Route.FieldGuide -> FieldGuideToolScreen(this)
            is Route.FieldGuidePage -> FieldGuidePageToolScreen(this)
            Route.FieldGuideSettings -> FieldGuideSettingsToolScreen(this)
            is Route.SightingList -> SightingListToolScreen(this)
            is Route.CreateFieldGuidePage -> CreateFieldGuidePageToolScreen(this)
            is Route.CreateFieldGuideSighting -> CreateFieldGuideSightingToolScreen(this)
            is Route.PhotoMaps -> PhotoMapsToolScreen(this)
            Route.Clinometer -> ClinometerToolScreen(
                host = this,
                onController = { screenController = it }
            )
            Route.Weather -> WeatherToolScreen(this)
            Route.LocalMessaging -> TrailSenseCommsToolScreen(this, "4c285dfe-1c8b-45eb-bb79-3f1d2eb6ae48")
            Route.LocalTalk -> TrailSenseCommsToolScreen(this, "b76f32bf-6a72-4992-a741-0c9bf19ebd11")
        }
    }
}
