package com.kylecorry.trail_sense.tools.map.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar

@Composable
fun Fragment.MapContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                android.view.LayoutInflater.from(ctx).inflate(R.layout.fragment_map, null)
            },
            modifier = Modifier.fillMaxSize()
        )

        TrailSenseSearchBar(
            value = "",
            onValueChange = {},
            placeholder = "Search map...",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        )

        FloatingActionButton(
            onClick = { },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(painterResource(R.drawable.ic_location), contentDescription = "My location")
        }
    }
}

class MapFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        MapContent()
    }
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
                CustomUiUtils.setButtonState(timeButton, mapTime != null || timeSheet.isVisible)
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

        useClickCallback(lockButton, lockMode, hasCompass) {
            setLockMode(getNextLockMode(lockMode, hasCompass))
        }

        useEffect(attributionView) {
            attributionView.movementMethod = LinkMovementMethod.getInstance()
        }

        // Layers
        val manager = useMemo { MapToolLayerManager() }
        useEffect(manager, mapView, mapTime, manager.key) {
            manager.setTime(mapView, mapTime)
        }
        useEffectWithCleanup(manager, mapView, resetOnResume) {
            manager.resume(context, mapView, this@MapFragment)
            setHasTimeDependentLayers(mapView.layerManager.getLayers().any { it.isTimeDependent })
            return@useEffectWithCleanup {
                manager.pause(mapView)
            }
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

        val traceViews = useMemo(zoomInButton, zoomOutButton, timeButton, menuButton, sensorStatusBadges) {
            listOf(zoomInButton, zoomOutButton, timeButton, menuButton, sensorStatusBadges)
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
            CustomUiUtils.setButtonState(zoomInButton, false)
            CustomUiUtils.setButtonState(zoomOutButton, false)

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

        useEffect(mapView, manager, navController, startDistanceMeasurement, prefs, navigation.location) {
            mapView.setOnLongPressListener { location ->
                if (manager.isMeasuringDistance()) {
                    return@setOnLongPressListener
                }
                manager.setSelectedLocation(location)
                inBackground {
                    val selectedElevation = DEM.getElevation(location).elevation
                    val formattedLocation = formatter.formatLocation(location)

                    onMain {
                        // Build the data point row using the shared LocationDataPointView
                        val infoView = LocationDataPointView(requireContext(), null)
                        infoView.setDistance(
                            Distance.meters(navigation.location.distanceTo(location))
                                .convertTo(prefs.baseDistanceUnits)
                        )
                        infoView.setDirection(navigation.location.bearingTo(location))
                        infoView.setElevationDiff(
                            Distance.meters(selectedElevation - navigation.elevation.meters().value)
                        )

                        Share.actions(
                            this@MapFragment,
                            formattedLocation,
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
                                        formattedLocation,
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
                                formatter.formatElevation(Distance.meters(selectedElevation))
                            ),
                            customView = infoView
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
            CustomUiUtils.setButtonState(menuButton, false)
        }

        useClickCallback(menuButton, startDistanceMeasurement) {
            val actions = listOf(
                MapAction.Measure to getString(R.string.measure),
                MapAction.CreatePath to getString(R.string.create_path),
                MapAction.AdjustLayers to getString(R.string.layers),
                MapAction.Trace to getString(R.string.trace)
            )

            Pickers.menu(
                menuButton,
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
        button: FloatingActionButton,
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
                CustomUiUtils.setButtonState(button, true)
            }

            MapLockMode.Compass -> {
                // Disable pan
                map.isPanEnabled = false

                // Show as locked
                button.setImageResource(R.drawable.ic_compass_icon)
                CustomUiUtils.setButtonState(button, true)
            }

            MapLockMode.Free -> {
                // Enable pan
                map.isPanEnabled = true

                // Reset the rotation
                map.mapAzimuth = 0f

                // Show as unlocked
                button.setImageResource(R.drawable.satellite)
                CustomUiUtils.setButtonState(button, false)
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
                CustomUiUtils.setButtonState(button, true)

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
