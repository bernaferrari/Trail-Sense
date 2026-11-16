package com.kylecorry.trail_sense.tools.navigation.ui

import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.clinometer.Clinometer
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.RouteLocation
import com.kylecorry.trail_sense.shared.navigation.toRouteLocation
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.tools.navigation.infrastructure.INavigator
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticResult
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.IWeatherRepo
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The compass's sensors, made once per screen from the app's sensor service. */
class NavigatorSensors(sensorService: SensorService, userPrefs: UserPreferences) {
    val orientation = sensorService.getOrientation()
    val compass = sensorService.getCompass(orientation)
    val gps = sensorService.getGPS(frequency = 200.milliseconds)
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
class NavigatorViewModel(
    route: Route.Navigation,
    val sensors: NavigatorSensors,
    private val navigator: INavigator,
    beaconUpdates: Flow<List<Beacon>>,
    pathService: IPathService,
    weatherRepo: IWeatherRepo,
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

    // Snapshot state: written from the beacon loader's IO thread and read by the dial each frame.
    var nearbyBeacons by mutableStateOf<List<Beacon>>(emptyList())
        private set

    var currentDestination by mutableStateOf<Destination?>(null)
        private set

    /** The destination as the navigator reports it. */
    val destination: Flow<Destination?> = navigator.destination

    var declination by mutableFloatStateOf(0f)
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
            navigator.destination.collect {
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
            beaconUpdates.collect {
                beacons = it
                updateNearbyBeacons()
            }
        }

        viewModelScope.launch {
            pathService.getRecentAltitudesLive(Clock.System.now().minus(MAX_ALTITUDE_FILTER_HISTORY_DURATION))
                .collect {
                    backtrackAltitudeReadings = it
                    updateAltitudeChart()
                }
        }

        viewModelScope.launch {
            weatherRepo.getAllLive().collect { readings ->
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
        return triggers.frequency("compass_invalidation", 1.seconds)
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
            withContext(ioDispatcher) {
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
            val currentAltitude = Reading(sensors.altimeter.altitude, kotlin.time.Clock.System.now())
            altitudeChartReadings = withContext(Dispatchers.Default) {
                val readings =
                    (backtrackAltitudeReadings + weatherAltitudeReadings + currentAltitude).sortedBy { it.time }
                        .filter {
                            (Clock.System.now() - it.time) < MAX_ALTITUDE_FILTER_HISTORY_DURATION
                        }

                val smoothed = DataUtils.smoothTemporal(readings, 0.3f)

                withContext(ioDispatcher) {
                    debugElevations(readings, smoothed)
                }

                smoothed.filter {
                    (Clock.System.now() - it.time).absoluteValue <= altitudeHistoryDuration
                }
            }
        }
    }

    companion object {
        private val ASTRONOMY_UPDATE_DISTANCE = Distance.kilometers(1f).meters()
        private val ASTRONOMY_UPDATE_FREQUENCY = 1.minutes
        private val MAX_ALTITUDE_HISTORY_DURATION = 1.days
        private val MAX_ALTITUDE_FILTER_HISTORY_DURATION = MAX_ALTITUDE_HISTORY_DURATION.plus(6.hours)
    }
}
