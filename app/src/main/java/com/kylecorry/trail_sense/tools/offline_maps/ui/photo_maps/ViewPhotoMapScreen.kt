package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import com.kylecorry.trail_sense.shared.navigation.toRouteLocation

import com.kylecorry.trail_sense.shared.navigation.Route
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.tools.map.ui.MapFeatureClickHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.asLiveData
import com.kylecorry.trail_sense.shared.extensions.withSuspendableCancelableLoading
import java.util.concurrent.atomic.AtomicBoolean
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.time.Throttle
import com.kylecorry.andromeda.torch.ScreenTorch
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersSheetState
import com.kylecorry.trail_sense.shared.map_layers.ui.MapAttributionState
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getAttribution
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationScreenLock
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.offline_maps.OfflineMapsToolRegistration
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.paths.infrastructure.commands.CreatePathCommand
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun ViewPhotoMapToolScreen(
    mapId: Long,
    viewModelKey: String,
    autoLockLocation: Boolean,
    commands: Flow<PhotoMapChildCommand>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val screenNavigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel(key = viewModelKey) {
        val appContext = context.applicationContext
        ViewPhotoMapViewModel(
            getAppService(),
            BeaconService(appContext),
            Navigator.getInstance(appContext),
            mapId,
            autoLockLocation
        )
    }
    val screen = remember(viewModel) {
        ViewPhotoMapRenderer(
            context,
            // The window (screen torch), screen lock and bottom navigation belong to the activity.
            requireNotNull(context.findMainActivity()),
            screenNavigator,
            effects,
            lifecycleOwner,
            scope,
            viewModel
        )
    }

    DisposableEffect(screen) {
        screen.onCreate()
        onDispose { screen.onDestroy() }
    }

    LifecycleResumeEffect(screen) {
        screen.onResume()
        onPauseOrDispose { screen.onPause() }
    }

    LaunchedEffect(screen) {
        viewModel.mapLoaded.collect { screen.onMapLoad(it) }
    }

    LaunchedEffect(screen, viewModel.destination) {
        screen.onDestinationChanged(viewModel.destination)
    }

    LaunchedEffect(screen, commands) {
        commands.collect {
            when (it) {
                PhotoMapChildCommand.Recenter -> screen.recenter()
                PhotoMapChildCommand.Reload -> viewModel.load()
                PhotoMapChildCommand.Measure -> screen.startDistanceMeasurement(emptyArray())
                PhotoMapChildCommand.Trace -> screen.trace()
                PhotoMapChildCommand.AdjustLayers -> screen.adjustLayers()
            }
        }
    }

    screen.Content(modifier = modifier)
}

private tailrec fun Context.findMainActivity(): MainActivity? {
    return when (this) {
        is MainActivity -> this
        is ContextWrapper -> baseContext.findMainActivity()
        else -> null
    }
}

internal enum class MapLockMode {
    Location,
    Compass,
    Free,
    Trace
}

/** The photo map being viewed, where the user is navigating to and how the map follows them. */
internal class ViewPhotoMapViewModel(
    private val service: MapService,
    private val beaconService: BeaconService,
    private val navigationService: Navigator,
    val mapId: Long,
    autoLockLocation: Boolean
) : ViewModel() {

    var map by mutableStateOf<PhotoMap?>(null)
        private set
    var destination by mutableStateOf<Destination?>(null)
        private set
    var lockMode by mutableStateOf(MapLockMode.Free)

    /** The first load locks the map to the user's location when the screen was opened that way. */
    private var shouldLockOnMapLoad = autoLockLocation

    private val _mapLoaded = Channel<PhotoMap>(Channel.BUFFERED)

    /** Each loaded map, to set the map view up for it. */
    val mapLoaded: Flow<PhotoMap> = _mapLoaded.receiveAsFlow()

    init {
        viewModelScope.launch {
            navigationService.destination2.collect {
                destination = it
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            map = withContext(Dispatchers.IO) {
                service.getPhotoMap(mapId)
            }
            map?.let { _mapLoaded.send(it) }
        }
    }

    fun consumeLockOnMapLoad(): Boolean {
        val shouldLock = shouldLockOnMapLoad
        shouldLockOnMapLoad = false
        return shouldLock
    }

    fun nextLockMode(hasCompass: Boolean): MapLockMode {
        return when (lockMode) {
            MapLockMode.Location -> if (hasCompass) MapLockMode.Compass else MapLockMode.Free
            MapLockMode.Compass -> MapLockMode.Free
            MapLockMode.Free -> MapLockMode.Location
            MapLockMode.Trace -> MapLockMode.Free
        }
    }

    /** Navigates to [location] through a temporary beacon. */
    fun navigateTo(location: Coordinate) {
        viewModelScope.launch {
            val beacon = Beacon(
                0L,
                map?.name ?: "",
                location,
                visible = false,
                temporary = true,
                color = AppColor.Orange.color,
                owner = BeaconOwner.Maps
            )
            val id = onIO {
                beaconService.add(beacon)
            }
            val saved = beacon.copy(id = id)
            navigationService.navigateTo(saved)
            destination = Destination.Beacon(saved)
        }
    }
}

/**
 * The photo map's rendering side: the map view and its layers, sensors, sheets and buttons. It lives in the
 * composition; the map, destination and lock mode come from [viewModel].
 */
private class ViewPhotoMapRenderer(
    private val context: Context,
    private val activity: MainActivity,
    private val screenNavigator: TrailSenseNavigator,
    private val effects: ScreenEffects,
    private val lifecycleOwner: LifecycleOwner,
    private val scope: CoroutineScope,
    private val viewModel: ViewPhotoMapViewModel
) {
    private var photoMapViews: ViewPhotoMapViews? =
        createViewPhotoMapViews(context)
    private val views: ViewPhotoMapViews
        get() = photoMapViews ?: error("Photo map views are not ready")
    private val isBound: Boolean
        get() = photoMapViews != null

    private val sensorService by lazy { SensorService(context) }
    private val gps by lazy { sensorService.getGPS() }
    private val altimeter by lazy { sensorService.getAltimeter() }
    private val compass by lazy { sensorService.getCompass() }
    private val hasCompass by lazy { sensorService.hasCompass() }
    private val formatService by lazy { FormatService.getInstance(context) }
    private val prefs by lazy { UserPreferences(context) }

    private val screenLock by lazy { NavigationScreenLock(prefs.photoMaps.keepScreenUnlockedWhileOpen) }

    private val screenLight by lazy { ScreenTorch(activity.window) }

    // Map layers
    private val layerManager = PhotoMapToolLayerManager()
    private val featureClickHandler =
        MapFeatureClickHandler(context, screenNavigator, effects, scope)
    private val layerSheet = MapLayersSheetState(
        OfflineMapsToolRegistration.PHOTO_MAPS_ID,
        PhotoMapToolLayerManager.alwaysEnabledLayers
    )

    // Paths
    private val pathService by lazy { PathService.getInstance(context) }

    private val map: PhotoMap?
        get() = viewModel.map

    private val mapLockMode: MapLockMode
        get() = viewModel.lockMode

    private val throttle = Throttle(20)
    private var updateJob: Job? = null

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        ViewPhotoMapHost(
            views = views,
            modifier = modifier
        )
        layerSheet.Content(
            context,
            { screenNavigator.navigate(it) }
        )
    }

    private val gpsData by lazy { gps.asLiveData() }
    private val altimeterData by lazy { altimeter.asLiveData() }
    private val compassData by lazy { compass.asLiveData() }

    private val gpsObserver = Observer<Any> {
        if (isBound) {
            views.map.userLocation = gps.location
            views.map.userLocationAccuracy = gps.horizontalAccuracy?.let { Distance.meters(it) }
            updateDestination()

            if (mapLockMode == MapLockMode.Location || mapLockMode == MapLockMode.Compass) {
                views.map.mapCenter = gps.location
            }
        }
    }
    private val altimeterObserver = Observer<Any> { updateDestination() }
    private val compassObserver = Observer<Any> {
        if (isBound) {
            compass.declination = Geophysics.getGeomagneticDeclination(gps.location, gps.altitude)
            val bearing = compass.rawBearing
            views.map.userAzimuth = compass.bearing
            if (mapLockMode == MapLockMode.Compass) {
                views.map.mapAzimuth = bearing
            }
            updateDestination()
        }
    }

    fun onCreate() {
        gpsData.observe(lifecycleOwner, gpsObserver)
        altimeterData.observe(lifecycleOwner, altimeterObserver)
        compassData.observe(lifecycleOwner, compassObserver)

        views.map.setOnLongPressListener {
            onLongPress(it)
        }

        views.map.setBackgroundColor(Resources.color(context, R.color.colorSecondary))

        val keepMapUp = prefs.photoMaps.keepMapFacingUp

        // TODO: Don't show if location not on map

        // Update initial map rotation
        views.map.mapAzimuth = getDefaultMapAzimuth(keepMapUp)

        // Set the button states
        views.lockBtn.setState(false)
        views.zoomInBtn.setState(false)
        views.zoomOutBtn.setState(false)

        // Handle when the lock button is pressed
        views.lockBtn.setOnClickListener {
            updateMapLockMode(viewModel.nextLockMode(hasCompass), keepMapUp)
        }

        views.zoomOutBtn.setOnClickListener {
            views.map.zoom(0.5f)
        }

        views.zoomInBtn.setOnClickListener {
            views.map.zoom(2f)
        }
    }

    fun onResume() {
        layerManager.setOnDistanceChangedCallback(this::showDistance)
        resetLayerManager()
        views.map.useDensityPixelsForZoom = !prefs.photoMaps.highDetailMode

        if (mapLockMode == MapLockMode.Trace) {
            updateMapLockMode(MapLockMode.Trace, prefs.photoMaps.keepMapFacingUp)
        }
        updateJob?.cancel()
        updateJob = scope.launch {
            while (true) {
                updatePeriodic()
                delay(5000)
            }
        }
    }

    private fun getDefaultMapAzimuth(keepMapUp: Boolean): Float {
        return if (keepMapUp) {
            -Trigonometry.deltaAngle(
                map?.georeference?.rotation ?: 0f,
                map?.baseRotation()?.toFloat() ?: 0f
            )
        } else {
            0f
        }
    }

    private fun onLongPress(location: Coordinate) {
        if (map?.isCalibrated != true || layerManager.isMeasuringDistance() || mapLockMode == MapLockMode.Trace) {
            return
        }

        selectLocation(location)

        scope.launch {
            val elevation = onIO { Distance.meters(DEM.getElevation(location).elevation) }

            val chosen = effects.chooseAction(
                formatService.formatLocation(location),
                context.getString(
                    R.string.elevation_value,
                    formatService.formatElevation(elevation)
                ),
                listOf(
                    ActionItem(context.getString(R.string.beacon), TrailIcons.Location) {
                        createBeacon(location)
                        selectLocation(null)
                    },
                    ActionItem(context.getString(R.string.navigate), TrailIcons.Beacon) {
                        viewModel.navigateTo(location)
                        selectLocation(null)
                    },
                    ActionItem(context.getString(R.string.distance), TrailIcons.Ruler) {
                        startDistanceMeasurement(arrayOf(gps.location, location))
                        selectLocation(null)
                    },
                )
            )
            if (chosen != null) {
                chosen.action()
            } else {
                selectLocation(null)
            }
        }
    }

    private fun updateDestination() {
        if (throttle.isThrottled() || !isBound) {
            return
        }

        val beacon = viewModel.destination ?: return
        views.navigationSheet.updateNavigationSensorValues(
            gps.location,
            altimeter.altitude,
            gps.speed.value,
            compass.declination
        )
        views.navigationSheet.setTrueNorthOverride(true)
        views.navigationSheet.show(beacon, true)
    }

    private fun selectLocation(location: Coordinate?) {
        layerManager.setSelectedLocation(location)
    }

    private fun createBeacon(location: Coordinate) {
        screenNavigator.navigate(Route.PlaceBeacon(initialLocation = location.toRouteLocation()))
    }

    private fun showDistance(distance: Distance) {
        val relative = distance
            .convertTo(prefs.baseDistanceUnits)
            .toRelativeDistance()
        views.distanceSheet.setDistance(relative)
    }

    fun startDistanceMeasurement(initialPoints: Array<Coordinate>) {
        if (map?.isCalibrated != true) {
            effects.showMessage(context.getString(R.string.map_is_not_calibrated))
            return
        }

        layerManager.startDistanceMeasurement(initialPoints)
        views.distanceSheet.show()
        views.distanceSheet.cancelListener = {
            stopDistanceMeasurement()
        }
        views.distanceSheet.createPathListener = {
            scope.launch {
                map?.let {
                    val skipElevations = AtomicBoolean(false)
                    var id = 0L
                    Alerts.withSuspendableCancelableLoading(
                        context,
                        context.getString(R.string.loading),
                        cancelText = context.getString(R.string.skip_elevations),
                        onCancel = { skipElevations.set(true) }
                    ) {
                        id = CreatePathCommand(
                            pathService,
                            prefs.paths,
                            it.name,
                            layerManager.getDistanceMeasurementElevations(),
                            skipElevations::get
                        ).execute(layerManager.getDistanceMeasurementPoints())
                    }

                    stopDistanceMeasurement()
                    screenNavigator.navigate(Route.PathOverview(pathId = id))
                }
            }
        }
        views.distanceSheet.undoListener = {
            layerManager.undoLastDistanceMeasurement()
        }
        views.distanceSheet.redoListener = {
            layerManager.redoDistanceMeasurement()
        }
        layerManager.setOnDistanceEditStateChangedCallback { state ->
            views.distanceSheet.setEditState(state)
        }
        views.distanceSheet.setEditState(layerManager.getDistanceEditState())
    }

    private fun stopDistanceMeasurement() {
        layerManager.setOnDistanceEditStateChangedCallback(null)
        layerManager.stopDistanceMeasurement()
        views.distanceSheet.hide()
    }

    fun trace() {
        updateMapLockMode(MapLockMode.Trace, prefs.photoMaps.keepMapFacingUp)
    }

    private fun resetLayerManager() {
        layerManager.resume(views.map, viewModel.mapId, lifecycleOwner, featureClickHandler::onFeatureClick)

        // Populate the last known location and map bounds
        layerManager.onBoundsChanged()
        views.map.userLocation = gps.location
        views.map.userLocationAccuracy = gps.horizontalAccuracy?.let { Distance.meters(it) }
    }

    fun adjustLayers() {
        layerSheet.dismissAndUpdateWidgets(context)
        layerManager.pause(views.map)
        layerSheet.setOnDismissListener {
            resetLayerManager()
        }
        layerSheet.show()
    }

    fun onDestinationChanged(destination: Destination?) {
        if (isBound) {
            if (destination == null) {
                views.navigationSheet.hide()
            } else {
                updateDestination()
            }
        }
        screenLock.updateLock(activity)
    }

    fun onMapLoad(map: PhotoMap) {
        layerManager.pause(views.map)
        views.map.minScale = 0f
        val bounds = map.boundary() ?: CoordinateBounds(85.0, 180.0, -85.0, -180.0)
        views.map.fitIntoView(bounds, paddingFactor = 1.05f)
        views.map.constraintBounds = bounds
        views.map.minScale = views.map.scale
        layerManager.improveResolution(views.map.mapBounds, views.map.zoom.roundToInt())
        if (viewModel.consumeLockOnMapLoad()) {
            updateMapLockMode(MapLockMode.Location, prefs.photoMaps.keepMapFacingUp)
        }
        views.map.mapAzimuth = getDefaultMapAzimuth(prefs.photoMaps.keepMapFacingUp)
        layerManager.resume(views.map, map.id, lifecycleOwner, featureClickHandler::onFeatureClick)
        layerManager.onBoundsChanged()
    }

    private fun updateMapLockMode(mode: MapLockMode, keepMapUp: Boolean) {
        viewModel.lockMode = mode

        // Show zoom buttons
        views.zoomInBtn.isVisible = true
        views.zoomOutBtn.isVisible = true
        views.map.isZoomEnabled = true

        // Make it interactive
        views.map.isInteractive = true
        views.map.isZoomEnabled = true

        // Show the bottom navigation
        activity.setBottomNavigationEnabled(true)

        when (mode) {
            MapLockMode.Location -> {
                // Disable pan
                views.map.isPanEnabled = false

                // Zoom in and center on location
                views.map.resolution = 2f
                views.map.mapCenter = gps.location

                // Reset the rotation
                views.map.mapAzimuth = getDefaultMapAzimuth(keepMapUp)

                // Show as locked
                views.lockBtn.setIcon(TrailIcons.Satellite)
                views.lockBtn.setState(true)

                // Reset brightness
                screenLight.off()
            }

            MapLockMode.Compass -> {
                // Disable pan
                views.map.isPanEnabled = false

                // Center on location
                views.map.mapCenter = gps.location

                // Rotate
                views.map.mapAzimuth = -compass.rawBearing

                // Show as locked
                views.lockBtn.setIcon(TrailIcons.CompassIcon)
                views.lockBtn.setState(true)

                // Reset brightness
                screenLight.off()
            }

            MapLockMode.Free -> {
                // Enable pan
                views.map.isPanEnabled = true

                // Reset the rotation
                views.map.mapAzimuth = getDefaultMapAzimuth(keepMapUp)

                // Show as unlocked
                views.lockBtn.setIcon(TrailIcons.Satellite)
                views.lockBtn.setState(false)

                // Reset brightness
                screenLight.off()
            }

            MapLockMode.Trace -> {
                CustomUiUtils.disclaimer(
                    context,
                    context.getString(R.string.trace),
                    context.getString(R.string.map_trace_instructions),
                    "disclaimer_shown_map_trace",
                    cancelText = null
                )

                // Disable pan
                views.map.isInteractive = false
                views.map.isZoomEnabled = false

                // Show as locked
                views.lockBtn.setIcon(TrailIcons.Lock)
                views.lockBtn.setState(true)

                // Full brightness
                screenLight.on()

                // Hide zoom buttons
                views.zoomInBtn.isVisible = false
                views.zoomOutBtn.isVisible = false

                // Hide the bottom navigation
                activity.setBottomNavigationEnabled(false)
            }
        }
    }

    fun onPause() {
        updateJob?.cancel()
        updateJob = null
        layerSheet.setOnDismissListener(null)
        layerSheet.dismissAndUpdateWidgets(context)
        photoMapViews?.let {
            layerManager.pause(it.map)
        }
        // Reset brightness
        screenLight.off()

        // Show the bottom navigation
        activity.setBottomNavigationEnabled(true)
    }

    fun recenter() {
        if (mapLockMode == MapLockMode.Trace) {
            return
        }
        views.map.recenter()
    }

    fun onDestroy() {
        gpsData.removeObserver(gpsObserver)
        altimeterData.removeObserver(altimeterObserver)
        compassData.removeObserver(compassObserver)
        screenLock.releaseLock(activity)
        photoMapViews = null
    }

    private suspend fun updatePeriodic() {
        screenLock.updateLock(activity)
        val views = photoMapViews ?: return
        val attribution = onIO { views.map.getAttribution(context) }
        views.mapAttribution.text = attribution
        views.mapAttribution.isVisible = attribution != null
    }
}

private data class ViewPhotoMapViews(
    val map: MapView,
    val mapAttribution: MapAttributionState,
    val zoomInBtn: PhotoMapIconButton,
    val zoomOutBtn: PhotoMapIconButton,
    val lockBtn: PhotoMapIconButton,
    val navigationSheet: com.kylecorry.trail_sense.tools.navigation.ui.NavigationSheetView,
    val distanceSheet: MapDistanceSheetState
)

@Suppress("FunctionNaming")
@Composable
private fun ViewPhotoMapHost(
    views: ViewPhotoMapViews,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        views.map.Content(Modifier.fillMaxSize())
        if (views.mapAttribution.isVisible) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = TrailSenseSpacing.screen,
                        bottom = 120.dp
                    ),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = TrailSenseSpacing.xxs,
                shadowElevation = TrailSenseSpacing.xs
            ) {
                Box(modifier = Modifier.padding(TrailSenseSpacing.xs)) {
                    views.mapAttribution.Content()
                }
            }
        }

        // Grouped zoom controls in a tonal cluster (mirrors ViewVectorMapScreen)
        if (views.zoomInBtn.isVisible || views.zoomOutBtn.isVisible) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = TrailSenseSpacing.screen, bottom = 176.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = TrailSenseSpacing.xxs,
                shadowElevation = TrailSenseSpacing.xs
            ) {
                Column(
                    modifier = Modifier.padding(TrailSenseSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
                ) {
                    views.zoomInBtn.Content()
                    views.zoomOutBtn.Content()
                }
            }
        }

        views.lockBtn.Content(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = TrailSenseSpacing.screen, bottom = 120.dp)
        )
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            views.navigationSheet.Content(Modifier.fillMaxWidth())
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            views.distanceSheet.Content(Modifier.fillMaxWidth())
        }
    }
}

private fun createViewPhotoMapViews(context: Context): ViewPhotoMapViews {
    val map = MapView(context).apply {
        id = R.id.map
        setBackgroundColor(Resources.color(context, R.color.colorSecondary))
    }

    val attribution = MapAttributionState()
    val lockBtn = PhotoMapIconButton(TrailIcons.Satellite).apply {
        alpha = 0.86f
    }

    val zoomOutBtn = PhotoMapIconButton(TrailIcons.ZoomOut).apply {
        alpha = 0.86f
    }

    val zoomInBtn = PhotoMapIconButton(TrailIcons.Add).apply {
        alpha = 0.86f
    }

    val navigationSheet =
        com.kylecorry.trail_sense.tools.navigation.ui.NavigationSheetView(context).apply {
            isVisible = false
        }

    val distanceSheet = MapDistanceSheetState(context)

    return ViewPhotoMapViews(
        map = map,
        mapAttribution = attribution,
        zoomInBtn = zoomInBtn,
        zoomOutBtn = zoomOutBtn,
        lockBtn = lockBtn,
        navigationSheet = navigationSheet,
        distanceSheet = distanceSheet
    )
}

@Suppress("FunctionNaming")
@Preview(showBackground = true)
@Composable
private fun ViewPhotoMapHostPreview() {
    TrailSenseTheme {
        ViewPhotoMapHost(
            views = createViewPhotoMapViews(androidx.compose.ui.platform.LocalContext.current)
        )
    }
}
