package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import com.kylecorry.trail_sense.shared.navigation.toRouteLocation

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.tools.map.ui.MapFeatureClickHandler
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
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.asLiveData
import com.kylecorry.trail_sense.shared.extensions.withSuspendableCancelableLoading
import java.util.concurrent.atomic.AtomicBoolean
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun ViewPhotoMapToolScreen(
    mapId: Long,
    autoLockLocation: Boolean,
    onController: (ViewPhotoMapController) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val screenNavigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember(mapId, autoLockLocation) {
        ViewPhotoMapController(
            context,
            // The window (screen torch), screen lock and bottom navigation belong to the activity.
            requireNotNull(context.findMainActivity()),
            screenNavigator,
            effects,
            lifecycleOwner,
            mapId,
            autoLockLocation
        )
    }

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

private tailrec fun Context.findMainActivity(): MainActivity? {
    return when (this) {
        is MainActivity -> this
        is ContextWrapper -> baseContext.findMainActivity()
        else -> null
    }
}

class ViewPhotoMapController(
    private val context: Context,
    private val activity: MainActivity,
    private val screenNavigator: TrailSenseNavigator,
    private val effects: ScreenEffects,
    private val lifecycleOwner: LifecycleOwner,
    private val mapId: Long,
    private val autoLockLocation: Boolean
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
    private val beaconService by lazy { BeaconService(context) }
    private val service = getAppService<MapService>()
    private val formatService by lazy { FormatService.getInstance(context) }
    private val prefs by lazy { UserPreferences(context) }

    private val navigator by lazy { Navigator.getInstance(context) }
    private val screenLock by lazy { NavigationScreenLock(prefs.photoMaps.keepScreenUnlockedWhileOpen) }

    private val screenLight by lazy { ScreenTorch(activity.window) }

    // Map layers
    private val layerManager = PhotoMapToolLayerManager()
    private val featureClickHandler =
        MapFeatureClickHandler(context, screenNavigator, effects, lifecycleOwner.lifecycleScope)
    private val layerSheet = MapLayersSheetState(
        OfflineMapsToolRegistration.PHOTO_MAPS_ID,
        PhotoMapToolLayerManager.alwaysEnabledLayers
    )

    // Paths
    private val pathService by lazy { PathService.getInstance(context) }

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
            context,
            { screenNavigator.navigate(it) }
        )
    }

    fun onCreate() {
        shouldLockOnMapLoad = autoLockLocation

        gps.asLiveData().observe(lifecycleOwner) {
            views.map.userLocation = gps.location
            views.map.userLocationAccuracy = gps.horizontalAccuracy?.let { Distance.meters(it) }
            updateDestination()

            if (mapLockMode == MapLockMode.Location || mapLockMode == MapLockMode.Compass) {
                views.map.mapCenter = gps.location
            }
        }
        altimeter.asLiveData().observe(lifecycleOwner) { updateDestination() }
        compass.asLiveData().observe(lifecycleOwner) {
            compass.declination = Geophysics.getGeomagneticDeclination(gps.location, gps.altitude)
            val bearing = compass.rawBearing
            views.map.userAzimuth = compass.bearing
            if (mapLockMode == MapLockMode.Compass) {
                views.map.mapAzimuth = bearing
            }
            updateDestination()
        }

        lifecycleOwner.lifecycleScope.launch {
            navigator.destination2.collect {
                setDestination(it)
            }
        }

        reloadMap()

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

        lifecycleOwner.inBackground {
            val elevation = Distance.meters(DEM.getElevation(location).elevation)

            onMain {
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
                            navigateTo(location)
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
    }

    fun reloadMap() {
        lifecycleOwner.inBackground {
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
            lifecycleOwner.inBackground {
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

                    onMain {
                        stopDistanceMeasurement()
                        screenNavigator.navigate(Route.PathOverview(pathId = id))
                    }
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
        layerManager.resume(views.map, mapId, lifecycleOwner, featureClickHandler::onFeatureClick)

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

    private fun navigateTo(location: Coordinate) {
        lifecycleOwner.inBackground {
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
        screenLock.updateLock(activity)
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
        layerManager.resume(views.map, map.id, lifecycleOwner, featureClickHandler::onFeatureClick)
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
        activity.setBottomNavigationEnabled(true)

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
        updateTimer.stop()
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
        screenLock.releaseLock(activity)
        photoMapViews = null
    }

    private fun updatePeriodic() {
        screenLock.updateLock(activity)
        val views = photoMapViews ?: return
        lifecycleOwner.inBackground {
            val attribution = views.map.getAttribution(context)
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
