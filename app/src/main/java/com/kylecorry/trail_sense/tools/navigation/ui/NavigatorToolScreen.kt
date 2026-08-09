package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.observe
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.trail_sense.shared.extensions.asLiveData
import com.kylecorry.andromeda.sense.clinometer.Clinometer
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.concurrency.onMain
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
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.NorthReferenceBadgeState
import com.kylecorry.trail_sense.shared.views.SensorStatusBadgeView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.navigation.domain.CompassStyle
import com.kylecorry.trail_sense.tools.navigation.domain.CompassStyleChooser
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

@Composable
fun NavigatorToolScreen(
    host: TrailSenseDestinationHost,
    modifier: Modifier = Modifier
) {
    val controller = remember(host) { NavigatorController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.onCreate()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            controller.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onDestroy()
        }
    }

    controller.Content(modifier = modifier.fillMaxSize())
}

private class NavigatorController(private val host: TrailSenseDestinationHost) {

    private val views = createNavigatorViews(
        host.requireContext(),
        onNorthReferenceSettingsClick = {
            host.findNavController().navigate(R.id.calibrateCompassFragment)
        },
        requestCameraPermission = { callback -> host.requestCamera(callback) },
        onCameraPermissionDenied = { host.alertNoCameraPermission() },
        onOpenBeacon = { beaconId ->
            host.findNavController().navigateWithAnimation(
                R.id.beaconDetailsFragment,
                Bundle().apply {
                    putLong("beacon_id", beaconId)
                }
            )
        }
    )
    private val isViewReady: Boolean
        get() = isCreated

    private val orientation by lazy { sensorService.getOrientation() }
    private val compass by lazy { sensorService.getCompass(orientation) }
    private val gps by lazy { sensorService.getGPS(frequency = Duration.ofMillis(200)) }
    private val clinometer by lazy { Clinometer(orientation, isAugmentedReality = true) }
    private val altimeter by lazy { sensorService.getAltimeter(gps = gps) }
    private val speedometer by lazy { sensorService.getSpeedometer(gps = gps) }
    private val declinationProvider by lazy {
        DeclinationFactory().getDeclinationStrategy(
            userPrefs,
            gps
        )
    }
    private var declination = 0f

    private val userPrefs by lazy { UserPreferences(host.requireContext()) }

    private val beaconRepo by lazy { BeaconRepo.getInstance(host.requireContext()) }

    private val sensorService by lazy { SensorService(host.requireContext()) }
    private val pathService by lazy { PathService.getInstance(host.requireContext()) }
    private val weatherRepo by lazy { WeatherRepo.getInstance(host.requireContext()) }

    private val navigationService = NavigationService()
    private val formatService by lazy { FormatService.getInstance(host.requireContext()) }

    private var beacons: Collection<Beacon> = listOf()
    private var nearbyBeacons: List<Beacon> = listOf()

    private var destination: Destination? = null
    private val navigator by lazy { Navigator.getInstance(host.requireContext()) }

    // Diagnostics
    private val errors by lazy { NavigatorUserErrors(host, this::displayAccuracyTips) }

    // Data commands
    private val updateAstronomyLayerCommand by lazy {
        UpdateAstronomyLayerCommand(
            astronomyCompassLayer,
            userPrefs,
            gps
        ) { declination }
    }

    private val loadBeaconsRunner = CoroutineQueueRunner()

    private val layers = NavigationCompassLayerManager()
    private val layerSheet = MapLayersSheetState(NavigationToolRegistration.MAP_ID)

    // Compass layers
    private val beaconCompassLayer = BeaconCompassLayer()
    private val astronomyCompassLayer = MarkerCompassLayer()
    private val navigationCompassLayer = NavigationCompassLayer()

    // Feature availability
    private val hasCompass by lazy { sensorService.hasCompass() }

    // Cached preferences
    private val baseDistanceUnits by lazy { userPrefs.baseDistanceUnits }
    private val isNearbyEnabled by lazy { userPrefs.navigation.showMultipleBeacons }
    private val isNearbyLinearOnly by lazy { userPrefs.navigation.showNearbyBeaconsOnlyOnLinearCompass }
    private val nearbyCount by lazy { userPrefs.navigation.numberOfVisibleBeacons }
    private val nearbyDistance
        get() = userPrefs.navigation.maxBeaconDistance
    private val styleChooser by lazy { CompassStyleChooser(userPrefs.navigation, hasCompass) }
    private val useTrueNorth by lazy { userPrefs.compass.useTrueNorth }
    private val screenLock by lazy { NavigationScreenLock(userPrefs.navigation.keepScreenUnlockedWhileOpen) }

    // State
    private var diagnosticResults: Map<Int, List<String>> = emptyMap()
    private var lastErrorCodes: List<String> = emptyList()
    private var lastAttributionLayerKey: Int? = null
    private var lastAttributionRadarVisible: Boolean? = null
    private var showLocationSheet by mutableStateOf(false)
    private var locationFormat = BuiltInCoordinateFormat.DecimalDegrees
    private var showAltitudeSheet by mutableStateOf(false)
    private var showAltitudeHistoryDurationPicker by mutableStateOf(false)
    private var altitudeHistorySummary by mutableStateOf("")
    private var altitudeChartReadings by mutableStateOf<List<Reading<Float>>>(emptyList())
    private var altitudeUnits = baseDistanceUnits
    private var altitudeHistoryDuration = MAX_ALTITUDE_HISTORY_DURATION
    private var backtrackAltitudeReadings = listOf<Reading<Float>>()
    private var weatherAltitudeReadings = listOf<Reading<Float>>()

    private val northReferenceHideTimer = CoroutineTimer {
        if (isViewReady) {
            views.northReferenceIndicator.showLabel = false
        }
    }

    private val compassLayerTimer = CoroutineTimer {
        updateCompassLayers()
    }

    private val updateTimer = CoroutineTimer {
        refreshNavigation()
    }

    private val triggers = HookTriggers()
    private var isCreated = false

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(modifier: Modifier = Modifier) {
        NavigatorHost(
            views = views,
            modifier = modifier
        )
        if (showLocationSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showLocationSheet = false
                }
            ) {
                LocationSheet(
                    initialFormat = locationFormat,
                    getState = ::getLocationSheetState,
                    onFormatSelected = {
                        locationFormat = it
                    },
                    onPickFormat = ::pickLocationFormat,
                    onShare = {
                        Share.shareLocation(host, gps.location, locationFormat)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (showAltitudeSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showAltitudeSheet = false
                }
            ) {
                val historyDurationTitle = host.getString(R.string.elevation_history_length)
                AltitudeContent(
                    historySummary = altitudeHistorySummary,
                    readings = altitudeChartReadings,
                    units = altitudeUnits,
                    onHistoryClick = {
                        showAltitudeHistoryDurationPicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (showAltitudeHistoryDurationPicker) {
                    DurationPickerDialog(
                        title = historyDurationTitle,
                        duration = altitudeHistoryDuration,
                        onDurationPicked = {
                            if (it != null) {
                                altitudeHistoryDuration = it
                                updateAltitudeChart()
                            }
                            showAltitudeHistoryDurationPicker = false
                        },
                        onDismiss = {
                            showAltitudeHistoryDurationPicker = false
                        }
                    )
                }
            }
        }
        layerSheet.Content(
            host.requireContext(),
            { host.findNavController().navigateWithAnimation(it) }
        )
    }

    fun onCreate() {
        if (isCreated) {
            return
        }
        isCreated = true
        val beaconId = host.destinationArguments?.getLong("destination") ?: 0L

        // Load the destination and start navigation
        if (beaconId != 0L) {
            showCalibrationDialog()
            navigator.navigateTo(beaconId)
        }

        host.lifecycleScope.launch {
            navigator.destination2.collect {
                destination = it
                updateNearbyBeacons()
                handleShowWhenLocked()
            }
        }

        // Observe diagnostics
        listOf(
            GPSDiagnosticScanner(gps),
            MagnetometerDiagnosticScanner()
        ).mapIndexed { index, scanner ->
            host.lifecycleScope.launch {
                scanner.fullScan(host.requireContext()).collect { results ->
                    diagnosticResults = diagnosticResults + (index to results.map { it.id })
                }
            }
        }

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
                if (isNearbyLinearOnly) null else beaconCompassLayer,
                navigationCompassLayer
            )
        )


        NavigationQuickActionBinder(
            host,
            views.navigationTitle.leftButton,
            views.navigationTitle.rightButton,
            userPrefs.navigation
        ).bind()

        host.lifecycleScope.launch {
            beaconRepo.getBeacons().collect {
                beacons = it
                updateNearbyBeacons()
            }
        }

        compass.asLiveData().observe(host.lifecycleOwner) { }
        clinometer.asLiveData().observe(host.lifecycleOwner) { }
        altimeter.asLiveData().observe(host.lifecycleOwner) { }
        gps.asLiveData().observe(host.lifecycleOwner) { }
        speedometer.asLiveData().observe(host.lifecycleOwner) { }
        getBacktrackAltitudeReadings().observe(host.lifecycleOwner) {
            backtrackAltitudeReadings = it
            updateAltitudeChart()
        }
        getWeatherAltitudeReadings().observe(host.lifecycleOwner) {
            weatherAltitudeReadings = it
            updateAltitudeChart()
        }

        views.navigationTitle.onSubtitleLongClick = {
            Share.shareLocation(
                host,
                gps.location
            )
            true
        }

        views.navigationTitle.onSubtitleClick = {
            locationFormat = userPrefs.navigation.coordinateFormat
            showLocationSheet = true
        }

        views.altitude.onClick = {
            altitudeUnits = baseDistanceUnits
            altitudeHistorySummary = formatAltitudeHistoryDuration()
            updateAltitudeChart()
            showAltitudeSheet = true
        }

        views.linearCompass.setOnClickListener {
            toggleDestinationBearing()
        }

        views.onBeaconClick = {
            host.findNavController().openTool(Tools.BEACONS)
        }

        views.onBeaconLongClick = {
            if (gps.hasValidReading) {
                val bundle = Bundle().apply {
                    putParcelable("initial_location", GeoUri(
                        gps.location,
                        if (altimeter.hasValidReading) altimeter.altitude else gps.altitude
                    ))
                }
                host.findNavController().openTool(Tools.BEACONS, bundle)
            } else {
                host.findNavController().openTool(Tools.BEACONS)
            }
        }

        views.accuracyView.setSensors(gps, compass)
        views.accuracyView.setOnClickListener { displayAccuracyTips() }

        views.radarCompassMap.setOnSingleTapListener {
            toggleDestinationBearing()
        }

        views.radarCompassMap.setOnLongPressListener {
            layerSheet.dismiss()
            layers.pause(views.radarCompassMap)
            layerSheet.setOnDismissListener {
                layers.resume(host.requireContext(), views.radarCompassMap, host)
            }
            layerSheet.show()
        }

        views.linearCompass.setOnClickListener {
            toggleDestinationBearing()
        }

        views.radarCompassMap.isPanEnabled = false
        views.radarCompassMap.isFlingEnabled = false
        views.radarCompassMap.backgroundColorOverride =
            Resources.color(host.requireContext(), R.color.colorSecondary)
        views.radarCompassMap.minScale = 0.001f
        views.radarCompassMap.mapCenter = gps.location
        views.radarCompassMap.metersPerProjectedUnit = 1.0
        views.radarCompassMap.latitudeScaleFactor = { 1f }
        views.radarCompassMap.projection = AzimuthalEquidistantProjection(gps.location)
        views.radarCompassMap.resolutionPixels = userPrefs.navigation.radarCompassScale
        views.radarCompassMap.setOnScaleChangeListener(true) { resolutionPixels ->
            val radiusMeters = resolutionPixels * views.radarCompassMap.width / 2f
            views.radarCompass.setRadiusDistance(Distance.meters(radiusMeters))
            userPrefs.navigation.radarCompassScale = resolutionPixels
        }

        if (!hasCompass) {
            views.radarCompass.shouldDrawDial = userPrefs.navigation.showDialTicksWhenNoCompass
            views.radarCompass.shouldDrawAzimuthIndicator = false
            views.navigationTitle.isTitleVisible = false
            views.northReferenceIndicator.isVisible = false
        } else {
            views.radarCompass.shouldDrawAzimuthIndicator =
                userPrefs.navigation.showAzimuthIndicator
        }

        refreshNavigation()
    }

    private fun toggleDestinationBearing() {
        host.inBackground {
            if (destination is Destination.Beacon) {
                // TODO: Prompt to cancel navigation?
                // Don't set destination bearing while navigating to a beacon
                return@inBackground
            }

            if (destination == null && hasCompass) {
                // TODO: Wait for GPS location to be up to date (show a loading indicator)
                navigator.navigateToBearing(compass.rawBearing, gps.location)
            } else {
                onMain {
                    if (isViewReady) {
                        views.navigationSheet.requestCancelNavigation()
                    }
                }
            }
        }
    }

    private fun handleShowWhenLocked() {
        screenLock.updateLock(host.mainActivity)
    }

    fun displayAccuracyTips() {
        host.findNavController().navigate(R.id.calibrateCompassFragment)
    }

    private fun pickLocationFormat(
        currentFormat: BuiltInCoordinateFormat,
        onSelected: (BuiltInCoordinateFormat) -> Unit
    ) {
        val formats = BuiltInCoordinateFormat.entries
        val formatStrings = formats.map { formatService.formatCoordinateType(it) }
        Pickers.item(
            host.requireContext(),
            host.getString(R.string.pref_coordinate_format_title),
            formatStrings,
            defaultSelectedIndex = formats.indexOf(currentFormat)
        ) {
            if (it != null) {
                onSelected(formats[it])
            }
        }
    }

    private fun getLocationSheetState(format: BuiltInCoordinateFormat): LocationSheetState {
        val accuracy = gps.horizontalAccuracy?.let {
            val accuracyStr = formatService.formatDistance(
                Distance.meters(it).convertTo(baseDistanceUnits)
            )
            host.getString(R.string.accuracy_distance_format, accuracyStr)
        }

        val timeAgo = Duration.between(gps.time, Instant.now())
        return LocationSheetState(
            location = formatService.formatLocation(gps.location, format),
            coordinateFormat = formatService.formatCoordinateType(format),
            accuracy = accuracy,
            satellites = host.getString(R.string.num_satellites, gps.satellites ?: 0),
            time = host.getString(R.string.time_ago, formatService.formatDuration(timeAgo)),
            datum = host.getString(R.string.wgs84)
        )
    }

    private fun updateAltitudeChart(readings: List<Reading<Float>>) {
        altitudeChartReadings = readings
        altitudeHistorySummary = formatAltitudeHistoryDuration()
    }

    private fun updateAltitudeChart() {
        host.inBackground {
            val currentAltitude = Reading(altimeter.altitude, Instant.now())
            val filteredReadings = onDefault {
                val readings =
                    (backtrackAltitudeReadings + weatherAltitudeReadings + currentAltitude).sortedBy { it.time }
                        .filter {
                            Duration.between(
                                it.time,
                                Instant.now()
                            ) < MAX_ALTITUDE_FILTER_HISTORY_DURATION
                        }

                val smoothed = DataUtils.smoothTemporal(readings, 0.3f)

                onIO {
                    DebugElevationsCommand(host.requireContext(), readings, smoothed).execute()
                }

                smoothed.filter {
                    Duration.between(it.time, Instant.now()).abs() <= altitudeHistoryDuration
                }
            }

            onMain { updateAltitudeChart(filteredReadings) }
        }
    }

    private fun formatAltitudeHistoryDuration(): String {
        return host.getString(
            R.string.last_duration,
            formatService.formatDuration(altitudeHistoryDuration)
        )
    }

    private fun getWeatherAltitudeReadings(): LiveData<List<Reading<Float>>> {
        return weatherRepo.getAllLive().map { readings ->
            readings.mapNotNull { reading ->
                if (reading.value.altitude == 0f) {
                    return@mapNotNull null
                }
                Reading(reading.value.altitude, reading.time)
            }
        }
    }

    private fun getBacktrackAltitudeReadings(): LiveData<List<Reading<Float>>> {
        return pathService.getRecentAltitudesLive(
            Instant.now().minus(MAX_ALTITUDE_FILTER_HISTORY_DURATION)
        )
    }

    private fun updateAstronomyData() {
        host.inBackground {
            if (gps.location == Coordinate.zero) {
                return@inBackground
            }

            updateAstronomyLayerCommand.execute()
        }
    }

    fun onResume() {
        if (!isViewReady) {
            return
        }

        views.radarCompassMap.useDensityPixelsForZoom =
            !userPrefs.navigation.highDetailMode
        layers.resume(host.requireContext(), views.radarCompassMap, host)
        views.radarCompass.bindMapView(views.radarCompassMap)

        // Show the north reference indicator
        views.northReferenceIndicator.showDetailsOnClick = true
        views.northReferenceIndicator.useTrueNorth = useTrueNorth
        views.northReferenceIndicator.showLabel = true
        northReferenceHideTimer.once(Duration.ofSeconds(5))
        lastErrorCodes = emptyList()
        updateTimer.interval(NAVIGATION_UPDATE_INTERVAL)
        compassLayerTimer.interval(100)
        refreshNavigation()
    }

    fun onPause() {
        loadBeaconsRunner.cancel()
        errors.reset()
        updateTimer.stop()
        compassLayerTimer.stop()
        layerSheet.setOnDismissListener(null)
        layerSheet.dismiss()
        if (isViewReady) {
            layers.pause(views.radarCompassMap)
        }
        northReferenceHideTimer.stop()
    }

    fun onDestroy() {
        onPause()
        screenLock.releaseLock(host.mainActivity)
        isCreated = false
    }

    private fun updateNearbyBeacons() {
        host.inBackground {
            onIO {
                loadBeaconsRunner.skipIfRunning {
                    val destinationBeacon = (destination as? Destination.Beacon)?.beacon
                    if (!isNearbyEnabled) {
                        nearbyBeacons = listOfNotNull(destinationBeacon)
                        return@skipIfRunning
                    }

                    nearbyBeacons = (navigationService.getNearbyBeacons(
                        gps.location,
                        beacons,
                        nearbyCount,
                        8f,
                        nearbyDistance
                    ) + listOfNotNull(destinationBeacon)).distinctBy { it.id }
                }
            }
        }
    }

    private fun getDestinationBearing(): Float? {
        return destination?.let { navigator.getBearing(gps.location, it).value }
    }

    private fun getSelectedBeacon(nearby: Collection<Beacon>): Beacon? {
        return (destination as? Destination.Beacon)?.beacon ?: getFacingBeacon(nearby)
    }

    private fun getFacingBeacon(nearby: Collection<Beacon>): Beacon? {
        return navigationService.getFacingBeacon(
            gps.location,
            compass.rawBearing,
            nearby,
            declination,
            useTrueNorth
        )
    }

    private fun refreshNavigation() {
        if (!isViewReady) {
            return
        }

        updateNavigationSheet()
        views.speed.title = formatService.formatSpeed(speedometer.speed.value)
        updateCompassBearing()

        if (triggers.frequency("compass_invalidation", Duration.ofSeconds(1))) {
            views.radarCompass.invalidate()
        }

        views.altitude.title = formatService.formatDistance(
            Distance.meters(altimeter.altitude).convertTo(baseDistanceUnits)
        )
        updateLocation()

        if (
            triggers.distance(
                "astronomy",
                gps.location,
                ASTRONOMY_UPDATE_DISTANCE,
                highAccuracy = false
            ) || triggers.frequency("astronomy", ASTRONOMY_UPDATE_FREQUENCY)
        ) {
            updateAstronomyData()
        }

        updateMapAttributionIfNeeded()
        updateCompassStyle()
        updateQuickActionAvailability()
        updateDiagnosticErrors()
    }

    private fun updateNavigationSheet() {
        val currentDestination =
            destination ?: getSelectedBeacon(nearbyBeacons)?.let { Destination.Beacon(it) }
        if (currentDestination != null) {
            views.navigationSheet.updateNavigationSensorValues(
                gps.location,
                altimeter.altitude,
                speedometer.speed.value,
                declination
            )
            views.navigationSheet.show(currentDestination, destination != null)
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

        host.inBackground {
            if (isRadarVisible) {
                val attribution = views.radarCompassMap.getAttribution(host.requireContext())
                onMain {
                    if (isViewReady) {
                        views.mapAttribution.text = attribution
                        views.mapAttribution.isVisible = attribution != null
                    }
                }
            } else {
                onMain {
                    if (isViewReady) {
                        views.mapAttribution.isVisible = false
                    }
                }
            }
        }
    }

    private fun updateCompassStyle() {
        val deviceOrientation = if (clinometer.incline > -30) {
            DeviceOrientation.Orientation.Portrait
        } else {
            DeviceOrientation.Orientation.Flat
        }
        val style = styleChooser.getStyle(deviceOrientation)

        views.linearCompass.isInvisible = style != CompassStyle.Linear
        views.radarCompass.isInvisible = style != CompassStyle.Radar
        views.radarCompassMap.isInvisible = style != CompassStyle.Radar
    }

    private fun updateQuickActionAvailability() {
        // TODO: Extract this logic to the flashlight (if camera is in use)
        if (userPrefs.navigation.rightButton == Tools.QUICK_ACTION_FLASHLIGHT) {
            views.navigationTitle.rightButton.isClickable =
                !views.linearCompass.isCameraActive
        }
        if (userPrefs.navigation.leftButton == Tools.QUICK_ACTION_FLASHLIGHT) {
            views.navigationTitle.leftButton.isClickable =
                !views.linearCompass.isCameraActive
        }
    }

    private fun updateDiagnosticErrors() {
        val codes = diagnosticResults.values.flatten().distinct()
        if (codes != lastErrorCodes) {
            lastErrorCodes = codes
            errors.update(codes)
        }
    }

    private fun updateCompassBearing() {
        val bearing = compass.rawBearing

        // Azimuth
        if (hasCompass) {
            val azimuthText =
                formatService.formatDegrees(bearing, replace360 = true)
                    .padStart(4, ' ')
            val directionText = formatService.formatDirection(CompassDirection.nearest(bearing))
                .padStart(2, ' ')
            val titleText = "$azimuthText   $directionText"
            views.navigationTitle.title = titleText
        }

        // Compass
        listOf<ICompassView>(
            views.radarCompass,
            views.linearCompass
        ).forEach {
            it.azimuth = bearing
            it.declination = declination
        }

        val actualBearing = if (useTrueNorth) {
            compass.bearing
        } else {
            compass.bearing.withDeclination(declination)
        }
        views.radarCompassMap.userAzimuth = actualBearing
        views.radarCompassMap.mapAzimuth = actualBearing.value
    }

    private fun updateLocation() {
        val location = gps.location

        declination = declinationProvider.getDeclination()
        compass.declination = declination

        views.navigationTitle.subtitle = formatService.formatLocation(location)

        views.radarCompassMap.userLocationAccuracy =
            gps.horizontalAccuracy?.let { Distance.meters(it) }

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

        updateNearbyBeacons()
    }

    private fun updateCompassLayers() {
        host.inBackground {
            val destBearing = getDestinationBearing()
            val destinationBeacon = (destination as? Destination.Beacon)?.beacon
            val destColor = destinationBeacon?.color ?: AppColor.Blue.color

            val direction = destBearing?.let {
                MappableBearing(it, destColor)
            }

            // Update beacon layers
            beaconCompassLayer.setBeacons(nearbyBeacons)
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

    private fun showCalibrationDialog() {
        if (userPrefs.navigation.showCalibrationOnNavigateDialog) {
            var doNotAskAgain = false
            val doNotAskAgainText = host.getString(R.string.do_not_ask_again)
            val contentView = TrailSenseComposeRootView(
                host.requireContext(),
                ViewCompositionStrategy.DisposeOnDetachedFromWindow
            ) {
                NavigationCalibrationDialogContent(
                    doNotAskAgainText = doNotAskAgainText,
                    onDoNotAskAgainChanged = {
                        doNotAskAgain = it
                    }
                )
            }
            Alerts.dialog(
                host.requireContext(),
                host.getString(R.string.calibrate_compass_dialog_title),
                host.getString(
                    R.string.calibrate_compass_on_navigate_dialog_content,
                    host.getString(android.R.string.ok)
                ),
                contentView = contentView,
                cancelText = null,
                cancelOnOutsideTouch = false,
                scrollable = true
            ) { cancelled ->
                if (!cancelled && doNotAskAgain) {
                    userPrefs.navigation.showCalibrationOnNavigateDialog = false
                }
            }
        }
    }

    companion object {
        private val ASTRONOMY_UPDATE_DISTANCE = Distance.kilometers(1f).meters()
        private val ASTRONOMY_UPDATE_FREQUENCY = Duration.ofMinutes(1)
        private val MAX_ALTITUDE_HISTORY_DURATION = Duration.ofDays(1)
        private val MAX_ALTITUDE_FILTER_HISTORY_DURATION = MAX_ALTITUDE_HISTORY_DURATION.plusHours(6)
        private const val NAVIGATION_UPDATE_INTERVAL = 33L
    }
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
        Row(
            modifier = modifier
                .clickable(enabled = onClick != null) { onClick?.invoke() }
                .padding(horizontal = TrailSenseSpacing.md, vertical = TrailSenseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavigationTitle(
    state: NavigationTitleState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TrailSenseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        state.leftButton.Content()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = TrailSenseSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
        ) {
            if (state.isTitleVisible) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = state.subtitle,
                modifier = Modifier
                    .combinedClickable(
                    onClick = { state.onSubtitleClick?.invoke() },
                    onLongClick = { state.onSubtitleLongClick?.invoke() }
                )
                    .padding(horizontal = TrailSenseSpacing.md, vertical = TrailSenseSpacing.xs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        state.rightButton.Content()
    }
}

@Composable
private fun NavigatorHost(
    views: NavigatorViews,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TrailSenseSpacing.lg)
        ) {
            NavigationTitle(views.navigationTitle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                views.altitude.Content(
                    Modifier.weight(1f)
                )
                views.speed.Content(Modifier.weight(1f))
            }
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.sm)
            ) {
                val compassSize = minOf(maxWidth, maxHeight)
                val radarModifier = Modifier
                    .align(Alignment.Center)
                    .size(compassSize)
                views.radarCompassMap.Content(radarModifier)
                views.radarCompass.Content(radarModifier)
                views.linearCompass.Content(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                )
                views.mapAttribution.Content(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(TrailSenseSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    views.accuracyView.Content()
                    views.northReferenceIndicator.Content()
                }
                NavigatorIconButton(
                    icon = TrailSenseIcons.Filled.LocationOn,
                    onClick = { views.onBeaconClick?.invoke() },
                    onLongClick = { views.onBeaconLongClick?.invoke() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(TrailSenseSpacing.sm)
                )
            }
            views.navigationSheet.Content()
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

@Composable
private fun NavigatorIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
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
