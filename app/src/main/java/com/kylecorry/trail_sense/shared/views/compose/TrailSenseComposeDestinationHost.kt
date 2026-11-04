package com.kylecorry.trail_sense.shared.views.compose

import com.kylecorry.trail_sense.settings.ui.ThemeSettingsScreen
import com.kylecorry.trail_sense.main.theme.ThemeChoices
import android.content.Context
import android.content.Intent
import android.Manifest
import android.app.PendingIntent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.text.util.Linkify.WEB_URLS
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import com.kylecorry.trail_sense.settings.ui.SettingsChoiceRow
import com.kylecorry.trail_sense.settings.ui.SettingsInfoRow
import com.kylecorry.trail_sense.settings.ui.SettingsSliderRow
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.andromeda.core.system.CurrentApp
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.permissions.PermissionRationale
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.permissions.SpecialPermission
import com.kylecorry.andromeda.pdf.PdfConvert
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.andromeda.print.ColorMode
import com.kylecorry.andromeda.print.Orientation
import com.kylecorry.andromeda.print.Printer
import com.kylecorry.andromeda.print.ScaleMode
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.torch.ScreenTorch
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.luna.topics.generic.getOrNull
import com.kylecorry.sol.math.interpolation.Interpolation.map
import com.kylecorry.sol.science.meteorology.forecast.ForecastSource
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.main.errors.ErrorHandlerScreen
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.plugins.domain.Plugin
import com.kylecorry.trail_sense.receivers.StartOnBootController
import com.kylecorry.trail_sense.settings.backup.BackupCommand
import com.kylecorry.trail_sense.settings.backup.ChangeAutomaticBackupDirectoryCommand
import com.kylecorry.trail_sense.settings.backup.RestoreCommand
import com.kylecorry.trail_sense.settings.infrastructure.CameraPreferences
import com.kylecorry.trail_sense.settings.infrastructure.ErrorPreferences
import com.kylecorry.trail_sense.settings.licenses.Library
import com.kylecorry.trail_sense.settings.licenses.Licenses
import com.kylecorry.trail_sense.settings.ui.PluginDetailsScreen
import com.kylecorry.trail_sense.settings.ui.PathsSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.CalibrateAltimeterToolScreen
import com.kylecorry.trail_sense.settings.ui.CalibrateBarometerToolScreen
import com.kylecorry.trail_sense.settings.ui.CalibrateCompassToolScreen
import com.kylecorry.trail_sense.settings.ui.CalibrateGPSToolScreen
import com.kylecorry.trail_sense.settings.ui.SettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.ThermometerSettingsToolScreen
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsPickerRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.settings.ui.SingleSwitchSettingsScreen
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem.GroupBehavior
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersSheetState
import com.kylecorry.trail_sense.shared.permissions.canStartLocationForgroundService
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.camera.PhotoImportDialog
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.gps.GPSLocationSource
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.sharing.ActionSheetContent
import com.kylecorry.trail_sense.shared.sharing.MapSite
import com.kylecorry.trail_sense.shared.tiles.TileManager
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.views.ColorPickerDialog
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.charts.TrailSenseLineChart
import com.kylecorry.trail_sense.tools.WhistleScreen
import com.kylecorry.trail_sense.tools.ballistics.ui.BallisticsScreen
import com.kylecorry.trail_sense.tools.astronomy.ui.AstronomySettingsToolScreen
import com.kylecorry.trail_sense.tools.astronomy.ui.AstronomyToolScreen
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityToolScreen
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReading
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryLogWorker
import com.kylecorry.trail_sense.tools.battery.infrastructure.LowPowerMode
import com.kylecorry.trail_sense.tools.battery.ui.BatteryToolScreen
import com.kylecorry.trail_sense.tools.beacons.ui.BeaconDetailsToolScreen
import com.kylecorry.trail_sense.tools.beacons.ui.PlaceBeaconToolScreen
import com.kylecorry.trail_sense.tools.beacons.ui.list.BeaconListControllerHost
import com.kylecorry.trail_sense.tools.beacons.ui.list.BeaconListToolScreen
import com.kylecorry.trail_sense.tools.cliffheight.ui.CliffHeightScreen
import com.kylecorry.trail_sense.tools.clock.ui.ClockToolScreen
import com.kylecorry.trail_sense.tools.clouds.ui.CloudResultsToolScreen
import com.kylecorry.trail_sense.tools.clouds.ui.CloudToolScreen
import com.kylecorry.trail_sense.tools.clinometer.ui.ClinometerLockController
import com.kylecorry.trail_sense.tools.clinometer.ui.ClinometerToolScreen
import com.kylecorry.trail_sense.tools.climate.ui.ClimateToolScreen
import com.kylecorry.trail_sense.tools.convert.ui.ConvertScreen
import com.kylecorry.trail_sense.tools.experimentation.ExperimentationContent
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.flashlight.ui.FlashlightController
import com.kylecorry.trail_sense.tools.flashlight.ui.FlashlightScreen
import com.kylecorry.trail_sense.tools.flashlight.ui.ScreenFlashlightController
import com.kylecorry.trail_sense.tools.flashlight.ui.ScreenFlashlightScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.CreateFieldGuideSightingToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.CreateFieldGuidePageToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuideToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuidePageToolScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.SightingListToolScreen
import com.kylecorry.trail_sense.tools.diagnostics.ui.DiagnosticAlerter
import com.kylecorry.trail_sense.tools.diagnostics.ui.DiagnosticsScreen
import com.kylecorry.trail_sense.tools.diagnostics.ui.DownloadDiagnosticsLogsCommand
import com.kylecorry.trail_sense.tools.declination.ui.DeclinationScreen
import com.kylecorry.trail_sense.tools.guide.infrastructure.Guides
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.guide.ui.GuideListScreen
import com.kylecorry.trail_sense.tools.guide.ui.GuideScreen
import com.kylecorry.trail_sense.tools.level.ui.LevelScreen
import com.kylecorry.trail_sense.tools.light.ui.LightMeterScreen
import com.kylecorry.trail_sense.tools.lightning.ui.LightningScreen
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.map.ui.MapControllerHost
import com.kylecorry.trail_sense.tools.map.ui.MapToolScreen
import com.kylecorry.trail_sense.tools.offline_maps.OfflineMapsToolRegistration
import com.kylecorry.trail_sense.tools.offline_maps.ui.OfflineMapListToolScreen
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.PhotoMapToolLayerManager
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.PhotoMapsToolScreen
import com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.ViewVectorMapToolScreen
import com.kylecorry.trail_sense.tools.magnifier.ui.MagnifierScreen
import com.kylecorry.trail_sense.tools.metaldetector.ui.MetalDetectorToolScreen
import com.kylecorry.trail_sense.tools.mirror.ui.MirrorCameraScreen
import com.kylecorry.trail_sense.tools.navigation.ui.NavigationSettingsToolScreen
import com.kylecorry.trail_sense.tools.navigation.ui.NavigatorToolScreen
import com.kylecorry.trail_sense.tools.notes.domain.Note
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import com.kylecorry.trail_sense.tools.notes.ui.NoteEditorScreen
import com.kylecorry.trail_sense.tools.notes.ui.NotesScreen
import com.kylecorry.trail_sense.tools.packs.ui.CreateItemToolScreen
import com.kylecorry.trail_sense.tools.packs.ui.PackItemListToolScreen
import com.kylecorry.trail_sense.tools.packs.ui.PackListToolScreen
import com.kylecorry.trail_sense.tools.paths.ui.PathOverviewToolScreen
import com.kylecorry.trail_sense.tools.paths.ui.PathsToolScreen
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.kylecorry.trail_sense.tools.pedometer.ui.PedometerSettingsToolScreen
import com.kylecorry.trail_sense.tools.pedometer.ui.PedometerScreen
import com.kylecorry.trail_sense.tools.pedometer.ui.StrideLengthEstimationToolScreen
import com.kylecorry.trail_sense.tools.qr.infrastructure.NoteQREncoder
import com.kylecorry.trail_sense.tools.qr.ui.QRShareScreen
import com.kylecorry.trail_sense.tools.qr.ui.ScanQRToolScreen
import com.kylecorry.trail_sense.tools.ruler.ui.RulerScreen
import com.kylecorry.trail_sense.tools.signal_finder.ui.SignalFinderToolScreen
import com.kylecorry.trail_sense.tools.solarpanel.ui.SolarPanelScreen
import com.kylecorry.trail_sense.tools.sensors.ui.SensorDetailsToolScreen
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapter
import com.kylecorry.trail_sense.tools.tides.ui.CreateTideToolScreen
import com.kylecorry.trail_sense.tools.tides.ui.TideListToolScreen
import com.kylecorry.trail_sense.tools.tides.ui.TidesToolScreen
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.GuideDetails
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.GuideLoader
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearch
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearchResult
import com.kylecorry.trail_sense.tools.survival_guide.ui.SurvivalGuideListItem
import com.kylecorry.trail_sense.tools.survival_guide.ui.SurvivalGuideListScreen
import com.kylecorry.trail_sense.tools.survival_guide.ui.SurvivalGuideReaderScreen
import com.kylecorry.trail_sense.tools.temperature_estimation.ui.TemperatureEstimationToolScreen
import com.kylecorry.trail_sense.tools.tools.ui.PinnedToolManager
import com.kylecorry.trail_sense.tools.tools.ui.ToolSearchFocusable
import com.kylecorry.trail_sense.tools.tools.ui.ToolsScreen
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.sort.AlphabeticalToolSort
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetTheme
import com.kylecorry.trail_sense.tools.turn_back.ui.TurnBackToolScreen
import com.kylecorry.trail_sense.tools.triangulate.ui.TriangulateToolScreen
import com.kylecorry.trail_sense.tools.waterpurification.domain.WaterService
import com.kylecorry.trail_sense.tools.waterpurification.ui.TimeSelection
import com.kylecorry.trail_sense.tools.waterpurification.ui.WaterPurificationScreen
import com.kylecorry.trail_sense.tools.weather.ui.WeatherSettingsToolScreen
import com.kylecorry.trail_sense.tools.weather.ui.WeatherToolScreen
import com.kylecorry.trail_sense.tools.whitenoise.ui.WhiteNoiseScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

class TrailSenseComposeDestinationHost(
    private val context: Context,
    private val activity: MainActivity,
    private val rootView: View,
    private val arguments: Bundle?,
    override val lifecycleOwner: LifecycleOwner,
    private val requestRuntimePermissions: (Array<String>, () -> Unit) -> Unit
) : ToolSearchFocusable,
    ScreenFlashlightController, FlashlightController, ClinometerLockController,
    BeaconListControllerHost, TrailSenseReactiveDestinationHost {

    private var layerSheet by mutableStateOf<MapLayersSheetState?>(null)
    private var actionSheet by mutableStateOf<ActionSheetState?>(null)
    private var photoImportDialog by mutableStateOf<PhotoImportDialog?>(null)
    private val pinnedToolManager by lazy { PinnedToolManager(UserPreferences(requireContext())) }
    private val screenFlashlight by lazy { ScreenTorch(requireActivity().window) }
    private val screenFlashlightCache by lazy {
        PreferencesSubsystem.getInstance(requireContext()).preferences
    }
    private val notesRepo by lazy { NoteRepo.getInstance(requireContext()) }
    private val flashlight by lazy { FlashlightSubsystem.getInstance(requireContext()) }
    private val haptics by lazy { HapticSubsystem.getInstance(requireContext()) }
    private var focusSearchRequest: (() -> Boolean)? = null
    private var screenFlashlightBrightness by mutableFloatStateOf(100f)
    private var screenFlashlightIsRed by mutableStateOf(false)
    private var editingNote: Note? = null
    private var noteTitle = ""
    private var noteContent = ""
    private var survivalGuideContent by mutableStateOf<String?>(null)
    private var survivalGuideQuery by mutableStateOf("")
    private var survivalGuideChapters by mutableStateOf(emptyList<GuideDetails>())
    private var survivalGuideSearchResults by mutableStateOf(emptyList<SurvivalGuideSearchResult>())
    private var survivalGuideSummary by mutableStateOf("")
    private var survivalGuideSearchJob: Job? = null
    private var selectedFlashlightMode = FlashlightMode.Torch
    internal var clinometerLockController: ClinometerLockController? = null
    internal var beaconListController: BeaconListControllerHost? = null
    internal var mapController: MapControllerHost? = null
    private var resumeCount = 0
    private var mapUpdatesEnabled = true
    private var reactiveUpdateScheduled = false
    private var specialPermissionAction: (() -> Unit)? = null
    internal val reactiveComponent = TrailSenseReactiveComponent(
        contextProvider = ::requireContext,
        activityProvider = ::requireActivity,
        rootViewProvider = ::requireView,
        lifecycleOwnerProvider = { lifecycleOwner },
        viewLifecycleOwnerProvider = { lifecycleOwner },
        argumentsProvider = ::requireArguments,
        onStateChanged = ::onReactiveUpdate
    )

    fun requireActivity(): MainActivity {
        return activity
    }

    fun requireView(): View {
        return rootView
    }

    fun requireArguments(): Bundle {
        return arguments ?: Bundle.EMPTY
    }

    override val lifecycleScope: CoroutineScope
        get() = lifecycleOwner.lifecycleScope

    override val mainActivity: MainActivity
        get() = activity

    override val intentResultRetriever: IntentResultRetriever
        get() = activity

    override val destinationArguments: Bundle?
        get() = arguments

    override fun requireContext(): Context {
        return context
    }

    override fun getResources(): android.content.res.Resources {
        return context.resources
    }

    override fun getString(resId: Int): String {
        return context.getString(resId)
    }

    override fun getString(resId: Int, vararg formatArgs: Any?): String {
        return context.getString(resId, *formatArgs)
    }

    override fun startActivity(intent: Intent) {
        context.startActivity(intent)
    }

    override val hookResetOnResume: Int
        get() = resumeCount

    override fun cleanupReactiveEffects() {
        reactiveComponent.cleanupEffects()
    }

    override fun showActionSheet(
        title: String,
        subtitle: String?,
        actions: List<ActionItem>,
        onAction: (ActionItem?) -> Unit
    ) {
        actionSheet = ActionSheetState(title, subtitle, actions, onAction)
    }

    override fun showSnackbar(
        text: String,
        duration: Int,
        action: String?,
        onAction: () -> Unit
    ): Snackbar {
        val playBar = rootView.findViewWithTag<View>(getString(R.string.tag_play_bar))
        return Snackbar.make(playBar ?: requireView(), text, duration).apply {
            if (action != null) {
                setAction(action) { onAction() }
            }
            show()
        }
    }

    override fun showPhotoImport(
        resolution: android.util.Size?,
        useZeroShutterLag: Boolean,
        onCapture: (android.net.Uri?) -> Unit
    ) {
        photoImportDialog = PhotoImportDialog(
            requireContext(),
            lifecycleOwner,
            lifecycleScope,
            resolution,
            useZeroShutterLag
        ) {
            photoImportDialog = null
            onCapture(it)
        }.also {
            it.show()
        }
    }

    override fun requestCamera(action: (hasPermission: Boolean) -> Unit) {
        requestPermissions(listOf(Manifest.permission.CAMERA)) {
            action(Camera.isAvailable(requireContext()))
        }
    }

    override fun requestActivityRecognition(action: (hasPermission: Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(listOf(Manifest.permission.ACTIVITY_RECOGNITION)) {
                action(Permissions.canRecognizeActivity(requireContext()))
            }
        } else {
            action(true)
        }
    }

    override fun requestScheduleExactAlarms(action: (hasPermission: Boolean) -> Unit) {
        requestPermission(
            SpecialPermission.SCHEDULE_EXACT_ALARMS,
            PermissionRationale(
                getString(R.string.allow_schedule_exact_alarms, getString(R.string.app_name)),
                DependencyRegistry.get<MarkdownService>().toMarkdown(
                    getString(
                        R.string.allow_schedule_exact_alarms_instructions,
                        getString(R.string.app_name),
                        getString(R.string.settings)
                    )
                ),
                ok = getString(R.string.settings),
            )
        ) {
            val isGranted = Permissions.hasPermission(
                requireContext(),
                SpecialPermission.SCHEDULE_EXACT_ALARMS
            )
            if (!isGranted) {
                Alerts.toast(
                    requireContext(),
                    getString(R.string.exact_alarm_permission_denied),
                    short = false
                )
            }
            action(isGranted)
        }
    }

    override fun requestBacktrackPermission(action: (hasPermission: Boolean) -> Unit) {
        if (Permissions.canStartLocationForgroundService(requireContext())) {
            action(true)
            return
        }

        requestPermissions(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            val hasPermission = Permissions.canStartLocationForgroundService(requireContext())
            if (!hasPermission) {
                Alerts.toast(requireContext(), getString(R.string.backtrack_no_permission))
            }
            action(hasPermission)
        }
    }

    override fun requestBackgroundLocation(action: (hasPermission: Boolean) -> Unit) {
        requestPermissions(listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            action(Permissions.isBackgroundLocationEnabled(requireContext()))
        }
    }

    override fun requestIgnoreBatteryOptimizations(action: (hasPermission: Boolean) -> Unit) {
        requestPermission(
            SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS,
            PermissionRationale(
                getString(
                    R.string.allow_ignore_battery_restrictions,
                    getString(R.string.app_name)
                ),
                DependencyRegistry.get<MarkdownService>().toMarkdown(
                    getString(
                        R.string.allow_ignore_battery_restrictions_instructions,
                        getString(R.string.settings)
                    )
                ),
                ok = getString(R.string.settings),
            )
        ) {
            val isGranted = Permissions.hasPermission(
                requireContext(),
                SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS
            )
            if (!isGranted) {
                toast(getString(R.string.battery_usage_restricted), short = false)
            }
            action(isGranted)
        }
    }

    override fun findNavController(): NavController {
        return requireMainActivity().getRootNavController()
            ?: error("Root NavController is not available")
    }

    override fun toast(message: CharSequence, short: Boolean) {
        Alerts.toast(requireContext(), message, short)
    }

    override fun inBackground(
        state: BackgroundMinimumState,
        cancelWhenRerun: Boolean,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return lifecycleOwner.inBackground(state, true, cancelWhenRerun, block)
    }

    override fun alertNoActivityRecognitionPermission() {
        Alerts.toast(
            requireContext(),
            getString(R.string.activity_recognition_permission_denied),
            short = false
        )
    }

    override fun alertNoCameraPermission() {
        Alerts.toast(
            requireContext(),
            getString(R.string.camera_permission_denied),
            short = false
        )
    }

    override fun openWidgets() {
        requireMainActivity().openWidgets()
    }

    override fun reloadTheme() {
        requireMainActivity().reloadTheme()
    }

    private fun requestPermissions(permissions: List<String>, action: () -> Unit) {
        val missingPermissions = permissions.filter {
            !Permissions.hasPermission(requireContext(), it)
        }
        if (missingPermissions.isEmpty()) {
            action()
            return
        }

        requestRuntimePermissions(missingPermissions.toTypedArray(), action)
    }

    private fun requestPermission(
        permission: SpecialPermission,
        rationale: PermissionRationale,
        action: () -> Unit
    ) {
        if (Permissions.hasPermission(requireContext(), permission)) {
            action()
            return
        }

        Alerts.dialog(
            requireContext(),
            rationale.title,
            rationale.message,
            okText = rationale.ok ?: getString(android.R.string.ok),
            cancelText = rationale.cancel ?: getString(android.R.string.cancel)
        ) { cancelled ->
            if (!cancelled) {
                specialPermissionAction = action
                Permissions.requestPermission(requireContext(), permission)
            } else {
                action()
            }
        }
    }

    fun onStart() {
        requireMainActivity().registerDestinationHost(this)
        when (requireArguments().getString(ARG_DESTINATION)) {
            DESTINATION_LOCAL_MESSAGING -> openApp(
                requireContext(),
                "com.kylecorry.trail_sense_comms",
                "4c285dfe-1c8b-45eb-bb79-3f1d2eb6ae48"
            )
            DESTINATION_LOCAL_TALK -> openApp(
                requireContext(),
                "com.kylecorry.trail_sense_comms",
                "b76f32bf-6a72-4992-a741-0c9bf19ebd11"
            )
            DESTINATION_CLIFF_HEIGHT -> CustomUiUtils.disclaimer(
                requireContext(),
                getString(R.string.disclaimer_message_title),
                getString(R.string.tool_cliff_height_disclaimer),
                "cache_dialog_tool_cliff_height",
                considerShownIfCancelled = true,
                cancelText = null
            )
            DESTINATION_SCREEN_FLASHLIGHT -> initializeScreenFlashlight()
            DESTINATION_NOTE_EDITOR -> promptIfUnsavedChanges(::hasNoteChanges)
            DESTINATION_SURVIVAL_GUIDE_READER -> loadSurvivalGuideChapter()
            DESTINATION_SURVIVAL_GUIDE_LIST -> initializeSurvivalGuideList()
        }
    }

    fun onStop() {
        survivalGuideSearchJob?.cancel()
        requireMainActivity().unregisterDestinationHost(this)
    }

    fun onResume() {
        resumeCount++
        mapUpdatesEnabled = true
        specialPermissionAction?.invoke()
        specialPermissionAction = null
        if (requireArguments().getString(ARG_DESTINATION) == DESTINATION_SCREEN_FLASHLIGHT) {
            turnOnScreenFlashlight()
        }
        if (requireArguments().getString(ARG_DESTINATION) == DESTINATION_MAP) {
            onReactiveUpdate()
        }
    }

    fun onPause() {
        mapUpdatesEnabled = false
        if (requireArguments().getString(ARG_DESTINATION) == DESTINATION_SCREEN_FLASHLIGHT) {
            turnOffScreenFlashlight()
        }
        if (requireArguments().getString(ARG_DESTINATION) == DESTINATION_MAP) {
            mapController?.onPause()
        }
        layerSheet?.dismissAndUpdateWidgets(requireContext())
    }

    internal fun onReactiveUpdate() {
        if (
            !mapUpdatesEnabled ||
            reactiveUpdateScheduled ||
            requireArguments().getString(ARG_DESTINATION) != DESTINATION_MAP
        ) {
            return
        }

        reactiveUpdateScheduled = true
        rootView.post {
            reactiveUpdateScheduled = false
            if (!mapUpdatesEnabled) {
                return@post
            }
            reactiveComponent.resetHookCursor()
            mapController?.onUpdate()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(modifier: Modifier = Modifier) {
        val destinationHost = this
        Box(modifier.fillMaxSize()) {
            DestinationContent(requireArguments().getString(ARG_DESTINATION), destinationHost, modifier)
            layerSheet?.Content(
                requireContext(),
                { findNavController().navigateWithAnimation(it) },
                Modifier.fillMaxSize()
            )
            photoImportDialog?.Content()
            val currentActionSheet = actionSheet
            if (currentActionSheet != null) {
                ModalBottomSheet(onDismissRequest = { dismissActionSheet(null) }) {
                    ActionSheetContent(
                        title = currentActionSheet.title,
                        subtitle = currentActionSheet.subtitle,
                        actions = currentActionSheet.actions,
                        onAction = ::dismissActionSheet,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private fun dismissActionSheet(action: ActionItem?) {
        val current = actionSheet ?: return
        actionSheet = null
        current.onAction(action)
    }

    override fun focusSearch(): Boolean {
        return focusSearchRequest?.invoke() == true
    }

    override fun handleVolumeButtonPress(isVolumeUp: Boolean) {
        if (isVolumeUp) {
            increaseScreenFlashlightBrightness()
        } else {
            decreaseScreenFlashlightBrightness()
        }
    }

    override fun toggle() {
        haptics.click()
        if (flashlight.getMode() != FlashlightMode.Off) {
            flashlight.set(FlashlightMode.Off)
        } else {
            flashlight.set(selectedFlashlightMode)
        }
    }

    override fun onTouchDown() {
        clinometerLockController?.onTouchDown()
    }

    override fun onTouchUp() {
        clinometerLockController?.onTouchUp()
    }

    override fun refreshBeaconList() {
        beaconListController?.refreshBeaconList()
    }

    override fun getBottomNavigationHeight(): Int {
        return requireMainActivity().getBottomNavigationHeight()
    }

    override fun addOverlayView(view: View) {
        requireMainActivity().addOverlayView(view)
    }

    override fun removeOverlayView(view: View) {
        requireMainActivity().removeOverlayView(view)
    }

    @Composable
    internal fun ScreenFlashlight() {
        ScreenFlashlightScreen(
            brightness = screenFlashlightBrightness,
            isRedLight = screenFlashlightIsRed,
            onBrightnessChange = ::setScreenFlashlightBrightness,
            onToggleColor = ::toggleScreenFlashlightColor,
            onTurnOff = {
                turnOffScreenFlashlight()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        )
    }

    @Composable
    internal fun Notes() {
        NotesScreen(
            notes = notesRepo.getNotes(),
            lifecycleOwner = lifecycleOwner,
            onAdd = {
                findNavController().navigate(R.id.action_fragmentToolNotes_to_fragmentToolNotesCreate)
            },
            onEdit = ::editNote,
            onDelete = ::deleteNote,
            onShowQR = ::showNoteQR
        )
    }

    @Composable
    internal fun NoteEditor() {
        NoteEditorScreen(
            noteId = requireArguments().getLong("edit_note_id"),
            loadNote = { notesRepo.getNote(it) },
            onStateChanged = { newTitle, newContent, note ->
                noteTitle = newTitle
                noteContent = newContent
                editingNote = note
            },
            onSave = { newTitle, newContent ->
                saveNote(newTitle, newContent)
            }
        )
    }

    @Composable
    internal fun SurvivalGuideReader() {
        val chapter = getSurvivalGuideChapter()
        SurvivalGuideReaderScreen(
            chapter = chapter,
            content = survivalGuideContent,
            headerIndex = requireArguments().getInt("header_index", 0),
            canPrint = isDebug(),
            onPrint = ::printSurvivalGuideChapter
        )
    }

    @Composable
    internal fun SurvivalGuideList() {
        SurvivalGuideListScreen(
            query = survivalGuideQuery,
            listItems = getSurvivalGuideListItems(),
            summary = survivalGuideSummary,
            firstResult = survivalGuideSearchResults.firstOrNull(),
            markdown = DependencyRegistry.get(),
            onQueryChange = ::updateSurvivalGuideQuery,
            onSummaryClick = ::openSurvivalGuideSearchResult
        )
    }

    @Composable
    internal fun Pedometer() {
        val pedometer = remember { PedometerSubsystem.getInstance(requireContext()) }
        val prefs = remember { UserPreferences(requireContext()) }
        val formatter = remember { FormatService.getInstance(requireContext()) }
        PedometerScreen(
            pedometer = pedometer,
            prefs = prefs,
            formatService = formatter,
            onReset = ::resetPedometer,
            onTogglePedometer = { togglePedometer(pedometer) },
            onToggleDistanceAlert = { togglePedometerDistanceAlert(prefs, formatter) }
        )
    }

    @Composable
    internal fun Flashlight() {
        FlashlightScreen(
            initialSelectedMode = selectedFlashlightMode,
            onSelectedModeChanged = { selectedFlashlightMode = it },
            onOpenScreenFlashlight = {
                findNavController().navigate(R.id.action_flashlight_to_screen_flashlight)
            }
        )
    }

    @Composable
    internal fun BatteryTool() {
        BatteryToolScreen(
            onShowHistory = ::showBatteryHistory,
            onOpenPowerSettings = {
                findNavController().navigate(R.id.action_settings_to_power_settings)
            }
        )
    }

    @Composable
    internal fun ToolsHome() {
        ToolsScreen(
            pinnedToolIds = pinnedToolManager.pinnedIds.value,
            onToolClick = { tool ->
                findNavController().navigateWithAnimation(tool.navAction)
            },
            onToolLongClick = { tool ->
                showToolActions(tool)
            },
            onSettingsClick = {
                findNavController().navigate(R.id.action_settings)
            },
            onSearchFocusRequestChanged = {
                focusSearchRequest = it
            }
        )
    }

    @Composable
    internal fun UnitSettings() {
        val cache = remember { PreferencesSubsystem.getInstance(requireContext()).preferences }
        val userPrefs = remember { UserPreferences(requireContext()) }
        val hasBarometer = remember { Sensors.hasBarometer(requireContext()) }
        val distanceOptions = remember {
            settingsOptions(R.array.distance_unit_entries, R.array.distance_unit_values)
        }
        val pressureOptions = remember {
            settingsOptions(R.array.pressure_unit_entries, R.array.pressure_unit_values)
        }
        val temperatureOptions = remember {
            settingsOptions(R.array.temperature_unit_entries, R.array.temperature_unit_values)
        }
        val weightOptions = remember {
            settingsOptions(R.array.weight_entries, R.array.weight_values)
        }
        val coordinateOptions = remember {
            settingsOptions(R.array.coordinate_format_entries, R.array.coordinate_format_values)
        }
        val mapOptions = remember {
            settingsOptions(R.array.map_url_entries, R.array.map_url_values)
        }

        var distanceUnits by remember {
            mutableStateOf(distanceUnitsValue(userPrefs.distanceUnits))
        }
        var pressureUnits by remember {
            mutableStateOf(pressureUnitsValue(userPrefs.pressureUnits))
        }
        var temperatureUnits by remember {
            mutableStateOf(temperatureUnitsValue(userPrefs.temperatureUnits))
        }
        var weightUnits by remember {
            mutableStateOf(weightUnitsValue(userPrefs.weightUnits))
        }
        var coordinateFormat by remember {
            mutableStateOf(coordinateFormatValue(userPrefs.coordinateFormat))
        }
        var mapSite by remember {
            mutableStateOf(mapSiteValue(userPrefs.mapSite))
        }
        var use24HourTime by remember { mutableStateOf(userPrefs.use24HourTime) }
        var includeLeadingZero by remember {
            mutableStateOf(userPrefs.addLeadingZeroToTime)
        }

        val measurementsTitle = stringResource(R.string.settings_section_measurements)
        val locationTitle = stringResource(R.string.location)
        val timeTitle = stringResource(R.string.time)
        val timeExample = stringResource(
            R.string.settings_time_format_example,
            exampleTime(use24HourTime, includeLeadingZero)
        )

        SettingsList(title = stringResource(R.string.units)) {
            trailSenseSection("measurements", title = measurementsTitle) {
                row { position ->
                    UnitChoiceRow(
                        label = stringResource(R.string.distance),
                        options = distanceOptions,
                        selected = distanceUnits,
                        onSelected = {
                            distanceUnits = it
                            putString(cache, R.string.pref_distance_units, it)
                        },
                        position = position
                    )
                }
                if (hasBarometer) {
                    row { position ->
                        UnitPickerRow(
                            label = stringResource(R.string.pressure),
                            options = pressureOptions,
                            selected = pressureUnits,
                            onSelected = {
                                pressureUnits = it
                                putString(cache, R.string.pref_pressure_units, it)
                            },
                            position = position
                        )
                    }
                }
                row { position ->
                    UnitChoiceRow(
                        label = stringResource(R.string.temperature),
                        options = temperatureOptions,
                        selected = temperatureUnits,
                        onSelected = {
                            temperatureUnits = it
                            putString(cache, R.string.pref_temperature_units, it)
                        },
                        position = position
                    )
                }
                row { position ->
                    UnitChoiceRow(
                        label = stringResource(R.string.weight),
                        options = weightOptions,
                        selected = weightUnits,
                        onSelected = {
                            weightUnits = it
                            putString(cache, R.string.pref_weight_units, it)
                        },
                        position = position
                    )
                }
            }
            trailSenseSection("location", title = locationTitle) {
                row { position ->
                    UnitPickerRow(
                        label = stringResource(R.string.pref_coordinate_format_title),
                        options = coordinateOptions,
                        selected = coordinateFormat,
                        onSelected = {
                            coordinateFormat = it
                            putString(cache, R.string.pref_coordinate_format, it)
                        },
                        position = position
                    )
                }
                row { position ->
                    UnitPickerRow(
                        label = stringResource(R.string.pref_map_url_source_title),
                        options = mapOptions,
                        selected = mapSite,
                        onSelected = {
                            mapSite = it
                            putString(cache, R.string.pref_map_url_source, it)
                        },
                        position = position
                    )
                }
            }
            trailSenseSection("time", title = timeTitle, footer = timeExample) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_use_24_hour_title),
                        checked = use24HourTime,
                        onCheckedChange = {
                            use24HourTime = it
                            putBoolean(cache, R.string.pref_use_24_hour, it)
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_include_leading_zero_title),
                        checked = includeLeadingZero,
                        onCheckedChange = {
                            includeLeadingZero = it
                            putBoolean(cache, R.string.pref_include_leading_zero, it)
                        },
                        position = position
                    )
                }
            }
        }
    }

    @Composable
    internal fun AugmentedRealitySettings() {
        val userPrefs = remember { UserPreferences(requireContext()) }
        val formatter = remember { FormatService.getInstance(requireContext()) }
        val distanceUnits = remember {
            formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
        }
        val arPrefs = userPrefs.augmentedReality
        val hasGyro = remember { Sensors.hasGyroscope(requireContext()) }
        val pathDistanceDescription = remember {
            getString(
                R.string.path_distance_performance_disclaimer,
                formatter.formatDistance(
                    relativeDistance(
                        userPrefs,
                        Distance.meters(arPrefs.maxRecommendedPathViewDistanceMeters)
                    )
                )
            )
        }

        var showBeaconLayer by remember { mutableStateOf(arPrefs.showBeaconLayer) }
        var showPathLayer by remember { mutableStateOf(arPrefs.showPathLayer) }
        var showAstronomyLayer by remember { mutableStateOf(arPrefs.showAstronomyLayer) }
        var showStars by remember { mutableStateOf(arPrefs.showStars) }
        var showLowBrightnessObjects by remember {
            mutableStateOf(arPrefs.showLowBrightnessObjects)
        }
        var showConstellations by remember { mutableStateOf(arPrefs.showConstellations) }
        var showAstronomyBelowHorizon by remember {
            mutableStateOf(arPrefs.showAstronomyBelowHorizon)
        }
        var showGridLayer by remember { mutableStateOf(arPrefs.showGridLayer) }
        var useGyroOnlyAfterCalibration by remember {
            mutableStateOf(arPrefs.useGyroOnlyAfterCalibration)
        }
        var increaseExposureAtNight by remember {
            mutableStateOf(arPrefs.increaseExposureAtNight)
        }
        var adjustForPathElevation by remember {
            mutableStateOf(arPrefs.adjustForPathElevation)
        }
        var beaconViewDistance by remember {
            mutableStateOf(relativeDistance(userPrefs, Distance.meters(arPrefs.beaconViewDistance)))
        }
        var pathViewDistance by remember {
            mutableStateOf(relativeDistance(userPrefs, Distance.meters(arPrefs.pathViewDistance)))
        }

        val beaconsTitle = stringResource(R.string.beacons)
        val pathsTitle = stringResource(R.string.paths)
        val astronomyTitle = stringResource(R.string.astronomy)
        val gridTitle = stringResource(R.string.grid)
        val advancedTitle = stringResource(R.string.advanced)

        // Each layer's own options only show while the layer does.
        SettingsList(title = stringResource(R.string.augmented_reality)) {
            trailSenseSection("beacons", title = beaconsTitle) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.visible),
                        checked = showBeaconLayer,
                        onCheckedChange = {
                            showBeaconLayer = it
                            arPrefs.showBeaconLayer = it
                        },
                        position = position
                    )
                }
                if (showBeaconLayer) {
                    row { position ->
                        SettingsActionRow(
                            label = stringResource(R.string.view_distance),
                            summary = formatDistance(formatter, beaconViewDistance),
                            onClick = {
                                pickDistance(
                                    distanceUnits,
                                    title = getString(R.string.view_distance),
                                    current = beaconViewDistance
                                ) {
                                    beaconViewDistance = it
                                    arPrefs.beaconViewDistance = it.meters().value
                                }
                            },
                            position = position
                        )
                    }
                }
            }
            trailSenseSection("paths", title = pathsTitle) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.visible),
                        checked = showPathLayer,
                        onCheckedChange = {
                            showPathLayer = it
                            arPrefs.showPathLayer = it
                        },
                        position = position
                    )
                }
                if (showPathLayer) {
                    row { position ->
                        SettingsActionRow(
                            label = stringResource(R.string.view_distance),
                            summary = formatDistance(formatter, pathViewDistance),
                            onClick = {
                                pickDistance(
                                    distanceUnits,
                                    title = getString(R.string.view_distance),
                                    current = pathViewDistance,
                                    description = pathDistanceDescription
                                ) {
                                    pathViewDistance = it
                                    arPrefs.pathViewDistance = it.meters().value
                                }
                            },
                            position = position
                        )
                    }
                    row { position ->
                        SettingsSwitchRow(
                            label = stringResource(R.string.use_path_elevation),
                            checked = adjustForPathElevation,
                            onCheckedChange = {
                                adjustForPathElevation = it
                                arPrefs.adjustForPathElevation = it
                            },
                            position = position
                        )
                    }
                }
            }
            trailSenseSection("astronomy", title = astronomyTitle) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.visible),
                        checked = showAstronomyLayer,
                        onCheckedChange = {
                            showAstronomyLayer = it
                            arPrefs.showAstronomyLayer = it
                        },
                        position = position
                    )
                }
                if (showAstronomyLayer) {
                    row { position ->
                        SettingsSwitchRow(
                            label = stringResource(R.string.show_stars),
                            checked = showStars,
                            onCheckedChange = {
                                showStars = it
                                arPrefs.showStars = it
                            },
                            position = position
                        )
                    }
                    if (showStars) {
                        row { position ->
                            SettingsSwitchRow(
                                label = stringResource(R.string.show_low_brightness_objects),
                                checked = showLowBrightnessObjects,
                                onCheckedChange = {
                                    showLowBrightnessObjects = it
                                    arPrefs.showLowBrightnessObjects = it
                                },
                                position = position
                            )
                        }
                    }
                    row { position ->
                        SettingsSwitchRow(
                            label = stringResource(R.string.show_constellations),
                            checked = showConstellations,
                            onCheckedChange = {
                                showConstellations = it
                                arPrefs.showConstellations = it
                            },
                            position = position
                        )
                    }
                    row { position ->
                        SettingsSwitchRow(
                            label = stringResource(R.string.show_below_horizon),
                            checked = showAstronomyBelowHorizon,
                            onCheckedChange = {
                                showAstronomyBelowHorizon = it
                                arPrefs.showAstronomyBelowHorizon = it
                            },
                            position = position
                        )
                    }
                }
            }
            trailSenseSection("grid", title = gridTitle) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.visible),
                        checked = showGridLayer,
                        onCheckedChange = {
                            showGridLayer = it
                            arPrefs.showGridLayer = it
                        },
                        position = position
                    )
                }
            }
            trailSenseSection("advanced", title = advancedTitle) {
                if (hasGyro) {
                    row { position ->
                        SettingsSwitchRow(
                            label = stringResource(R.string.use_gyroscope_after_calibration),
                            summary = stringResource(R.string.use_gyroscope_after_calibration_description),
                            checked = useGyroOnlyAfterCalibration,
                            onCheckedChange = {
                                useGyroOnlyAfterCalibration = it
                                arPrefs.useGyroOnlyAfterCalibration = it
                            },
                            position = position
                        )
                    }
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.increase_exposure_at_night),
                        checked = increaseExposureAtNight,
                        onCheckedChange = {
                            increaseExposureAtNight = it
                            arPrefs.increaseExposureAtNight = it
                        },
                        position = position
                    )
                }
            }
        }
    }

    @Composable
    internal fun ThemeSettings() {
        val cache = remember { PreferencesSubsystem.getInstance(requireContext()).preferences }
        val prefs = remember { UserPreferences(requireContext()) }
        val widgetThemeOptions = remember { widgetThemeOptions() }
        var widgetTheme by remember { mutableStateOf(getDefaultWidgetTheme(cache)) }

        ThemeSettingsScreen(
            prefs = prefs,
            wallpaperAvailable = remember { DynamicColors.isDynamicColorAvailable() },
            widgetTheme = optionLabel(widgetThemeOptions, widgetTheme),
            nightFullscreen = getBoolean(cache, R.string.pref_night_mode_fullscreen, false),
            compassWallpaperColors = getBoolean(cache, R.string.pref_use_dynamic_colors_on_compass, false),
            onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
            onThemeChanged = {
                prefs.theme = it
                requireMainActivity().reloadTheme()
            },
            onPaletteChanged = { ThemeChoices.changed() },
            onCompassWallpaperColorsChanged = {
                putBoolean(cache, R.string.pref_use_dynamic_colors_on_compass, it)
            },
            onPickWidgetTheme = {
                pickOption(getString(R.string.default_widget_theme), widgetThemeOptions, widgetTheme) {
                    widgetTheme = it
                    putString(cache, R.string.pref_default_widget_theme, it)
                    Tools.triggerWidgetUpdate(requireContext(), null)
                }
            },
            onCompactModeChanged = { requireMainActivity().changeBottomNavLabelsVisibility(it) },
            onShowAllLabelsChanged = { requireMainActivity().showAllBottomNavLabels(it) },
            onNightFullscreenChanged = {
                putBoolean(cache, R.string.pref_night_mode_fullscreen, it)
                requireMainActivity().reloadTheme()
            }
        )
    }

    @Composable
    internal fun ExperimentalSettings() {
        val cache = remember { PreferencesSubsystem.getInstance(requireContext()).preferences }
        val userPrefs = remember { UserPreferences(requireContext()) }
        val hasMetalDirectionSupport = remember {
            Sensors.hasGyroscope(requireContext()) && SensorService(requireContext()).hasCompass()
        }
        val showForecastSource = remember {
            Tools.isToolAvailable(requireContext(), Tools.WEATHER) && isDebug()
        }
        val showNotificationGrouping = remember {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
        }
        val forecastOptions = remember { forecastOptions() }
        val notificationGroupingOptions = remember { notificationGroupingOptions() }

        var showMetalDirection by remember {
            mutableStateOf(getBoolean(cache, R.string.pref_experimental_metal_direction, false))
        }
        var useZeroShutterLag by remember {
            mutableStateOf(userPrefs.camera.useZeroShutterLag)
        }
        var isCliffHeightEnabled by remember {
            mutableStateOf(userPrefs.isCliffHeightEnabled)
        }
        var useFilteredGps by remember {
            mutableStateOf(userPrefs.gps.smoothing > 0)
        }
        var forecastSource by remember {
            mutableStateOf(
                getStringPreference(
                    cache,
                    R.string.pref_weather_forecast_source,
                    forecastSourceValue(ForecastSource.Sol)
                )
            )
        }
        var showPressureForecast by remember {
            mutableStateOf(userPrefs.weather.showPressureForecast)
        }
        var notificationGrouping by remember {
            mutableStateOf(notificationGroupingValue(userPrefs.notificationGroupingBehavior))
        }

        val toolsTitle = stringResource(R.string.tools)
        val cameraTitle = stringResource(R.string.camera)
        val gpsTitle = stringResource(R.string.gps)
        val weatherTitle = stringResource(R.string.weather)
        val notificationsTitle = stringResource(R.string.settings_section_notifications)

        // Each experiment sits under the part of the app it changes, so no row needs to say where it applies.
        SettingsList(title = stringResource(R.string.experimental)) {
            trailSenseSection("tools", title = toolsTitle) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.tool_cliff_height_title),
                        summary = stringResource(R.string.tool_cliff_height_description),
                        checked = isCliffHeightEnabled,
                        onCheckedChange = {
                            isCliffHeightEnabled = it
                            userPrefs.isCliffHeightEnabled = it
                            requireMainActivity().updateBottomNavigation()
                        },
                        position = position
                    )
                }
                if (hasMetalDirectionSupport) {
                    row { position ->
                        SettingsSwitchRow(
                            label = stringResource(R.string.pref_experimental_metal_direction_title),
                            summary = stringResource(R.string.tool_metal_detector_title),
                            checked = showMetalDirection,
                            onCheckedChange = {
                                showMetalDirection = it
                                putBoolean(cache, R.string.pref_experimental_metal_direction, it)
                            },
                            position = position
                        )
                    }
                }
            }
            trailSenseSection("camera", title = cameraTitle) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.zero_shutter_lag),
                        checked = useZeroShutterLag,
                        onCheckedChange = {
                            useZeroShutterLag = it
                            putBoolean(cache, R.string.pref_use_zero_shutter_lag, it)
                        },
                        position = position
                    )
                }
            }
            trailSenseSection("gps", title = gpsTitle) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.smooth_gps_readings),
                        checked = useFilteredGps,
                        onCheckedChange = {
                            useFilteredGps = it
                            cache.putInt(getString(R.string.pref_gps_smoothing), if (it) 50 else 0)
                        },
                        position = position
                    )
                }
            }
            trailSenseSection("weather", title = weatherTitle) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.show_pressure_forecast),
                        checked = showPressureForecast,
                        onCheckedChange = {
                            showPressureForecast = it
                            putBoolean(cache, R.string.pref_show_pressure_forecast, it)
                        },
                        position = position
                    )
                }
                if (showForecastSource) {
                    row { position ->
                        UnitChoiceRow(
                            label = stringResource(R.string.forecast_algorithm),
                            options = forecastOptions,
                            selected = forecastSource,
                            onSelected = {
                                forecastSource = it
                                putString(cache, R.string.pref_weather_forecast_source, it)
                            },
                            position = position
                        )
                    }
                }
            }
            if (showNotificationGrouping) {
                trailSenseSection("notifications", title = notificationsTitle) {
                    row { position ->
                        UnitPickerRow(
                            label = stringResource(R.string.notification_grouping),
                            options = notificationGroupingOptions,
                            selected = notificationGrouping,
                            onSelected = {
                                notificationGrouping = it
                                putString(cache, R.string.pref_notification_group_behavior, it)
                            },
                            position = position
                        )
                    }
                }
            }
        }
    }

    @Composable
    internal fun BackupSettings() {
        val prefs = remember { UserPreferences(requireContext()) }
        var autoBackupEnabled by remember {
            mutableStateOf(prefs.backup.isAutoBackupEnabled)
        }
        var autoBackupSummary by remember {
            mutableStateOf(getAutoBackupSummary(prefs, autoBackupEnabled))
        }
        var includeMapsforge by remember {
            mutableStateOf(prefs.backup.includeMapsforgeMaps)
        }

        SettingsList(title = stringResource(R.string.backup_restore)) {
            trailSenseSection("manual") {
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.backup),
                        onClick = ::backup,
                        position = position
                    )
                }
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.restore),
                        onClick = ::restore,
                        position = position
                    )
                }
            }
            trailSenseSection("options") {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.automatic_backup),
                        summary = autoBackupSummary,
                        checked = autoBackupEnabled,
                        onCheckedChange = {
                            autoBackupEnabled = it
                            prefs.backup.isAutoBackupEnabled = it
                            if (it) {
                                changeAutoBackupDirectory(prefs) { isEnabled ->
                                    autoBackupEnabled = isEnabled
                                    autoBackupSummary = getAutoBackupSummary(prefs, isEnabled)
                                }
                            } else {
                                autoBackupSummary = null
                            }
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.include_mapsforge_backups),
                        summary = stringResource(R.string.include_mapsforge_backups_summary),
                        checked = includeMapsforge,
                        onCheckedChange = {
                            includeMapsforge = it
                            prefs.backup.includeMapsforgeMaps = it
                        },
                        position = position
                    )
                }
            }
        }
    }

    @Composable
    internal fun WaterPurification() {
        val sensorService = remember { SensorService(requireContext()) }
        val formatService = remember { FormatService.getInstance(requireContext()) }
        val altimeter = remember { sensorService.getAltimeter() }
        val cache = remember { PreferencesSubsystem.getInstance(requireContext()).preferences }
        val waterService = remember { WaterService() }
        WaterPurificationScreen(
            cache = cache,
            formatService = formatService,
            getSelectedDuration = { selection ->
                onDefault {
                    if (selection == TimeSelection.Auto) {
                        if (!altimeter.hasValidReading) {
                            withTimeoutOrNull(Duration.ofSeconds(10).toMillis()) {
                                altimeter.read()
                            }
                        }

                        if (altimeter.hasValidReading) {
                            return@onDefault waterService.getPurificationTime(
                                Distance.from(altimeter.altitude, DistanceUnits.Meters)
                            )
                        }
                    } else if (selection == TimeSelection.LowAltitude) {
                        return@onDefault Duration.ofMinutes(1)
                    }

                    Duration.ofMinutes(3)
                }
            }
        )
    }

    @Composable
    internal fun PhotoMapSettings() {
        val prefs = remember { UserPreferences(requireContext()).photoMaps }
        var reducePhotoResolution by remember {
            mutableStateOf(prefs.autoReducePhotoMaps)
        }
        var reducePdfResolution by remember {
            mutableStateOf(prefs.autoReducePdfMaps)
        }
        var showMapPreviews by remember { mutableStateOf(prefs.showMapPreviews) }
        var keepMapFacingUp by remember { mutableStateOf(prefs.keepMapFacingUp) }
        var keepUnlocked by remember {
            mutableStateOf(prefs.keepScreenUnlockedWhileOpen)
        }
        var highDetailMode by remember { mutableStateOf(prefs.highDetailMode) }
        val importTitle = stringResource(R.string.on_import)

        SettingsList(title = stringResource(R.string.photo_maps)) {
            trailSenseSection("import", title = importTitle) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.reduce_photo_resolution),
                        checked = reducePhotoResolution,
                        onCheckedChange = {
                            reducePhotoResolution = it
                            prefs.autoReducePhotoMaps = it
                            onReduceMapResolutionChange(reducePdfResolution)
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.reduce_pdf_resolution),
                        checked = reducePdfResolution,
                        onCheckedChange = {
                            reducePdfResolution = it
                            prefs.autoReducePdfMaps = it
                            onReduceMapResolutionChange(reducePhotoResolution)
                        },
                        position = position
                    )
                }
            }
            trailSenseSection("display") {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_show_map_previews_title),
                        checked = showMapPreviews,
                        onCheckedChange = {
                            showMapPreviews = it
                            prefs.showMapPreviews = it
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_keep_map_facing_up_title),
                        summary = stringResource(R.string.pref_keep_map_facing_up_summary),
                        checked = keepMapFacingUp,
                        onCheckedChange = {
                            keepMapFacingUp = it
                            prefs.keepMapFacingUp = it
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_navigation_lock_screen_when_open_title),
                        checked = keepUnlocked,
                        onCheckedChange = {
                            keepUnlocked = it
                            prefs.keepScreenUnlockedWhileOpen = it
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_high_detail_mode_title),
                        summary = stringResource(R.string.pref_high_detail_mode_summary),
                        checked = highDetailMode,
                        onCheckedChange = {
                            highDetailMode = it
                            prefs.highDetailMode = it
                        },
                        position = position
                    )
                }
            }
            trailSenseSection("layers") {
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.layers),
                        onClick = ::showPhotoMapLayerSheet,
                        position = position
                    )
                }
            }
        }
    }

    @Composable
    internal fun MirrorCamera() {
        val flashlight = remember { ScreenTorch(requireActivity().window) }
        MirrorCameraScreen(
            onRequestCamera = ::requestCameraForMirror,
            onResumeScreen = { flashlight.on() },
            onPauseScreen = { flashlight.off() }
        )
    }

    @Composable
    internal fun MapSettings() {
        val prefs = remember { UserPreferences(requireContext()).map }
        var keepUnlocked by remember {
            mutableStateOf(prefs.keepScreenUnlockedWhileOpen)
        }
        var saveMapState by remember { mutableStateOf(prefs.saveMapState) }
        var highDetailMode by remember { mutableStateOf(prefs.highDetailMode) }

        SettingsList(title = stringResource(R.string.maps)) {
            trailSenseSection("display") {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_navigation_lock_screen_when_open_title),
                        checked = keepUnlocked,
                        onCheckedChange = {
                            keepUnlocked = it
                            prefs.keepScreenUnlockedWhileOpen = it
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.remember_map_state),
                        checked = saveMapState,
                        onCheckedChange = {
                            saveMapState = it
                            prefs.saveMapState = it
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_high_detail_mode_title),
                        summary = stringResource(R.string.pref_high_detail_mode_summary),
                        checked = highDetailMode,
                        onCheckedChange = {
                            highDetailMode = it
                            prefs.highDetailMode = it
                        },
                        position = position
                    )
                }
            }
            trailSenseSection("layers") {
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.layers),
                        onClick = ::showMapLayerSheet,
                        position = position
                    )
                }
            }
        }
    }

    @Composable
    internal fun PluginsSettings() {
        val lifecycleOwner = LocalLifecycleOwner.current
        val scope = rememberCoroutineScope()
        val pluginSubsystem = remember { PluginSubsystem.getInstance(requireContext()) }
        var plugins by remember { mutableStateOf<PluginSettingsState?>(null) }

        fun refresh() {
            scope.launch {
                plugins = loadPlugins(pluginSubsystem)
            }
        }

        LaunchedEffect(Unit) {
            plugins = loadPlugins(pluginSubsystem)
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    refresh()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        val connectedTitle = stringResource(R.string.connected_plugins)
        val availableTitle = stringResource(R.string.available_plugins)

        SettingsList(title = stringResource(R.string.plugins)) {
            // Nothing until the first load finishes, so "None" never flashes over plugins that are there.
            val loaded = plugins ?: return@SettingsList
            trailSenseSection("connected", title = connectedTitle) {
                if (loaded.connected.isEmpty()) {
                    row { position ->
                        SettingsInfoRow(label = stringResource(R.string.none), position = position)
                    }
                }
                loaded.connected.forEach { plugin ->
                    row { position ->
                        SettingsActionRow(
                            label = plugin.name,
                            summary = plugin.packageId,
                            onClick = { openPluginDetails(plugin) },
                            position = position
                        )
                    }
                }
            }
            trailSenseSection("available", title = availableTitle) {
                if (loaded.available.isEmpty()) {
                    row { position ->
                        SettingsInfoRow(label = stringResource(R.string.none), position = position)
                    }
                }
                loaded.available.forEach { plugin ->
                    row { position ->
                        SettingsActionRow(
                            label = plugin.name,
                            summary = plugin.packageId,
                            onClick = { confirmConnect(pluginSubsystem, plugin, ::refresh) },
                            position = position
                        )
                    }
                }
            }
        }
    }

    @Composable
    internal fun ToolsSettings() {
        val prefs = remember { UserPreferences(requireContext()) }
        val tools = remember {
            AlphabeticalToolSort().sort(Tools.getTools(requireContext())).first().tools
        }
        var bottomNavSelection by remember {
            mutableStateOf(prefs.bottomNavigationTools)
        }
        val bottomNavigationTitle = stringResource(R.string.bottom_navigation)
        // The last slot can't be changed, so it is said once below the slots instead of drawn as a dead row.
        val toolsSlotFooter = stringResource(
            R.string.settings_bottom_navigation_tools_slot,
            bottomNavSelection.size + 1
        )

        SettingsList(title = stringResource(R.string.tools)) {
            trailSenseSection("shortcuts") {
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.quick_actions),
                        summary = stringResource(R.string.tool_quick_action_summary),
                        onClick = { pickQuickActions(prefs) },
                        position = position
                    )
                }
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.widget_pref_title),
                        summary = stringResource(R.string.widget_bottom_sheet_instructions),
                        onClick = { pickWidgets(prefs) },
                        position = position
                    )
                }
            }
            trailSenseSection(
                "bottom_navigation",
                title = bottomNavigationTitle,
                footer = toolsSlotFooter
            ) {
                bottomNavSelection.indices.forEach { index ->
                    row { position ->
                        val pickerTitle = stringResource(R.string.bottom_navigation_slot, index + 1)
                        SettingsActionRow(
                            label = stringResource(R.string.settings_bottom_navigation_slot, index + 1),
                            summary = getBottomNavToolName(tools, bottomNavSelection.getOrNull(index)),
                            onClick = {
                                pickBottomNavTool(pickerTitle, index, tools, bottomNavSelection, prefs) {
                                    bottomNavSelection = it
                                }
                            },
                            position = position
                        )
                    }
                }
            }
        }
    }

    @Composable
    internal fun GuideList() {
        GuideListScreen(
            guides = Guides.guides(requireContext()),
            onOpenGuide = {
                tryOrNothing {
                    findNavController().navigate(
                        R.id.action_guideListFragment_to_guideFragment,
                        Bundle().apply {
                            putString("guide_name", it.name)
                            putInt("guide_contents", it.contents)
                        }
                    )
                }
            }
        )
    }

    @Composable
    internal fun Diagnostics() {
        DiagnosticsScreen(
            onShowSensorDetails = {
                findNavController().navigate(R.id.action_diagnostics_to_sensor_details)
            },
            onShowDiagnostic = {
                DiagnosticAlerter(requireContext()).alert(it) { action ->
                    action.action(requireContext(), findNavController())
                }
            },
            onExportLogs = {
                DownloadDiagnosticsLogsCommand(this).execute()
            }
        )
    }

    @Composable
    internal fun ErrorHandler() {
        val toolId = requireArguments().getLong("tool_id", 0L)
        val errorDetails = requireArguments().getString("error").orEmpty()
        val navController = findNavController()
        ErrorHandlerScreen(
            toolId = toolId,
            onEmail = {
                startActivity(
                    Intents.email(
                        getString(R.string.email),
                        "Error in ${getString(R.string.app_name)}",
                        errorDetails
                    )
                )
            },
            onCopyError = {
                Clipboard.copy(
                    requireContext(),
                    errorDetails,
                    getString(R.string.copied_to_clipboard_toast)
                )
            },
            onViewDetails = {
                Alerts.dialog(
                    requireContext(),
                    getString(R.string.error_occurred),
                    errorDetails,
                    cancelText = null
                )
            },
            onReopenTool = {
                navController.openTool(toolId)
            },
            onRestartApp = {
                CurrentApp.restart(requireContext())
            },
            onOpenSettings = {
                navController.openTool(Tools.SETTINGS)
            }
        )
    }

    @Composable
    internal fun BubbleLevelSettings() {
        val prefs = remember { DependencyRegistry.get<UserPreferences>().bubbleLevel }
        val formatter = remember { DependencyRegistry.get<FormatService>() }
        var threshold by remember { mutableIntStateOf(prefs.threshold.toInt()) }
        SettingsList(title = stringResource(R.string.tool_bubble_level_title)) {
            trailSenseSection("threshold") {
                row { position ->
                    // Chosen in place: a whole-degree slider with its value beside the label, no dialog.
                    SettingsSliderRow(
                        label = stringResource(R.string.pref_bubble_level_threshold_title),
                        value = threshold.toFloat(),
                        valueRange = 0f..10f,
                        steps = 9,
                        formatValue = { formatter.formatDegrees(it.roundToInt().toFloat()) },
                        onValueChange = {
                            val degrees = it.roundToInt()
                            prefs.threshold = degrees.toFloat()
                            threshold = degrees
                        },
                        position = position
                    )
                }
            }
        }
    }

    @Composable
    internal fun BeaconSettings() {
        val prefs = remember { UserPreferences(requireContext()) }
        var defaultColor by remember {
            mutableStateOf(prefs.beacons.defaultBeaconColor)
        }
        var showDefaultColorPicker by remember { mutableStateOf(false) }
        val defaultColorTitle = stringResource(R.string.pref_default_beacon_color_title)
        SettingsList(title = stringResource(R.string.beacons)) {
            trailSenseSection("color") {
                row { position ->
                    SettingsActionRow(
                        label = defaultColorTitle,
                        onClick = {
                            showDefaultColorPicker = true
                        },
                        trailing = {
                            Box(
                                modifier = Modifier.size(TrailSenseSize.minimumTouchTarget),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            Color(defaultColor.color),
                                            CircleShape
                                        )
                                )
                            }
                        },
                        position = position
                    )
                }
            }
        }

        if (showDefaultColorPicker) {
            ColorPickerDialog(
                title = defaultColorTitle,
                selectedColor = defaultColor,
                onColorPicked = {
                    if (it != null) {
                        prefs.beacons.defaultBeaconColor = it
                        defaultColor = it
                    }
                    showDefaultColorPicker = false
                },
                onDismiss = {
                    showDefaultColorPicker = false
                }
            )
        }
    }

    @Composable
    internal fun CameraSettings() {
        val prefs = remember { UserPreferences(requireContext()).camera }
        val projectionNames = projectionTypeNames()
        val projectionTypes = projectionNames.keys.toList()
        var projectionType by remember { mutableStateOf(prefs.projectionType) }
        val title = stringResource(R.string.projection_type)
        SettingsList(title = stringResource(R.string.camera)) {
            trailSenseSection("projection") {
                row { position ->
                    SettingsPickerRow(
                        label = title,
                        selected = projectionNames.getValue(projectionType),
                        onClick = {
                            Pickers.item(
                                requireContext(),
                                title,
                                projectionNames.values.toList(),
                                projectionTypes.indexOf(projectionType)
                            ) {
                                if (it != null) {
                                    projectionType = projectionTypes[it]
                                    prefs.projectionType = projectionTypes[it]
                                }
                            }
                        },
                        position = position
                    )
                }
            }
        }
    }

    @Composable
    internal fun ErrorSettings() {
        val prefs = remember { ErrorPreferences(requireContext()) }
        val errors = remember { ErrorBannerReason.values().sortedBy { it.id } }
        var values by remember {
            mutableStateOf(errors.associateWith { prefs.canShowError(it) })
        }
        val footer = stringResource(R.string.settings_error_banners_footer)

        SettingsList(title = stringResource(R.string.error_banners)) {
            trailSenseSection("errors", footer = footer) {
                errors.forEach { error ->
                    row { position ->
                        SettingsSwitchRow(
                            label = getErrorDisplayName(error),
                            checked = values[error] ?: true,
                            onCheckedChange = { canShow ->
                                prefs.setCanShowError(error, canShow)
                                values = values + (error to canShow)
                            },
                            position = position
                        )
                    }
                }
            }
        }
    }

    @Composable
    internal fun ClinometerSettings() {
        val prefs = remember { UserPreferences(requireContext()) }
        val formatter = remember { FormatService.getInstance(requireContext()) }
        var lockWithVolume by remember {
            mutableStateOf(prefs.clinometer.lockWithVolumeButtons)
        }
        var baselineDistance by remember {
            mutableStateOf(prefs.clinometer.baselineDistance)
        }
        val baselineTitle = stringResource(R.string.pref_clinometer_baseline_distance_title)

        SettingsList(title = stringResource(R.string.clinometer_title)) {
            trailSenseSection("clinometer") {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_clinometer_lock_with_volume_buttons_title),
                        checked = lockWithVolume,
                        onCheckedChange = {
                            lockWithVolume = it
                            prefs.clinometer.lockWithVolumeButtons = it
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsActionRow(
                        label = baselineTitle,
                        summary = formatDistance(formatter, baselineDistance),
                        onClick = {
                            pickBaselineDistance(prefs, formatter, baselineTitle, baselineDistance) {
                                baselineDistance = it
                            }
                        },
                        position = position
                    )
                }
            }
        }
    }

    @Composable
    internal fun PrivacySettings() {
        val prefs = remember { UserPreferences(requireContext()) }
        val lifecycleOwner = LocalLifecycleOwner.current
        var isLocationMocked by remember { mutableStateOf(isLocationMocked(prefs)) }
        var isScreenshotProtectionOn by remember {
            mutableStateOf(prefs.privacy.isScreenshotProtectionOn)
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isLocationMocked = isLocationMocked(prefs)
                    isScreenshotProtectionOn = prefs.privacy.isScreenshotProtectionOn
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        SettingsList(title = stringResource(R.string.privacy)) {
            trailSenseSection("privacy") {
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.location),
                        summary = if (isLocationMocked) {
                            stringResource(R.string.location_mocked)
                        } else {
                            stringResource(R.string.location_not_mocked)
                        },
                        onClick = {
                            findNavController().navigateWithAnimation(
                                R.id.action_privacy_settings_to_gps_calibration
                            )
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_privacy_screenshot_protection_title),
                        checked = isScreenshotProtectionOn,
                        onCheckedChange = {
                            isScreenshotProtectionOn = it
                            prefs.privacy.isScreenshotProtectionOn = it
                            Screen.setAllowScreenshots(requireActivity().window, !it)
                        },
                        position = position
                    )
                }
            }
        }
    }

    @Composable
    internal fun SensorSettings() {
        val hasBarometer = remember { Sensors.hasBarometer(requireContext()) }
        val items = remember(hasBarometer) {
            sensorSettingsItems(hasBarometer)
        }
        // The sensors to calibrate, then the raw readings of every sensor on their own.
        val (details, sensors) = items.partition { it.key == "details" }

        SettingsList(title = stringResource(R.string.sensors)) {
            listOf("sensors" to sensors, "details" to details).forEach { (key, group) ->
                trailSenseSection(key) {
                    group.forEach { item ->
                        row { position ->
                            TrailSenseListItem(
                                title = stringResource(item.title),
                                icon = painterResource(item.icon),
                                trailing = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    findNavController().navigate(item.destination)
                                },
                                position = position,
                                animateEntrance = false
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    internal fun RulerSettings() {
        val prefs = remember { UserPreferences(requireContext()).ruler }
        var scale by remember { mutableFloatStateOf(prefs.rulerScale) }
        var flipped by remember { mutableStateOf(prefs.rulerFlipped) }
        var showCalibrationDialog by remember { mutableStateOf(false) }
        val calibrationTitle = stringResource(R.string.pref_ruler_calibration_title)

        SettingsList(title = stringResource(R.string.tool_ruler_title)) {
            trailSenseSection("ruler") {
                row { position ->
                    SettingsActionRow(
                        label = calibrationTitle,
                        summary = formatScale(scale),
                        onClick = {
                            showCalibrationDialog = true
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_ruler_flip_direction_title),
                        checked = flipped,
                        onCheckedChange = {
                            flipped = it
                            prefs.rulerFlipped = it
                        },
                        position = position
                    )
                }
            }
        }

        if (showCalibrationDialog) {
            RulerScaleDialog(
                title = calibrationTitle,
                current = scale,
                onDismiss = { showCalibrationDialog = false },
                onConfirm = {
                    scale = it
                    prefs.rulerScale = it
                    showCalibrationDialog = false
                }
            )
        }
    }

    @Composable
    internal fun FlashlightSettings() {
        val formatter = remember { FormatService.getInstance(requireContext()) }
        val prefs = remember { UserPreferences(requireContext()).flashlight }
        val hasFlashlight = remember {
            FlashlightSubsystem.getInstance(requireContext()).isAvailable()
        }
        var toggleWithVolume by remember {
            mutableStateOf(prefs.toggleWithVolumeButtons)
        }
        var shouldTimeout by remember { mutableStateOf(prefs.shouldTimeout) }
        var timeout by remember { mutableStateOf(prefs.timeout) }
        var controlScreenWithVolume by remember {
            mutableStateOf(prefs.controlScreenFlashlightWithVolumeButtons)
        }
        var showTimeoutPicker by remember { mutableStateOf(false) }
        val timeoutTitle = stringResource(R.string.pref_flashlight_timeout_title)
        val screenTitle = stringResource(R.string.screen_flashlight)

        SettingsList(title = stringResource(R.string.flashlight_title)) {
            // Without a torch only the screen flashlight is left, so its header would have nothing to set it apart.
            if (hasFlashlight) {
                trailSenseSection("torch") {
                    row { position ->
                        SettingsSwitchRow(
                            label = stringResource(R.string.pref_flashlight_toggle_with_volume_title),
                            summary = stringResource(R.string.pref_flashlight_toggle_with_volume_summary),
                            checked = toggleWithVolume,
                            onCheckedChange = {
                                toggleWithVolume = it
                                prefs.toggleWithVolumeButtons = it
                            },
                            position = position
                        )
                    }
                    row { position ->
                        SettingsSwitchRow(
                            label = stringResource(R.string.pref_flashlight_should_timeout_title),
                            checked = shouldTimeout,
                            onCheckedChange = {
                                shouldTimeout = it
                                prefs.shouldTimeout = it
                            },
                            position = position
                        )
                    }
                    if (shouldTimeout) {
                        row { position ->
                            SettingsActionRow(
                                label = timeoutTitle,
                                summary = formatter.formatDuration(
                                    timeout,
                                    short = false,
                                    includeSeconds = true
                                ),
                                onClick = {
                                    showTimeoutPicker = true
                                },
                                position = position
                            )
                        }
                    }
                }
            }
            trailSenseSection("screen", title = screenTitle.takeIf { hasFlashlight }) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_flashlight_control_screen_with_volume_title),
                        checked = controlScreenWithVolume,
                        onCheckedChange = {
                            controlScreenWithVolume = it
                            prefs.controlScreenFlashlightWithVolumeButtons = it
                        },
                        position = position
                    )
                }
            }
        }

        if (showTimeoutPicker) {
            DurationPickerDialog(
                title = timeoutTitle,
                duration = timeout,
                showSeconds = true,
                onDurationPicked = {
                    if (it != null && !it.isZero) {
                        prefs.timeout = it
                        timeout = it
                    }
                    showTimeoutPicker = false
                },
                onDismiss = {
                    showTimeoutPicker = false
                }
            )
        }
    }

    @Composable
    internal fun PowerSettings() {
        val prefs = remember { UserPreferences(requireContext()) }
        var lowPowerMode by remember { mutableStateOf(prefs.isLowPowerModeOn) }
        var autoLowPower by remember { mutableStateOf(prefs.power.autoLowPower) }
        var startOnBoot by remember { mutableStateOf(prefs.power.startOnBoot) }
        var tilesEnabled by remember { mutableStateOf(prefs.power.areTilesEnabled) }
        var batteryLogEnabled by remember { mutableStateOf(prefs.power.enableBatteryLog) }
        val backgroundTitle = stringResource(R.string.background)

        SettingsList(title = stringResource(R.string.tool_battery_title)) {
            trailSenseSection("power_saving") {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_low_power_mode_title),
                        summary = stringResource(R.string.pref_low_power_mode_summary),
                        checked = lowPowerMode,
                        onCheckedChange = {
                            lowPowerMode = it
                            setLowPowerMode(prefs, it)
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_auto_low_power_title),
                        summary = stringResource(R.string.pref_auto_low_power_summary),
                        checked = autoLowPower,
                        onCheckedChange = {
                            autoLowPower = it
                            prefs.power.autoLowPower = it
                        },
                        position = position
                    )
                }
            }
            trailSenseSection("background", title = backgroundTitle) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_start_on_boot_title),
                        checked = startOnBoot,
                        onCheckedChange = {
                            startOnBoot = it
                            prefs.power.startOnBoot = it
                            setStartOnBoot(it)
                        },
                        position = position
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    row { position ->
                        SettingsSwitchRow(
                            label = stringResource(R.string.pref_tiles_enabled_title),
                            summary = stringResource(R.string.pref_tiles_enabled_summary),
                            checked = tilesEnabled,
                            onCheckedChange = {
                                tilesEnabled = it
                                prefs.power.areTilesEnabled = it
                                TileManager().setTilesEnabled(requireContext(), it)
                            },
                            position = position
                        )
                    }
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_tiles_battery_log),
                        summary = stringResource(R.string.pref_tiles_enabled_battery_log_summary),
                        checked = batteryLogEnabled,
                        onCheckedChange = {
                            batteryLogEnabled = it
                            prefs.power.enableBatteryLog = it
                            BatteryLogWorker.enableBatteryLog(requireContext(), it)
                        },
                        position = position
                    )
                }
            }
        }
    }

    @Composable
    internal fun RulerScaleDialog(
        title: String,
        current: Float,
        onDismiss: () -> Unit,
        onConfirm: (Float) -> Unit
    ) {
        var value by remember(current) { mutableStateOf(formatScale(current)) }
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(text = title)
            },
            text = {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
                ) {
                    Text(
                        text = stringResource(R.string.pref_ruler_calibration_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text(text = title) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm((value.toFloatCompat() ?: 1f).coerceAtLeast(0f))
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    /** Every settings page of the host: the family's page with its large title and a way back. */
    @Composable
    internal fun SettingsList(
        title: String,
        content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
    ) {
        TrailSensePage(
            title = title,
            onBack = ::navigateBack,
            backLabel = stringResource(R.string.back),
            content = content
        )
    }

    private fun navigateBack() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    /** How a time reads with these choices, shown under them so the switches explain themselves. */
    private fun exampleTime(use24HourTime: Boolean, includeLeadingZero: Boolean): String {
        val hour = if (use24HourTime) "H" else "h"
        val pattern = (if (includeLeadingZero) hour + hour else hour) +
                ":mm" + if (use24HourTime) "" else " a"
        return LocalTime.of(9, 5).format(DateTimeFormatter.ofPattern(pattern))
    }

    @Composable
    internal fun LicensesScreen() {
        SettingsList(title = stringResource(R.string.pref_open_source_licenses_title)) {
            trailSenseSection("libraries") {
                Licenses.libraries.forEach { library ->
                    row { position ->
                        SettingsActionRow(
                            label = library.name,
                            summary = library.url,
                            onClick = { showLicense(library) },
                            position = position
                        )
                    }
                }
            }
        }
    }

    @Composable
    internal fun CellSignalSettings() {
        val prefs = remember { UserPreferences(requireContext()).cellSignal }
        var populateCache by remember { mutableStateOf(prefs.populateCache) }
        SingleSwitchSettingsScreen(
            title = stringResource(R.string.cell_signal),
            label = stringResource(R.string.pref_cell_signal_refresh_cache_title),
            summary = stringResource(R.string.pref_cell_signal_refresh_cache_summary),
            checked = populateCache,
            onCheckedChange = {
                populateCache = it
                prefs.populateCache = it
            },
            onBack = ::navigateBack
        )
    }

    @Composable
    internal fun ClockSettings() {
        val prefs = remember { UserPreferences(requireContext()).clock }
        var showAnalogClock by remember { mutableStateOf(prefs.showAnalogClock) }
        SingleSwitchSettingsScreen(
            title = stringResource(R.string.tool_clock_title),
            label = stringResource(R.string.pref_enable_analog_title),
            checked = showAnalogClock,
            onCheckedChange = {
                showAnalogClock = it
                prefs.showAnalogClock = it
            },
            onBack = ::navigateBack
        )
    }

    @Composable
    internal fun TideSettings() {
        val prefs = remember { UserPreferences(requireContext()).tides }
        var showNearestTide by remember { mutableStateOf(prefs.showNearestTide) }
        SingleSwitchSettingsScreen(
            title = stringResource(R.string.tides),
            label = stringResource(R.string.pref_show_nearest_tide_title),
            checked = showNearestTide,
            onCheckedChange = {
                showNearestTide = it
                prefs.showNearestTide = it
            },
            onBack = ::navigateBack
        )
    }

    @Composable
    internal fun TurnBackSettings() {
        val prefs = remember { UserPreferences(requireContext()).turnBack }
        var useAlarm by remember { mutableStateOf(prefs.useAlarm) }
        SingleSwitchSettingsScreen(
            title = stringResource(R.string.tool_turn_back),
            label = stringResource(R.string.use_alarm_for_turn_back_alert),
            footer = stringResource(R.string.alarm_description),
            checked = useAlarm,
            onCheckedChange = {
                useAlarm = it
                prefs.useAlarm = it
            },
            onBack = ::navigateBack
        )
    }

    @Composable
    internal fun WaterBoilSettings() {
        val prefs = remember { UserPreferences(requireContext()).waterBoilTimer }
        var useAlarm by remember { mutableStateOf(prefs.useAlarm) }
        SingleSwitchSettingsScreen(
            title = stringResource(R.string.water_boil_timer),
            label = stringResource(R.string.use_alarm_for_water_boil_timer),
            footer = stringResource(R.string.alarm_description),
            checked = useAlarm,
            onCheckedChange = {
                useAlarm = it
                prefs.useAlarm = it
            },
            onBack = ::navigateBack
        )
    }

    private fun openApp(context: Context, packageName: String, tool: String) {
        if (!Package.isPackageInstalled(context, packageName)) return
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtras(Bundle().apply {
            putString("tool", tool)
        })
        context.startActivity(intent)
    }

    private fun showLicense(library: Library) {
        val content = (library.url + "\n\n" + library.license()).toSpannable()
        LinkifyCompat.addLinks(content, WEB_URLS)

        Alerts.dialog(
            requireContext(),
            library.name,
            content,
            cancelText = null,
            allowLinks = true
        )
    }

    private fun pickBaselineDistance(
        prefs: UserPreferences,
        formatter: FormatService,
        title: String,
        current: Distance?,
        onChange: (Distance?) -> Unit
    ) {
        CustomUiUtils.pickDistance(
            requireContext(),
            formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits),
            current,
            title
        ) { distance, cancelled ->
            if (cancelled) {
                return@pickDistance
            }
            val value = if (distance != null && distance.value > 0) {
                distance
            } else {
                null
            }
            prefs.clinometer.baselineDistance = value
            onChange(value)
        }
    }

    private fun formatDistance(formatter: FormatService, distance: Distance?): String {
        return distance?.let {
            formatter.formatDistance(
                it,
                Units.getDecimalPlaces(it.units)
            )
        } ?: getString(R.string.dash)
    }

    private fun isLocationMocked(prefs: UserPreferences): Boolean {
        return prefs.gps.locationSource == GPSLocationSource.Manual ||
            !SensorService(requireContext()).hasLocationPermission()
    }

    private fun setLowPowerMode(prefs: UserPreferences, enabled: Boolean) {
        if (enabled) {
            prefs.power.userEnabledLowPower = true
            LowPowerMode(requireContext()).enable()
        } else {
            prefs.power.userEnabledLowPower = false
            LowPowerMode(requireContext()).disable()
        }
    }

    private fun setStartOnBoot(enabled: Boolean) {
        val controller = StartOnBootController(requireContext())
        if (enabled) {
            controller.enable()
        } else {
            controller.disable()
        }
    }

    private fun formatScale(scale: Float): String {
        return scale.toString()
    }

    private fun pickQuickActions(prefs: UserPreferences) {
        val potentialActions = Tools.getQuickActions(requireContext())
            .filterNot { it.id == Tools.QUICK_ACTION_NONE }

        val selected = prefs.toolQuickActions

        val selectedIndices = potentialActions.mapIndexedNotNull { index, action ->
            if (selected.contains(action.id)) index else null
        }

        Pickers.items(
            requireContext(),
            getString(R.string.quick_actions),
            potentialActions.map { it.name },
            selectedIndices
        ) {
            if (it != null) {
                prefs.toolQuickActions = it.map { potentialActions[it].id }
            }
        }
    }

    private fun pickWidgets(prefs: UserPreferences) {
        val tools = Tools.getTools(requireContext())
        val potentialWidgets = tools
            .flatMap { it.widgets }
            .filter { it.isEnabled(requireContext()) && it.canPlaceInApp }
            .distinctBy { it.id }
            .sortedBy { it.name }

        val selected = prefs.toolWidgets

        val selectedIndices = potentialWidgets.mapIndexedNotNull { index, widget ->
            if (selected.contains(widget.id)) index else null
        }

        Pickers.items(
            requireContext(),
            getString(R.string.widget_pref_title),
            potentialWidgets.map { it.name },
            selectedIndices
        ) {
            if (it != null) {
                prefs.toolWidgets = it.map { potentialWidgets[it].id }
            }
        }
    }

    private fun pickBottomNavTool(
        title: String,
        index: Int,
        tools: List<Tool>,
        bottomNavSelection: List<Long>,
        prefs: UserPreferences,
        onSelectionChanged: (List<Long>) -> Unit
    ) {
        val names = listOf(getString(R.string.none)) + tools.map { it.name }
        val ids = listOf(0L) + tools.map { it.id }

        Pickers.item(
            requireContext(),
            title,
            names,
            ids.indexOfFirst { it == bottomNavSelection.getOrNull(index) }
        ) {
            if (it != null) {
                val selected = ids[it]
                val newSelection = bottomNavSelection.mapIndexed { selectionIndex, id ->
                    if (selectionIndex == index) selected else id
                }
                prefs.bottomNavigationTools = newSelection
                onSelectionChanged(newSelection)
                requireMainActivity().updateBottomNavigation()
            }
        }
    }

    private fun getBottomNavToolName(tools: List<Tool>, id: Long?): String {
        return tools.firstOrNull { it.id == id }?.name ?: getString(R.string.none)
    }

    private fun showMapLayerSheet() {
        layerSheet?.dismissAndUpdateWidgets(requireContext())
        layerSheet = MapLayersSheetState(MapToolRegistration.MAP_ID)
        layerSheet?.show()
    }

    private fun showPhotoMapLayerSheet() {
        layerSheet?.dismissAndUpdateWidgets(requireContext())
        layerSheet = MapLayersSheetState(
            OfflineMapsToolRegistration.PHOTO_MAPS_ID,
            PhotoMapToolLayerManager.alwaysEnabledLayers
        )
        layerSheet?.show()
    }

    private fun onReduceMapResolutionChange(shouldReduce: Boolean) {
        if (shouldReduce) {
            return
        }

        CustomUiUtils.disclaimer(
            requireContext(),
            getString(R.string.reduce_map_resolution),
            getString(R.string.reduce_map_resolution_crop_disclaimer),
            getString(R.string.reduce_map_resolution_crop_disclaimer_shown),
            cancelText = null
        )
    }

    internal fun requestCameraForMagnifier(action: (hasPermission: Boolean) -> Unit) {
        requestPermissions(listOf(Manifest.permission.CAMERA)) {
            val isAvailable = Camera.isAvailable(requireContext())
            if (!isAvailable) {
                alertNoCameraPermission()
            }
            action(isAvailable)
        }
    }

    private fun requestCameraForMirror(action: (hasPermission: Boolean) -> Unit) {
        requestPermissions(listOf(Manifest.permission.CAMERA)) {
            val isAvailable = Permissions.isCameraEnabled(requireContext())
            if (!isAvailable) {
                alertNoCameraPermission()
            }
            action(isAvailable)
        }
    }

    private fun backup() {
        val uriPicker = IntentUriPicker(requireActivity() as IntentResultRetriever, requireContext())
        lifecycleOwner.lifecycleScope.launch {
            BackupCommand(requireContext(), uriPicker).execute()
        }
    }

    private fun restore() {
        val uriPicker = IntentUriPicker(requireActivity() as IntentResultRetriever, requireContext())
        lifecycleOwner.lifecycleScope.launch {
            RestoreCommand(requireContext(), uriPicker).execute()
        }
    }

    private fun changeAutoBackupDirectory(
        prefs: UserPreferences,
        onChanged: (Boolean) -> Unit
    ) {
        val resultRetriever = requireActivity() as IntentResultRetriever
        ChangeAutomaticBackupDirectoryCommand(requireContext(), resultRetriever) { isSuccessful ->
            if (!isSuccessful) {
                prefs.backup.isAutoBackupEnabled = false
            }
            onChanged(isSuccessful)
        }.execute()
    }

    private fun getAutoBackupSummary(prefs: UserPreferences, isEnabled: Boolean): String? {
        return if (isEnabled) {
            prefs.backup.userFriendlyAutoBackupPathName
        } else {
            null
        }
    }

    private fun pickOption(
        title: String,
        options: List<SettingsOption>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        Pickers.item(
            requireContext(),
            title,
            options.map { it.label },
            options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        ) { index ->
            if (index != null) {
                onSelected(options[index].value)
            }
        }
    }

    private fun optionLabel(options: List<SettingsOption>, selected: String): String {
        return options.firstOrNull { it.value == selected }?.label ?: selected
    }

    private fun settingsOptions(entriesId: Int, valuesId: Int): List<SettingsOption> {
        val entries = getResources().getStringArray(entriesId)
        val values = getResources().getStringArray(valuesId)
        return values.mapIndexed { index, value ->
            SettingsOption(value, entries.getOrElse(index) { value })
        }
    }

    private fun widgetThemeOptions(): List<SettingsOption> {
        return listOf(
            SettingsOption(
                WidgetTheme.System.id.toString(),
                getString(R.string.theme_system)
            ),
            SettingsOption(
                WidgetTheme.TransparentBlack.id.toString(),
                getString(
                    R.string.theme_transparent_type,
                    getString(R.string.widget_theme_black_text)
                )
            ),
            SettingsOption(
                WidgetTheme.TransparentWhite.id.toString(),
                getString(
                    R.string.theme_transparent_type,
                    getString(R.string.widget_theme_white_text)
                )
            )
        )
    }

    private fun forecastOptions(): List<SettingsOption> {
        return ForecastSource.entries.map {
            SettingsOption(forecastSourceValue(it), it.name)
        }
    }

    private fun notificationGroupingOptions(): List<SettingsOption> {
        val labels = mapOf(
            GroupBehavior.System to getString(R.string.theme_system),
            GroupBehavior.UngroupAll to getString(R.string.ungroup_all),
            GroupBehavior.UngroupHigh to getString(R.string.ungroup_alerts)
        )
        return GroupBehavior.entries.map {
            SettingsOption(
                notificationGroupingValue(it),
                labels[it] ?: it.name
            )
        }
    }

    private fun forecastSourceValue(source: ForecastSource): String {
        return when (source) {
            ForecastSource.Sol -> "1"
            ForecastSource.Zambretti -> "2"
        }
    }

    private fun notificationGroupingValue(behavior: GroupBehavior): String {
        return when (behavior) {
            GroupBehavior.System -> "1"
            GroupBehavior.UngroupAll -> "2"
            GroupBehavior.UngroupHigh -> "3"
        }
    }

    private fun getDefaultWidgetTheme(cache: IPreferences): String {
        val key = getString(R.string.pref_default_widget_theme)
        val current = cache.getString(key)
        if (current != null) {
            return current
        }

        val default = WidgetTheme.System.id.toString()
        cache.putString(key, default)
        return default
    }

    private fun getStringPreference(cache: IPreferences, key: Int, default: String): String {
        return cache.getString(getString(key)) ?: default
    }

    private fun getBoolean(cache: IPreferences, key: Int, default: Boolean): Boolean {
        return cache.getBoolean(getString(key)) ?: default
    }

    private fun putString(cache: IPreferences, key: Int, value: String) {
        cache.putString(getString(key), value)
    }

    private fun putBoolean(cache: IPreferences, key: Int, value: Boolean) {
        cache.putBoolean(getString(key), value)
    }

    @Composable
    internal fun UnitPickerRow(
        label: String,
        options: List<SettingsOption>,
        selected: String,
        onSelected: (String) -> Unit,
        position: TrailSenseGroupPosition
    ) {
        SettingsPickerRow(
            label = label,
            selected = optionLabel(options, selected),
            onClick = {
                pickOption(label, options, selected, onSelected)
            },
            position = position
        )
    }

    /** A unit with two to four short options, all on view at once. */
    @Composable
    internal fun UnitChoiceRow(
        label: String,
        options: List<SettingsOption>,
        selected: String,
        onSelected: (String) -> Unit,
        position: TrailSenseGroupPosition
    ) {
        SettingsChoiceRow(
            label = label,
            options = remember(options) { options.map { TrailSenseChoice(it.value, it.label) } },
            selected = selected,
            onSelect = onSelected,
            position = position
        )
    }

    private fun pickDistance(
        units: List<DistanceUnits>,
        title: String,
        current: Distance,
        description: String? = null,
        onPicked: (Distance) -> Unit
    ) {
        CustomUiUtils.pickDistance(
            requireContext(),
            units,
            current,
            title,
            description = description
        ) { distance, cancelled ->
            if (cancelled || distance == null || distance.value <= 0) {
                return@pickDistance
            }

            onPicked(distance)
        }
    }

    private fun relativeDistance(prefs: UserPreferences, distance: Distance): Distance {
        return distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
    }

    private fun distanceUnitsValue(units: UserPreferences.DistanceUnits): String {
        return when (units) {
            UserPreferences.DistanceUnits.Meters -> "meters"
            UserPreferences.DistanceUnits.Feet -> "feet_miles"
        }
    }

    private fun pressureUnitsValue(units: PressureUnits): String {
        return when (units) {
            PressureUnits.Hpa -> "hpa"
            PressureUnits.Inhg -> "in"
            PressureUnits.MmHg -> "mm"
            PressureUnits.Mbar -> "mbar"
            PressureUnits.Psi -> "psi"
        }
    }

    private fun temperatureUnitsValue(units: TemperatureUnits): String {
        return when (units) {
            TemperatureUnits.Celsius -> "c"
            TemperatureUnits.Fahrenheit -> "f"
        }
    }

    private fun weightUnitsValue(units: WeightUnits): String {
        return when (units) {
            WeightUnits.Kilograms -> "kg"
            WeightUnits.Pounds -> "lbs"
            WeightUnits.Stone -> "stone"
            else -> "kg"
        }
    }

    private fun coordinateFormatValue(format: BuiltInCoordinateFormat): String {
        return when (format) {
            BuiltInCoordinateFormat.DecimalDegrees -> "dd"
            BuiltInCoordinateFormat.DegreesDecimalMinutes -> "ddm"
            BuiltInCoordinateFormat.DegreesMinutesSeconds -> "dms"
            BuiltInCoordinateFormat.UTM -> "utm"
            BuiltInCoordinateFormat.MGRS -> "mgrs"
            BuiltInCoordinateFormat.USNG -> "usng"
            BuiltInCoordinateFormat.OSGB -> "osng"
        }
    }

    private fun mapSiteValue(site: MapSite): String {
        return when (site) {
            MapSite.Apple -> "apple"
            MapSite.Bing -> "bing"
            MapSite.Caltopo -> "caltopo"
            MapSite.Google -> "google"
            MapSite.OSM -> "osm"
        }
    }

    internal data class SettingsOption(
        val value: String,
        val label: String
    )

    private suspend fun loadPlugins(pluginSubsystem: PluginSubsystem): PluginSettingsState {
        return PluginSettingsState(
            connected = pluginSubsystem.getConnectedPlugins().sortedPlugins(),
            available = pluginSubsystem.getUnconnectedPlugins().sortedPlugins()
        )
    }

    private fun confirmConnect(
        pluginSubsystem: PluginSubsystem,
        plugin: Plugin,
        onConnected: () -> Unit
    ) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.plugin_connect_title),
            getString(R.string.plugin_connect_message, plugin.name)
        ) { cancelled ->
            if (!cancelled) {
                lifecycleOwner.lifecycleScope.launch {
                    pluginSubsystem.connect(plugin)
                    onConnected()
                }
            }
        }
    }

    private fun openPluginDetails(plugin: Plugin) {
        findNavController().navigateWithAnimation(
            R.id.pluginDetailsFragment,
            Bundle().apply {
                putString("package_id", plugin.packageId)
            }
        )
    }

    private fun showToolActions(tool: Tool) {
        val actions = listOfNotNull(
            if (tool.isExperimental) getString(R.string.experimental) else null,
            if (tool.description != null) getString(R.string.pref_category_about) else null,
            if (pinnedToolManager.isPinned(tool.id)) getString(R.string.unpin) else getString(R.string.pin),
            if (tool.guideId != null) getString(R.string.tool_user_guide_title) else null,
            if (tool.settingsNavAction != null) getString(R.string.settings) else null,
            getString(R.string.add_to_homescreen)
        )

        Pickers.item(requireContext(), tool.name, actions) { selected ->
            when (actions.getOrNull(selected ?: return@item)) {
                getString(R.string.experimental) -> Unit
                getString(R.string.pref_category_about) -> Alerts.dialog(
                    requireContext(),
                    tool.name,
                    tool.description,
                    cancelText = null
                )
                getString(R.string.unpin) -> pinnedToolManager.unpin(tool.id)
                getString(R.string.pin) -> pinnedToolManager.pin(tool.id)
                getString(R.string.tool_user_guide_title) -> UserGuideUtils.showGuide(
                    requireContext(),
                    findNavController(),
                    tool.guideId!!
                )
                getString(R.string.settings) -> findNavController().navigateWithAnimation(tool.settingsNavAction!!)
                getString(R.string.add_to_homescreen) -> addToHomescreen(tool)
            }
        }
    }

    private fun addToHomescreen(tool: Tool) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext())) {
            return
        }

        val shortcutInfo = ShortcutInfoCompat.Builder(requireContext(), "tool-${tool.id}")
            .setIcon(
                IconCompat.createFromIcon(
                    requireContext(),
                    android.graphics.drawable.Icon.createWithResource(requireContext(), tool.icon)
                )
            )
            .setIntent(MainActivity.openToolIntent(requireContext(), tool.id))
            .setShortLabel(tool.name)
            .setLongLabel(tool.name)
            .build()

        val callbackIntent =
            ShortcutManagerCompat.createShortcutResultIntent(requireContext(), shortcutInfo)
        val successCallback = PendingIntent.getBroadcast(
            requireContext(),
            0,
            callbackIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        ShortcutManagerCompat.requestPinShortcut(
            requireContext(),
            shortcutInfo,
            successCallback.intentSender
        )
    }

    private fun initializeScreenFlashlight() {
        if (screenFlashlightCache.getBoolean("cache_red_light") == null) {
            screenFlashlightCache.putBoolean("cache_red_light", false)
        }
        screenFlashlightIsRed = screenFlashlightCache.getBoolean("cache_red_light") == true
        screenFlashlightBrightness = (
                screenFlashlightCache.getInt(getString(R.string.pref_screen_torch_brightness))
                    ?: 100
                ).toFloat()
    }

    private fun toggleScreenFlashlightColor() {
        screenFlashlightIsRed = !screenFlashlightIsRed
        screenFlashlightCache.putBoolean("cache_red_light", screenFlashlightIsRed)
    }

    private fun turnOnScreenFlashlight() {
        setScreenFlashlightBrightness(
            screenFlashlightCache.getInt(getString(R.string.pref_screen_torch_brightness)) ?: 100
        )
    }

    private fun turnOffScreenFlashlight() {
        screenFlashlight.off()
    }

    private fun setScreenFlashlightBrightness(percent: Int) {
        val coerced = percent.coerceIn(0, 100)
        screenFlashlightBrightness = coerced.toFloat()
        screenFlashlightCache.putInt(getString(R.string.pref_screen_torch_brightness), coerced)
        screenFlashlight.on(map(coerced / 100f, 0f, 1f, 0.1f, 1f))
    }

    private fun increaseScreenFlashlightBrightness() {
        setScreenFlashlightBrightness((screenFlashlightBrightness.toInt() + 10).coerceAtMost(100))
    }

    private fun decreaseScreenFlashlightBrightness() {
        setScreenFlashlightBrightness((screenFlashlightBrightness.toInt() - 10).coerceAtLeast(0))
    }

    private fun showNoteQR(note: Note) {
        CustomUiUtils.showQR(
            requireContext(),
            findNavController(),
            note.title ?: getString(android.R.string.untitled),
            NoteQREncoder().encode(note)
        )
    }

    private fun deleteNote(note: Note) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.delete_note_title),
            if (note.title?.trim().isNullOrEmpty()) {
                getString(android.R.string.untitled)
            } else {
                note.title
            }
        ) { cancelled ->
            if (!cancelled) {
                lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    notesRepo.deleteNote(note)
                }
            }
        }
    }

    private fun editNote(note: Note) {
        findNavController().navigate(
            R.id.action_fragmentToolNotes_to_fragmentToolNotesCreate,
            Bundle().apply {
                putLong("edit_note_id", note.id)
            }
        )
    }

    private suspend fun saveNote(title: String, content: String) {
        val existingNote = editingNote
        val note = existingNote?.copy(title = title, contents = content)
            ?.apply { id = existingNote.id }
            ?: Note(title, content, Instant.now().toEpochMilli())
        withContext(Dispatchers.IO) {
            notesRepo.addNote(note)
        }
        withContext(Dispatchers.Main) {
            findNavController().navigateUp()
        }
    }

    private fun hasNoteChanges(): Boolean {
        return !nothingEnteredForNote() &&
                (noteTitle != editingNote?.title.orEmpty() ||
                        noteContent != editingNote?.contents.orEmpty())
    }

    private fun nothingEnteredForNote(): Boolean {
        if (editingNote != null) {
            return false
        }

        return noteTitle.isBlank() && noteContent.isBlank()
    }

    private fun getSurvivalGuideChapter(): Chapter {
        val chapterResourceId = requireArguments().getInt("chapter_resource_id")
        return Chapters.getChapters(requireContext()).first { it.resource == chapterResourceId }
    }

    private fun loadSurvivalGuideChapter() {
        val chapterResourceId = requireArguments().getInt("chapter_resource_id")
        lifecycleOwner.lifecycleScope.launch {
            survivalGuideContent = withContext(Dispatchers.IO) {
                TextUtils.loadTextFromResources(requireContext(), chapterResourceId)
            }
        }
    }

    private fun printSurvivalGuideChapter(chapter: Chapter) {
        val context = requireContext()
        val files = FileSubsystem.getInstance(context)
        val markdown = DependencyRegistry.get<com.kylecorry.andromeda.markdown.MarkdownService>()
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val loading = withContext(Dispatchers.Main) {
                Alerts.loading(context, getString(R.string.loading))
            }
            try {
                val printer = Printer(context)
                printer.setColorMode(ColorMode.Color)
                printer.setScaleMode(ScaleMode.Fit)
                printer.setOrientation(Orientation.Portrait)
                val content = TextUtils.removeMarkdownComments(
                    "# ${chapter.title}\n" + TextUtils.loadTextFromResources(
                        context,
                        chapter.resource
                    )
                )
                val file = files.createTemp("pdf")
                val parsed = markdown.toMarkdown(content)
                val textView = TextView(context)
                textView.textSize = 6f
                textView.setTextColor(AndroidColor.BLACK)
                markdown.setParsedMarkdown(textView, parsed)
                delay(2000)
                val uri = file.toUri()
                files.output(uri)?.use {
                    PdfConvert.toPdf(textView, it)
                }
                withContext(Dispatchers.Main) {
                    printer.print(uri)
                }
            } finally {
                DeleteTempFilesCommand(context).execute()
                withContext(Dispatchers.Main) {
                    loading.dismiss()
                }
            }
        }
    }

    private fun initializeSurvivalGuideList() {
        survivalGuideQuery = destinationArguments?.getString("search_query").orEmpty()
        CustomUiUtils.disclaimer(
            requireContext(),
            getString(R.string.survival_guide),
            getString(R.string.survival_guide_disclaimer),
            "pref_survival_guide_disclaimer_shown",
            cancelText = null
        )
        requireActivity().onBackPressedDispatcher.addCallback(
            lifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (survivalGuideQuery.isNotBlank()) {
                        updateSurvivalGuideQuery("")
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
        loadSurvivalGuideChapters()
        if (survivalGuideQuery.isNotBlank()) {
            searchSurvivalGuide(survivalGuideQuery)
        }
    }

    private fun loadSurvivalGuideChapters() {
        lifecycleOwner.lifecycleScope.launch {
            survivalGuideChapters = GuideLoader(requireContext()).load(includeContent = false)
        }
    }

    private fun updateSurvivalGuideQuery(value: String) {
        if (survivalGuideQuery == value) {
            return
        }
        survivalGuideQuery = value
        searchSurvivalGuide(value)
    }

    private fun searchSurvivalGuide(value: String) {
        survivalGuideSearchJob?.cancel()
        if (value.isBlank()) {
            survivalGuideSearchResults = emptyList()
            survivalGuideSummary = ""
            return
        }
        val searchService = SurvivalGuideSearch(requireContext())
        survivalGuideSearchJob = lifecycleOwner.lifecycleScope.launch {
            delay(200)
            val results = searchService.search(value)
            if (survivalGuideQuery != value) {
                return@launch
            }
            survivalGuideSearchResults = results
            val firstResult = results.firstOrNull()
            survivalGuideSummary = if (firstResult != null && firstResult.score >= 0.5f) {
                searchService.getSummary(firstResult)
            } else {
                ""
            }
        }
    }

    private fun getSurvivalGuideListItems(): List<SurvivalGuideListItem> {
        return if (survivalGuideQuery.isBlank() || survivalGuideSearchResults.isEmpty()) {
            survivalGuideChapters.map {
                SurvivalGuideListItem(
                    id = it.chapter.resource.toLong(),
                    title = it.chapter.title,
                    subtitle = it.sections.firstOrNull()?.summary,
                    icon = it.chapter.icon,
                    tag = null,
                    onClick = { openSurvivalGuideChapter(it.chapter.resource) }
                )
            }
        } else {
            survivalGuideSearchResults.map {
                SurvivalGuideListItem(
                    id = it.chapter.resource.toLong(),
                    title = it.heading ?: getString(R.string.overview),
                    subtitle = it.summary,
                    icon = it.chapter.icon,
                    tag = it.chapter.title,
                    onClick = { openSurvivalGuideSearchResult(it) }
                )
            }
        }
    }

    private fun openSurvivalGuideChapter(chapterResource: Int) {
        findNavController().navigateWithAnimation(
            R.id.fragmentToolSurvivalGuideReader,
            Bundle().apply {
                putInt("chapter_resource_id", chapterResource)
            }
        )
    }

    private fun openSurvivalGuideSearchResult(result: SurvivalGuideSearchResult) {
        findNavController().navigateWithAnimation(
            R.id.fragmentToolSurvivalGuideReader,
            Bundle().apply {
                putInt("chapter_resource_id", result.chapter.resource)
                putInt("header_index", result.headingIndex)
            }
        )
    }

    private fun resetPedometer() {
        Alerts.dialog(requireContext(), getString(R.string.reset_distance_title)) {
            if (!it) {
                lifecycleOwner.lifecycleScope.launch {
                    getAppService<StepTrackerService>().startNewStepTrackingPeriod()
                }
            }
        }
    }

    private fun togglePedometer(pedometer: PedometerSubsystem) {
        when (pedometer.state.getOrNull()) {
            FeatureState.On -> pedometer.disable()
            FeatureState.Off -> startStepCounter(pedometer)
            else -> {
                if (pedometer.isDisabledDueToPermissions()) {
                    startStepCounter(pedometer)
                }
            }
        }
    }

    private fun togglePedometerDistanceAlert(prefs: UserPreferences, formatter: FormatService) {
        val alertDistance = prefs.pedometer.alertDistance
        if (alertDistance == null) {
            val units = formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
            CustomUiUtils.pickDistance(
                requireContext(),
                units,
                title = getString(R.string.distance_alert),
            ) { distance, _ ->
                if (distance != null) {
                    prefs.pedometer.alertDistance = distance
                }
            }
        } else {
            val distance = alertDistance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
            Alerts.dialog(
                requireContext(),
                getString(R.string.distance_alert),
                getString(
                    R.string.remove_distance_alert,
                    formatter.formatDistance(
                        distance,
                        Units.getDecimalPlaces(distance.units),
                        false
                    )
                )
            ) {
                if (!it) {
                    prefs.pedometer.alertDistance = null
                }
            }
        }
    }

    private fun startStepCounter(pedometer: PedometerSubsystem) {
        requestActivityRecognition { hasPermission ->
            if (hasPermission) {
                pedometer.enable()
            } else {
                pedometer.disable()
                alertNoActivityRecognitionPermission()
            }
        }
    }

    private fun showBatteryHistory(readings: List<BatteryReading>, formatter: FormatService) {
        val readingDuration = Duration.between(
            readings.firstOrNull()?.time,
            readings.lastOrNull()?.time
        )
        CustomUiUtils.showComposeContentDialog(
            context = requireContext(),
            title = getString(R.string.battery_history, formatter.formatDuration(readingDuration, false))
        ) {
            TrailSenseLineChart(
                readings = readings.map {
                    Reading(it.percent, it.time)
                },
                minimum = 0f,
                maximum = 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.chart_height))
            )
        }
    }

    private data class PluginSettingsState(
        val connected: List<Plugin>,
        val available: List<Plugin>
    )

    private fun List<Plugin>.sortedPlugins(): List<Plugin> {
        return sortedWith(
            compareBy<Plugin, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
                .thenBy { it.packageId }
        )
    }

    private fun projectionTypeNames(): Map<CameraPreferences.ProjectionType, String> {
        return mapOf(
            CameraPreferences.ProjectionType.EstimatedIntrinsics to
                    "${getString(R.string.projection_estimated_intrinsics)} (${
                        getString(R.string.default_string)
                    })",
            CameraPreferences.ProjectionType.ManufacturerIntrinsics to
                    getString(R.string.projection_manufacturer_intrinsics),
            CameraPreferences.ProjectionType.Perspective to getString(R.string.projection_perspective),
            CameraPreferences.ProjectionType.Linear to getString(R.string.projection_linear)
        )
    }

    private fun sensorSettingsItems(hasBarometer: Boolean): List<SensorSettingsItem> {
        return listOfNotNull(
            SensorSettingsItem(
                key = "compass",
                title = R.string.pref_compass_sensor_title,
                icon = R.drawable.ic_compass_icon,
                destination = R.id.calibrateCompassFragment
            ),
            SensorSettingsItem(
                key = "gps",
                title = R.string.gps,
                icon = R.drawable.satellite,
                destination = R.id.calibrateGPSFragment
            ),
            SensorSettingsItem(
                key = "altimeter",
                title = R.string.pref_altimeter_calibration_title,
                icon = R.drawable.ic_altitude,
                destination = R.id.calibrateAltimeterFragment
            ),
            if (hasBarometer) {
                SensorSettingsItem(
                    key = "barometer",
                    title = R.string.barometer,
                    icon = R.drawable.ic_barometer,
                    destination = R.id.calibrateBarometerFragment
                )
            } else {
                null
            },
            SensorSettingsItem(
                key = "temperature",
                title = R.string.tool_thermometer_title,
                icon = R.drawable.thermometer,
                destination = R.id.thermometerSettingsFragment
            ),
            SensorSettingsItem(
                key = "camera",
                title = R.string.camera,
                icon = R.drawable.ic_camera,
                destination = R.id.cameraSettingsFragment
            ),
            SensorSettingsItem(
                key = "cell_signal",
                title = R.string.cell_signal,
                icon = R.drawable.signal_cellular_3,
                destination = R.id.cellSignalSettingsFragment
            ),
            SensorSettingsItem(
                key = "details",
                title = R.string.pref_sensor_details_title,
                icon = R.drawable.ic_sensors,
                destination = R.id.sensorDetailsFragment
            )
        )
    }

    private data class SensorSettingsItem(
        val key: String,
        val title: Int,
        @DrawableRes val icon: Int,
        @IdRes val destination: Int
    )

    @Composable
    internal fun getErrorDisplayName(reason: ErrorBannerReason): String {
        return when (reason) {
            ErrorBannerReason.NoGPS -> stringResource(R.string.gps_unavailable)
            ErrorBannerReason.LocationNotSet -> stringResource(R.string.location_not_set)
            ErrorBannerReason.CompassPoor -> stringResource(R.string.compass_accuracy)
            ErrorBannerReason.NoCompass -> stringResource(R.string.compass_unavailable)
            ErrorBannerReason.GPSTimeout -> stringResource(R.string.gps_timeouts)
            ErrorBannerReason.LocationPermissionDenied -> stringResource(R.string.location_permission)
        }
    }

}

private data class ActionSheetState(
    val title: String,
    val subtitle: String?,
    val actions: List<ActionItem>,
    val onAction: (ActionItem?) -> Unit
)
