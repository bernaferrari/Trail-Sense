package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.Throttle
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.andromeda.fragments.show
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
import com.kylecorry.trail_sense.shared.map_layers.ui.MapAttributionView
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersBottomSheet
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getAttribution
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
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
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class ViewPhotoMapFragment : TrailSenseComposeFragment() {
    private var photoMapViews: ViewPhotoMapViews? = null
    private val views: ViewPhotoMapViews
        get() = photoMapViews ?: error("Photo map views are not ready")
    private val isBound: Boolean
        get() = photoMapViews != null

    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val altimeter by lazy { sensorService.getAltimeter() }
    private val compass by lazy { sensorService.getCompass() }
    private val hasCompass by lazy { sensorService.hasCompass() }
    private val beaconService by lazy { BeaconService(requireContext()) }
    private val service = getAppService<MapService>()
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private val navigator by lazy { Navigator.getInstance(requireContext()) }
    private val screenLock by lazy { NavigationScreenLock(prefs.photoMaps.keepScreenUnlockedWhileOpen) }

    private val screenLight by lazy { ScreenTorch(requireActivity().window) }

    // Map layers
    private val layerManager = PhotoMapToolLayerManager()
    private var layerSheet: MapLayersBottomSheet? = null

    // Paths
    private val pathService by lazy { PathService.getInstance(requireContext()) }

    private var mapId = 0L
    private var map: PhotoMap? = null
    private var destination: Destination? = null

    private var mapLockMode = MapLockMode.Free

    private val throttle = Throttle(20)
    private val updateTimer = CoroutineTimer {
        updatePeriodic()
    }

    private var shouldLockOnMapLoad = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapId = requireArguments().getLong("mapId")
        shouldLockOnMapLoad = requireArguments().getBoolean("autoLockLocation", false)
    }

    override fun beforeCreateContent() {
        photoMapViews = createViewPhotoMapViews(requireContext())
    }

    @Composable
    override fun FragmentContent() {
        ViewPhotoMapHost(
            views = views,
            modifier = Modifier.fillMaxSize()
        )
    }

    override fun onResume() {
        super.onResume()
        layerManager.setOnDistanceChangedCallback(this::showDistance)
        resetLayerManager()
        views.map.useDensityPixelsForZoom = !prefs.photoMaps.highDetailMode

        if (mapLockMode == MapLockMode.Trace) {
            updateMapLockMode(MapLockMode.Trace, prefs.photoMaps.keepMapFacingUp)
        }
        updateTimer.interval(5000)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe(gps) {
            views.map.userLocation = gps.location
            views.map.userLocationAccuracy = gps.horizontalAccuracy?.let { Distance.meters(it) }
            updateDestination()

            if (mapLockMode == MapLockMode.Location || mapLockMode == MapLockMode.Compass) {
                views.map.mapCenter = gps.location
            }
        }
        observe(altimeter) { updateDestination() }
        observe(compass) {
            compass.declination = Geophysics.getGeomagneticDeclination(gps.location, gps.altitude)
            val bearing = compass.rawBearing
            views.map.userAzimuth = compass.bearing
            if (mapLockMode == MapLockMode.Compass) {
                views.map.mapAzimuth = bearing
            }
            updateDestination()
        }

        observeFlow(navigator.destination2) {
            setDestination(it)
        }

        reloadMap()

        views.map.setOnLongPressListener {
            onLongPress(it)
        }

        views.map.setBackgroundColor(Resources.color(requireContext(), R.color.colorSecondary))

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

        inBackground {
            val elevation = Distance.meters(DEM.getElevation(location).elevation)

            onMain {
                Share.actions(
                    this@ViewPhotoMapFragment,
                    formatService.formatLocation(location),
                    listOf(
                        ActionItem(getString(R.string.beacon), R.drawable.ic_location) {
                            createBeacon(location)
                            selectLocation(null)
                        },
                        ActionItem(getString(R.string.navigate), R.drawable.ic_beacon) {
                            navigateTo(location)
                            selectLocation(null)
                        },
                        ActionItem(getString(R.string.distance), R.drawable.ruler) {
                            startDistanceMeasurement(arrayOf(gps.location, location))
                            selectLocation(null)
                        },
                    ),
                    subtitle = getString(
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
        inBackground {
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
        findNavController().navigate(R.id.placeBeaconFragment, bundle)
    }

    private fun showDistance(distance: Distance) {
        val relative = distance
            .convertTo(prefs.baseDistanceUnits)
            .toRelativeDistance()
        views.distanceSheet.setDistance(relative)
    }

    fun startDistanceMeasurement(initialPoints: Array<Coordinate>) {
        if (map?.isCalibrated != true) {
            toast(getString(R.string.map_is_not_calibrated))
            return
        }

        layerManager.startDistanceMeasurement(initialPoints)
        views.distanceSheet.show()
        views.distanceSheet.cancelListener = {
            stopDistanceMeasurement()
        }
        views.distanceSheet.createPathListener = {
            inBackground {
                map?.let {
                    val id = CreatePathCommand(
                        pathService,
                        prefs.navigation,
                        it.name
                    ).execute(layerManager.getDistanceMeasurementPoints())

                    onMain {
                        findNavController().navigate(
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
        layerManager.resume(requireContext(), views.map, mapId, this)

        // Populate the last known location and map bounds
        layerManager.onBoundsChanged()
        views.map.userLocation = gps.location
        views.map.userLocationAccuracy = gps.horizontalAccuracy?.let { Distance.meters(it) }
    }

    fun adjustLayers() {
        layerSheet?.dismiss()
        layerSheet = MapLayersBottomSheet(
            OfflineMapsToolRegistration.PHOTO_MAPS_ID,
            PhotoMapToolLayerManager.alwaysEnabledLayers
        )
        layerManager.pause(views.map)
        layerSheet?.setOnDismissListener {
            resetLayerManager()
        }
        layerSheet?.show(this)
    }

    private fun navigateTo(location: Coordinate) {
        inBackground {
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
        this@ViewPhotoMapFragment.destination = destination
        if (isBound) {
            if (destination == null) {
                views.navigationSheet.hide()
            } else {
                updateDestination()
            }
        }
        activity?.let { screenLock.updateLock(it) }
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
        layerManager.resume(requireContext(), views.map, map.id, this)
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
        requireMainActivity().setBottomNavigationEnabled(true)

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
                    requireContext(),
                    getString(R.string.trace),
                    getString(R.string.map_trace_instructions),
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
                requireMainActivity().setBottomNavigationEnabled(false)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        updateTimer.stop()
        layerSheet?.setOnDismissListener(null)
        layerSheet?.dismiss()
        photoMapViews?.let {
            layerManager.pause(it.map)
        }
        // Reset brightness
        screenLight.off()

        // Show the bottom navigation
        requireMainActivity().setBottomNavigationEnabled(true)
    }

    fun recenter() {
        if (mapLockMode == MapLockMode.Trace) {
            return
        }
        views.map.recenter()
    }

    override fun onDestroyView() {
        activity?.let { screenLock.releaseLock(it) }
        photoMapViews = null
        super.onDestroyView()
    }

    private fun updatePeriodic() {
        activity?.let { screenLock.updateLock(it) }
        val views = photoMapViews ?: return
        inBackground {
            val attribution = views.map.getAttribution(requireContext())
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

    companion object {
        fun create(mapId: Long, autoLockLocation: Boolean = false): ViewPhotoMapFragment {
            return ViewPhotoMapFragment().apply {
                arguments = Bundle().apply {
                    putLong("mapId", mapId)
                    putBoolean("autoLockLocation", autoLockLocation)
                }
            }
        }
    }

}

private data class ViewPhotoMapViews(
    val root: View,
    val map: MapView,
    val mapAttribution: MapAttributionView,
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
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createViewPhotoMapViews(context: Context): ViewPhotoMapViews {
    val margin = Resources.dp(context, 16f).toInt()
    val bottomReserve = Resources.dp(context, 120f).toInt()
    val fabStep = Resources.dp(context, 56f).toInt()

    val root = ConstraintLayout(context)

    val map = MapView(context).apply {
        id = R.id.map
        setBackgroundColor(Resources.color(context, R.color.colorSecondary))
    }

    val attribution = MapAttributionView(context).apply {
        id = R.id.map_attribution
        visibility = View.GONE
    }

    val lockBtn = PhotoMapIconButton(context, R.drawable.satellite).apply {
        id = R.id.lock_btn
        alpha = 0.86f
        isClickable = true
        isFocusable = true
    }

    val zoomOutBtn = PhotoMapIconButton(context, R.drawable.ic_zoom_out).apply {
        id = R.id.zoom_out_btn
        alpha = 0.86f
        isClickable = true
        isFocusable = true
    }

    val zoomInBtn = PhotoMapIconButton(context, R.drawable.ic_add).apply {
        id = R.id.zoom_in_btn
        alpha = 0.86f
        isClickable = true
        isFocusable = true
    }

    val navigationSheet =
        com.kylecorry.trail_sense.tools.navigation.ui.NavigationSheetView(context).apply {
            id = R.id.navigation_sheet
            visibility = View.GONE
        }

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
        map,
        ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )
    root.addView(
        attribution,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            bottomMargin = bottomReserve
        }
    )
    root.addView(
        zoomInBtn,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginEnd = margin
            bottomMargin = bottomReserve + fabStep * 2
        }
    )
    root.addView(
        zoomOutBtn,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginEnd = margin
            bottomMargin = bottomReserve + fabStep
        }
    )
    root.addView(
        lockBtn,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginEnd = margin
            bottomMargin = bottomReserve
        }
    )
    root.addView(
        navigationSheet,
        ConstraintLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )
    root.addView(
        distanceSheetView,
        ConstraintLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    return ViewPhotoMapViews(
        root = root,
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
