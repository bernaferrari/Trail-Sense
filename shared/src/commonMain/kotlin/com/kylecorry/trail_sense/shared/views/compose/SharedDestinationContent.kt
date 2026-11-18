@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.sense.clinometer.Clinometer
import com.kylecorry.andromeda.torch.ScreenTorch
import com.kylecorry.andromeda.torch.screenBrightness
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.trail_sense.shared.from
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.errors.ErrorHandlerScreen
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.ui.AboutScreen
import com.kylecorry.trail_sense.settings.ui.AugmentedRealitySettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.BeaconSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.BubbleLevelSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.CalibrateBarometerToolScreen
import com.kylecorry.trail_sense.settings.ui.CameraSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.CellSignalSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.ClinometerSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.ClockSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.CompassCalibrationContent
import com.kylecorry.trail_sense.settings.ui.ErrorSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.FlashlightSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.LicensesToolScreen
import com.kylecorry.trail_sense.settings.backup.PortableBackup
import com.kylecorry.trail_sense.settings.backup.PortableBackupScreen
import com.kylecorry.trail_sense.settings.ui.PathsSettingsScreen
import com.kylecorry.trail_sense.settings.ui.PhotoMapSettingsScreen
import com.kylecorry.trail_sense.settings.ui.RulerSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.SensorSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.ThermometerSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.TideSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.TurnBackSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.UnitSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.WaterBoilSettingsToolScreen
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.dialogs.oneTimeSnackbar
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.morse.ISignalingDevice
import com.kylecorry.trail_sense.shared.morse.SignalPlayer
import com.kylecorry.trail_sense.shared.morse.Signals
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.RouteLocation
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sound.TonePlayerFactory
import com.kylecorry.trail_sense.shared.text.LocalAppStrings
import com.kylecorry.trail_sense.shared.text.RawTextReader
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.shared.text.SystemString
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.tools.astronomy.ui.AstronomySettingsScreen
import com.kylecorry.trail_sense.tools.astronomy.ui.AstronomyToolScreen
import com.kylecorry.trail_sense.tools.ballistics.ui.BallisticsScreen
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.infrastructure.export.BeaconGpxImporter
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.BeaconSortMethod
import com.kylecorry.trail_sense.tools.beacons.ui.BeaconDetailsToolScreen
import com.kylecorry.trail_sense.tools.beacons.ui.PlaceBeaconToolScreen
import com.kylecorry.trail_sense.tools.beacons.ui.list.BeaconAction
import com.kylecorry.trail_sense.tools.beacons.ui.list.BeaconCreateAction
import com.kylecorry.trail_sense.tools.beacons.ui.list.BeaconGroupAction
import com.kylecorry.trail_sense.tools.beacons.ui.list.BeaconListScreen
import com.kylecorry.trail_sense.tools.beacons.ui.list.BeaconListViewModel
import com.kylecorry.trail_sense.tools.beacons.ui.list.BeaconToolbarAction
import com.kylecorry.trail_sense.tools.cliffheight.ui.CliffHeightToolScreen
import com.kylecorry.trail_sense.tools.climate.ui.ClimateToolScreen
import com.kylecorry.trail_sense.tools.clinometer.ui.ClinometerView
import com.kylecorry.trail_sense.tools.clinometer.ui.ClinometerViewModel
import com.kylecorry.trail_sense.tools.clock.ui.ClockScreen
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.tools.clouds.ui.CloudScreen
import com.kylecorry.trail_sense.tools.clouds.ui.CloudViewModel
import com.kylecorry.trail_sense.tools.convert.ui.ConvertScreen
import com.kylecorry.trail_sense.tools.declination.ui.DeclinationScreen
import com.kylecorry.trail_sense.tools.diagnostics.ui.DiagnosticsToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.CreateFieldGuideSightingToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuidePageToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuideSettingsToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuideToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.SightingListToolScreen
import com.kylecorry.trail_sense.tools.flashlight.ui.FlashlightToolScreen
import com.kylecorry.trail_sense.tools.flashlight.ui.ScreenFlashlightToolScreen
import com.kylecorry.trail_sense.tools.flashlight.ui.ScreenLight
import com.kylecorry.trail_sense.tools.guide.ui.GuideListToolScreen
import com.kylecorry.trail_sense.tools.guide.ui.GuideScreen
import com.kylecorry.trail_sense.tools.level.ui.LevelScreen
import com.kylecorry.trail_sense.tools.light.ui.LightMeterScreen
import com.kylecorry.trail_sense.tools.lightning.ui.LightningScreen
import com.kylecorry.trail_sense.tools.metaldetector.ui.MetalDetectorToolScreen
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.infrastructure.INavigator
import com.kylecorry.trail_sense.tools.navigation.ui.CardinalLabels
import com.kylecorry.trail_sense.tools.navigation.ui.CompassBearingIndicator
import com.kylecorry.trail_sense.tools.navigation.ui.CompassMarker
import com.kylecorry.trail_sense.tools.navigation.ui.LinearCompass
import com.kylecorry.trail_sense.tools.navigation.ui.NavigationSettingsScreen
import com.kylecorry.trail_sense.tools.navigation.ui.NavigatorSensors
import com.kylecorry.trail_sense.tools.navigation.ui.NavigatorViewModel
import com.kylecorry.trail_sense.tools.navigation.ui.RadarCompass
import com.kylecorry.trail_sense.tools.notes.ui.NoteEditorToolScreen
import com.kylecorry.trail_sense.tools.notes.ui.NotesToolScreen
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.ui.CreateItemToolScreen
import com.kylecorry.trail_sense.tools.packs.ui.PackItemListToolScreen
import com.kylecorry.trail_sense.tools.packs.ui.PackListToolScreen
import com.kylecorry.trail_sense.tools.packs.ui.PackingListFiles
import com.kylecorry.trail_sense.tools.paths.ui.commands.ToggleBacktrackCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangeBacktrackFrequencyCommand
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.tools.turn_back.ui.TurnBackToolScreen
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.offline_maps.ui.SharedOfflineMap
import com.kylecorry.trail_sense.tools.offline_maps.ui.SharedOfflineMapList
import com.kylecorry.trail_sense.tools.paths.PathsToolServices
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup
import com.kylecorry.trail_sense.tools.paths.domain.PathMetadata
import com.kylecorry.trail_sense.tools.paths.domain.PathStyle
import com.kylecorry.trail_sense.tools.paths.ui.PathAction
import com.kylecorry.trail_sense.tools.paths.ui.PathGroupAction
import com.kylecorry.trail_sense.tools.paths.ui.PathSortMethod
import com.kylecorry.trail_sense.tools.paths.ui.PathsScreen
import com.kylecorry.trail_sense.tools.paths.ui.PathsToolbarAction
import com.kylecorry.trail_sense.tools.paths.ui.PathsViewModel
import com.kylecorry.trail_sense.shared.io.SharedGpxIOService
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.sharing.copyToClipboard
import com.kylecorry.trail_sense.shared.sharing.openEmail
import com.kylecorry.trail_sense.shared.sharing.openExternalUrl
import com.kylecorry.trail_sense.shared.sharing.pickImageFile
import com.kylecorry.trail_sense.shared.sharing.shareText
import com.kylecorry.trail_sense.tools.mirror.ui.SharedMirrorCameraScreen
import com.kylecorry.trail_sense.tools.packs.ui.SharedPackingListFiles
import com.kylecorry.trail_sense.tools.paths.ui.commands.ExportPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ImportPathsCommand
import com.kylecorry.trail_sense.tools.pedometer.ui.PedometerSettingsScreen
import com.kylecorry.trail_sense.tools.pedometer.ui.PedometerSystemStatus
import com.kylecorry.trail_sense.tools.pedometer.ui.PedometerToolScreen
import com.kylecorry.trail_sense.tools.pedometer.ui.StrideLengthEstimationToolScreen
import com.kylecorry.trail_sense.tools.qr.ui.QRShareScreen
import com.kylecorry.trail_sense.tools.qr.ui.SharedScanQRScreen
import com.kylecorry.trail_sense.tools.ruler.ui.RulerScreen
import com.kylecorry.trail_sense.tools.solarpanel.ui.SolarPanelScreen
import com.kylecorry.trail_sense.tools.survival_guide.ui.SurvivalGuideListToolScreen
import com.kylecorry.trail_sense.tools.survival_guide.ui.SurvivalGuideReaderContent
import com.kylecorry.trail_sense.tools.temperature_estimation.ui.TemperatureEstimationToolScreen
import com.kylecorry.trail_sense.tools.tides.ui.CreateTideToolScreen
import com.kylecorry.trail_sense.tools.tides.ui.TideListToolScreen
import com.kylecorry.trail_sense.tools.tides.ui.TidesToolScreen
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import com.kylecorry.trail_sense.tools.tools.ui.PinnedToolManager
import com.kylecorry.trail_sense.tools.tools.ui.ToolSearchFocusable
import com.kylecorry.trail_sense.tools.tools.ui.ToolsHomeContent
import com.kylecorry.trail_sense.tools.triangulate.ui.TriangulateToolScreen
import com.kylecorry.trail_sense.tools.waterpurification.ui.WaterPurificationToolScreen
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.IWeatherRepo
import com.kylecorry.trail_sense.tools.weather.ui.WeatherSettingsScreen
import com.kylecorry.trail_sense.tools.weather.ui.WeatherToolScreen
import com.kylecorry.trail_sense.tools.whitenoise.ui.WhiteNoiseScreen
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch

private object SharedPedometerStatus : PedometerSystemStatus {
    override fun canRecognizeActivity(): Boolean = true
    override fun isBackgroundLocationEnabled(): Boolean = true
    override fun isNotificationChannelBlocked(): Boolean = false
}

/** Dispatches a [Route] to its multiplatform composable implementation. */
@Composable
fun TrailSenseDestinationHost.SharedDestinationContent(
    modifier: Modifier = Modifier
) {
    val userPrefs = remember { getAppService<UserPreferences>() }
    val formatter = remember { getAppService<FormatService>() }
    val strings = LocalAppStrings.current
    val host = AppHost.current()

    when (val r = route) {
        // --- Tools hub ---
        Route.Tools -> {
            val catalog = remember { getAppService<ToolCatalog>() }
            val pinnedTools = remember(userPrefs) { PinnedToolManager(userPrefs) }
            val rawTexts = remember { getAppService<RawTextReader>() }
            val tools = remember(catalog) { catalog.getTools() }
            ToolsHomeContent(
                tools = tools,
                pinnedTools = pinnedTools,
                keywordReader = rawTexts,
                quickActions = emptyList(),
                onShowLongPressHint = {
                    Alerts.oneTimeSnackbar(
                        host,
                        strings.get(Res.string.tool_long_press_hint_toast),
                        "tools_long_press_notice_shown",
                        short = false
                    )
                },
                onToolClick = { navigator.navigate(it.route) },
                onToolLongClick = { tool, model ->
                    val pinLabel = strings.get(if (model.isPinned(tool.id)) Res.string.unpin else Res.string.pin)
                    val guide = tool.guideId
                    val actions = buildList<Pair<String, () -> Unit>> {
                        add(pinLabel to { if (model.isPinned(tool.id)) model.unpin(tool.id) else model.pin(tool.id) })
                        if (guide != null) {
                            add(strings.get(Res.string.tool_user_guide_title) to { navigator.navigate(Route.Guide(tool.name, guide)) })
                        }
                        tool.settingsRoute?.let { settingsRoute ->
                            add(strings.get(Res.string.settings) to { navigator.navigate(settingsRoute) })
                        }
                    }
                    Pickers.item(host, tool.name, actions.map { it.first }) { selected ->
                        selected?.let { actions.getOrNull(it)?.second?.invoke() }
                    }
                },
                onSettingsClick = { navigator.navigate(Route.Settings) },
                onController = { controller: ToolSearchFocusable? -> screenController = controller }
            )
        }

        // --- Core Outdoor & Sensor Tools ---
        is Route.Navigation -> SharedNavigationScreen(r, modifier)
        Route.Weather -> WeatherToolScreen(modifier = modifier) { _, _ -> }
        Route.Astronomy -> AstronomyToolScreen(bindQuickActions = { _, _, _ -> }, modifier = modifier)
        Route.Tides -> TidesToolScreen()
        Route.TideList -> TideListToolScreen()
        is Route.CreateTide -> CreateTideToolScreen(r)
        Route.Climate -> ClimateToolScreen()
        Route.Lightning -> LightningScreen(modifier)
        Route.TemperatureEstimation -> TemperatureEstimationToolScreen()
        Route.Pedometer -> PedometerToolScreen()
        Route.StrideLengthEstimation -> StrideLengthEstimationToolScreen()
        Route.CliffHeight -> CliffHeightToolScreen()
        Route.Triangulate -> TriangulateToolScreen(modifier)
        Route.Ruler -> RulerScreen(modifier)
        Route.Level -> LevelScreen(modifier)
        Route.Clinometer -> SharedClinometerScreen(modifier)
        Route.Declination -> DeclinationScreen(modifier)
        Route.SolarPanel -> SolarPanelScreen(modifier)
        Route.Clock -> {
            val sensorService = remember { getAppService<SensorService>() }
            val gps = remember(sensorService) { sensorService.getGPS() }
            ClockScreen(
                formatService = formatter,
                prefs = userPrefs,
                gps = gps,
                isGpsTimedOut = { false },
                getGpsTime = { Clock.System.now() },
                requestExactAlarms = { it(true) },
                onScheduleNextMinute = null
            )
        }
        Route.WaterPurification -> WaterPurificationToolScreen()
        Route.TurnBack -> if (getAppService<ToolCatalog>().isToolAvailable(ToolIds.TURN_BACK)) {
            TurnBackToolScreen(remember { getAppService() })
        } else UnsupportedSharedToolScreen(appString(Res.string.tool_turn_back))
        Route.WhiteNoise -> WhiteNoiseScreen(modifier)
        Route.Flashlight -> FlashlightToolScreen(offerController = { ScreenControllerEffect(this, it) })
        Route.ScreenFlashlight -> {
            val brightness = remember { screenBrightness() }
            if (brightness != null) {
                val screenLight = remember(brightness) {
                    val torch = ScreenTorch(brightness)
                    object : ScreenLight {
                        override fun on(brightness: Float) = torch.on(brightness)
                        override fun off() = torch.off()
                    }
                }
                ScreenFlashlightToolScreen(
                    light = screenLight,
                    offerController = { ScreenControllerEffect(this, it) }
                )
            } else {
                UnsupportedSharedToolScreen(appString(Res.string.flashlight_title))
            }
        }
        Route.Whistle -> SharedWhistleScreen(modifier)
        Route.MetalDetector -> MetalDetectorToolScreen(modifier)
        Route.LightMeter -> LightMeterScreen(modifier)
        Route.Convert -> ConvertScreen(modifier)
        Route.Ballistics -> BallisticsScreen(modifier)

        // --- Notes, Packs, Guides, Field Guide, Clouds, Beacons, Paths ---
        Route.Notes -> NotesToolScreen(getAppService(), strings, formatter)
        is Route.NoteEditor -> NoteEditorToolScreen(r, getAppService())
        Route.PackList -> {
            val coroutineScope = rememberCoroutineScope()
            val packingFiles = remember(coroutineScope, navigator, this) {
                SharedPackingListFiles(coroutineScope, navigator, this)
            }
            PackListToolScreen(packingFiles)
        }
        is Route.PackItemList -> {
            val coroutineScope = rememberCoroutineScope()
            val packingFiles = remember(coroutineScope, navigator, this) {
                SharedPackingListFiles(coroutineScope, navigator, this)
            }
            PackItemListToolScreen(r, packingFiles)
        }
        is Route.CreatePackItem -> CreateItemToolScreen(r)
        is Route.SurvivalGuideList -> SurvivalGuideListToolScreen(r)
        is Route.SurvivalGuideReader -> SurvivalGuideReaderContent(r)
        Route.FieldGuide -> FieldGuideToolScreen()
        is Route.FieldGuidePage -> FieldGuidePageToolScreen(r)
        is Route.SightingList -> SightingListToolScreen(r)
        is Route.CreateFieldGuideSighting -> CreateFieldGuideSightingToolScreen(r)
        Route.FieldGuideSettings -> FieldGuideSettingsToolScreen()
        Route.GuideList -> GuideListToolScreen()
        is Route.Guide -> GuideScreen(r.name, r.contents)
        is Route.ViewQr -> QRShareScreen(r.title, r.text)
        Route.ScanQr -> SharedScanQRScreen(
            onBack = { navigator.navigateUp() },
            onOpenBeacon = { navigator.navigate(Route.BeaconDetails(it)) },
            onOpenNote = { navigator.navigate(Route.NoteEditor(it)) },
            modifier = modifier
        )
        Route.MirrorCamera -> SharedMirrorCameraScreen(
            onBack = { navigator.navigateUp() },
            modifier = modifier
        )
        is Route.PlaceBeacon -> PlaceBeaconToolScreen(r, modifier)
        is Route.BeaconDetails -> {
            val coroutineScope = rememberCoroutineScope()
            BeaconDetailsToolScreen(
                route = r,
                onShare = { beacon ->
                    val text = "${beacon.name}: ${formatter.formatLocation(beacon.coordinate)}"
                    coroutineScope.launch {
                        val chosen = chooseAction(
                            title = beacon.name,
                            actions = listOf(
                                ActionItem("Share", Icons.Default.Share) {},
                                ActionItem("QR Code", Icons.Default.QrCode) {}
                            )
                        )
                        when (chosen?.name) {
                            "Share" -> shareText(beacon.name, text)
                            "QR Code" -> navigator.navigate(Route.ViewQr(beacon.name, GeoUri.from(beacon).toString()))
                        }
                    }
                }
            )
        }
        is Route.BeaconList -> SharedBeaconListScreen(r, modifier)
        Route.Paths -> SharedPathsScreen(modifier)
        is Route.Map -> if (getAppService<ToolCatalog>().isToolAvailable(ToolIds.MAP)) {
            SharedOfflineMap()
        } else UnsupportedSharedToolScreen(appString(Res.string.map))
        is Route.OfflineMapList -> if (getAppService<ToolCatalog>().isToolAvailable(ToolIds.OFFLINE_MAPS)) {
            SharedOfflineMapList()
        } else UnsupportedSharedToolScreen(appString(Res.string.offline_maps))
        is Route.VectorMapView -> if (getAppService<ToolCatalog>().isToolAvailable(ToolIds.OFFLINE_MAPS)) {
            SharedOfflineMap(r.offlineMapFileId)
        } else UnsupportedSharedToolScreen(appString(Res.string.offline_maps))
        is Route.Clouds -> SharedCloudsScreen(modifier)
        Route.Diagnostics, Route.SensorDetails -> DiagnosticsToolScreen(exportLogs = null)
        is Route.ErrorHandler -> {
            val coroutineScope = rememberCoroutineScope()
            val diagnosticsTitle = appString(Res.string.diagnostics)
            ErrorHandlerScreen(
                toolId = r.toolId,
                onEmail = {
                    coroutineScope.launch {
                        openEmail(
                            recipient = "trailsense@kylecorry.com",
                            subject = "Trail Sense Error Report",
                            body = r.error
                        )
                    }
                },
                onCopyError = {
                    coroutineScope.launch {
                        val copied = copyToClipboard(r.error)
                        showMessage(if (copied) "Copied error details" else "Could not copy error details")
                    }
                },
                onViewDetails = { Alerts.dialog(host, diagnosticsTitle, r.error, cancelText = null) },
                onReopenTool = { navigator.navigateUp() },
                onRestartApp = { navigator.switchTab(Route.Tools) },
                onOpenSettings = { navigator.navigate(Route.Settings) }
            )
        }

        // --- Settings & Calibration ---
        Route.Settings -> SharedSettingsHubScreen(modifier)
        Route.PortableBackup -> {
            val backup = remember { runCatching { getAppService<PortableBackup>() }.getOrNull() }
            if (backup != null) PortableBackupScreen(backup)
            else UnsupportedSharedToolScreen(appString(Res.string.portable_backup))
        }
        Route.UnitSettings -> UnitSettingsToolScreen()
        Route.SensorSettings -> SensorSettingsToolScreen()
        Route.NavigationSettings -> NavigationSettingsScreen(emptyList()) {}
        Route.WeatherSettings -> WeatherSettingsScreen(emptyList(), null, null)
        Route.AstronomySettings -> AstronomySettingsScreen(emptyList(), showNativeAlarmOptions = false)
        Route.PathsSettings -> PathsSettingsScreen(null)
        Route.BeaconSettings -> BeaconSettingsToolScreen()
        Route.PedometerSettings -> PedometerSettingsScreen(SharedPedometerStatus, null, null)
        Route.FlashlightSettings -> FlashlightSettingsToolScreen()
        Route.TideSettings -> TideSettingsToolScreen()
        Route.ClinometerSettings -> ClinometerSettingsToolScreen()
        Route.BubbleLevelSettings -> BubbleLevelSettingsToolScreen(userPrefs.bubbleLevel, formatter)
        Route.RulerSettings -> RulerSettingsToolScreen()
        Route.ClockSettings -> ClockSettingsToolScreen()
        Route.TurnBackSettings -> TurnBackSettingsToolScreen()
        Route.WaterBoilSettings -> WaterBoilSettingsToolScreen()
        Route.ThermometerSettings -> ThermometerSettingsToolScreen()
        Route.CellSignalSettings -> CellSignalSettingsToolScreen()
        Route.CameraSettings -> CameraSettingsToolScreen()
        Route.AugmentedRealitySettings -> AugmentedRealitySettingsToolScreen()
        Route.PhotoMapSettings -> PhotoMapSettingsScreen(null)
        Route.ErrorSettings -> ErrorSettingsToolScreen()
        Route.CalibrateBarometer -> CalibrateBarometerToolScreen()
        Route.CalibrateCompass -> {
            TrailSensePage(
                title = appString(Res.string.pref_compass_sensor_title),
                backLabel = appString(Res.string.back)
            ) {
                item("calibration") {
                    CompassCalibrationContent(Modifier.fillMaxWidth())
                }
            }
        }
        Route.About -> {
            val coroutineScope = rememberCoroutineScope()
            AboutScreen(
                version = "Shared Multiplatform",
                onOpenUrl = { url -> coroutineScope.launch { openExternalUrl(url) } },
                onEmailDeveloper = {
                    coroutineScope.launch {
                        openEmail(
                            recipient = "trailsense@kylecorry.com",
                            subject = "Trail Sense",
                            body = ""
                        )
                    }
                },
                onLicenses = { navigator.navigate(Route.Licenses) }
            )
        }
        Route.Licenses -> LicensesToolScreen()

        // --- Platform-specific or staged routes ---
        else -> UnsupportedSharedToolScreen(r::class.simpleName ?: appString(Res.string.tools))
    }
}

@Composable
private fun SharedNavigationScreen(
    route: Route.Navigation,
    modifier: Modifier = Modifier
) {
    val userPrefs = remember { getAppService<UserPreferences>() }
    val formatter = remember { getAppService<FormatService>() }
    val sensorService = remember { getAppService<SensorService>() }
    val strings = LocalAppStrings.current
    val navigator = LocalNavigator.current
    val sharedNav = remember { getAppService<INavigator>() }
    val pathService = remember { getAppService<IPathService>() }
    val weatherRepo = remember { getAppService<IWeatherRepo>() }
    val beaconRepo = remember { getAppService<BeaconRepo>() }
    val viewModel = viewModel {
        val sensors = NavigatorSensors(sensorService, userPrefs)
        NavigatorViewModel(
            route = route,
            sensors = sensors,
            navigator = sharedNav,
            beaconUpdates = beaconRepo.getBeacons(),
            pathService = pathService,
            weatherRepo = weatherRepo,
            prefs = userPrefs.navigation,
            useTrueNorth = userPrefs.compass.useTrueNorth,
            diagnostics = emptyList(),
            debugElevations = { _, _ -> }
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.startSensors()
                Lifecycle.Event.ON_PAUSE -> viewModel.stopSensors()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val sensors = viewModel.sensors
    var azimuth by remember(viewModel) {
        mutableStateOf(sensors.compass.bearing.value.takeIf { sensors.compass.hasValidReading })
    }
    var location by remember(viewModel) {
        mutableStateOf(sensors.gps.location.takeIf { sensors.gps.hasValidReading })
    }
    var altitude by remember(viewModel) {
        mutableStateOf(sensors.altimeter.altitude.takeIf { sensors.altimeter.hasValidReading })
    }
    val destination by viewModel.destination.collectAsStateWithLifecycle(initialValue = null)

    DisposableEffect(viewModel) {
        val compassListener = {
            azimuth = sensors.compass.bearing.value.takeIf { sensors.compass.hasValidReading }
            true
        }
        val gpsListener = {
            location = sensors.gps.location.takeIf { sensors.gps.hasValidReading }
            true
        }
        val altListener = {
            altitude = sensors.altimeter.altitude.takeIf { sensors.altimeter.hasValidReading }
            true
        }
        viewModel.sensors.compass.start(compassListener)
        viewModel.sensors.gps.start(gpsListener)
        viewModel.sensors.altimeter.start(altListener)
        onDispose {
            viewModel.sensors.compass.stop(compassListener)
            viewModel.sensors.gps.stop(gpsListener)
            viewModel.sensors.altimeter.stop(altListener)
        }
    }

    val cardinalLabels = CardinalLabels(
        north = formatter.formatDirection(CompassDirection.North),
        east = formatter.formatDirection(CompassDirection.East),
        south = formatter.formatDirection(CompassDirection.South),
        west = formatter.formatDirection(CompassDirection.West)
    )
    val position = location
    val heading = azimuth
    val markers = remember(viewModel.nearbyBeacons, position) {
        if (position == null) emptyList() else viewModel.nearbyBeacons.map {
            CompassMarker(
                bearing = position.bearingTo(it.coordinate).value,
                icon = TrailIcons.Location
            )
        }
    }
    val primaryColor = MaterialTheme.colorScheme.primary
    val bearings = remember(destination, location, primaryColor) {
        when (val d = destination) {
            is Destination.Beacon -> position?.let {
                listOf(CompassBearingIndicator(it.bearingTo(d.beacon.coordinate).value, color = primaryColor))
            }.orEmpty()
            is Destination.Bearing -> listOf(
                CompassBearingIndicator(d.trueBearing.value, color = primaryColor)
            )
            else -> emptyList()
        }
    }

    TrailSensePage(
        title = appString(Res.string.navigation),
        onBack = if (route == Route.Navigation()) null else ({ navigator.navigateUp() }),
        modifier = modifier,
        actions = {
            FilledTonalButton(onClick = { navigator.navigate(Route.BeaconList()) }) {
                Text(appString(Res.string.beacons))
            }
        }
    ) {
        item("heading") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = heading?.let { formatter.formatDegrees(it) } ?: appString(Res.string.compass_unavailable),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = position?.let { formatter.formatLocation(it) } ?: appString(Res.string.gps_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = altitude?.let { formatter.formatDistance(Distance.meters(it)) } ?: appString(Res.string.unavailable),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (heading != null) item("compass") {
            LinearCompass(
                azimuth = heading,
                labels = cardinalLabels,
                markers = markers,
                bearings = bearings,
                modifier = Modifier.fillMaxWidth().height(72.dp)
            )
            Spacer(Modifier.height(TrailSenseSpacing.md))
            RadarCompass(
                azimuth = heading,
                labels = cardinalLabels,
                modifier = Modifier.fillMaxWidth().height(280.dp)
            )
        }
        destination?.let { active ->
            item("destination") {
                val destTitle = when (active) {
                    is Destination.Beacon -> active.beacon.name
                    is Destination.Bearing -> formatter.formatDegrees(active.trueBearing.value)
                    is Destination.Path -> active.path.name ?: appString(Res.string.paths)
                }
                val destSubtitle = when (active) {
                    is Destination.Beacon -> position?.let {
                        formatter.formatDistance(Distance.meters(it.distanceTo(active.beacon.coordinate)))
                    } ?: appString(Res.string.gps_unavailable)
                    is Destination.Bearing -> formatter.formatDegrees(active.trueBearing.value)
                    is Destination.Path -> formatter.formatDistance(active.path.metadata.distance)
                }
                val cancelLabel = strings.system(SystemString.Cancel)
                TrailSenseListItem(
                    title = destTitle,
                    subtitle = destSubtitle,
                    trailing = {
                        OutlinedButton(onClick = { sharedNav.cancelBeaconNavigation() }) {
                            Text(cancelLabel)
                        }
                    }
                )
            }
        }
        item("actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
            ) {
                Button(
                    onClick = {
                        val loc = position?.let { RouteLocation(it.latitude, it.longitude) }
                        navigator.navigate(Route.PlaceBeacon(initialLocation = loc))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(appString(Res.string.beacons))
                }
                FilledTonalButton(
                    onClick = { navigator.navigate(Route.NavigationSettings) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(appString(Res.string.settings))
                }
            }
        }
    }
}

@Composable
private fun SharedBeaconListScreen(
    route: Route.BeaconList,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.current
    val host = AppHost.current()
    val strings = LocalAppStrings.current
    val formatter = remember { FormatService.getInstance() }
    val prefs = remember { getAppService<UserPreferences>() }
    val beaconService = remember { getAppService<IBeaconService>() }
    val sensorService = remember { getAppService<SensorService>() }
    val gps = remember(sensorService) { sensorService.getGPS() }
    val sharedNav = remember { getAppService<INavigator>() }
    val scope = rememberCoroutineScope()

    val viewModel = viewModel {
        BeaconListViewModel(
            route = route,
            beaconService = beaconService,
            preferences = prefs.beacons,
            gps = gps,
            importer = BeaconGpxImporter()
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onResume()
                Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val sortOptions = remember { BeaconSortMethod.entries }
    val sortName = when (viewModel.sort) {
        BeaconSortMethod.MostRecent -> appString(Res.string.most_recent)
        BeaconSortMethod.Closest -> appString(Res.string.closest)
        BeaconSortMethod.Name -> appString(Res.string.name)
    }

    BeaconListScreen(
        title = viewModel.groupName ?: appString(Res.string.places_title),
        sortMenuText = strings.get(Res.string.sort_by, sortName),
        searchText = viewModel.searchText,
        items = viewModel.items,
        loaded = viewModel.isLoaded,
        resetKey = viewModel.resetKey,
        formatService = formatter,
        baseDistanceUnits = prefs.baseDistanceUnits,
        currentLocation = viewModel.location,
        isCreateMenuOpen = viewModel.isCreateMenuOpen,
        onSearchChange = viewModel::search,
        onToolbarAction = { action ->
            when (action) {
                BeaconToolbarAction.Sort -> {
                    val labels = sortOptions.map {
                        when (it) {
                            BeaconSortMethod.MostRecent -> strings.get(Res.string.most_recent)
                            BeaconSortMethod.Closest -> strings.get(Res.string.closest)
                            BeaconSortMethod.Name -> strings.get(Res.string.name)
                        }
                    }
                    Pickers.item(host, strings.get(Res.string.sort_by, sortName), labels, sortOptions.indexOf(viewModel.sort)) { idx ->
                        idx?.let { viewModel.changeSort(sortOptions[it]) }
                    }
                }
                BeaconToolbarAction.Delete -> {
                    Alerts.dialog(host, strings.get(Res.string.delete), strings.get(Res.string.delete)) { cancelled ->
                        if (!cancelled) viewModel.delete(null)
                    }
                }
                BeaconToolbarAction.Export -> {
                    scope.launch {
                        val gpx = viewModel.getExportGpx()
                        val exported = SharedGpxIOService().export(
                            gpx,
                            "trail-sense-${Clock.System.now().epochSeconds}.gpx"
                        )
                        if (exported) {
                            Alerts.snackbar(host, strings.plural(Res.plurals.beacons_exported, gpx.waypoints.size, gpx.waypoints.size))
                        }
                    }
                }
                else -> Unit
            }
        },
        onCreateMenuOpenChange = { viewModel.isCreateMenuOpen = it },
        onCreateAction = { action ->
            viewModel.isCreateMenuOpen = false
            when (action) {
                BeaconCreateAction.CreateBeacon -> navigator.navigate(
                    Route.PlaceBeacon(initialGroupId = viewModel.rootId ?: 0L)
                )
                BeaconCreateAction.CreateGroup -> {
                    Pickers.text(host, strings.get(Res.string.group), null, null, null) { name ->
                        if (!name.isNullOrBlank()) {
                            scope.launch {
                                beaconService.add(BeaconGroup(0, name.trim(), parentId = viewModel.rootId))
                                viewModel.refresh()
                            }
                        }
                    }
                }
                BeaconCreateAction.ImportGpx -> {
                    scope.launch {
                        val gpx = SharedGpxIOService().import() ?: return@launch
                        val count = viewModel.import(gpx)
                        Alerts.snackbar(host, strings.plural(Res.plurals.beacons_imported, count, count))
                    }
                }
                BeaconCreateAction.ImportQr -> navigator.navigate(Route.ScanQr)
                else -> Unit
            }
        },
        onBeaconAction = { beacon: Beacon, action: BeaconAction ->
            when (action) {
                BeaconAction.View -> navigator.navigate(Route.BeaconDetails(beacon.id))
                BeaconAction.Edit -> navigator.navigate(Route.PlaceBeacon(editBeaconId = beacon.id))
                BeaconAction.Navigate -> {
                    sharedNav.navigateTo(beacon)
                    navigator.navigate(Route.Navigation(beacon.id, navigationStarted = true))
                }
                BeaconAction.ToggleVisibility -> viewModel.toggleVisibility(beacon)
                BeaconAction.Share -> scope.launch {
                    shareText(beacon.name, "${beacon.name}: ${formatter.formatLocation(beacon.coordinate)}")
                }
                BeaconAction.Delete -> scope.launch {
                    beaconService.delete(beacon)
                    viewModel.refresh()
                }
                BeaconAction.Move -> Unit
            }
        },
        onGroupAction = { group: BeaconGroup, action: BeaconGroupAction ->
            when (action) {
                BeaconGroupAction.Open -> viewModel.open(group.id)
                BeaconGroupAction.HideAll -> scope.launch { viewModel.setGroupVisibility(group, false) }
                BeaconGroupAction.ShowAll -> scope.launch { viewModel.setGroupVisibility(group, true) }
                BeaconGroupAction.Delete -> viewModel.delete(group)
                else -> Unit
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun SharedPathsScreen(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    val host = AppHost.current()
    val strings = LocalAppStrings.current
    val formatter = remember { FormatService.getInstance() }
    val prefs = remember { getAppService<UserPreferences>() }
    val pathService = remember { getAppService<IPathService>() }
    val catalog = remember { getAppService<ToolCatalog>() }
    val backtrack = remember(catalog) { catalog.getService(PathsToolServices.BACKTRACK) }
    val gps = remember { getAppService<SensorService>().getGPS() }
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()

    var showBacktrackFrequency by remember { mutableStateOf(false) }

    val viewModel = viewModel {
        PathsViewModel(
            pathService = pathService,
            preferences = prefs.paths,
            backtrack = backtrack,
            gps = gps
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onResume()
                Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val sortOptions = remember { PathSortMethod.entries }
    val sortName = when (viewModel.sort) {
        PathSortMethod.MostRecent -> appString(Res.string.most_recent)
        PathSortMethod.Longest -> appString(Res.string.longest)
        PathSortMethod.Shortest -> appString(Res.string.shortest)
        PathSortMethod.Closest -> appString(Res.string.closest)
        PathSortMethod.Name -> appString(Res.string.name)
    }

    PathsScreen(
        title = viewModel.groupName ?: appString(Res.string.paths),
        sortMenuText = strings.get(Res.string.sort_by, sortName),
        searchText = viewModel.searchText,
        items = viewModel.items,
        isLoaded = viewModel.isLoaded,
        resetKey = viewModel.resetKey,
        backtrackState = viewModel.backtrackState,
        backtrackFrequency = viewModel.backtrackFrequency,
        formatService = formatter,
        baseDistanceUnits = prefs.baseDistanceUnits,
        onBack = { if (!viewModel.up()) navigator.navigateUp() },
        onSearchChange = viewModel::search,
        onMenuAction = { action ->
            if (action == PathsToolbarAction.Sort) {
                val labels = sortOptions.map {
                    when (it) {
                        PathSortMethod.MostRecent -> strings.get(Res.string.most_recent)
                        PathSortMethod.Longest -> strings.get(Res.string.longest)
                        PathSortMethod.Shortest -> strings.get(Res.string.shortest)
                        PathSortMethod.Closest -> strings.get(Res.string.closest)
                        PathSortMethod.Name -> strings.get(Res.string.name)
                    }
                }
                Pickers.item(host, strings.get(Res.string.sort_by, sortName), labels, sortOptions.indexOf(viewModel.sort)) { idx ->
                    idx?.let { viewModel.changeSort(sortOptions[it]) }
                }
            }
        },
        onPathAction = { path: Path, action: PathAction ->
            when (action) {
                PathAction.Delete -> scope.launch { pathService.deletePath(path) }
                PathAction.ToggleVisibility -> scope.launch {
                    pathService.addPath(path.copy(style = path.style.copy(visible = !path.style.visible)))
                }
                PathAction.Export -> ExportPathCommand(host, scope, SharedGpxIOService(), pathService).execute(path)
                else -> Unit
            }
        },
        onGroupAction = { group: PathGroup, action: PathGroupAction ->
            when (action) {
                PathGroupAction.Open -> viewModel.open(group.id)
                PathGroupAction.ShowAll -> scope.launch { viewModel.setGroupVisibility(group, true) }
                PathGroupAction.HideAll -> scope.launch { viewModel.setGroupVisibility(group, false) }
                PathGroupAction.Delete -> scope.launch { pathService.deleteGroup(group) }
                PathGroupAction.Export -> ExportPathCommand(host, scope, SharedGpxIOService(), pathService).execute(group)
                else -> Unit
            }
        },
        onImportPath = {
            ImportPathsCommand(host, scope, SharedGpxIOService(), pathService, prefs.paths).execute(viewModel.rootId)
        },
        onCreateGroup = {
            Pickers.text(host, strings.get(Res.string.group), null, null, null) { name ->
                if (!name.isNullOrBlank()) {
                    scope.launch {
                        pathService.addGroup(PathGroup(0, name.trim(), parentId = viewModel.rootId))
                        viewModel.refresh()
                    }
                }
            }
        },
        onCreatePath = {
            Pickers.text(host, strings.get(Res.string.paths), null, null, null) { name ->
                if (!name.isNullOrBlank()) {
                    scope.launch {
                        pathService.addPath(
                            Path(
                                id = 0,
                                name = name.trim(),
                                style = PathStyle.default(),
                                metadata = PathMetadata.empty,
                                parentId = viewModel.rootId
                            )
                        )
                        viewModel.refresh()
                    }
                }
            }
        },
        onCreatePathOnMap = null,
        onBacktrackFrequencyClick = if (backtrack?.isSupported == true) {
            { showBacktrackFrequency = true }
        } else null,
        onBacktrackToggleClick = if (backtrack?.isSupported == true) {
            { ToggleBacktrackCommand(effects, scope) {
                effects.showMessage(strings.get(Res.string.backtrack_no_permission))
            }.execute() }
        } else null,
        modifier = modifier.fillMaxSize()
    )

    if (showBacktrackFrequency) {
        DurationPickerDialog(
            title = appString(Res.string.pref_backtrack_frequency_title),
            duration = prefs.paths.backtrackRecordFrequency,
            message = appString(Res.string.actual_frequency_disclaimer),
            hint = appString(Res.string.frequency),
            showSeconds = true,
            onDurationPicked = { frequency ->
                if (frequency != null) {
                    ChangeBacktrackFrequencyCommand(host, scope, onChange = { viewModel.updateBacktrackStatus() })
                        .execute(frequency)
                }
                showBacktrackFrequency = false
            },
            onDismiss = { showBacktrackFrequency = false }
        )
    }
}

@Composable
private fun SharedClinometerScreen(modifier: Modifier = Modifier) {
    val formatter = remember { FormatService.getInstance() }
    val prefs = remember { getAppService<UserPreferences>() }
    val sensorService = remember { getAppService<SensorService>() }
    val orientation = remember(sensorService) { sensorService.getOrientation() }
    val clinometer = remember(orientation) { Clinometer(orientation, isAugmentedReality = false) }
    val viewModel = viewModel { ClinometerViewModel(baselineDistance = { prefs.clinometer.baselineDistance }) }
    val clinometerView = remember { ClinometerView(formatter::formatDegrees) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, clinometer) {
        val listener = {
            clinometerView.angle = clinometer.angle
            true
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.onResume()
                    clinometer.start(listener)
                }
                Lifecycle.Event.ON_PAUSE -> clinometer.stop(listener)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            clinometer.stop(listener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val displayedAngle = viewModel.displayedAngle(clinometerView.angle)
    val displayedIncline = viewModel.displayedIncline(clinometer.incline)
    val estimatedHeight = viewModel.distanceAway?.let { viewModel.getHeight(it, clinometer.incline) }
    val clinometerTitle = appString(Res.string.clinometer_title)

    TrailSensePage(
        title = clinometerTitle,
        backLabel = appString(Res.string.back),
        modifier = modifier
    ) {
        item("dial") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (viewModel.isLocked) {
                                    viewModel.clearLock()
                                } else {
                                    viewModel.startHold(clinometer.angle, clinometer.incline)
                                    viewModel.endHold(clinometer.angle, clinometer.incline)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                clinometerView.Content(Modifier.fillMaxSize())
            }
        }
        item("stats") {
            TrailSenseRowGroup {
                row { position ->
                    TrailSenseListItem(
                        title = formatter.formatDegrees(displayedIncline),
                        subtitle = "$clinometerTitle · ${formatter.formatDegrees(displayedAngle)}",
                        position = position
                    )
                }
                estimatedHeight?.let { height ->
                    row { position ->
                        TrailSenseListItem(
                            title = formatter.formatDistance(height),
                            subtitle = formatter.formatDistance(viewModel.distanceAway!!),
                            position = position
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedWhistleScreen(modifier: Modifier = Modifier) {
    val toneFactory = remember { getAppService<TonePlayerFactory>() }
    val player = remember(toneFactory) { toneFactory.create(2800) }
    var isHolding by remember { mutableStateOf(false) }
    var isSos by remember { mutableStateOf(false) }
    val sosSignal = remember { Signals.sos(200.milliseconds) }
    val signalPlayer = remember(player) {
        SignalPlayer(ISignalingDevice.from({ player.on() }, { player.off() }))
    }

    DisposableEffect(player) {
        onDispose {
            signalPlayer.cancel()
            player.off()
            player.release()
        }
    }

    TrailSensePage(
        title = appString(Res.string.tool_whistle_title),
        backLabel = appString(Res.string.back),
        modifier = modifier
    ) {
        item("hold") {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = if (isHolding) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isSos = false
                                signalPlayer.cancel()
                                isHolding = true
                                player.on()
                                tryAwaitRelease()
                                isHolding = false
                                player.off()
                            }
                        )
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(TrailIcons.ToolWhistle, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(TrailSenseSpacing.sm))
                        Text(appString(Res.string.whistle_hold), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        item("sos") {
            val sosLabel = appString(Res.string.sos)
            val sosSubtitle = appString(if (isSos) Res.string.whistle_playing else Res.string.whistle_signal_repeats)
            val sosPainter = rememberVectorPainter(TrailIcons.WhistleSos)
            TrailSenseRowGroup {
                row { position ->
                    TrailSenseListItem(
                        title = sosLabel,
                        subtitle = sosSubtitle,
                        icon = sosPainter,
                        position = position,
                        modifier = Modifier.clickable {
                            if (isSos) {
                                isSos = false
                                signalPlayer.cancel()
                                player.off()
                            } else {
                                isSos = true
                                signalPlayer.play(sosSignal, loop = true)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SharedCloudsScreen(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val cloudsTitle = appString(Res.string.clouds)
    val repo = remember { getAppService<CloudRepo>() }
    val viewModel = viewModel { CloudViewModel(repo) }
    val clouds by viewModel.clouds.collectAsStateWithLifecycle()

    CloudScreen(
        subtitle = "48h",
        clouds = clouds ?: emptyList(),
        onGuideClick = {
            navigator.navigate(Route.Guide(cloudsTitle, RawTexts.guide_tool_clouds))
        },
        onCameraClick = {
            scope.launch {
                val uri = effects.capturePhoto()
                if (uri != null) {
                    effects.showMessage("Photo captured")
                }
            }
        },
        onFileClick = {
            scope.launch {
                val picked = pickImageFile()
                if (picked != null) {
                    effects.showMessage("Selected ${picked.name}")
                }
            }
        },
        onManualClick = {},
        onCloudClick = { viewModel.selectedCloudImage = it.value.genus },
        onCloudImageClick = { viewModel.selectedCloudImage = it.value.genus },
        onDeleteCloud = { viewModel.delete(it) },
        selectedCloudImage = viewModel.selectedCloudImage,
        onDismissCloudImage = { viewModel.selectedCloudImage = null },
        modifier = modifier,
        loaded = clouds != null
    )
}

@Composable
private fun SharedSettingsHubScreen(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    val compassLabel = appString(Res.string.pref_compass_sensor_title)
    val barometerLabel = appString(Res.string.barometer)
    val compassPainter = rememberVectorPainter(TrailIcons.CompassIcon)
    val barometerPainter = rememberVectorPainter(TrailIcons.Barometer)
    val hasPortableBackup = remember { runCatching { getAppService<PortableBackup>() }.isSuccess }
    val entries = listOfNotNull(
        if (hasPortableBackup) appString(Res.string.portable_backup) to Route.PortableBackup else null,
        appString(Res.string.settings_section_measurements) to Route.UnitSettings,
        appString(Res.string.sensors) to Route.SensorSettings,
        appString(Res.string.navigation) to Route.NavigationSettings,
        appString(Res.string.beacons) to Route.BeaconSettings,
        appString(Res.string.paths) to Route.PathsSettings,
        appString(Res.string.weather) to Route.WeatherSettings,
        appString(Res.string.astronomy) to Route.AstronomySettings,
        appString(Res.string.pedometer) to Route.PedometerSettings,
        appString(Res.string.flashlight_title) to Route.FlashlightSettings,
        appString(Res.string.tides) to Route.TideSettings,
        appString(Res.string.clinometer_title) to Route.ClinometerSettings,
        appString(Res.string.tool_bubble_level_title) to Route.BubbleLevelSettings,
        appString(Res.string.tool_ruler_title) to Route.RulerSettings,
        appString(Res.string.tool_clock_title) to Route.ClockSettings,
        appString(Res.string.tool_turn_back) to Route.TurnBackSettings,
        appString(Res.string.water_boil_timer) to Route.WaterBoilSettings,
        appString(Res.string.tool_thermometer_title) to Route.ThermometerSettings,
        appString(Res.string.field_guide) to Route.FieldGuideSettings,
        appString(Res.string.diagnostics) to Route.Diagnostics,
        appString(Res.string.pref_category_about) to Route.About,
        appString(Res.string.pref_open_source_licenses_title) to Route.Licenses
    )

    TrailSensePage(
        title = appString(Res.string.settings),
        onBack = null,
        modifier = modifier
    ) {
        item("calibration") {
            TrailSenseRowGroup {
                row { position ->
                    TrailSenseListItem(
                        title = compassLabel,
                        icon = compassPainter,
                        position = position,
                        modifier = Modifier.clickable { navigator.navigate(Route.CalibrateCompass) }
                    )
                }
                row { position ->
                    TrailSenseListItem(
                        title = barometerLabel,
                        icon = barometerPainter,
                        position = position,
                        modifier = Modifier.clickable { navigator.navigate(Route.CalibrateBarometer) }
                    )
                }
            }
        }
        item("sections") {
            TrailSenseRowGroup {
                entries.forEach { (label, target) ->
                    row { position ->
                        TrailSenseListItem(
                            title = label,
                            position = position,
                            modifier = Modifier.clickable { navigator.navigate(target) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnsupportedSharedToolScreen(title: String) {
    TrailSensePage(title = title, backLabel = appString(Res.string.back)) {
        item("unsupported") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(TrailSenseSpacing.lg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appString(Res.string.no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
