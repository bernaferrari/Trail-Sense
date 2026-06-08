package com.kylecorry.trail_sense.tools.map.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.fragments.useClickCallback
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
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersBottomSheet
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseReactiveComposeFragment
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationScreenLock
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.NavigationSheetView
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.MapDistanceSheetState
import com.kylecorry.trail_sense.tools.paths.infrastructure.commands.CreatePathCommand
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import java.time.Instant

class MapFragment : TrailSenseReactiveComposeFragment() {

    private var mapViews: MapViews? = null
    private var wasViewCreated = false

    override fun beforeCreateContent() {
        mapViews = createMapViews(requireContext())
        wasViewCreated = true
    }

    @Composable
    override fun FragmentContent() {
        MapHost(
            views = mapViews ?: error("Map views are not ready"),
            modifier = Modifier.fillMaxSize()
        )
    }

    override fun onDestroyView() {
        wasViewCreated = false
        mapViews = null
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        cleanupEffects()
    }

    override fun onUpdate() {
        if (!wasViewCreated) {
            return
        }

        val views = mapViews ?: return
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
        val (mapTime, setMapTime) = useState<Instant?>(null)
        val (hasTimeDependentLayers, setHasTimeDependentLayers) = useState(false)

        useEffect(timeButton, timeSheet, mapTime, hasTimeDependentLayers) {
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

        val navigation = useNavigationSensors(trueNorth = true)
        val context = useAndroidContext()
        val sensors = useService<SensorService>()
        val hasCompass = useMemo(sensors) { sensors.hasCompass() }
        val navigator = useService<Navigator>()
        val pathService = useService<PathService>()
        val destination = useFlow(navigator.destination2, state = BackgroundMinimumState.Resumed)
        val prefs = useService<UserPreferences>()
        val formatter = useService<FormatService>()
        val activity = useActivity()
        val navController = useNavController()
        val (lockMode, setLockMode) = useLockMode()

        val screenLight = useMemo(activity) {
            ScreenTorch(activity.window)
        }

        val screenLock = useMemo(prefs) {
            NavigationScreenLock(prefs.map.keepScreenUnlockedWhileOpen)
        }

        useEffect(mapView, prefs, resetOnResume) {
            mapView.useDensityPixelsForZoom = !prefs.map.highDetailMode
            mapView.layerManager.invalidate()
            mapView.invalidate()
        }

        useEffect(screenLock, activity, resetOnResume, destination) {
            screenLock.updateLock(activity)
        }

        useDestroyEffect(screenLock, activity) {
            screenLock.releaseLock(activity)
        }

        useClickCallback(lockButton.view, lockMode, hasCompass) {
            setLockMode(getNextLockMode(lockMode, hasCompass))
        }

        // Layers
        val manager = useMemo { MapToolLayerManager() }
        useEffect(manager, mapView, mapTime, manager.key) {
            manager.setTime(mapView, mapTime)
        }
        useEffectWithCleanup(manager, mapView, resetOnResume) {
            manager.resume(context, mapView, this@MapFragment)
            return@useEffectWithCleanup {
                manager.pause(mapView)
            }
        }

        useEffect(manager.key, mapView) {
            setHasTimeDependentLayers(mapView.layerManager.getLayers().any { it.isTimeDependent })
        }

        useEffect(mapView) {
            mapView.setBackgroundColor(Color.rgb(127, 127, 127))
        }

        useBackgroundEffect(mapView, manager.key, attributionView) {
            val attribution = mapView.getAttribution(context)
            onMain {
                attributionView.text = attribution
                attributionView.isVisible = attribution != null
            }
        }

        val layerEditSheet = useMemo(prefs) {
            MapLayersBottomSheet(
                MapToolRegistration.MAP_ID
            )
        }

        usePauseEffect(layerEditSheet) {
            tryOrNothing {
                layerEditSheet.setOnDismissListener(null)
                layerEditSheet.dismiss()
            }
        }

        val adjustLayers = useCallback<Unit>(manager, layerEditSheet, context, mapView) {
            manager.pause(mapView)
            layerEditSheet.setOnDismissListener {
                manager.resume(context, mapView, this@MapFragment)
                setHasTimeDependentLayers(
                    mapView.layerManager.getLayers().any { it.isTimeDependent })
            }
            layerEditSheet.show(this)
        }

        // Distance
        val stopDistanceMeasurement = useCallback<Unit>(mapDistanceSheetView, manager) {
            manager.stopDistanceMeasurement()
            mapDistanceSheetView.hide()
        }

        val startDistanceMeasurement =
            useCallback(
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
                    inBackground {
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
        useEffect(mapView, navigation.location, navigation.locationAccuracy) {
            mapView.userLocation = navigation.location
            mapView.userLocationAccuracy = navigation.locationAccuracy
            mapView.invalidate()
        }

        useEffect(mapView, navigation.bearing) {
            mapView.userAzimuth = navigation.bearing
        }

        useEffect(manager, mapView.mapBounds, manager.key) {
            manager.onBoundsChanged()
        }

        val traceViews =
            useMemo(zoomInButton, zoomOutButton, timeButton, menuButton, sensorStatusBadges, hasTimeDependentLayers) {
                listOfNotNull(
                    zoomInButton.view,
                    zoomOutButton.view,
                    if (hasTimeDependentLayers) timeButton.view else null,
                    menuButton.view,
                    sensorStatusBadges.view
                )
            }

        useEffect(lockMode, mapView, lockButton, traceViews, screenLight, context, resetOnResume) {
            switchMapLockMode(lockMode, mapView, lockButton, traceViews, screenLight, context)
        }

        usePauseEffect(screenLight) {
            screenLight.off()
            requireMainActivity().setBottomNavigationEnabled(true)
        }

        useSavedMapState(mapView)

        useEffect(mapView, lockMode, navigation.location) {
            if (mapView.mapCenter == Coordinate.zero) {
                mapView.mapCenter = navigation.location
                mapView.resolution = 2f
            }

            if (lockMode == MapLockMode.Location || lockMode == MapLockMode.Compass) {
                mapView.mapCenter = navigation.location
            }
        }

        useEffect(mapView, lockMode, navigation.bearing) {
            if (lockMode == MapLockMode.Compass) {
                mapView.mapAzimuth = navigation.bearing.value
            }
        }

        useEffect(zoomInButton, zoomOutButton) {
            zoomInButton.setState(false)
            zoomOutButton.setState(false)

            zoomInButton.setOnClickListener {
                mapView.zoom(2f)
            }

            zoomOutButton.setOnClickListener {
                mapView.zoom(0.5f)
            }
        }

        useEffect(navigationSheetView, destination, navigation) {
            if (destination != null) {
                navigationSheetView.updateNavigationSensorValues(navigation)
                navigationSheetView.setTrueNorthOverride(true)
                navigationSheetView.show(destination, true)
            } else {
                navigationSheetView.hide()
            }
        }

        useEffect(mapView, manager, navController, startDistanceMeasurement, prefs) {
            mapView.setOnLongPressListener { location ->
                if (manager.isMeasuringDistance()) {
                    return@setOnLongPressListener
                }
                manager.setSelectedLocation(location)
                inBackground {
                    val elevation = Distance.meters(DEM.getElevation(location).elevation)

                    onMain {
                        Share.actions(
                            this@MapFragment,
                            formatter.formatLocation(location),
                            listOf(
                                ActionItem(getString(R.string.beacon), R.drawable.ic_location) {
                                    val bundle = Bundle().apply {
                                        putParcelable("initial_location", GeoUri(location))
                                    }
                                    navController.navigateWithAnimation(
                                        R.id.placeBeaconFragment,
                                        bundle
                                    )
                                    manager.setSelectedLocation(null)
                                },
                                ActionItem(getString(R.string.navigate), R.drawable.ic_beacon) {
                                    navigator.navigateTo(
                                        location,
                                        formatter.formatLocation(location),
                                        BeaconOwner.Maps
                                    )
                                    manager.setSelectedLocation(null)
                                },
                                ActionItem(getString(R.string.distance), R.drawable.ruler) {
                                    startDistanceMeasurement(location, true)
                                    manager.setSelectedLocation(null)
                                },
                            ),
                            subtitle = getString(
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

        useEffect(mapDistanceSheetView, manager, prefs) {
            manager.setOnDistanceChangedCallback { distance ->
                val relative = distance
                    .convertTo(prefs.baseDistanceUnits)
                    .toRelativeDistance()
                mapDistanceSheetView.setDistance(relative)
            }
        }

        // Menu
        useEffect(menuButton) {
            menuButton.setState(false)
        }

        useClickCallback(menuButton.view, startDistanceMeasurement) {
            val actions = listOf(
                MapAction.Measure to getString(R.string.measure),
                MapAction.CreatePath to getString(R.string.create_path),
                MapAction.AdjustLayers to getString(R.string.layers),
                MapAction.Trace to getString(R.string.trace)
            )

            Pickers.menu(
                menuButton.view,
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

        // Disclaimer
        useEffect(context) {
            CustomUiUtils.disclaimer(
                context,
                getString(R.string.map),
                getString(R.string.map_tool_disclaimer),
                "pref_map_tool_disclaimer_shown"
            )
        }

        // Sensor status
        useEffect(sensorStatusBadges, navigation.gps, navigation.compass) {
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
        traceViews: List<View>,
        screenLight: ScreenTorch,
        context: android.content.Context
    ) {
        // Reset trace mode defaults
        map.isInteractive = true
        map.isZoomEnabled = true
        traceViews.forEach { it.isVisible = true }
        screenLight.off()
        requireMainActivity().setBottomNavigationEnabled(true)

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
                    getString(R.string.trace),
                    getString(R.string.map_trace_instructions),
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
                traceViews.forEach { it.isVisible = false }

                // Hide the bottom navigation
                requireMainActivity().setBottomNavigationEnabled(false)
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

    private fun useSavedMapState(mapView: MapView) {
        val prefs = useService<UserPreferences>()
        val shouldSave = useMemo(prefs, resetOnResume) {
            prefs.map.saveMapState
        }
        val key = "cache_map_state"
        val (center, setCenter) = useCoordinatePreference("${key}_coordinate")
        val (scale, setScale) = useFloatPreference("${key}_scale")

        useEffect(mapView, shouldSave) {
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
        val prefs = useService<UserPreferences>()
        val shouldSave = useMemo(prefs, resetOnResume) {
            prefs.map.saveMapState
        }
        val key = "cache_map_state"
        val (savedLockModeId, setSavedLockModeId) = useIntPreference("${key}_lock_mode")
        val savedLockMode = useMemo(savedLockModeId) {
            MapLockMode.entries.firstOrNull { it.id == savedLockModeId && it != MapLockMode.Trace }
        }
        val (lockMode, setLockMode) = useState(savedLockMode ?: MapLockMode.Free)
        val exposedSetLockMode = useCallback(shouldSave) { newLockMode: MapLockMode ->
            setLockMode(newLockMode)
            if (shouldSave) {
                setSavedLockModeId(newLockMode.id)
            }
        }

        useEffect(shouldSave) {
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
    val root: View,
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
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createMapViews(context: Context): MapViews {
    val root = ConstraintLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val map = MapView(context).apply {
        id = R.id.map
        keepScreenOn = true
    }
    root.addView(
        map,
        ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToTop = R.id.time_sheet
        }
    )

    val attribution = MapAttributionState()
    val attributionView = ComposeView(context).apply {
        id = R.id.map_attribution
        setContent {
            TrailSenseTheme {
                attribution.Content()
            }
        }
    }
    root.addView(
        attributionView,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            endToEnd = R.id.map
            bottomToBottom = R.id.map
        }
    )

    val menuButton = smallMapFab(context, R.id.menu_btn, R.drawable.ic_menu_dots)
    root.addView(
        menuButton.view,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            endToEnd = R.id.map
            topToTop = R.id.map
            topMargin = 8.dp(context)
            marginEnd = 16.dp(context)
        }
    )

    val timeButton = smallMapFab(context, R.id.time_btn, R.drawable.ic_tool_clock).apply {
        visibility = View.GONE
    }
    root.addView(
        timeButton.view,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomToTop = R.id.zoom_in_btn
            startToStart = R.id.zoom_in_btn
        }
    )

    val zoomInButton = smallMapFab(context, R.id.zoom_in_btn, R.drawable.ic_add)
    root.addView(
        zoomInButton.view,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomToTop = R.id.zoom_out_btn
            startToStart = R.id.zoom_out_btn
        }
    )

    val zoomOutButton = smallMapFab(context, R.id.zoom_out_btn, R.drawable.ic_zoom_out)
    root.addView(
        zoomOutButton.view,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomToTop = R.id.lock_btn
            startToStart = R.id.lock_btn
            endToEnd = R.id.lock_btn
            bottomMargin = 16.dp(context)
        }
    )

    val lockButton = smallMapFab(context, R.id.lock_btn, R.drawable.satellite)
    root.addView(
        lockButton.view,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToTop = R.id.time_sheet
            marginEnd = 16.dp(context)
            bottomMargin = 48.dp(context)
        }
    )

    val sensorStatusBadges = SensorStatusBadgeView(context).apply {
        id = R.id.sensor_status_badges
        alpha = 0.6f
    }
    root.addView(
        sensorStatusBadges.view,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToTop = R.id.time_sheet
            marginStart = 16.dp(context)
            bottomMargin = 32.dp(context)
        }
    )

    val navigationSheet = NavigationSheetView(context).apply {
        id = R.id.navigation_sheet
        visibility = View.GONE
    }
    root.addView(
        navigationSheet.view,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToTop = R.id.distance_sheet
        }
    )

    val distanceSheet = MapDistanceSheetState(context)
    val distanceSheetView = ComposeView(context).apply {
        id = R.id.distance_sheet
        visibility = View.GONE
        distanceSheet.view = this
        setContent {
            TrailSenseTheme {
                distanceSheet.Content()
            }
        }
    }
    root.addView(
        distanceSheetView,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    val timeSheet = DateTimeSliderSheetState(context)
    val timeSheetView = ComposeView(context).apply {
        id = R.id.time_sheet
        visibility = View.GONE
        timeSheet.view = this
        setContent {
            TrailSenseTheme {
                timeSheet.Content()
            }
        }
    }
    root.addView(
        timeSheetView,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToTop = R.id.navigation_sheet
        }
    )

    return MapViews(
        root = root,
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

private fun smallMapFab(
    context: Context,
    id: Int,
    drawable: Int
): MapIconButton {
    return MapIconButton(context, drawable).apply {
        this.id = id
        alpha = 0.86f
        isClickable = true
        isFocusable = true
    }
}

private class MapIconButton(
    context: Context,
    icon: Int
) {
    private var iconRes by mutableIntStateOf(icon)
    private var isOn by mutableStateOf(false)

    val view = ComposeView(context).apply {
        setContent {
            TrailSenseTheme {
                Content()
            }
        }
    }

    var id: Int
        get() = view.id
        set(value) {
            view.id = value
        }

    var alpha: Float
        get() = view.alpha
        set(value) {
            view.alpha = value
        }

    var isClickable: Boolean
        get() = view.isClickable
        set(value) {
            view.isClickable = value
        }

    var isFocusable: Boolean
        get() = view.isFocusable
        set(value) {
            view.isFocusable = value
        }

    var isVisible: Boolean
        get() = view.visibility == View.VISIBLE
        set(value) {
            view.visibility = if (value) View.VISIBLE else View.GONE
        }

    var visibility: Int
        get() = view.visibility
        set(value) {
            view.visibility = value
        }

    fun setOnClickListener(listener: View.OnClickListener?) {
        view.setOnClickListener(listener)
    }

    fun setImageResource(icon: Int) {
        iconRes = icon
    }

    fun setState(isOn: Boolean) {
        this.isOn = isOn
    }

    @Composable
    private fun Content() {
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
            modifier = Modifier.size(40.dp),
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

private fun Int.dp(context: Context): Int {
    return Resources.dp(context, toFloat()).toInt()
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
