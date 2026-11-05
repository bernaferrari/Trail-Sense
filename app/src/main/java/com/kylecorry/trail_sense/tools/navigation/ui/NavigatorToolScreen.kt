package com.kylecorry.trail_sense.tools.navigation.ui

import com.kylecorry.trail_sense.shared.navigation.toRouteLocation


import com.kylecorry.trail_sense.shared.navigation.Route

import androidx.compose.ui.text.font.FontWeight
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.andromeda.sense.clinometer.Clinometer
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.sol.science.geography.projections.AzimuthalEquidistantProjection
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.CompassCalibrationContent
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.shared.debugging.DebugElevationsCommand
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersSheetState
import com.kylecorry.trail_sense.shared.map_layers.ui.MapAttributionState
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getAttribution
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.NorthReferenceBadgeState
import com.kylecorry.trail_sense.shared.views.SensorStatusBadgeView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration

import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationScreenLock
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.quickactions.NavigationQuickActionBinder
import com.kylecorry.trail_sense.tools.navigation.ui.data.UpdateAstronomyLayerCommand
import com.kylecorry.trail_sense.tools.navigation.ui.errors.NavigatorUserErrors
import com.kylecorry.trail_sense.tools.navigation.ui.layers.NavigationCompassLayerManager
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.BeaconCompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassView
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.MarkerCompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.NavigationCompassLayer
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
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
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.RouteLocation
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.tools.navigation.infrastructure.share.shareLocation
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigatorToolScreen(
    route: Route.Navigation,
    quickActionHost: TrailSenseDestinationHost,
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
        views.accuracyView.setOnClickListener { appNavigator.navigate(Route.CalibrateCompass) }
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

    // The dials redraw on timers while the screen is in front.
    LifecycleResumeEffect(display) {
        val updateTimer = CoroutineTimer { display.refresh(errors) }
        val compassLayerTimer = CoroutineTimer { display.updateCompassLayers(lifecycleOwner.lifecycleScope) }
        val northReferenceHideTimer = CoroutineTimer { views.northReferenceIndicator.showLabel = false }
        display.resume(lifecycleOwner.lifecycleScope)
        northReferenceHideTimer.once(Duration.ofSeconds(5))
        updateTimer.interval(NAVIGATION_UPDATE_INTERVAL)
        compassLayerTimer.interval(100)
        display.refresh(errors)
        onPauseOrDispose {
            viewModel.cancelBeaconLoading()
            errors.reset()
            updateTimer.stop()
            compassLayerTimer.stop()
            northReferenceHideTimer.stop()
            display.pause()
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

private const val NAVIGATION_UPDATE_INTERVAL = 33L

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

    val timeAgo = Duration.between(gps.eventTime, Instant.now())
    return LocationSheetState(
        location = formatService.formatLocation(gps.location, format),
        coordinateFormat = formatService.formatCoordinateType(format),
        accuracy = accuracy,
        satellites = (gps.satellites ?: 0).toString(),
        time = context.getString(R.string.time_ago, formatService.formatDuration(timeAgo)),
        datum = context.getString(R.string.wgs84)
    )
}

/** The compass's sensors, made once per screen from the app's sensor service. */
internal class NavigatorSensors(sensorService: SensorService, userPrefs: UserPreferences) {
    val orientation = sensorService.getOrientation()
    val compass = sensorService.getCompass(orientation)
    val gps = sensorService.getGPS(frequency = Duration.ofMillis(200))
    val clinometer = Clinometer(orientation, isAugmentedReality = true)
    val altimeter = sensorService.getAltimeter(gps = gps)
    val speedometer = sensorService.getSpeedometer(gps = gps)
    val declinationProvider = DeclinationFactory().getDeclinationStrategy(userPrefs, gps)
    val hasCompass = sensorService.hasCompass()
}

/**
 * The compass's state: the destination, the nearby beacons, the declination, the diagnostics, the elevation history
 * and which sheet or dialog is open. Plain Kotlin over the navigator, the repositories and the sensors; the dials and
 * the radar map are rendering objects that stay with the composition and read from here.
 */
internal class NavigatorViewModel(
    route: Route.Navigation,
    val sensors: NavigatorSensors,
    private val navigator: Navigator,
    private val beaconRepo: BeaconRepo,
    pathService: PathService,
    weatherRepo: WeatherRepo,
    private val prefs: NavigationPreferences,
    val useTrueNorth: Boolean,
    diagnostics: List<Flow<List<ToolDiagnosticResult>>>,
    private val debugElevations: suspend (List<Reading<Float>>, List<Reading<Float>>) -> Unit
) : ViewModel() {

    private val navigationService = NavigationService()
    private val loadBeaconsRunner = CoroutineQueueRunner()
    private val triggers = HookTriggers()
    private val styleChooser = CompassStyleChooser(prefs, sensors.hasCompass)

    val hasCompass: Boolean
        get() = sensors.hasCompass

    // Cached preferences
    private val isNearbyEnabled = prefs.showMultipleBeacons
    val isNearbyLinearOnly = prefs.showNearbyBeaconsOnlyOnLinearCompass
    private val nearbyCount = prefs.numberOfVisibleBeacons
    private val nearbyDistance
        get() = prefs.maxBeaconDistance

    private var beacons: Collection<Beacon> = listOf()

    @Volatile
    var nearbyBeacons: List<Beacon> = listOf()
        private set

    @Volatile
    var currentDestination: Destination? = null
        private set

    /** The destination as the navigator reports it. */
    val destination: Flow<Destination?> = navigator.destination2

    @Volatile
    var declination = 0f
        private set

    private var diagnosticResults by mutableStateOf<Map<Int, List<String>>>(emptyMap())

    /** The diagnostic codes the sensors report right now. */
    val diagnosticCodes: List<String>
        get() = diagnosticResults.values.flatten().distinct()

    private var checkCalibrationOnNextReading = route.navigationStarted

    // Sheets and dialogs
    var showCalibrationDialog by mutableStateOf(false)
        private set
    var showLocationSheet by mutableStateOf(false)
        private set
    var locationFormat by mutableStateOf(BuiltInCoordinateFormat.DecimalDegrees)
    var showAltitudeSheet by mutableStateOf(false)
        private set
    var showAltitudeHistoryDurationPicker by mutableStateOf(false)
    var altitudeChartReadings by mutableStateOf<List<Reading<Float>>>(emptyList())
        private set
    var altitudeHistoryDuration by mutableStateOf(MAX_ALTITUDE_HISTORY_DURATION)
        private set
    private var backtrackAltitudeReadings = listOf<Reading<Float>>()
    private var weatherAltitudeReadings = listOf<Reading<Float>>()

    private val _cancelNavigationRequests = Channel<Unit>(Channel.CONFLATED)

    /** One event each time a tap on the dial asks to cancel the current navigation. */
    val cancelNavigationRequests: Flow<Unit> = _cancelNavigationRequests.receiveAsFlow()

    private val keepAlive: () -> Boolean = { true }
    private val onCompassReading: () -> Boolean = {
        val compass = sensors.compass
        if (checkCalibrationOnNextReading && compass.quality != Quality.Unknown) {
            checkCalibrationOnNextReading = false
            if (compass.quality == Quality.Poor) {
                requestCalibrationDialog()
            }
        }
        true
    }

    init {
        // Load the destination and start navigation
        val beaconId = route.destinationBeaconId
        if (beaconId != 0L) {
            requestCalibrationDialog()
            navigator.navigateTo(beaconId)
        }

        viewModelScope.launch {
            navigator.destination2.collect {
                currentDestination = it
                updateNearbyBeacons()
            }
        }

        diagnostics.forEachIndexed { index, scan ->
            viewModelScope.launch {
                scan.collect { results ->
                    diagnosticResults = diagnosticResults + (index to results.map { it.id })
                }
            }
        }

        viewModelScope.launch {
            beaconRepo.getBeacons().collect {
                beacons = it
                updateNearbyBeacons()
            }
        }

        viewModelScope.launch {
            pathService.getRecentAltitudesLive(Instant.now().minus(MAX_ALTITUDE_FILTER_HISTORY_DURATION))
                .asFlow()
                .collect {
                    backtrackAltitudeReadings = it
                    updateAltitudeChart()
                }
        }

        viewModelScope.launch {
            weatherRepo.getAllLive().asFlow().collect { readings ->
                weatherAltitudeReadings = readings.mapNotNull { reading ->
                    if (reading.value.altitude == 0f) {
                        return@mapNotNull null
                    }
                    Reading(reading.value.altitude, reading.time)
                }
                updateAltitudeChart()
            }
        }
    }

    fun startSensors() {
        sensors.compass.start(onCompassReading)
        sensors.clinometer.start(keepAlive)
        sensors.altimeter.start(keepAlive)
        sensors.gps.start(keepAlive)
        sensors.speedometer.start(keepAlive)
    }

    fun stopSensors() {
        sensors.compass.stop(onCompassReading)
        sensors.clinometer.stop(keepAlive)
        sensors.altimeter.stop(keepAlive)
        sensors.gps.stop(keepAlive)
        sensors.speedometer.stop(keepAlive)
    }

    override fun onCleared() {
        stopSensors()
        loadBeaconsRunner.cancel()
    }

    fun cancelBeaconLoading() {
        loadBeaconsRunner.cancel()
    }

    private fun requestCalibrationDialog() {
        if (prefs.showCalibrationOnNavigateDialog) {
            showCalibrationDialog = true
        }
    }

    fun closeCalibrationDialog(doNotAskAgain: Boolean) {
        showCalibrationDialog = false
        if (doNotAskAgain) {
            prefs.showCalibrationOnNavigateDialog = false
        }
    }

    fun openLocationSheet(coordinateFormat: BuiltInCoordinateFormat) {
        locationFormat = coordinateFormat
        showLocationSheet = true
    }

    fun closeLocationSheet() {
        showLocationSheet = false
    }

    fun openAltitudeSheet() {
        updateAltitudeChart()
        showAltitudeSheet = true
    }

    fun closeAltitudeSheet() {
        showAltitudeSheet = false
    }

    fun changeAltitudeHistoryDuration(duration: Duration) {
        altitudeHistoryDuration = duration
        updateAltitudeChart()
    }

    fun setRadarScale(resolutionPixels: Float) {
        prefs.radarCompassScale = resolutionPixels
    }

    /** Where a place saved from here starts: the current location and elevation, once the GPS has a fix. */
    fun currentLocationForNewPlace(): RouteLocation? {
        val gps = sensors.gps
        if (!gps.hasValidReading) {
            return null
        }
        val altimeter = sensors.altimeter
        val altitude = if (altimeter.hasValidReading) altimeter.altitude else gps.altitude
        return gps.location.toRouteLocation(altitude)
    }

    /** A tap on the dial: aim at the heading, or ask to cancel what is being navigated to. */
    fun toggleDestinationBearing() {
        viewModelScope.launch {
            val destination = currentDestination
            if (destination is Destination.Beacon) {
                // TODO: Prompt to cancel navigation?
                // Don't set destination bearing while navigating to a beacon
                return@launch
            }

            if (destination == null && hasCompass) {
                // TODO: Wait for GPS location to be up to date (show a loading indicator)
                navigator.navigateToBearing(sensors.compass.rawBearing, sensors.gps.location)
            } else {
                _cancelNavigationRequests.trySend(Unit)
            }
        }
    }

    /** Called on each frame: refreshes the declination and the nearby beacons for the current location. */
    fun updateLocation() {
        declination = sensors.declinationProvider.getDeclination()
        sensors.compass.declination = declination
        updateNearbyBeacons()
    }

    /** Whether the sun and moon markers should be recomputed (every kilometre or minute). */
    fun shouldUpdateAstronomy(): Boolean {
        return triggers.distance(
            "astronomy",
            sensors.gps.location,
            ASTRONOMY_UPDATE_DISTANCE,
            highAccuracy = false
        ) || triggers.frequency("astronomy", ASTRONOMY_UPDATE_FREQUENCY)
    }

    /** Whether the radar should be redrawn this frame (once a second). */
    fun shouldInvalidateCompass(): Boolean {
        return triggers.frequency("compass_invalidation", Duration.ofSeconds(1))
    }

    /** The speed while moving; null standing still (a standing 0.0 km/h is noise). */
    fun movingSpeed(): Float? {
        return sensors.speedometer.speed.value.takeIf { it > 0.5f }
    }

    fun compassStyle(): CompassStyle {
        val deviceOrientation = if (sensors.clinometer.incline > -30) {
            DeviceOrientation.Orientation.Portrait
        } else {
            DeviceOrientation.Orientation.Flat
        }
        return styleChooser.getStyle(deviceOrientation)
    }

    /** What the destination card shows: the destination, or the beacon you face; and whether it is navigating. */
    fun sheetDestination(): Pair<Destination, Boolean>? {
        val destination = currentDestination
        val shown = destination ?: getSelectedBeacon(nearbyBeacons)?.let { Destination.Beacon(it) }
        return shown?.let { it to (destination != null) }
    }

    fun getDestinationBearing(): Float? {
        return currentDestination?.let { navigator.getBearing(sensors.gps.location, it).value }
    }

    private fun getSelectedBeacon(nearby: Collection<Beacon>): Beacon? {
        return (currentDestination as? Destination.Beacon)?.beacon ?: getFacingBeacon(nearby)
    }

    private fun getFacingBeacon(nearby: Collection<Beacon>): Beacon? {
        return navigationService.getFacingBeacon(
            sensors.gps.location,
            sensors.compass.rawBearing,
            nearby,
            declination,
            useTrueNorth
        )
    }

    private fun updateNearbyBeacons() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                loadBeaconsRunner.skipIfRunning {
                    val destinationBeacon = (currentDestination as? Destination.Beacon)?.beacon
                    if (!isNearbyEnabled) {
                        nearbyBeacons = listOfNotNull(destinationBeacon)
                        return@skipIfRunning
                    }

                    nearbyBeacons = (navigationService.getNearbyBeacons(
                        sensors.gps.location,
                        beacons,
                        nearbyCount,
                        8f,
                        nearbyDistance
                    ) + listOfNotNull(destinationBeacon)).distinctBy { it.id }
                }
            }
        }
    }

    private fun updateAltitudeChart() {
        viewModelScope.launch {
            val currentAltitude = Reading(sensors.altimeter.altitude, Instant.now())
            altitudeChartReadings = withContext(Dispatchers.Default) {
                val readings =
                    (backtrackAltitudeReadings + weatherAltitudeReadings + currentAltitude).sortedBy { it.time }
                        .filter {
                            Duration.between(
                                it.time,
                                Instant.now()
                            ) < MAX_ALTITUDE_FILTER_HISTORY_DURATION
                        }

                val smoothed = DataUtils.smoothTemporal(readings, 0.3f)

                withContext(Dispatchers.IO) {
                    debugElevations(readings, smoothed)
                }

                smoothed.filter {
                    Duration.between(it.time, Instant.now()).abs() <= altitudeHistoryDuration
                }
            }
        }
    }

    companion object {
        private val ASTRONOMY_UPDATE_DISTANCE = Distance.kilometers(1f).meters()
        private val ASTRONOMY_UPDATE_FREQUENCY = Duration.ofMinutes(1)
        private val MAX_ALTITUDE_HISTORY_DURATION = Duration.ofDays(1)
        private val MAX_ALTITUDE_FILTER_HISTORY_DURATION = MAX_ALTITUDE_HISTORY_DURATION.plusHours(6)
    }
}

/**
 * The compass's rendering objects (the dials, the radar map and its layers, the destination card) and how a frame of
 * the ViewModel's state is drawn on them. It holds no state of its own beyond what drawing needs.
 */
private class NavigatorDisplay(
    private val context: Context,
    private val viewModel: NavigatorViewModel,
    private val userPrefs: UserPreferences,
    private val formatService: FormatService,
    onNorthReferenceSettingsClick: () -> Unit,
    requestCameraPermission: ((Boolean) -> Unit) -> Unit,
    onCameraPermissionDenied: () -> Unit,
    onOpenBeacon: (Long) -> Unit
) {
    val views = createNavigatorViews(
        context,
        onNorthReferenceSettingsClick = onNorthReferenceSettingsClick,
        requestCameraPermission = requestCameraPermission,
        onCameraPermissionDenied = onCameraPermissionDenied,
        onOpenBeacon = onOpenBeacon
    )

    val layers = NavigationCompassLayerManager()
    val layerSheet = MapLayersSheetState(NavigationToolRegistration.MAP_ID)

    // Compass layers
    private val beaconCompassLayer = BeaconCompassLayer()
    private val astronomyCompassLayer = MarkerCompassLayer()
    private val navigationCompassLayer = NavigationCompassLayer()

    private val updateAstronomyLayerCommand = UpdateAstronomyLayerCommand(
        astronomyCompassLayer,
        userPrefs,
        viewModel.sensors.gps
    ) { viewModel.declination }

    private val baseDistanceUnits = userPrefs.baseDistanceUnits
    private var lastErrorCodes: List<String> = emptyList()
    private var lastAttributionLayerKey: Int? = null
    private var lastAttributionRadarVisible: Boolean? = null
    private var scope: CoroutineScope? = null

    init {
        val sensors = viewModel.sensors
        views.linearCompass.setCompassLayers(
            listOf(
                astronomyCompassLayer,
                beaconCompassLayer,
                navigationCompassLayer
            )
        )
        views.radarCompass.setCompassLayers(
            listOfNotNull(
                astronomyCompassLayer,
                if (viewModel.isNearbyLinearOnly) null else beaconCompassLayer,
                navigationCompassLayer
            )
        )

        views.accuracyView.setSensors(sensors.gps, sensors.compass)

        val map = views.radarCompassMap
        map.isPanEnabled = false
        map.isFlingEnabled = false
        // No disc of its own: the radar's paths and places draw straight onto the dial.
        map.backgroundColorOverride = null
        map.minScale = 0.001f
        map.mapCenter = sensors.gps.location
        map.metersPerProjectedUnit = 1.0
        map.latitudeScaleFactor = { 1f }
        map.projection = AzimuthalEquidistantProjection(sensors.gps.location)
        map.resolutionPixels = userPrefs.navigation.radarCompassScale
        map.setOnScaleChangeListener(true) { resolutionPixels ->
            val radiusMeters = resolutionPixels * map.width / 2f
            views.radarCompass.setRadiusDistance(Distance.meters(radiusMeters))
            viewModel.setRadarScale(resolutionPixels)
        }

        if (!viewModel.hasCompass) {
            views.radarCompass.shouldDrawDial = userPrefs.navigation.showDialTicksWhenNoCompass
            views.radarCompass.shouldDrawAzimuthIndicator = false
            views.navigationTitle.isTitleVisible = false
            views.northReferenceIndicator.isVisible = false
        } else {
            views.radarCompass.shouldDrawAzimuthIndicator = userPrefs.navigation.showAzimuthIndicator
        }
    }

    fun resume(scope: CoroutineScope) {
        this.scope = scope
        views.radarCompassMap.useDensityPixelsForZoom = !userPrefs.navigation.highDetailMode
        layers.resume(views.radarCompassMap, scope)
        views.radarCompass.bindMapView(views.radarCompassMap)

        // Show the north reference indicator
        views.northReferenceIndicator.showDetailsOnClick = true
        views.northReferenceIndicator.useTrueNorth = viewModel.useTrueNorth
        views.northReferenceIndicator.showLabel = true
        lastErrorCodes = emptyList()
    }

    fun pause() {
        layerSheet.setOnDismissListener(null)
        layerSheet.dismiss()
        layers.pause(views.radarCompassMap)
        scope = null
    }

    fun refresh(errors: NavigatorUserErrors) {
        val sensors = viewModel.sensors
        updateNavigationSheet()
        // Speed only while moving: a standing 0.0 km/h is noise.
        views.speed.title = viewModel.movingSpeed()?.let { formatService.formatSpeed(it) } ?: ""
        updateCompassBearing()

        if (viewModel.shouldInvalidateCompass()) {
            views.radarCompass.invalidate()
        }

        views.altitude.title = formatService.formatDistance(
            Distance.meters(sensors.altimeter.altitude).convertTo(baseDistanceUnits)
        )
        updateLocation()

        if (viewModel.shouldUpdateAstronomy()) {
            scope?.launch {
                if (sensors.gps.location != Coordinate.zero) {
                    updateAstronomyLayerCommand.execute()
                }
            }
        }

        updateMapAttributionIfNeeded()
        updateCompassStyle()
        updateQuickActionAvailability()

        val codes = viewModel.diagnosticCodes
        if (codes != lastErrorCodes) {
            lastErrorCodes = codes
            errors.update(codes)
        }
    }

    private fun updateNavigationSheet() {
        val sensors = viewModel.sensors
        val shown = viewModel.sheetDestination()
        if (shown != null) {
            // The heading too, so the card's arrow turns with you (it pointed at 0° forever without it).
            views.navigationSheet.updateNavigationSensorValues(
                sensors.gps.location,
                sensors.altimeter.altitude,
                sensors.speedometer.speed.value,
                viewModel.declination,
                sensors.compass.rawBearing
            )
            views.navigationSheet.show(shown.first, shown.second)
        } else {
            views.navigationSheet.hide()
        }
    }

    private fun updateMapAttributionIfNeeded() {
        val isRadarVisible = views.radarCompass.isVisible
        if (lastAttributionLayerKey == layers.key && lastAttributionRadarVisible == isRadarVisible) {
            return
        }

        lastAttributionLayerKey = layers.key
        lastAttributionRadarVisible = isRadarVisible

        scope?.launch {
            if (isRadarVisible) {
                val attribution = views.radarCompassMap.getAttribution(context)
                views.mapAttribution.text = attribution
                views.mapAttribution.isVisible = attribution != null
            } else {
                views.mapAttribution.isVisible = false
            }
        }
    }

    private fun updateCompassStyle() {
        val style = viewModel.compassStyle()
        views.linearCompass.isInvisible = style != CompassStyle.Linear
        views.radarCompass.isInvisible = style != CompassStyle.Radar
        views.radarCompassMap.isInvisible = style != CompassStyle.Radar
    }

    private fun updateQuickActionAvailability() {
        // TODO: Extract this logic to the flashlight (if camera is in use)
        if (userPrefs.navigation.rightButton == Tools.QUICK_ACTION_FLASHLIGHT) {
            views.navigationTitle.rightButton.isClickable = !views.linearCompass.isCameraActive
        }
        if (userPrefs.navigation.leftButton == Tools.QUICK_ACTION_FLASHLIGHT) {
            views.navigationTitle.leftButton.isClickable = !views.linearCompass.isCameraActive
        }
    }

    private fun updateCompassBearing() {
        val compass = viewModel.sensors.compass
        val bearing = compass.rawBearing
        val declination = viewModel.declination

        // Azimuth
        if (viewModel.hasCompass) {
            views.navigationTitle.title = formatService.formatDegrees(bearing, replace360 = true)
            views.navigationTitle.shortDirection = formatService.formatDirection(CompassDirection.nearest(bearing))
            // Spelled out: the dial already shows the letters, the readout says it the way a person would.
            views.navigationTitle.direction = context.getString(
                when (CompassDirection.nearest(bearing)) {
                    CompassDirection.North -> R.string.direction_north_full
                    CompassDirection.NorthEast -> R.string.direction_north_east_full
                    CompassDirection.East -> R.string.direction_east_full
                    CompassDirection.SouthEast -> R.string.direction_south_east_full
                    CompassDirection.South -> R.string.direction_south_full
                    CompassDirection.SouthWest -> R.string.direction_south_west_full
                    CompassDirection.West -> R.string.direction_west_full
                    CompassDirection.NorthWest -> R.string.direction_north_west_full
                }
            )
        }

        // Compass
        listOf<ICompassView>(
            views.radarCompass,
            views.linearCompass
        ).forEach {
            it.azimuth = bearing
            it.declination = declination
        }

        val actualBearing = if (viewModel.useTrueNorth) {
            compass.bearing
        } else {
            compass.bearing.withDeclination(declination)
        }
        views.radarCompassMap.userAzimuth = actualBearing
        views.radarCompassMap.mapAzimuth = actualBearing.value
    }

    private fun updateLocation() {
        val gps = viewModel.sensors.gps
        val location = gps.location

        viewModel.updateLocation()

        // Readable at a glance: hemispheres instead of minus signs, 4 decimals (about 10 m) for decimal degrees; other
        // formats (UTM, MGRS, DMS…) as the person chose them.
        views.navigationTitle.subtitle = if (userPrefs.coordinateFormat == BuiltInCoordinateFormat.DecimalDegrees) {
            val lat = "%.4f° %s".format(kotlin.math.abs(location.latitude), if (location.latitude >= 0) "N" else "S")
            val lon = "%.4f° %s".format(kotlin.math.abs(location.longitude), if (location.longitude >= 0) "E" else "W")
            "$lat  $lon"
        } else {
            formatService.formatLocation(location)
        }

        views.radarCompassMap.userLocationAccuracy = gps.horizontalAccuracy?.let { Distance.meters(it) }

        // Compass center point
        listOf<ICompassView>(
            views.radarCompass,
            views.linearCompass
        ).forEach {
            it.compassCenter = location
        }

        views.radarCompassMap.mapCenter = location
        views.radarCompassMap.userLocation = location
        views.radarCompassMap.projection = AzimuthalEquidistantProjection(location)
    }

    fun updateCompassLayers(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            val destBearing = viewModel.getDestinationBearing()
            val destinationBeacon = (viewModel.currentDestination as? Destination.Beacon)?.beacon
            val destColor = destinationBeacon?.color ?: AppColor.Blue.color

            val direction = destBearing?.let {
                MappableBearing(it, destColor)
            }

            // Update beacon layers
            beaconCompassLayer.setBeacons(viewModel.nearbyBeacons)
            beaconCompassLayer.highlight(destinationBeacon)

            // Destination
            if (destinationBeacon != null) {
                navigationCompassLayer.setDestination(destinationBeacon)
            } else if (direction != null) {
                navigationCompassLayer.setDestination(direction)
            } else {
                navigationCompassLayer.setDestination(null as MappableBearing?)
            }
        }
    }
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

private data class NavigatorViews(
    val navigationTitle: NavigationTitleState,
    val altitude: NavigatorDataPointState,
    val speed: NavigatorDataPointState,
    val radarCompassMap: MapView,
    val radarCompass: RadarCompassView,
    val linearCompass: LinearSightingCompassView,
    val mapAttribution: MapAttributionState,
    val northReferenceIndicator: NorthReferenceBadgeState,
    val accuracyView: SensorStatusBadgeView,
    val navigationSheet: NavigationSheetView
) {
    var onBeaconClick by mutableStateOf<(() -> Unit)?>(null)
    var onBeaconLongClick by mutableStateOf<(() -> Unit)?>(null)
}

private class NavigationTitleState {
    val leftButton = ComposeQuickActionButtonView()
    val rightButton = ComposeQuickActionButtonView()
    var title by mutableStateOf("")
    var direction by mutableStateOf("")
    var shortDirection by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var isTitleVisible by mutableStateOf(true)
    var onSubtitleClick by mutableStateOf<(() -> Unit)?>(null)
    var onSubtitleLongClick by mutableStateOf<(() -> Boolean)?>(null)
}

private class NavigatorDataPointState(
    private val icon: ImageVector
) {
    var title by mutableStateOf("")
    var onClick by mutableStateOf<(() -> Unit)?>(null)

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        // Quiet supporting text, not a chip: altitude and speed support the heading, they don't compete with it.
        Text(
            text = title,
            modifier = modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(TrailSenseSpacing.sm))
                .clickable(enabled = onClick != null) { onClick?.invoke() }
                .padding(horizontal = TrailSenseSpacing.xs, vertical = TrailSenseSpacing.xxs),
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavigationTitle(
    state: NavigationTitleState,
    modifier: Modifier = Modifier
) {
    if (!state.isTitleVisible) return
    // The heading sits inside the dial, like a watch face: the number on the lubber line, the direction under it.
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // The number is centred on its own; the degree sign hangs to its right (an invisible twin on the left
        // balances it), so "40°" never sits left of centre.
        val digits = state.title.removeSuffix("°")
        val headingStyle = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum")
        val headingColor = if (DialVariant.hub != 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        Row {
            Text("°", style = headingStyle, fontWeight = FontWeight.Medium, color = androidx.compose.ui.graphics.Color.Transparent, maxLines = 1)
            Text(digits, style = headingStyle, fontWeight = FontWeight.Medium, color = headingColor, maxLines = 1)
            Text("°", style = headingStyle, fontWeight = FontWeight.Medium, color = headingColor, maxLines = 1)
        }
        Text(
            text = state.direction,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            fontWeight = FontWeight.Medium,
            color = if (DialVariant.hub != 0) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** The classic readout under the dial: "336° NW", the degrees large, the direction letters beside them. */
@Composable
private fun NavigationClassicHeading(state: NavigationTitleState, modifier: Modifier = Modifier) {
    if (!state.isTitleVisible) return
    Text(
        text = androidx.compose.ui.text.buildAnnotatedString {
            append(state.title)
            if (state.shortDirection.isNotEmpty()) {
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant))
                append(" ")
                append(state.shortDirection)
                pop()
            }
        },
        modifier = modifier,
        style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1
    )
}

/** Where you are: the coordinates, then altitude and speed in one quiet line. Each part opens its own detail. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavigationWhereabouts(
    state: NavigationTitleState,
    altitude: NavigatorDataPointState,
    speed: NavigatorDataPointState,
    modifier: Modifier = Modifier
) {
    // Two calm lines, Apple Compass-style: where you are, then how high (and how fast, while moving).
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = state.subtitle,
            modifier = Modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(TrailSenseSpacing.sm))
                .combinedClickable(
                    onClick = { state.onSubtitleClick?.invoke() },
                    onLongClick = { state.onSubtitleLongClick?.invoke() }
                )
                .padding(horizontal = TrailSenseSpacing.sm, vertical = TrailSenseSpacing.xxs),
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (altitude.title.isNotEmpty()) {
                Text(
                    stringResource(R.string.navigation_elevation_value, altitude.title),
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(TrailSenseSpacing.sm))
                        .clickable(enabled = altitude.onClick != null) { altitude.onClick?.invoke() }
                        .padding(horizontal = TrailSenseSpacing.xs, vertical = TrailSenseSpacing.xxs),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (speed.title.isNotEmpty()) {
                NavigationDot()
                speed.Content()
            }
        }
    }
}

@Composable
private fun NavigationDot() {
    Text(
        text = "·",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun NavigatorHost(
    views: NavigatorViews,
    modifier: Modifier = Modifier
) {
    // Which way am I facing, and which way do I go? One object answers: the dial, with the heading in a cookie at its
    // centre that turns as you turn. Everything else is quiet: sensor trouble in the top bar's empty corner, the quick
    // actions as plain icons, one centred where-you-are line, and one labelled action.
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TrailSenseSpacing.lg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    views.accuracyView.Content(quiet = true)
                }
                views.navigationTitle.leftButton.Content(offContainerColor = androidx.compose.ui.graphics.Color.Transparent)
                views.navigationTitle.rightButton.Content(offContainerColor = androidx.compose.ui.graphics.Color.Transparent)
            }
            // The dial and where-you-are are one group, centred in all the room between the top bar and the actions;
            // the dial takes nearly the full width.
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // The dial is the screen's one object: it runs past the page margins to nearly the full width.
                val compassSize = minOf(maxWidth, maxHeight - 72.dp)
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(compassSize)) {
                        val radarModifier = Modifier.fillMaxSize()
                        views.radarCompass.Content(radarModifier)
                        // Nearby paths are a quiet hint under the dial's marks, not a stripe across it.
                        views.radarCompassMap.Content(radarModifier.then(Modifier.alpha(0.4f)))
                        if (DialVariant.bezel != 5 || DialVariant.hub != 0) {
                            NavigationTitle(views.navigationTitle, Modifier.align(Alignment.Center))
                        }
                        views.mapAttribution.Content(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        )
                    }
                    if (DialVariant.bezel == 5 && DialVariant.hub == 0) {
                        NavigationClassicHeading(views.navigationTitle, Modifier.padding(top = TrailSenseSpacing.md))
                    }
                    NavigationWhereabouts(
                        views.navigationTitle,
                        views.altitude,
                        views.speed,
                        Modifier.padding(top = TrailSenseSpacing.xs)
                    )
                }
                views.linearCompass.Content(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                )
            }
            views.navigationSheet.Content()
            // The two things most people come here for: go back to a place they saved, or save where they stand
            // (the car, the camp). Two plain, labelled buttons; no hidden long press.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.xs, bottom = TrailSenseSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
            ) {
                com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
                    text = stringResource(R.string.navigation_places),
                    onClick = { views.onBeaconClick?.invoke() },
                    icon = TrailSenseIcons.Filled.LocationOn,
                    style = com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Tonal,
                    modifier = Modifier.weight(1f)
                )
                com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
                    text = stringResource(R.string.navigation_save_spot),
                    onClick = { views.onBeaconLongClick?.invoke() },
                    icon = TrailSenseIcons.Filled.Add,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun createNavigatorViews(
    context: Context,
    onNorthReferenceSettingsClick: (() -> Unit)? = null,
    requestCameraPermission: (((Boolean) -> Unit) -> Unit)? = null,
    onCameraPermissionDenied: (() -> Unit)? = null,
    onOpenBeacon: ((Long) -> Unit)? = null
): NavigatorViews {
    val title = NavigationTitleState()
    val altitude = NavigatorDataPointState(TrailSenseIcons.Filled.Straighten)
    val speed = NavigatorDataPointState(TrailSenseIcons.Filled.Speed)
    val radarCompassMap = MapView(context).apply {
        id = R.id.radar_compass_map
    }

    val radarCompass = RadarCompassView(context).apply {
        isInvisible = true
    }

    val linearCompass = LinearSightingCompassView(context).apply {
        this.requestCameraPermission = requestCameraPermission
        this.onCameraPermissionDenied = onCameraPermissionDenied
    }

    val mapAttribution = MapAttributionState()
    val northReferenceIndicator = NorthReferenceBadgeState(context).apply {
        onSettingsClick = onNorthReferenceSettingsClick
    }

    val accuracyView = SensorStatusBadgeView(context).apply {
        alpha = 0.6f
    }

    val navigationSheet = NavigationSheetView(context).apply {
        this.onOpenBeacon = onOpenBeacon
    }

    return NavigatorViews(
        navigationTitle = title,
        altitude = altitude,
        speed = speed,
        radarCompassMap = radarCompassMap,
        radarCompass = radarCompass,
        linearCompass = linearCompass,
        mapAttribution = mapAttribution,
        northReferenceIndicator = northReferenceIndicator,
        accuracyView = accuracyView,
        navigationSheet = navigationSheet
    )
}

/** The beacon button: a standard tonal FAB; long press navigates to the last beacon. */
@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NavigatorIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // An extended FAB that says what it does (ChangeDetection's "Watch a page"): open the beacons; a long press
    // places a beacon here.
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    androidx.compose.material3.Surface(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .combinedClickable(
                onClickLabel = label,
                onLongClickLabel = stringResource(R.string.create_beacon),
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NavigatorHostPreview() {
    TrailSenseTheme {
        NavigatorHost(
            views = createNavigatorViews(LocalContext.current)
        )
    }
}

private tailrec fun Context.findMainActivity(): MainActivity? = when (this) {
    is MainActivity -> this
    is ContextWrapper -> baseContext.findMainActivity()
    else -> null
}
