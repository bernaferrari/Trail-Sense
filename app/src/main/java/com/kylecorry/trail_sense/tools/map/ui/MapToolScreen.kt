@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.map.ui

import androidx.compose.runtime.saveable.rememberSaveable
import com.kylecorry.andromeda.torch.screenBrightness

import com.kylecorry.trail_sense.shared.navigation.toRouteLocation

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.withSuspendableCancelableLoading
import java.util.concurrent.atomic.AtomicBoolean
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.trail_sense.shared.sensors.gps.ISatelliteGPS
import com.kylecorry.andromeda.torch.ScreenTorch
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.ImproveAccuracyAlerter
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersSheetState
import com.kylecorry.trail_sense.shared.map_layers.ui.MapAttributionState
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getAttribution
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import android.content.ContextWrapper
import kotlinx.coroutines.launch
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.views.DateTimeSliderSheetState
import com.kylecorry.trail_sense.shared.views.SensorStatusBadgeView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseElevation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseMotion
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationScreenLock
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.NavigationSheetView
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.MapDistanceSheetState
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.paths.infrastructure.commands.CreatePathCommand
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import kotlin.time.Instant
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.rememberNavigationSensors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The map tool: the map with its controls and sheets, or a prompt to add one when no visible map can be drawn. */
@Composable
fun MapToolScreen(startPathCreation: Boolean = false, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel = viewModel { MapToolViewModel(MapService.getInstance(appContext)) }
    val resumeCount = rememberResumeCount()
    // The map, its controls and sheets are rendering objects that live as long as the screen, so the controller that
    // wires them stays in the composition; only whether there is a map to show lives in the ViewModel.
    val controller = remember { MapController(context, navigator, effects, lifecycleOwner) }
    LaunchedEffect(viewModel, resumeCount) {
        viewModel.refreshMaps()
    }
    // Opened to draw a path: start once, when the map is first shown (not again after returning to it)
    var pendingPathCreation by rememberSaveable { mutableStateOf(startPathCreation) }
    LaunchedEffect(viewModel.hasRenderableMap, pendingPathCreation) {
        if (pendingPathCreation && viewModel.hasRenderableMap == true) {
            pendingPathCreation = false
            controller.startPathCreation()
        }
    }
    controller.Content(
        hasRenderableMap = viewModel.hasRenderableMap,
        resumeCount = resumeCount,
        modifier = modifier.fillMaxSize()
    )
}

/** How many times this screen came back after being paused; effects keyed on it rerun on each return. */
@Composable
private fun rememberResumeCount(): Int {
    var count by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        paused = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (paused) {
            paused = false
            count++
        }
    }
    return count
}

/** Whether any visible offline map can be drawn; checked again each time the map resumes. */
internal class MapToolViewModel(private val mapService: MapService) : ViewModel() {

    var hasRenderableMap by mutableStateOf<Boolean?>(null)
        private set

    suspend fun refreshMaps() {
        hasRenderableMap = withContext(Dispatchers.IO) {
            runCatching {
                mapService.getAllVectorMaps().any { it.visible } ||
                    mapService.getAllPhotoMaps().any { it.visible && it.isCalibrated }
            }.getOrDefault(false)
        }
    }
}

/**
 * Wires the map, its controls and sheets to the sensors, navigation, layers and saved map state. The map tool's
 * state (lock mode, map time, time-dependent layers) is Compose state, and [Effects] keeps the views in sync with it.
 */
private class MapController(
    private val context: Context,
    private val appNavigator: TrailSenseNavigator,
    private val effects: ScreenEffects,
    private val lifecycleOwner: LifecycleOwner
) {

    // Only the activity can hide its bottom navigation (trace mode)
    private val mainActivity = context.findMainActivity()
    private val featureClickHandler =
        MapFeatureClickHandler(context, appNavigator, effects, lifecycleOwner.lifecycleScope)

    private val sensors = DependencyRegistry.get<SensorService>()
    private val navigator = DependencyRegistry.get<Navigator>()
    private val pathService = DependencyRegistry.get<PathService>()
    private val prefs = DependencyRegistry.get<UserPreferences>()
    private val formatter = DependencyRegistry.get<FormatService>()
    private val mapState = DependencyRegistry.get<PreferencesSubsystem>().preferences

    val views = createMapViews(context)
    private val mapView = views.map
    private val layerEditSheet = MapLayersSheetState(MapToolRegistration.MAP_ID)
    private val manager = MapToolLayerManager()
    private val hasCompass = sensors.hasCompass()
    private val screenLight = ScreenTorch(screenBrightness(mainActivity.window))
    private val screenLock = NavigationScreenLock(prefs.map.keepScreenUnlockedWhileOpen)

    private var mapTime by mutableStateOf<Instant?>(null)
    private var hasTimeDependentLayers by mutableStateOf(false)
    private var lockMode by mutableStateOf(
        MapLockMode.entries.firstOrNull {
            it.id == mapState.getInt(LOCK_MODE_KEY) && it != MapLockMode.Trace
        } ?: MapLockMode.Free
    )

    // Plain values the views' callbacks read; they don't drive composition
    private var shouldSaveState = false
    private var userLocation = Coordinate.zero
    private var lastBounds: CoordinateBounds? = null
    private var lastLayersKey: Int? = null

    @Composable
    fun Content(hasRenderableMap: Boolean?, resumeCount: Int, modifier: Modifier = Modifier) {
        when (hasRenderableMap) {
            true -> {
                Effects(resumeCount)
                MapHost(views = views, modifier = modifier)
                layerEditSheet.Content(context, ::navigateFromLayerSheet)
            }
            false -> MapEmptyState(
                onAddMap = {
                    appNavigator.navigate(Route.OfflineMapList())
                },
                modifier = modifier
            )
            null -> Box(modifier = modifier)
        }
    }

    private fun navigateFromLayerSheet(route: Route) {
        appNavigator.navigate(route)
    }

    @Composable
    private fun Effects(resumeCount: Int) {
        val navigation = rememberNavigationSensors(tag = "MapToolScreen", trueNorth = true)
        val destination by navigator.destination2.collectAsStateWithLifecycle(
            initialValue = null,
            minActiveState = Lifecycle.State.RESUMED
        )
        val shouldSave = remember(resumeCount) { prefs.map.saveMapState }
        val layersKey = manager.key
        SideEffect {
            userLocation = navigation.location
        }

        // Time
        LaunchedEffect(mapTime, hasTimeDependentLayers) {
            updateTimeControls()
        }

        // Settings and screen lock, read again on each resume
        LaunchedEffect(resumeCount) {
            mapView.useDensityPixelsForZoom = !prefs.map.highDetailMode
            mapView.layerManager.invalidate()
            mapView.invalidate()
        }

        LaunchedEffect(resumeCount, destination) {
            screenLock.updateLock(mainActivity)
        }

        DisposableEffect(Unit) {
            onDispose {
                screenLock.releaseLock(mainActivity)
            }
        }

        // Layers
        LaunchedEffect(mapTime, layersKey) {
            manager.setTime(mapView, mapTime)
        }

        LifecycleResumeEffect(Unit) {
            resumeLayers()
            onPauseOrDispose {
                manager.pause(mapView)
            }
        }

        LaunchedEffect(layersKey) {
            hasTimeDependentLayers = mapView.layerManager.getLayers().any { it.isTimeDependent }
        }

        LaunchedEffect(layersKey, lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val attribution = mapView.getAttribution(context)
                views.mapAttribution.text = attribution
                views.mapAttribution.isVisible = attribution != null
            }
        }

        LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
            tryOrNothing {
                layerEditSheet.setOnDismissListener(null)
                layerEditSheet.dismissAndUpdateWidgets(context)
            }
            screenLight.off()
            mainActivity.setBottomNavigationEnabled(true)
        }

        // User location and heading
        LaunchedEffect(navigation.location, navigation.locationAccuracy) {
            mapView.userLocation = navigation.location
            mapView.userLocationAccuracy = navigation.locationAccuracy
            mapView.invalidate()
        }

        LaunchedEffect(navigation.bearing) {
            mapView.userAzimuth = navigation.bearing
        }

        // The bounds follow the map's center, scale, size and rotation, so they are checked after each update
        SideEffect {
            onBoundsMayHaveChanged()
        }

        // Lock mode
        LaunchedEffect(lockMode, hasTimeDependentLayers, resumeCount) {
            switchMapLockMode(lockMode)
        }

        LaunchedEffect(shouldSave) {
            restoreSavedMapState(shouldSave)
        }

        LaunchedEffect(lockMode, navigation.location) {
            if (mapView.mapCenter == Coordinate.zero) {
                mapView.mapCenter = navigation.location
                mapView.resolution = 2f
            }

            if (lockMode == MapLockMode.Location || lockMode == MapLockMode.Compass) {
                mapView.mapCenter = navigation.location
            }
        }

        LaunchedEffect(lockMode, navigation.bearing) {
            if (lockMode == MapLockMode.Compass) {
                mapView.mapAzimuth = navigation.bearing.value
            }
        }

        // Navigation
        LaunchedEffect(destination, navigation) {
            val target = destination
            if (target != null) {
                views.navigationSheet.updateNavigationSensorValues(navigation)
                views.navigationSheet.setTrueNorthOverride(true)
                views.navigationSheet.show(target, true)
            } else {
                views.navigationSheet.hide()
            }
        }

        // Sensor status
        LaunchedEffect(navigation.gps, navigation.compass) {
            val gps = navigation.gps ?: return@LaunchedEffect
            val compass = navigation.compass ?: return@LaunchedEffect
            views.sensorStatusBadges.setSensors(gps as ISatelliteGPS, compass)
        }

        // Controls
        LaunchedEffect(Unit) {
            bindControls()
        }
    }

    private fun updateTimeControls() {
        val timeButton = views.timeButton
        val timeSheet = views.timeSheet
        timeButton.isVisible = hasTimeDependentLayers
        if (hasTimeDependentLayers) {
            timeButton.setState(mapTime != null || timeSheet.isVisible)
        } else if (timeSheet.isVisible) {
            mapTime = null
            timeSheet.hide()
        }
    }

    private fun bindControls() {
        val timeButton = views.timeButton
        val timeSheet = views.timeSheet
        timeButton.setOnClickListener {
            if (timeSheet.isVisible) {
                mapTime = null
                timeSheet.hide()
            } else {
                timeSheet.setTime(mapTime)
                timeSheet.show()
            }
            timeButton.setState(mapTime != null || timeSheet.isVisible)
        }
        timeSheet.onTimeChanged = { mapTime = it }

        views.lockButton.setOnClickListener {
            changeLockMode(getNextLockMode(lockMode, hasCompass))
        }

        views.zoomInButton.setState(false)
        views.zoomOutButton.setState(false)
        views.zoomInButton.setOnClickListener {
            mapView.zoom(2f)
        }
        views.zoomOutButton.setOnClickListener {
            mapView.zoom(0.5f)
        }

        views.menuButton.setState(false)
        views.menuButton.setOnClickListener {
            showMapMenu()
        }

        views.sensorStatusBadges.setOnClickListener {
            ImproveAccuracyAlerter(context).alert(views.sensorStatusBadges.getSensors())
        }

        mapView.setOnLongPressListener(::onLongPress)

        manager.setOnDistanceChangedCallback { distance ->
            val relative = distance
                .convertTo(prefs.baseDistanceUnits)
                .toRelativeDistance()
            views.distanceSheet.setDistance(relative)
        }
    }

    private fun resumeLayers() {
        manager.resume(mapView, lifecycleOwner, featureClickHandler::onFeatureClick)
    }

    private fun onBoundsMayHaveChanged() {
        val bounds = mapView.mapBounds
        val layersKey = manager.key
        if (bounds == lastBounds && layersKey == lastLayersKey) {
            return
        }
        lastBounds = bounds
        lastLayersKey = layersKey
        manager.onBoundsChanged()
    }

    private fun adjustLayers() {
        manager.pause(mapView)
        layerEditSheet.setOnDismissListener {
            resumeLayers()
            hasTimeDependentLayers = mapView.layerManager.getLayers().any { it.isTimeDependent }
        }
        layerEditSheet.show()
    }

    // Distance

    private fun stopDistanceMeasurement() {
        manager.setOnDistanceEditStateChangedCallback(null)
        manager.stopDistanceMeasurement()
        views.distanceSheet.hide()
    }

    /** Starts drawing a new path from no points, as the map menu's Create path does. */
    fun startPathCreation() {
        startDistanceMeasurement(null, false)
    }

    private fun startDistanceMeasurement(location: Coordinate?, startWithUserLocation: Boolean) {
        val distanceSheet = views.distanceSheet
        val initialPoints = listOfNotNull(
            if (startWithUserLocation) userLocation else null,
            location
        )
        manager.startDistanceMeasurement(initialPoints)
        distanceSheet.show()
        distanceSheet.cancelListener = {
            stopDistanceMeasurement()
        }
        distanceSheet.createPathListener = {
            lifecycleOwner.inBackground {
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
                        null,
                        manager.getDistanceMeasurementElevations(),
                        skipElevations::get
                    ).execute(manager.getDistanceMeasurementPoints())
                }

                onMain {
                    stopDistanceMeasurement()
                    appNavigator.navigate(Route.PathOverview(pathId = id))
                }
            }
        }
        distanceSheet.undoListener = {
            manager.undoLastDistanceMeasurement()
        }
        distanceSheet.redoListener = {
            manager.redoDistanceMeasurement()
        }
        manager.setOnDistanceEditStateChangedCallback { state ->
            distanceSheet.setEditState(state)
        }
        distanceSheet.setEditState(manager.getDistanceEditState())
    }

    private fun onLongPress(location: Coordinate) {
        if (manager.isMeasuringDistance()) {
            return
        }
        manager.setSelectedLocation(location)
        lifecycleOwner.inBackground {
            val elevation = Distance.meters(DEM.getElevation(location).elevation)

            // The sheet outlives this pause-bound job, so its choice is handled on the screen's scope
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                val chosen = effects.chooseAction(
                    formatter.formatLocation(location),
                    context.getString(
                        R.string.elevation_value,
                        formatter.formatElevation(elevation)
                    ),
                    listOf(
                        ActionItem(context.getString(R.string.beacon), TrailIcons.Location) {
                            appNavigator.navigate(
                                Route.PlaceBeacon(initialLocation = location.toRouteLocation())
                            )
                            manager.setSelectedLocation(null)
                        },
                        ActionItem(context.getString(R.string.navigate), TrailIcons.Beacon) {
                            navigator.navigateTo(
                                location,
                                formatter.formatLocation(location),
                                BeaconOwner.Maps
                            )
                            manager.setSelectedLocation(null)
                        },
                        ActionItem(context.getString(R.string.distance), TrailIcons.Ruler) {
                            startDistanceMeasurement(location, true)
                            manager.setSelectedLocation(null)
                        },
                    )
                )
                if (chosen != null) {
                    chosen.action()
                } else {
                    manager.setSelectedLocation(null)
                }
            }
        }
    }

    // Lock mode and saved map state

    private fun changeLockMode(mode: MapLockMode) {
        lockMode = mode
        if (shouldSaveState) {
            mapState.putInt(LOCK_MODE_KEY, mode.id)
        }
    }

    /** Restores the saved center and scale and keeps saving them, or forgets them when saving is off. */
    private fun restoreSavedMapState(shouldSave: Boolean) {
        shouldSaveState = shouldSave
        if (shouldSave) {
            mapState.getCoordinate(CENTER_KEY)?.let { mapView.mapCenter = it }
            mapState.getFloat(SCALE_KEY)?.let { mapView.resolutionPixels = it }
        } else {
            mapState.remove(CENTER_KEY)
            mapState.remove(SCALE_KEY)
            mapState.remove(LOCK_MODE_KEY)
        }

        mapView.setOnScaleChangeListener {
            if (shouldSaveState) {
                mapState.putFloat(SCALE_KEY, it)
            }
            onBoundsMayHaveChanged()
        }
        mapView.setOnCenterChangeListener {
            if (shouldSaveState) {
                mapState.putCoordinate(CENTER_KEY, it)
            }
            onBoundsMayHaveChanged()
        }
    }

    /** The controls hidden while tracing. */
    private fun setTraceControlsVisible(visible: Boolean) {
        views.zoomInButton.isVisible = visible
        views.zoomOutButton.isVisible = visible
        if (hasTimeDependentLayers) {
            views.timeButton.isVisible = visible
        }
        views.menuButton.isVisible = visible
        views.sensorStatusBadges.isVisible = visible
    }

    private fun switchMapLockMode(mode: MapLockMode) {
        val map = mapView
        val button = views.lockButton

        // Reset trace mode defaults
        map.isInteractive = true
        map.isZoomEnabled = true
        setTraceControlsVisible(true)
        screenLight.off()
        mainActivity.setBottomNavigationEnabled(true)

        when (mode) {
            MapLockMode.Location -> {
                // Disable pan
                map.isPanEnabled = false

                // Zoom in and center on location
                map.resolution = 2f

                // Reset the rotation
                map.mapAzimuth = 0f

                // Show as locked
                button.setImageVector(TrailSenseIcons.Filled.MyLocation)
                button.setState(true)
            }

            MapLockMode.Compass -> {
                // Disable pan
                map.isPanEnabled = false

                // Show as locked
                button.setImageVector(TrailSenseIcons.Filled.Explore)
                button.setState(true)
            }

            MapLockMode.Free -> {
                // Enable pan
                map.isPanEnabled = true

                // Reset the rotation
                map.mapAzimuth = 0f

                // Show as unlocked
                button.setImageVector(TrailSenseIcons.Filled.MyLocation)
                button.setState(false)
            }

            MapLockMode.Trace -> {
                CustomUiUtils.disclaimer(
                    context,
                    context.getString(R.string.trace),
                    context.getString(R.string.map_trace_instructions),
                    "disclaimer_shown_map_trace",
                    cancelText = null
                )

                // Disable all interaction
                map.isInteractive = false
                map.isZoomEnabled = false

                // Show as locked
                button.setImageVector(TrailSenseIcons.Filled.Lock)
                button.setState(true)

                // Full brightness
                screenLight.on()

                // Hide buttons
                setTraceControlsVisible(false)

                // Hide the bottom navigation
                mainActivity.setBottomNavigationEnabled(false)
            }
        }
    }

    private fun showMapMenu() {
        val actions = listOf(
            MapAction.Measure to context.getString(R.string.measure),
            MapAction.CreatePath to context.getString(R.string.create_path),
            MapAction.AdjustLayers to context.getString(R.string.layers),
            MapAction.Trace to context.getString(R.string.trace)
        )

        views.menu.open(actions.map { it.second }) { index ->
            when (actions[index].first) {
                MapAction.Measure, MapAction.CreatePath -> startDistanceMeasurement(
                    null,
                    false
                )

                MapAction.AdjustLayers -> adjustLayers()
                MapAction.Trace -> changeLockMode(MapLockMode.Trace)
            }
        }
    }

    private fun getNextLockMode(mode: MapLockMode, hasCompass: Boolean): MapLockMode {
        return when (mode) {
            MapLockMode.Location -> {
                if (hasCompass) {
                    MapLockMode.Compass
                } else {
                    MapLockMode.Free
                }
            }

            MapLockMode.Compass -> {
                MapLockMode.Free
            }

            MapLockMode.Free -> {
                MapLockMode.Location
            }

            MapLockMode.Trace -> {
                MapLockMode.Free
            }
        }
    }

    private enum class MapLockMode(val id: Int) {
        Location(1),
        Compass(2),
        Free(3),
        Trace(4)
    }

    private companion object {
        const val CENTER_KEY = "cache_map_state_coordinate"
        const val SCALE_KEY = "cache_map_state_scale"
        const val LOCK_MODE_KEY = "cache_map_state_lock_mode"
    }
}

private data class MapViews(
    val map: MapView,
    val mapAttribution: MapAttributionState,
    val menuButton: MapIconButton,
    val menu: MapMenuState,
    val timeButton: MapIconButton,
    val zoomInButton: MapIconButton,
    val zoomOutButton: MapIconButton,
    val lockButton: MapIconButton,
    val sensorStatusBadges: SensorStatusBadgeView,
    val navigationSheet: NavigationSheetView,
    val distanceSheet: MapDistanceSheetState,
    val timeSheet: DateTimeSliderSheetState
)

@Composable
private fun MapHost(
    views: MapViews,
    modifier: Modifier = Modifier
) {
    val canvasColor = MaterialTheme.colorScheme.surface
    SideEffect {
        views.map.setBackgroundColor(canvasColor.toArgb())
    }
    Box(modifier = modifier) {
        views.map.Content(Modifier.fillMaxSize())
        if (views.mapAttribution.isVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 96.dp)
            ) {
                views.mapAttribution.Content()
            }
        }

        // Menu control (top end), with its options dropping down from it
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = TrailSenseSpacing.sm, end = TrailSenseSpacing.screen)
        ) {
            views.menuButton.Content()
            views.menu.Content()
        }

        MapControlRail(
            views = views,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = TrailSenseSpacing.screen, bottom = 88.dp)
        )

        views.sensorStatusBadges.Content(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = TrailSenseSpacing.screen, bottom = 96.dp)
        )
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            views.timeSheet.Content(Modifier.fillMaxWidth())
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            views.navigationSheet.Content(Modifier.fillMaxWidth())
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            views.distanceSheet.Content(Modifier.fillMaxWidth())
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun MapControlRail(views: MapViews, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
    ) {
        views.timeButton.Content()
        androidx.compose.material3.Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = TrailSenseElevation.low
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                views.zoomInButton.Content(grouped = true)
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                views.zoomOutButton.Content(grouped = true)
            }
        }
        views.lockButton.Content()
    }
}

@Suppress("FunctionNaming")
@Composable
private fun MapEmptyState(
    onAddMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    // A tab root like the other tabs: the kit's large top app bar (no back arrow), and the empty state filling the
    // rest of the page.
    com.kylecorry.trail_sense.shared.views.compose.TrailSensePage(
        title = stringResource(R.string.map),
        onBack = null,
        modifier = modifier
    ) {
        item(key = "empty") {
            // Where am I on my map? Until there is a map, the one thing to do is add one: one filled button, no
            // chips that all lead to the same place.
            TrailSenseEmptyState(
                // A map with a "you are here" dot: this tab answers where you are, not which maps you keep.
                icon = MapHereIcon,
                // The same living map as the Offline Maps list (a tile rising, a route, a "you are here" pulse), so
                // the tab and the tool tell one story.
                illustration = { com.kylecorry.trail_sense.shared.views.OfflineMapsEmptyIllustration() },
                title = stringResource(R.string.no_offline_maps),
                description = stringResource(R.string.map_empty_description),
                actionLabel = stringResource(R.string.add_offline_map),
                actionIcon = TrailSenseIcons.Filled.Add,
                onAction = onAddMap,
                modifier = Modifier.fillParentMaxHeight(0.9f)
            )
        }
    }
}

private fun createMapViews(context: Context): MapViews {
    val map = MapView(context).apply {
        id = R.id.map
        keepScreenOn = true
    }

    val attribution = MapAttributionState()

    val menuButton = mapControl(
        TrailSenseIcons.Filled.MoreVert,
        context.getString(R.string.map_options)
    )

    val timeButton = mapControl(
        TrailSenseIcons.Filled.Schedule,
        context.getString(R.string.time)
    ).apply {
        isVisible = false
    }

    val zoomInButton = mapControl(
        TrailSenseIcons.Filled.Add,
        context.getString(R.string.zoom_in)
    )

    val zoomOutButton = mapControl(
        TrailSenseIcons.Filled.Remove,
        context.getString(R.string.zoom_out)
    )

    val lockButton = mapControl(
        TrailSenseIcons.Filled.MyLocation,
        context.getString(R.string.map_lock_mode)
    )

    val sensorStatusBadges = SensorStatusBadgeView(context).apply {
        alpha = 0.6f
    }

    val navigationSheet = NavigationSheetView(context).apply {
        isVisible = false
    }

    val distanceSheet = MapDistanceSheetState(context)

    val timeSheet = DateTimeSliderSheetState(context)

    return MapViews(
        map = map,
        mapAttribution = attribution,
        menuButton = menuButton,
        menu = MapMenuState(),
        timeButton = timeButton,
        zoomInButton = zoomInButton,
        zoomOutButton = zoomOutButton,
        lockButton = lockButton,
        sensorStatusBadges = sensorStatusBadges,
        navigationSheet = navigationSheet,
        distanceSheet = distanceSheet,
        timeSheet = timeSheet
    )
}

/** The map's overflow menu: its options, open while [open] has been called and nothing was picked yet. */
private class MapMenuState {
    private var items by mutableStateOf<List<String>>(emptyList())
    private var onSelect: (Int) -> Unit = {}
    private var expanded by mutableStateOf(false)

    fun open(items: List<String>, onSelect: (index: Int) -> Unit) {
        this.items = items
        this.onSelect = onSelect
        expanded = true
    }

    @Composable
    fun Content() {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    }
                )
            }
        }
    }
}

private fun mapControl(icon: ImageVector, contentDescription: String): MapIconButton {
    return MapIconButton(icon, contentDescription)
}

private class MapIconButton(icon: ImageVector, private val contentDescription: String) {
    private var currentIcon by mutableStateOf(icon)
    private var isOn by mutableStateOf(false)
    private var buttonVisible by mutableStateOf(true)
    private var clickListener by mutableStateOf<(() -> Unit)?>(null)

    var isVisible: Boolean
        get() = buttonVisible
        set(value) {
            buttonVisible = value
        }

    fun setOnClickListener(listener: (() -> Unit)?) {
        clickListener = listener
    }

    fun setImageVector(icon: ImageVector) {
        currentIcon = icon
    }

    fun setState(isOn: Boolean) {
        this.isOn = isOn
    }

    @Composable
    fun Content(modifier: Modifier = Modifier, grouped: Boolean = false) {
        if (!buttonVisible) {
            return
        }
        val background by animateColorAsState(
            targetValue = if (isOn) {
                MaterialTheme.colorScheme.primaryContainer
            } else if (grouped) {
                androidx.compose.ui.graphics.Color.Transparent
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            animationSpec = tween(durationMillis = TrailSenseMotion.medium),
            label = "map-fab-bg"
        )
        val foreground by animateColorAsState(
            targetValue = if (isOn) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            animationSpec = tween(durationMillis = TrailSenseMotion.medium),
            label = "map-fab-fg"
        )

        androidx.compose.material3.Surface(
            onClick = { clickListener?.invoke() },
            enabled = clickListener != null,
            modifier = modifier
                .size(48.dp)
                .alpha(0.94f),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = background,
            shadowElevation = if (grouped) 0.dp else TrailSenseElevation.low
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = currentIcon,
                    contentDescription = contentDescription,
                    tint = foreground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MapHostPreview() {
    TrailSenseTheme {
        MapHost(
            views = createMapViews(LocalContext.current)
        )
    }
}

/** The activity hosting this screen. */
private tailrec fun Context.findMainActivity(): MainActivity = when (this) {
    is MainActivity -> this
    is ContextWrapper -> baseContext.findMainActivity()
    else -> error("The map is only shown inside MainActivity")
}
