@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.map.ui

import com.kylecorry.trail_sense.shared.navigation.toRouteLocation

import com.kylecorry.trail_sense.shared.navigation.Route

import androidx.compose.foundation.layout.heightIn
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
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
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.withSuspendableCancelableLoading
import java.util.concurrent.atomic.AtomicBoolean
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.trail_sense.shared.dialogs.Pickers
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
import com.kylecorry.trail_sense.shared.extensions.useCoordinatePreference
import com.kylecorry.trail_sense.shared.extensions.useDestroyEffect
import com.kylecorry.trail_sense.shared.extensions.useFloatPreference
import com.kylecorry.trail_sense.shared.extensions.useFlow
import com.kylecorry.trail_sense.shared.extensions.useBackgroundEffect
import com.kylecorry.trail_sense.shared.extensions.useIntPreference
import com.kylecorry.trail_sense.shared.extensions.useNavigationSensors
import com.kylecorry.trail_sense.shared.extensions.usePauseEffect
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
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MapControllerHost {
    fun onUpdate()
    fun onPause()
}

/**
 * The map tool. Its view updates run on the destination host's reactive component, which the host still owns:
 * [component] holds the hooks, [resumeCount] changes each time the screen resumes (hooks keyed on it rerun),
 * [onCleanupReactiveEffects] tears the hooks down when paused, and [onController] hands the host the controller it
 * calls on each reactive update and pause.
 */
@Composable
fun MapToolScreen(
    component: ReactiveComponent,
    resumeCount: () -> Int,
    onCleanupReactiveEffects: () -> Unit,
    onController: (MapControllerHost?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember(component) {
        MapController(
            component,
            context,
            navigator,
            effects,
            lifecycleOwner,
            resumeCount,
            onCleanupReactiveEffects
        )
    }
    DisposableEffect(controller) {
        onController(controller)
        onDispose {
            onController(null)
            controller.onDestroy()
        }
    }
    controller.Content(modifier = modifier.fillMaxSize())
}

private class MapController(
    private val component: ReactiveComponent,
    private val context: Context,
    private val appNavigator: TrailSenseNavigator,
    private val effects: ScreenEffects,
    private val lifecycleOwner: LifecycleOwner,
    private val resumeCount: () -> Int,
    private val onCleanupReactiveEffects: () -> Unit
) : MapControllerHost {

    // Only the activity can hide its bottom navigation (trace mode)
    private val mainActivity = context.findMainActivity()
    private val featureClickHandler =
        MapFeatureClickHandler(context, appNavigator, effects, lifecycleOwner.lifecycleScope)

    val views = createMapViews(context)
    private var wasViewCreated = true
    private val layerEditSheet = MapLayersSheetState(MapToolRegistration.MAP_ID)
    private val mapService = MapService.getInstance(context)
    private var hasRenderableMap by mutableStateOf<Boolean?>(null)

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        LaunchedEffect(resumeCount()) {
            hasRenderableMap = withContext(Dispatchers.IO) {
                runCatching {
                    mapService.getAllVectorMaps().any { it.visible } ||
                            mapService.getAllPhotoMaps().any { it.visible && it.isCalibrated }
                }.getOrDefault(false)
            }
        }

        when (hasRenderableMap) {
            true -> {
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

    override fun onPause() {
        onCleanupReactiveEffects()
    }

    fun onDestroy() {
        wasViewCreated = false
        onPause()
    }

    override fun onUpdate() {
        if (!wasViewCreated) {
            return
        }

        val views = views
        val mapView = views.map
        val lockButton = views.lockButton
        val zoomInButton = views.zoomInButton
        val zoomOutButton = views.zoomOutButton
        val timeButton = views.timeButton
        val menuButton = views.menuButton
        val navigationSheetView = views.navigationSheet
        val mapDistanceSheetView = views.distanceSheet
        val attributionView = views.mapAttribution
        val timeSheet = views.timeSheet
        val sensorStatusBadges = views.sensorStatusBadges
        val (mapTime, setMapTime) = component.useState<Instant?>(null)
        val (hasTimeDependentLayers, setHasTimeDependentLayers) = component.useState(false)

        component.useEffect(timeButton, timeSheet, mapTime, hasTimeDependentLayers) {
            timeButton.isVisible = hasTimeDependentLayers
            if (hasTimeDependentLayers) {
                timeButton.setState(mapTime != null || timeSheet.isVisible)
            } else {
                if (timeSheet.isVisible) {
                    setMapTime(null)
                    timeSheet.hide()
                }
            }

            timeButton.setOnClickListener {
                if (timeSheet.isVisible) {
                    setMapTime(null)
                    timeSheet.hide()
                } else {
                    timeSheet.setTime(mapTime)
                    timeSheet.show()
                }
                timeButton.setState(mapTime != null || timeSheet.isVisible)
            }

            timeSheet.onTimeChanged = {
                setMapTime(it)
            }
        }

        val navigation = component.useNavigationSensors(trueNorth = true)
        val context = component.useAndroidContext()
        val sensors = component.useService<SensorService>()
        val hasCompass = component.useMemo(sensors) { sensors.hasCompass() }
        val navigator = component.useService<Navigator>()
        val pathService = component.useService<PathService>()
        val destination = component.useFlow(navigator.destination2, state = BackgroundMinimumState.Resumed)
        val prefs = component.useService<UserPreferences>()
        val formatter = component.useService<FormatService>()
        val activity = component.useActivity()
        val rootView = component.useRootView()
        val (lockMode, setLockMode) = useLockMode()

        val screenLight = component.useMemo(activity) {
            ScreenTorch(activity.window)
        }

        val screenLock = component.useMemo(prefs) {
            NavigationScreenLock(prefs.map.keepScreenUnlockedWhileOpen)
        }

        component.useEffect(mapView, prefs, resumeCount()) {
            mapView.useDensityPixelsForZoom = !prefs.map.highDetailMode
            mapView.layerManager.invalidate()
            mapView.invalidate()
        }

        component.useEffect(screenLock, activity, resumeCount(), destination) {
            screenLock.updateLock(activity)
        }

        component.useDestroyEffect(screenLock, activity) {
            screenLock.releaseLock(activity)
        }

        component.useEffect(lockButton, lockMode, hasCompass) {
            lockButton.setOnClickListener {
                setLockMode(getNextLockMode(lockMode, hasCompass))
            }
        }

        // Layers
        val manager = component.useMemo { MapToolLayerManager() }
        component.useEffect(manager, mapView, mapTime, manager.key) {
            manager.setTime(mapView, mapTime)
        }
        component.useEffectWithCleanup(manager, mapView, resumeCount()) {
            manager.resume(mapView, lifecycleOwner, featureClickHandler::onFeatureClick)
            return@useEffectWithCleanup {
                manager.pause(mapView)
            }
        }

        component.useEffect(manager.key, mapView) {
            setHasTimeDependentLayers(mapView.layerManager.getLayers().any { it.isTimeDependent })
        }

        component.useBackgroundEffect(mapView, manager.key, attributionView) {
            val attribution = mapView.getAttribution(context)
            onMain {
                attributionView.text = attribution
                attributionView.isVisible = attribution != null
            }
        }

        component.usePauseEffect(layerEditSheet) {
            tryOrNothing {
                layerEditSheet.setOnDismissListener(null)
                layerEditSheet.dismissAndUpdateWidgets(context)
            }
        }

        val adjustLayers = component.useCallback<Unit>(manager, layerEditSheet, context, mapView) {
            manager.pause(mapView)
            layerEditSheet.setOnDismissListener {
                manager.resume(mapView, lifecycleOwner, featureClickHandler::onFeatureClick)
                setHasTimeDependentLayers(
                    mapView.layerManager.getLayers().any { it.isTimeDependent }
                )
            }
            layerEditSheet.show()
        }

        // Distance
        val stopDistanceMeasurement = component.useCallback<Unit>(mapDistanceSheetView, manager) {
            manager.setOnDistanceEditStateChangedCallback(null)
            manager.stopDistanceMeasurement()
            mapDistanceSheetView.hide()
        }

        val startDistanceMeasurement =
            component.useCallback(
                mapDistanceSheetView,
                stopDistanceMeasurement,
                appNavigator,
                manager,
                pathService,
                navigation.location
            ) { location: Coordinate?, startWithUserLocation: Boolean ->
                val initialPoints = listOfNotNull(
                    if (startWithUserLocation) navigation.location else null,
                    location
                )
                manager.startDistanceMeasurement(initialPoints)
                mapDistanceSheetView.show()
                mapDistanceSheetView.cancelListener = {
                    stopDistanceMeasurement()
                }
                mapDistanceSheetView.createPathListener = {
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
                mapDistanceSheetView.undoListener = {
                    manager.undoLastDistanceMeasurement()
                }
                mapDistanceSheetView.redoListener = {
                    manager.redoDistanceMeasurement()
                }
                manager.setOnDistanceEditStateChangedCallback { state ->
                    mapDistanceSheetView.setEditState(state)
                }
                mapDistanceSheetView.setEditState(manager.getDistanceEditState())
            }

        // Update layer values
        component.useEffect(mapView, navigation.location, navigation.locationAccuracy) {
            mapView.userLocation = navigation.location
            mapView.userLocationAccuracy = navigation.locationAccuracy
            mapView.invalidate()
        }

        component.useEffect(mapView, navigation.bearing) {
            mapView.userAzimuth = navigation.bearing
        }

        component.useEffect(manager, mapView.mapBounds, manager.key) {
            manager.onBoundsChanged()
        }

        val traceViews =
            component.useMemo(zoomInButton, zoomOutButton, timeButton, menuButton, sensorStatusBadges, hasTimeDependentLayers) {
                listOfNotNull(
                    { visible: Boolean -> zoomInButton.isVisible = visible },
                    { visible: Boolean -> zoomOutButton.isVisible = visible },
                    if (hasTimeDependentLayers) {
                        { visible: Boolean -> timeButton.isVisible = visible }
                    } else {
                        null
                    },
                    { visible: Boolean -> menuButton.isVisible = visible },
                    { visible: Boolean -> sensorStatusBadges.isVisible = visible }
                )
            }

        component.useEffect(lockMode, mapView, lockButton, traceViews, screenLight, resumeCount()) {
            switchMapLockMode(lockMode, mapView, lockButton, traceViews, screenLight)
        }

        component.usePauseEffect(screenLight) {
            screenLight.off()
            mainActivity.setBottomNavigationEnabled(true)
        }

        useSavedMapState(mapView)

        component.useEffect(mapView, lockMode, navigation.location) {
            if (mapView.mapCenter == Coordinate.zero) {
                mapView.mapCenter = navigation.location
                mapView.resolution = 2f
            }

            if (lockMode == MapLockMode.Location || lockMode == MapLockMode.Compass) {
                mapView.mapCenter = navigation.location
            }
        }

        component.useEffect(mapView, lockMode, navigation.bearing) {
            if (lockMode == MapLockMode.Compass) {
                mapView.mapAzimuth = navigation.bearing.value
            }
        }

        component.useEffect(zoomInButton, zoomOutButton) {
            zoomInButton.setState(false)
            zoomOutButton.setState(false)

            zoomInButton.setOnClickListener {
                mapView.zoom(2f)
            }

            zoomOutButton.setOnClickListener {
                mapView.zoom(0.5f)
            }
        }

        component.useEffect(navigationSheetView, destination, navigation) {
            if (destination != null) {
                navigationSheetView.updateNavigationSensorValues(navigation)
                navigationSheetView.setTrueNorthOverride(true)
                navigationSheetView.show(destination, true)
            } else {
                navigationSheetView.hide()
            }
        }

        component.useEffect(mapView, manager, appNavigator, startDistanceMeasurement, prefs) {
            mapView.setOnLongPressListener { location ->
                if (manager.isMeasuringDistance()) {
                    return@setOnLongPressListener
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
        }

        component.useEffect(mapDistanceSheetView, manager, prefs) {
            manager.setOnDistanceChangedCallback { distance ->
                val relative = distance
                    .convertTo(prefs.baseDistanceUnits)
                    .toRelativeDistance()
                mapDistanceSheetView.setDistance(relative)
            }
        }

        // Menu
        component.useEffect(menuButton, startDistanceMeasurement) {
            menuButton.setState(false)
            menuButton.setOnClickListener {
                showMapMenu(rootView, startDistanceMeasurement, { adjustLayers() }, setLockMode)
            }
        }

        // Sensor status
        component.useEffect(sensorStatusBadges, navigation.gps, navigation.compass) {
            val gps = navigation.gps ?: return@useEffect
            val compass = navigation.compass ?: return@useEffect
            sensorStatusBadges.setSensors(gps as ISatelliteGPS, compass)
            sensorStatusBadges.setOnClickListener {
                ImproveAccuracyAlerter(context).alert(sensorStatusBadges.getSensors())
            }
        }
    }

    private fun switchMapLockMode(
        mode: MapLockMode,
        map: MapView,
        button: MapIconButton,
        traceViews: List<(Boolean) -> Unit>,
        screenLight: ScreenTorch
    ) {
        // Reset trace mode defaults
        map.isInteractive = true
        map.isZoomEnabled = true
        traceViews.forEach { it(true) }
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
                traceViews.forEach { it(false) }

                // Hide the bottom navigation
                mainActivity.setBottomNavigationEnabled(false)
            }
        }
    }

    private fun showMapMenu(
        anchor: View,
        startDistanceMeasurement: (Coordinate?, Boolean) -> Unit,
        adjustLayers: () -> Unit,
        setLockMode: (MapLockMode) -> Unit
    ) {
        val actions = listOf(
            MapAction.Measure to context.getString(R.string.measure),
            MapAction.CreatePath to context.getString(R.string.create_path),
            MapAction.AdjustLayers to context.getString(R.string.layers),
            MapAction.Trace to context.getString(R.string.trace)
        )

        Pickers.menu(
            anchor,
            actions.map { action -> action.second }
        ) { index ->
            when (actions[index].first) {
                MapAction.Measure, MapAction.CreatePath -> startDistanceMeasurement(
                    null,
                    false
                )

                MapAction.AdjustLayers -> adjustLayers()
                MapAction.Trace -> setLockMode(MapLockMode.Trace)
            }
            true
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

    private fun useSavedMapState(mapView: MapView) {
        val prefs = component.useService<UserPreferences>()
        val shouldSave = component.useMemo(prefs, resumeCount()) {
            prefs.map.saveMapState
        }
        val key = "cache_map_state"
        val (center, setCenter) = component.useCoordinatePreference("${key}_coordinate")
        val (scale, setScale) = component.useFloatPreference("${key}_scale")

        component.useEffect(mapView, shouldSave) {
            if (!shouldSave) {
                setCenter(null)
                setScale(null)
            } else {
                // Intentionally not in the useEffect dependencies
                center?.let { mapView.mapCenter = it }
                scale?.let { mapView.resolutionPixels = it }
            }

            mapView.setOnScaleChangeListener {
                if (shouldSave) {
                    setScale(it)
                }
            }
            mapView.setOnCenterChangeListener {
                if (shouldSave) {
                    setCenter(it)
                }
            }
        }
    }

    private fun useLockMode(): Pair<MapLockMode, (MapLockMode) -> Unit> {
        val prefs = component.useService<UserPreferences>()
        val shouldSave = component.useMemo(prefs, resumeCount()) {
            prefs.map.saveMapState
        }
        val key = "cache_map_state"
        val (savedLockModeId, setSavedLockModeId) = component.useIntPreference("${key}_lock_mode")
        val savedLockMode = component.useMemo(savedLockModeId) {
            MapLockMode.entries.firstOrNull { it.id == savedLockModeId && it != MapLockMode.Trace }
        }
        val (lockMode, setLockMode) = component.useState(savedLockMode ?: MapLockMode.Free)
        val exposedSetLockMode = component.useCallback(shouldSave) { newLockMode: MapLockMode ->
            setLockMode(newLockMode)
            if (shouldSave) {
                setSavedLockModeId(newLockMode.id)
            }
        }

        component.useEffect(shouldSave) {
            if (!shouldSave) {
                setSavedLockModeId(null)
            }
        }

        return lockMode to exposedSetLockMode
    }

    private enum class MapLockMode(val id: Int) {
        Location(1),
        Compass(2),
        Free(3),
        Trace(4)
    }
}

private data class MapViews(
    val map: MapView,
    val mapAttribution: MapAttributionState,
    val menuButton: MapIconButton,
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

        // Menu control (top end)
        views.menuButton.Content(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = TrailSenseSpacing.sm, end = TrailSenseSpacing.screen)
        )

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
