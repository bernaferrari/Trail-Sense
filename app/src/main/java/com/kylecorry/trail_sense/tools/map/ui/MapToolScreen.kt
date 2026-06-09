package com.kylecorry.trail_sense.tools.map.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
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
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.ReactiveAndromedaFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.show
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
import com.kylecorry.trail_sense.shared.extensions.useNavController
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
    fragment: ReactiveAndromedaFragment,
    onController: (MapControllerHost?) -> Unit,
    modifier: Modifier = Modifier
) {
    val controller = remember(fragment) { MapController(fragment) }
    DisposableEffect(controller) {
        onController(controller)
        onDispose {
            onController(null)
            controller.onDestroy()
        }
    }
    controller.Content(modifier = modifier.fillMaxSize())
}

private class MapController(private val fragment: ReactiveAndromedaFragment) : MapControllerHost {

    val views = createMapViews(fragment.requireContext())
    private val host: TrailSenseReactiveDestinationHost
        get() = fragment as TrailSenseReactiveDestinationHost
    private var wasViewCreated = true
    private val layerEditSheet = MapLayersSheetState(MapToolRegistration.MAP_ID)

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        MapHost(
            views = views,
            modifier = modifier
        )
        layerEditSheet.Content(fragment.requireContext(), ::navigateFromLayerSheet)
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
        val (mapTime, setMapTime) = fragment.useState<Instant?>(null)
        val (hasTimeDependentLayers, setHasTimeDependentLayers) = fragment.useState(false)

        fragment.useEffect(timeButton, timeSheet, mapTime, hasTimeDependentLayers) {
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

        val navigation = fragment.useNavigationSensors(trueNorth = true)
        val context = fragment.useAndroidContext()
        val sensors = fragment.useService<SensorService>()
        val hasCompass = fragment.useMemo(sensors) { sensors.hasCompass() }
        val navigator = fragment.useService<Navigator>()
        val pathService = fragment.useService<PathService>()
        val destination = fragment.useFlow(navigator.destination2, state = BackgroundMinimumState.Resumed)
        val prefs = fragment.useService<UserPreferences>()
        val formatter = fragment.useService<FormatService>()
        val activity = fragment.useActivity()
        val navController = fragment.useNavController()
        val (lockMode, setLockMode) = useLockMode()

        val screenLight = fragment.useMemo(activity) {
            ScreenTorch(activity.window)
        }

        val screenLock = fragment.useMemo(prefs) {
            NavigationScreenLock(prefs.map.keepScreenUnlockedWhileOpen)
        }

        fragment.useEffect(mapView, prefs, host.hookResetOnResume) {
            mapView.useDensityPixelsForZoom = !prefs.map.highDetailMode
            mapView.layerManager.invalidate()
            mapView.invalidate()
        }

        fragment.useEffect(screenLock, activity, host.hookResetOnResume, destination) {
            screenLock.updateLock(activity)
        }

        fragment.useDestroyEffect(screenLock, activity) {
            screenLock.releaseLock(activity)
        }

        fragment.useEffect(lockButton, lockMode, hasCompass) {
            lockButton.setOnClickListener {
                setLockMode(getNextLockMode(lockMode, hasCompass))
            }
        }

        // Layers
        val manager = fragment.useMemo { MapToolLayerManager() }
        fragment.useEffect(manager, mapView, mapTime, manager.key) {
            manager.setTime(mapView, mapTime)
        }
        fragment.useEffectWithCleanup(manager, mapView, host.hookResetOnResume) {
            manager.resume(context, mapView, host)
            return@useEffectWithCleanup {
                manager.pause(mapView)
            }
        }

        fragment.useEffect(manager.key, mapView) {
            setHasTimeDependentLayers(mapView.layerManager.getLayers().any { it.isTimeDependent })
        }

        fragment.useEffect(mapView) {
            mapView.setBackgroundColor(Color.rgb(127, 127, 127))
        }

        fragment.useBackgroundEffect(mapView, manager.key, attributionView) {
            val attribution = mapView.getAttribution(context)
            onMain {
                attributionView.text = attribution
                attributionView.isVisible = attribution != null
            }
        }

        fragment.usePauseEffect(layerEditSheet) {
            tryOrNothing {
                layerEditSheet.setOnDismissListener(null)
                layerEditSheet.dismissAndUpdateWidgets(context)
            }
        }

        val adjustLayers = fragment.useCallback<Unit>(manager, layerEditSheet, context, mapView) {
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
        val stopDistanceMeasurement = fragment.useCallback<Unit>(mapDistanceSheetView, manager) {
            manager.stopDistanceMeasurement()
            mapDistanceSheetView.hide()
        }

        val startDistanceMeasurement =
            fragment.useCallback(
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
                    fragment.inBackground {
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
        fragment.useEffect(mapView, navigation.location, navigation.locationAccuracy) {
            mapView.userLocation = navigation.location
            mapView.userLocationAccuracy = navigation.locationAccuracy
            mapView.invalidate()
        }

        fragment.useEffect(mapView, navigation.bearing) {
            mapView.userAzimuth = navigation.bearing
        }

        fragment.useEffect(manager, mapView.mapBounds, manager.key) {
            manager.onBoundsChanged()
        }

        val traceViews =
            fragment.useMemo(zoomInButton, zoomOutButton, timeButton, menuButton, sensorStatusBadges, hasTimeDependentLayers) {
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

        fragment.useEffect(lockMode, mapView, lockButton, traceViews, screenLight, context, host.hookResetOnResume) {
            switchMapLockMode(lockMode, mapView, lockButton, traceViews, screenLight, context)
        }

        fragment.usePauseEffect(screenLight) {
            screenLight.off()
            host.requireMainActivity().setBottomNavigationEnabled(true)
        }

        useSavedMapState(mapView)

        fragment.useEffect(mapView, lockMode, navigation.location) {
            if (mapView.mapCenter == Coordinate.zero) {
                mapView.mapCenter = navigation.location
                mapView.resolution = 2f
            }

            if (lockMode == MapLockMode.Location || lockMode == MapLockMode.Compass) {
                mapView.mapCenter = navigation.location
            }
        }

        fragment.useEffect(mapView, lockMode, navigation.bearing) {
            if (lockMode == MapLockMode.Compass) {
                mapView.mapAzimuth = navigation.bearing.value
            }
        }

        fragment.useEffect(zoomInButton, zoomOutButton) {
            zoomInButton.setState(false)
            zoomOutButton.setState(false)

            zoomInButton.setOnClickListener {
                mapView.zoom(2f)
            }

            zoomOutButton.setOnClickListener {
                mapView.zoom(0.5f)
            }
        }

        fragment.useEffect(navigationSheetView, destination, navigation) {
            if (destination != null) {
                navigationSheetView.updateNavigationSensorValues(navigation)
                navigationSheetView.setTrueNorthOverride(true)
                navigationSheetView.show(destination, true)
            } else {
                navigationSheetView.hide()
            }
        }

        fragment.useEffect(mapView, manager, navController, startDistanceMeasurement, prefs) {
            mapView.setOnLongPressListener { location ->
                if (manager.isMeasuringDistance()) {
                    return@setOnLongPressListener
                }
                manager.setSelectedLocation(location)
                fragment.inBackground {
                    val elevation = Distance.meters(DEM.getElevation(location).elevation)

                    onMain {
                        Share.actions(
                            host,
                            formatter.formatLocation(location),
                            listOf(
                                ActionItem(fragment.getString(R.string.beacon), R.drawable.ic_location) {
                                    val bundle = Bundle().apply {
                                        putParcelable("initial_location", GeoUri(location))
                                    }
                                    navController.navigateWithAnimation(
                                        R.id.placeBeaconFragment,
                                        bundle
                                    )
                                    manager.setSelectedLocation(null)
                                },
                                ActionItem(fragment.getString(R.string.navigate), R.drawable.ic_beacon) {
                                    navigator.navigateTo(
                                        location,
                                        formatter.formatLocation(location),
                                        BeaconOwner.Maps
                                    )
                                    manager.setSelectedLocation(null)
                                },
                                ActionItem(fragment.getString(R.string.distance), R.drawable.ruler) {
                                    startDistanceMeasurement(location, true)
                                    manager.setSelectedLocation(null)
                                },
                            ),
                            subtitle = fragment.getString(
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

        fragment.useEffect(mapDistanceSheetView, manager, prefs) {
            manager.setOnDistanceChangedCallback { distance ->
                val relative = distance
                    .convertTo(prefs.baseDistanceUnits)
                    .toRelativeDistance()
                mapDistanceSheetView.setDistance(relative)
            }
        }

        // Menu
        fragment.useEffect(menuButton, startDistanceMeasurement) {
            menuButton.setState(false)
            menuButton.setOnClickListener {
                showMapMenu(startDistanceMeasurement, { adjustLayers() }, setLockMode)
            }
        }

        // Disclaimer
        fragment.useEffect(context) {
            CustomUiUtils.disclaimer(
                context,
                fragment.getString(R.string.map),
                fragment.getString(R.string.map_tool_disclaimer),
                "pref_map_tool_disclaimer_shown"
            )
        }

        // Sensor status
        fragment.useEffect(sensorStatusBadges, navigation.gps, navigation.compass) {
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
                    fragment.getString(R.string.trace),
                    fragment.getString(R.string.map_trace_instructions),
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
        startDistanceMeasurement: (Coordinate?, Boolean) -> Unit,
        adjustLayers: () -> Unit,
        setLockMode: (MapLockMode) -> Unit
    ) {
        val actions = listOf(
            MapAction.Measure to fragment.getString(R.string.measure),
            MapAction.CreatePath to fragment.getString(R.string.create_path),
            MapAction.AdjustLayers to fragment.getString(R.string.layers),
            MapAction.Trace to fragment.getString(R.string.trace)
        )

        Pickers.menu(
            fragment.requireView(),
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
        val prefs = fragment.useService<UserPreferences>()
        val shouldSave = fragment.useMemo(prefs, host.hookResetOnResume) {
            prefs.map.saveMapState
        }
        val key = "cache_map_state"
        val (center, setCenter) = fragment.useCoordinatePreference("${key}_coordinate")
        val (scale, setScale) = fragment.useFloatPreference("${key}_scale")

        fragment.useEffect(mapView, shouldSave) {
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
        val prefs = fragment.useService<UserPreferences>()
        val shouldSave = fragment.useMemo(prefs, host.hookResetOnResume) {
            prefs.map.saveMapState
        }
        val key = "cache_map_state"
        val (savedLockModeId, setSavedLockModeId) = fragment.useIntPreference("${key}_lock_mode")
        val savedLockMode = fragment.useMemo(savedLockModeId) {
            MapLockMode.entries.firstOrNull { it.id == savedLockModeId && it != MapLockMode.Trace }
        }
        val (lockMode, setLockMode) = fragment.useState(savedLockMode ?: MapLockMode.Free)
        val exposedSetLockMode = fragment.useCallback(shouldSave) { newLockMode: MapLockMode ->
            setLockMode(newLockMode)
            if (shouldSave) {
                setSavedLockModeId(newLockMode.id)
            }
        }

        fragment.useEffect(shouldSave) {
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
