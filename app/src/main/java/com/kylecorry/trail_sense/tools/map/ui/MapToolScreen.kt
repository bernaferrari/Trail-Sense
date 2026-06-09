package com.kylecorry.trail_sense.tools.map.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.fragments.useFlow
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.torch.ScreenTorch
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.ImproveAccuracyAlerter
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.extensions.useCoordinatePreference
import com.kylecorry.trail_sense.shared.extensions.useDestroyEffect
import com.kylecorry.trail_sense.shared.extensions.useFloatPreference
import com.kylecorry.trail_sense.shared.extensions.useIntPreference
import com.kylecorry.trail_sense.shared.extensions.useNavigationSensors
import com.kylecorry.trail_sense.shared.extensions.usePauseEffect
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersSheetState
import com.kylecorry.trail_sense.shared.map_layers.ui.MapAttributionState
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getAttribution
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.shared.views.DateTimeSliderSheetState
import com.kylecorry.trail_sense.shared.views.SensorStatusBadgeView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseReactiveDestinationHost
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationScreenLock
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.NavigationSheetView
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.MapDistanceSheetState
import com.kylecorry.trail_sense.tools.paths.infrastructure.commands.CreatePathCommand
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import java.time.Instant

interface MapControllerHost {
    fun onUpdate()
    fun onPause()
}

@Composable
fun MapToolScreen(
    component: ReactiveComponent,
    host: TrailSenseReactiveDestinationHost,
    onController: (MapControllerHost?) -> Unit,
    modifier: Modifier = Modifier
) {
    val controller = remember(component, host) { MapController(component, host) }
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
    private val host: TrailSenseReactiveDestinationHost
) : MapControllerHost {

    val views = createMapViews(host.requireContext())
    private var wasViewCreated = true
    private val layerEditSheet = MapLayersSheetState(MapToolRegistration.MAP_ID)

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        MapHost(
            views = views,
            modifier = modifier
        )
        layerEditSheet.Content(host.requireContext(), ::navigateFromLayerSheet)
    }

    private fun navigateFromLayerSheet(action: Int) {
        host.findNavController().navigateWithAnimation(action)
    }

    override fun onPause() {
        host.cleanupReactiveEffects()
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
        val navController = host.findNavController()
        val rootView = component.useRootView()
        val (lockMode, setLockMode) = useLockMode()

        val screenLight = component.useMemo(activity) {
            ScreenTorch(activity.window)
        }

        val screenLock = component.useMemo(prefs) {
            NavigationScreenLock(prefs.map.keepScreenUnlockedWhileOpen)
        }

        component.useEffect(mapView, prefs, host.hookResetOnResume) {
            mapView.useDensityPixelsForZoom = !prefs.map.highDetailMode
            mapView.layerManager.invalidate()
            mapView.invalidate()
        }

        component.useEffect(screenLock, activity, host.hookResetOnResume, destination) {
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
        component.useEffectWithCleanup(manager, mapView, host.hookResetOnResume) {
            manager.resume(context, mapView, host)
            return@useEffectWithCleanup {
                manager.pause(mapView)
            }
        }

        component.useEffect(manager.key, mapView) {
            setHasTimeDependentLayers(mapView.layerManager.getLayers().any { it.isTimeDependent })
        }

        component.useEffect(mapView) {
            mapView.setBackgroundColor(Color.rgb(127, 127, 127))
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
                manager.resume(context, mapView, host)
                setHasTimeDependentLayers(
                    mapView.layerManager.getLayers().any { it.isTimeDependent }
                )
            }
            layerEditSheet.show()
        }

        // Distance
        val stopDistanceMeasurement = component.useCallback<Unit>(mapDistanceSheetView, manager) {
            manager.stopDistanceMeasurement()
            mapDistanceSheetView.hide()
        }

        val startDistanceMeasurement =
            component.useCallback(
                mapDistanceSheetView,
                stopDistanceMeasurement,
                navController,
                manager,
                pathService,
                navigation.location
            ) { location: Coordinate?, startWithUserLocation: Boolean ->
                val initialPoints = listOfNotNull(
                    if (startWithUserLocation) navigation.location else null,
                    location
                ).toTypedArray()
                manager.startDistanceMeasurement(initialPoints)
                mapDistanceSheetView.show()
                mapDistanceSheetView.cancelListener = {
                    stopDistanceMeasurement()
                }
                mapDistanceSheetView.createPathListener = {
                    host.inBackground {
                        val id = CreatePathCommand(
                            pathService,
                            prefs.navigation,
                            null
                        ).execute(manager.getDistanceMeasurementPoints())

                        onMain {
                            navController.navigateWithAnimation(
                                R.id.pathDetailsFragment,
                                Bundle().apply {
                                    putLong("path_id", id)
                                }
                            )
                        }
                    }
                }
                mapDistanceSheetView.undoListener = {
                    manager.undoLastDistanceMeasurement()
                }
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

        component.useEffect(lockMode, mapView, lockButton, traceViews, screenLight, context, host.hookResetOnResume) {
            switchMapLockMode(lockMode, mapView, lockButton, traceViews, screenLight, context)
        }

        component.usePauseEffect(screenLight) {
            screenLight.off()
            host.requireMainActivity().setBottomNavigationEnabled(true)
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

        component.useEffect(mapView, manager, navController, startDistanceMeasurement, prefs) {
            mapView.setOnLongPressListener { location ->
                if (manager.isMeasuringDistance()) {
                    return@setOnLongPressListener
                }
                manager.setSelectedLocation(location)
                host.inBackground {
                    val elevation = Distance.meters(DEM.getElevation(location).elevation)

                    onMain {
                        Share.actions(
                            host,
                            formatter.formatLocation(location),
                            listOf(
                                ActionItem(host.getString(R.string.beacon), R.drawable.ic_location) {
                                    val bundle = Bundle().apply {
                                        putParcelable("initial_location", GeoUri(location))
                                    }
                                    navController.navigateWithAnimation(
                                        R.id.placeBeaconFragment,
                                        bundle
                                    )
                                    manager.setSelectedLocation(null)
                                },
                                ActionItem(host.getString(R.string.navigate), R.drawable.ic_beacon) {
                                    navigator.navigateTo(
                                        location,
                                        formatter.formatLocation(location),
                                        BeaconOwner.Maps
                                    )
                                    manager.setSelectedLocation(null)
                                },
                                ActionItem(host.getString(R.string.distance), R.drawable.ruler) {
                                    startDistanceMeasurement(location, true)
                                    manager.setSelectedLocation(null)
                                },
                            ),
                            subtitle = host.getString(
                                R.string.elevation_value,
                                formatter.formatElevation(elevation)
                            ),
                        ) {
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

        // Disclaimer
        component.useEffect(context) {
            CustomUiUtils.disclaimer(
                context,
                host.getString(R.string.map),
                host.getString(R.string.map_tool_disclaimer),
                "pref_map_tool_disclaimer_shown"
            )
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
        screenLight: ScreenTorch,
        context: android.content.Context
    ) {
        // Reset trace mode defaults
        map.isInteractive = true
        map.isZoomEnabled = true
        traceViews.forEach { it(true) }
        screenLight.off()
        host.requireMainActivity().setBottomNavigationEnabled(true)

        when (mode) {
            MapLockMode.Location -> {
                // Disable pan
                map.isPanEnabled = false

                // Zoom in and center on location
                map.resolution = 2f

                // Reset the rotation
                map.mapAzimuth = 0f

                // Show as locked
                button.setImageResource(R.drawable.satellite)
                button.setState(true)
            }

            MapLockMode.Compass -> {
                // Disable pan
                map.isPanEnabled = false

                // Show as locked
                button.setImageResource(R.drawable.ic_compass_icon)
                button.setState(true)
            }

            MapLockMode.Free -> {
                // Enable pan
                map.isPanEnabled = true

                // Reset the rotation
                map.mapAzimuth = 0f

                // Show as unlocked
                button.setImageResource(R.drawable.satellite)
                button.setState(false)
            }

            MapLockMode.Trace -> {
                CustomUiUtils.disclaimer(
                    context,
                    host.getString(R.string.trace),
                    host.getString(R.string.map_trace_instructions),
                    "disclaimer_shown_map_trace",
                    cancelText = null
                )

                // Disable all interaction
                map.isInteractive = false
                map.isZoomEnabled = false

                // Show as locked
                button.setImageResource(R.drawable.lock)
                button.setState(true)

                // Full brightness
                screenLight.on()

                // Hide buttons
                traceViews.forEach { it(false) }

                // Hide the bottom navigation
                host.requireMainActivity().setBottomNavigationEnabled(false)
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
            MapAction.Measure to host.getString(R.string.measure),
            MapAction.CreatePath to host.getString(R.string.create_path),
            MapAction.AdjustLayers to host.getString(R.string.layers),
            MapAction.Trace to host.getString(R.string.trace)
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
        val shouldSave = component.useMemo(prefs, host.hookResetOnResume) {
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
        val shouldSave = component.useMemo(prefs, host.hookResetOnResume) {
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
        views.menuButton.Content(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 16.dp)
        )
        views.timeButton.Content(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 256.dp)
        )
        views.zoomInButton.Content(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 200.dp)
        )
        views.zoomOutButton.Content(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 144.dp)
        )
        views.lockButton.Content(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 88.dp)
        )
        views.sensorStatusBadges.Content(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 96.dp)
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

private fun createMapViews(context: Context): MapViews {
    val map = MapView(context).apply {
        id = R.id.map
        keepScreenOn = true
    }

    val attribution = MapAttributionState()

    val menuButton = smallMapFab(R.drawable.ic_menu_dots)

    val timeButton = smallMapFab(R.drawable.ic_tool_clock).apply {
        isVisible = false
    }

    val zoomInButton = smallMapFab(R.drawable.ic_add)

    val zoomOutButton = smallMapFab(R.drawable.ic_zoom_out)

    val lockButton = smallMapFab(R.drawable.satellite)

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

private fun smallMapFab(drawable: Int): MapIconButton {
    return MapIconButton(drawable).apply {
        alpha = 0.86f
    }
}

private class MapIconButton(icon: Int) {
    private var iconRes by mutableIntStateOf(icon)
    private var isOn by mutableStateOf(false)
    private var buttonVisible by mutableStateOf(true)
    private var alphaState by mutableStateOf(1f)
    private var clickListener by mutableStateOf<(() -> Unit)?>(null)

    var alpha: Float
        get() = alphaState
        set(value) {
            alphaState = value
        }

    var isVisible: Boolean
        get() = buttonVisible
        set(value) {
            buttonVisible = value
        }

    fun setOnClickListener(listener: (() -> Unit)?) {
        clickListener = listener
    }

    fun setImageResource(icon: Int) {
        iconRes = icon
    }

    fun setState(isOn: Boolean) {
        this.isOn = isOn
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!buttonVisible) {
            return
        }
        val background = if (isOn) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
        val foreground = if (isOn) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        androidx.compose.material3.Surface(
            modifier = modifier
                .size(40.dp)
                .alpha(alphaState)
                .clickable(enabled = clickListener != null) {
                    clickListener?.invoke()
                },
            shape = androidx.compose.foundation.shape.CircleShape,
            color = background,
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
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
