package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.content.Context
import android.os.Bundle
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.observe
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.trail_sense.shared.extensions.asLiveData
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.Throttle
import com.kylecorry.andromeda.torch.ScreenTorch
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersSheetState
import com.kylecorry.trail_sense.shared.map_layers.ui.MapAttributionState
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getAttribution
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun ViewPhotoMapToolScreen(
    host: TrailSenseDestinationHost,
    mapId: Long,
    autoLockLocation: Boolean,
    onController: (ViewPhotoMapController) -> Unit,
    modifier: Modifier = Modifier
) {
    val controller = remember(host, mapId, autoLockLocation) {
        ViewPhotoMapController(host, mapId, autoLockLocation)
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.onCreate()
        onController(controller)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onDestroy()
        }
    }

    controller.Content(modifier = modifier)
}

class ViewPhotoMapController(
    private val host: TrailSenseDestinationHost,
    private val mapId: Long,
    private val autoLockLocation: Boolean
) {
    private var photoMapViews: ViewPhotoMapViews? =
        createViewPhotoMapViews(host.requireContext())
    private val views: ViewPhotoMapViews
        get() = photoMapViews ?: error("Photo map views are not ready")
    private val isBound: Boolean
        get() = photoMapViews != null

    private val sensorService by lazy { SensorService(host.requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val altimeter by lazy { sensorService.getAltimeter() }
    private val compass by lazy { sensorService.getCompass() }
    private val hasCompass by lazy { sensorService.hasCompass() }
    private val beaconService by lazy { BeaconService(host.requireContext()) }
    private val service = getAppService<MapService>()
    private val formatService by lazy { FormatService.getInstance(host.requireContext()) }
    private val prefs by lazy { UserPreferences(host.requireContext()) }

    private val navigator by lazy { Navigator.getInstance(host.requireContext()) }
    private val screenLock by lazy { NavigationScreenLock(prefs.photoMaps.keepScreenUnlockedWhileOpen) }

    private val screenLight by lazy { ScreenTorch(host.mainActivity.window) }

    // Map layers
    private val layerManager = PhotoMapToolLayerManager()
    private val layerSheet = MapLayersSheetState(
        OfflineMapsToolRegistration.PHOTO_MAPS_ID,
        PhotoMapToolLayerManager.alwaysEnabledLayers
    )

    // Paths
    private val pathService by lazy { PathService.getInstance(host.requireContext()) }

    private var map: PhotoMap? = null
    private var destination: Destination? = null

    private var mapLockMode = MapLockMode.Free

    private val throttle = Throttle(20)
    private val updateTimer = CoroutineTimer {
        updatePeriodic()
    }

    private var shouldLockOnMapLoad = false

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        ViewPhotoMapHost(
            views = views,
            modifier = modifier
        )
        layerSheet.Content(
            host.requireContext(),
            { host.findNavController().navigateWithAnimation(it) }
        )
    }

    fun onCreate() {
        shouldLockOnMapLoad = autoLockLocation

        gps.asLiveData().observe(host.lifecycleOwner) {
            views.map.userLocation = gps.location
            views.map.userLocationAccuracy = gps.horizontalAccuracy?.let { Distance.meters(it) }
            updateDestination()

            if (mapLockMode == MapLockMode.Location || mapLockMode == MapLockMode.Compass) {
                views.map.mapCenter = gps.location
            }
        }
        altimeter.asLiveData().observe(host.lifecycleOwner) { updateDestination() }
        compass.asLiveData().observe(host.lifecycleOwner) {
            compass.declination = Geophysics.getGeomagneticDeclination(gps.location, gps.altitude)
            val bearing = compass.rawBearing
            views.map.userAzimuth = compass.bearing
            if (mapLockMode == MapLockMode.Compass) {
                views.map.mapAzimuth = bearing
            }
            updateDestination()
        }

        host.lifecycleScope.launch {
            navigator.destination2.collect {
                setDestination(it)
            }
        }

        reloadMap()

        views.map.setOnLongPressListener {
            onLongPress(it)
        }

        views.map.setBackgroundColor(Resources.color(host.requireContext(), R.color.colorSecondary))

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
            updateMapLockMode(getNextLockMode(mapLockMode), keepMapUp)
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
        updateTimer.interval(5000)
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

        host.inBackground {
            val elevation = Distance.meters(DEM.getElevation(location).elevation)

            onMain {
                Share.actions(
                    host,
                    formatService.formatLocation(location),
                    listOf(
                        ActionItem(host.getString(R.string.beacon), R.drawable.ic_location) {
                            createBeacon(location)
                            selectLocation(null)
                        },
                        ActionItem(host.getString(R.string.navigate), R.drawable.ic_beacon) {
                            navigateTo(location)
                            selectLocation(null)
                        },
                        ActionItem(host.getString(R.string.distance), R.drawable.ruler) {
                            startDistanceMeasurement(arrayOf(gps.location, location))
                            selectLocation(null)
                        },
                    ),
                    subtitle = host.getString(
                        R.string.elevation_value,
                        formatService.formatElevation(elevation)
                    )
                ) {
                    selectLocation(null)
                }
            }
        }
    }

    fun reloadMap() {
        host.inBackground {
            withContext(Dispatchers.IO) {
                map = service.getPhotoMap(mapId)
            }
            withContext(Dispatchers.Main) {
                map?.let {
                    onMapLoad(it)
                }
            }
        }
    }

    private fun updateDestination() {
        if (throttle.isThrottled() || !isBound) {
            return
        }

        val beacon = destination ?: return
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
        val bundle = Bundle().apply {
            putParcelable("initial_location", GeoUri(location))
        }
        host.findNavController().navigate(R.id.placeBeaconFragment, bundle)
    }

    private fun showDistance(distance: Distance) {
        val relative = distance
            .convertTo(prefs.baseDistanceUnits)
            .toRelativeDistance()
        views.distanceSheet.setDistance(relative)
    }

    fun startDistanceMeasurement(initialPoints: Array<Coordinate>) {
        if (map?.isCalibrated != true) {
            host.toast(host.getString(R.string.map_is_not_calibrated))
            return
        }

        layerManager.startDistanceMeasurement(initialPoints)
        views.distanceSheet.show()
        views.distanceSheet.cancelListener = {
            stopDistanceMeasurement()
        }
        views.distanceSheet.createPathListener = {
            host.inBackground {
                map?.let {
                    val id = CreatePathCommand(
                        pathService,
                        prefs.navigation,
                        it.name
                    ).execute(layerManager.getDistanceMeasurementPoints())

                    onMain {
                        host.findNavController().navigate(
                            R.id.action_maps_to_path,
                            Bundle().apply {
                                putLong("path_id", id)
                            }
                        )
                    }
                }
            }
        }
        views.distanceSheet.undoListener = {
            layerManager.undoLastDistanceMeasurement()
        }
    }

    private fun stopDistanceMeasurement() {
        layerManager.stopDistanceMeasurement()
        views.distanceSheet.hide()
    }

    fun trace() {
        updateMapLockMode(MapLockMode.Trace, prefs.photoMaps.keepMapFacingUp)
    }

    private fun resetLayerManager() {
        layerManager.resume(host.requireContext(), views.map, mapId, host)

        // Populate the last known location and map bounds
        layerManager.onBoundsChanged()
        views.map.userLocation = gps.location
        views.map.userLocationAccuracy = gps.horizontalAccuracy?.let { Distance.meters(it) }
    }

    fun adjustLayers() {
        layerSheet.dismissAndUpdateWidgets(host.requireContext())
        layerManager.pause(views.map)
        layerSheet.setOnDismissListener {
            resetLayerManager()
        }
        layerSheet.show()
    }

    private fun navigateTo(location: Coordinate) {
        host.inBackground {
            // Create a temporary beacon
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

            navigateTo(beacon.copy(id = id))
        }
    }

    private fun navigateTo(beacon: Beacon): Boolean {
        navigator.navigateTo(beacon)
        setDestination(Destination.Beacon(beacon))
        return true
    }

    private fun setDestination(destination: Destination?) {
        this.destination = destination
        if (isBound) {
            if (destination == null) {
                views.navigationSheet.hide()
            } else {
                updateDestination()
            }
        }
        screenLock.updateLock(host.mainActivity)
    }

    private fun onMapLoad(map: PhotoMap) {
        layerManager.pause(views.map)
        this.map = map
        views.map.minScale = 0f
        val bounds = map.boundary() ?: CoordinateBounds(85.0, 180.0, -85.0, -180.0)
        views.map.fitIntoView(bounds, paddingFactor = 1.05f)
        views.map.constraintBounds = bounds
        views.map.minScale = views.map.scale
        layerManager.improveResolution(views.map.mapBounds, views.map.zoom.roundToInt())
        if (shouldLockOnMapLoad) {
            updateMapLockMode(MapLockMode.Location, prefs.photoMaps.keepMapFacingUp)
            shouldLockOnMapLoad = false
        }
        views.map.mapAzimuth = getDefaultMapAzimuth(prefs.photoMaps.keepMapFacingUp)
        layerManager.resume(host.requireContext(), views.map, map.id, host)
        layerManager.onBoundsChanged()
    }

    private fun updateMapLockMode(mode: MapLockMode, keepMapUp: Boolean) {
        mapLockMode = mode

        // Show zoom buttons
        views.zoomInBtn.isVisible = true
        views.zoomOutBtn.isVisible = true
        views.map.isZoomEnabled = true

        // Make it interactive
        views.map.isInteractive = true
        views.map.isZoomEnabled = true

        // Show the bottom navigation
        (host.mainActivity).setBottomNavigationEnabled(true)

        when (mapLockMode) {
            MapLockMode.Location -> {
                // Disable pan
                views.map.isPanEnabled = false

                // Zoom in and center on location
                views.map.resolution = 2f
                views.map.mapCenter = gps.location

                // Reset the rotation
                views.map.mapAzimuth = getDefaultMapAzimuth(keepMapUp)

                // Show as locked
                views.lockBtn.setImageResource(R.drawable.satellite)
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
                views.lockBtn.setImageResource(R.drawable.ic_compass_icon)
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
                views.lockBtn.setImageResource(R.drawable.satellite)
                views.lockBtn.setState(false)

                // Reset brightness
                screenLight.off()
            }

            MapLockMode.Trace -> {
                CustomUiUtils.disclaimer(
                    host.requireContext(),
                    host.getString(R.string.trace),
                    host.getString(R.string.map_trace_instructions),
                    "disclaimer_shown_map_trace",
                    cancelText = null
                )

                // Disable pan
                views.map.isInteractive = false
                views.map.isZoomEnabled = false

                // Show as locked
                views.lockBtn.setImageResource(R.drawable.lock)
                views.lockBtn.setState(true)

                // Full brightness
                screenLight.on()

                // Hide zoom buttons
                views.zoomInBtn.isVisible = false
                views.zoomOutBtn.isVisible = false

                // Hide the bottom navigation
                (host.mainActivity).setBottomNavigationEnabled(false)
            }
        }
    }

    fun onPause() {
        updateTimer.stop()
        layerSheet.setOnDismissListener(null)
        layerSheet.dismissAndUpdateWidgets(host.requireContext())
        photoMapViews?.let {
            layerManager.pause(it.map)
        }
        // Reset brightness
        screenLight.off()

        // Show the bottom navigation
        (host.mainActivity).setBottomNavigationEnabled(true)
    }

    fun recenter() {
        if (mapLockMode == MapLockMode.Trace) {
            return
        }
        views.map.recenter()
    }

    fun onDestroy() {
        screenLock.releaseLock(host.mainActivity)
        photoMapViews = null
    }

    private fun updatePeriodic() {
        screenLock.updateLock(host.mainActivity)
        val views = photoMapViews ?: return
        host.inBackground {
            val attribution = views.map.getAttribution(host.requireContext())
            onMain {
                views.mapAttribution.text = attribution
                views.mapAttribution.isVisible = attribution != null
            }
        }
    }

    private fun getNextLockMode(mode: MapLockMode): MapLockMode {
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

    private enum class MapLockMode {
        Location,
        Compass,
        Free,
        Trace
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
                        start = TrailSenseSpacing.lg,
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
                    .padding(end = TrailSenseSpacing.lg, bottom = 176.dp),
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
                .padding(end = TrailSenseSpacing.lg, bottom = 120.dp)
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
    val lockBtn = PhotoMapIconButton(R.drawable.satellite).apply {
        alpha = 0.86f
    }

    val zoomOutBtn = PhotoMapIconButton(R.drawable.ic_zoom_out).apply {
        alpha = 0.86f
    }

    val zoomInBtn = PhotoMapIconButton(R.drawable.ic_add).apply {
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
