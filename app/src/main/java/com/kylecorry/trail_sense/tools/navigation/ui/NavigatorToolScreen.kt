package com.kylecorry.trail_sense.tools.navigation.ui

import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import com.kylecorry.trail_sense.shared.copy
import com.kylecorry.trail_sense.shared.navigation.Route
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.kylecorry.trail_sense.settings.ui.ImproveAccuracyAlerter
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.CompassCalibrationContent
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.debugging.DebugElevationsCommand
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationScreenLock
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.quickactions.NavigationQuickActionBinder
import com.kylecorry.trail_sense.tools.navigation.ui.errors.NavigatorUserErrors
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.GPSDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.MagnetometerDiagnosticScanner
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.WeatherRepo
import kotlinx.coroutines.launch
import android.content.ContextWrapper
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.tools.navigation.infrastructure.share.shareLocation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSheet
import com.kylecorry.trail_sense.shared.dialogs.item

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigatorToolScreen(
    route: Route.Navigation,
    quickActionHost: AndroidDestinationHost,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val appNavigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel {
        val userPrefs = UserPreferences(appContext)
        val sensors = NavigatorSensors(SensorService(appContext), userPrefs)
        NavigatorViewModel(
            route = route,
            sensors = sensors,
            navigator = Navigator.getInstance(appContext),
            beaconRepo = BeaconRepo.getInstance(appContext),
            pathService = PathService.getInstance(appContext),
            weatherRepo = WeatherRepo.getInstance(appContext),
            prefs = userPrefs.navigation,
            useTrueNorth = userPrefs.compass.useTrueNorth,
            diagnostics = listOf(
                GPSDiagnosticScanner(sensors.gps).fullScan(appContext),
                MagnetometerDiagnosticScanner().fullScan(appContext)
            ),
            debugElevations = { readings, smoothed ->
                DebugElevationsCommand(appContext, readings, smoothed).execute()
            }
        )
    }
    val userPrefs = remember(appContext) { UserPreferences(appContext) }
    val formatService = remember(appContext) { FormatService.getInstance(appContext) }
    // The screen lock and the error banner are the activity's own.
    val activity = remember(context) { requireNotNull(context.findMainActivity()) }
    val screenLock = remember { NavigationScreenLock(userPrefs.navigation.keepScreenUnlockedWhileOpen) }

    val display = remember(context) {
        NavigatorDisplay(
            context = context,
            viewModel = viewModel,
            userPrefs = userPrefs,
            formatService = formatService,
            onNorthReferenceSettingsClick = { appNavigator.navigate(Route.CalibrateCompass) },
            requestCameraPermission = { callback ->
                scope.launch { callback(effects.requestPermission(AppPermission.Camera)) }
            },
            onCameraPermissionDenied = { effects.explainPermissionDenied(AppPermission.Camera) },
            onOpenBeacon = { beaconId -> appNavigator.navigate(Route.BeaconDetails(beaconId = beaconId)) }
        )
    }
    val views = display.views
    val errors = remember(context) {
        NavigatorUserErrors(context, activity.errorBanner, appNavigator) {
            appNavigator.navigate(Route.CalibrateCompass)
        }
    }

    // One-time wiring of the rendering objects to the ViewModel.
    DisposableEffect(display) {
        NavigationQuickActionBinder(
            quickActionHost,
            views.navigationTitle.leftButton,
            views.navigationTitle.rightButton,
            userPrefs.navigation
        ).bind()

        views.navigationTitle.onSubtitleLongClick = {
            scope.launch {
                shareLocation(context, effects, appNavigator, viewModel.sensors.gps.location)
            }
            true
        }
        views.navigationTitle.onSubtitleClick = { viewModel.openLocationSheet(userPrefs.coordinateFormat) }
        views.altitude.onClick = { viewModel.openAltitudeSheet() }
        views.linearCompass.setOnClickListener { viewModel.toggleDestinationBearing() }
        views.onBeaconClick = { appNavigator.navigate(Route.BeaconList()) }
        views.onBeaconLongClick = {
            // Straight to the editor, as the map and the field guide do: Save and Back return here.
            appNavigator.navigate(Route.PlaceBeacon(initialLocation = viewModel.currentLocationForNewPlace()))
        }
        // As upstream: a sensor's status opens the tips for improving accuracy
        views.accuracyView.setOnClickListener {
            ImproveAccuracyAlerter(context).alert(views.accuracyView.getSensors())
        }
        views.radarCompassMap.setOnSingleTapListener { viewModel.toggleDestinationBearing() }
        views.radarCompassMap.setOnLongPressListener {
            display.layerSheet.dismiss()
            display.layers.pause(views.radarCompassMap)
            display.layerSheet.setOnDismissListener {
                display.layers.resume(views.radarCompassMap, lifecycleOwner.lifecycleScope)
            }
            display.layerSheet.show()
        }
        onDispose {
            screenLock.releaseLock(activity)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.destination.collect { screenLock.updateLock(activity) }
    }
    LaunchedEffect(viewModel) {
        viewModel.cancelNavigationRequests.collect { views.navigationSheet.requestCancelNavigation() }
    }

    // The sensors run while the screen is started, as their LiveData observers did.
    LifecycleStartEffect(viewModel) {
        viewModel.startSensors()
        onStopOrDispose {
            viewModel.stopSensors()
        }
    }

    // While the screen is in front the dials follow the sensors on the display's frames, at most
    // every NAVIGATION_UPDATE_INTERVAL; everything stops with the coroutine when it pauses.
    LaunchedEffect(display) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            display.resume(lifecycleOwner.lifecycleScope)
            try {
                display.refresh(errors)
                launch {
                    delay(5.seconds)
                    views.northReferenceIndicator.showLabel = false
                }
                launch {
                    while (true) {
                        delay(COMPASS_LAYER_UPDATE_INTERVAL)
                        display.updateCompassLayers(lifecycleOwner.lifecycleScope)
                    }
                }
                var lastRefresh = withFrameNanos { it }
                while (true) {
                    val frame = withFrameNanos { it }
                    if (frame - lastRefresh >= NAVIGATION_UPDATE_INTERVAL.inWholeNanoseconds) {
                        lastRefresh = frame
                        display.refresh(errors)
                    }
                }
            } finally {
                viewModel.cancelBeaconLoading()
                errors.reset()
                display.pause()
            }
        }
    }

    NavigatorHost(
        views = views,
        modifier = modifier.fillMaxSize()
    )

    if (viewModel.showCalibrationDialog) {
        NavigationCalibrationDialog(
            onClose = viewModel::closeCalibrationDialog
        )
    }

    if (viewModel.showLocationSheet) {
        TrailSenseSheet(
            onDismissRequest = viewModel::closeLocationSheet
        ) {
            LocationSheet(
                initialFormat = viewModel.locationFormat,
                getState = { format -> getLocationSheetState(context, formatService, viewModel, userPrefs, format) },
                onFormatSelected = { viewModel.locationFormat = it },
                onPickFormat = { currentFormat, onSelected ->
                    val formats = BuiltInCoordinateFormat.entries
                    Pickers.item(
                        context,
                        context.getString(R.string.pref_coordinate_format_title),
                        formats.map { formatService.formatCoordinateType(it) },
                        defaultSelectedIndex = formats.indexOf(currentFormat)
                    ) {
                        if (it != null) {
                            onSelected(formats[it])
                        }
                    }
                },
                onShare = {
                    scope.launch {
                        shareLocation(
                            context,
                            effects,
                            appNavigator,
                            viewModel.sensors.gps.location,
                            viewModel.locationFormat
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                onCopy = { text ->
                    com.kylecorry.andromeda.clipboard.Clipboard.copy(
                        context,
                        text,
                        context.getString(R.string.copied_to_clipboard_toast)
                    )
                }
            )
        }
    }
    if (viewModel.showAltitudeSheet) {
        TrailSenseSheet(
            onDismissRequest = viewModel::closeAltitudeSheet,
            title = stringResource(R.string.elevation_history)
        ) {
            AltitudeContent(
                historySummary = stringResource(
                    R.string.last_duration,
                    formatService.formatDuration(viewModel.altitudeHistoryDuration)
                ),
                readings = viewModel.altitudeChartReadings,
                units = userPrefs.baseDistanceUnits,
                onHistoryClick = { viewModel.showAltitudeHistoryDurationPicker = true },
                modifier = Modifier.fillMaxWidth()
            )
            if (viewModel.showAltitudeHistoryDurationPicker) {
                DurationPickerDialog(
                    title = stringResource(R.string.elevation_history_length),
                    duration = viewModel.altitudeHistoryDuration,
                    onDurationPicked = {
                        if (it != null) {
                            viewModel.changeAltitudeHistoryDuration(it)
                        }
                        viewModel.showAltitudeHistoryDurationPicker = false
                    },
                    onDismiss = {
                        viewModel.showAltitudeHistoryDurationPicker = false
                    }
                )
            }
        }
    }
    display.layerSheet.Content(
        context,
        { appNavigator.navigate(it) }
    )
}

private val NAVIGATION_UPDATE_INTERVAL = 33.milliseconds
private val COMPASS_LAYER_UPDATE_INTERVAL = 100.milliseconds

private fun getLocationSheetState(
    context: Context,
    formatService: FormatService,
    viewModel: NavigatorViewModel,
    userPrefs: UserPreferences,
    format: BuiltInCoordinateFormat
): LocationSheetState {
    val gps = viewModel.sensors.gps
    val accuracy = gps.horizontalAccuracy?.let {
        val accuracyStr = formatService.formatDistance(
            Distance.meters(it).convertTo(userPrefs.baseDistanceUnits)
        )
        "± $accuracyStr"
    }

    val timeAgo = (Clock.System.now() - gps.timestamp)
    return LocationSheetState(
        location = formatService.formatLocation(gps.location, format),
        coordinateFormat = formatService.formatCoordinateType(format),
        accuracy = accuracy,
        satellites = (gps.satellites ?: 0).toString(),
        time = context.getString(R.string.time_ago, formatService.formatDuration(timeAgo)),
        datum = context.getString(R.string.wgs84)
    )
}

/** Asks for a calibration before navigating, with a way to stop asking. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NavigationCalibrationDialog(onClose: (doNotAskAgain: Boolean) -> Unit) {
    var doNotAskAgain by remember { mutableStateOf(false) }
    val okText = stringResource(android.R.string.ok)
    AlertDialog(
        onDismissRequest = { onClose(false) },
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.calibrate_compass_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                Text(stringResource(R.string.calibrate_compass_on_navigate_dialog_content, okText))
                NavigationCalibrationDialogContent(
                    doNotAskAgainText = stringResource(R.string.do_not_ask_again),
                    onDoNotAskAgainChanged = { doNotAskAgain = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onClose(doNotAskAgain) }, shapes = ButtonDefaults.shapes()) {
                Text(okText)
            }
        }
    )
}

@Composable
private fun NavigationCalibrationDialogContent(
    doNotAskAgainText: String,
    onDoNotAskAgainChanged: (Boolean) -> Unit
) {
    var checked by remember { mutableStateOf(false) }

    fun updateChecked(value: Boolean) {
        checked = value
        onDoNotAskAgainChanged(value)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        CompassCalibrationContent(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    updateChecked(!checked)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = ::updateChecked
            )
            Text(
                text = doNotAskAgainText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


internal tailrec fun Context.findMainActivity(): MainActivity? = when (this) {
    is MainActivity -> this
    is ContextWrapper -> baseContext.findMainActivity()
    else -> null
}
