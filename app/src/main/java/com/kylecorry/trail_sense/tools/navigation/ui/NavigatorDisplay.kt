package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.map
import com.kylecorry.sol.science.geography.projections.AzimuthalEquidistantProjection
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersSheetState
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getAttribution
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.ui.data.UpdateAstronomyLayerCommand
import com.kylecorry.trail_sense.tools.navigation.ui.errors.NavigatorUserErrors
import com.kylecorry.trail_sense.tools.navigation.ui.layers.NavigationCompassLayerManager
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.BeaconCompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassView
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.MarkerCompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.NavigationCompassLayer
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * The compass's rendering objects (the dials, the radar map and its layers, the destination card) and how a frame of
 * the ViewModel's state is drawn on them. It holds no state of its own beyond what drawing needs.
 */
internal class NavigatorDisplay(
    private val context: Context,
    private val viewModel: NavigatorViewModel,
    private val userPrefs: UserPreferences,
    private val formatService: FormatService,
    onNorthReferenceSettingsClick: () -> Unit,
    requestCameraPermission: ((Boolean) -> Unit) -> Unit,
    onCameraPermissionDenied: () -> Unit,
    onOpenBeacon: (Long) -> Unit
) {
    val views = createNavigatorViews(
        context,
        onNorthReferenceSettingsClick = onNorthReferenceSettingsClick,
        requestCameraPermission = requestCameraPermission,
        onCameraPermissionDenied = onCameraPermissionDenied,
        onOpenBeacon = onOpenBeacon
    )

    val layers = NavigationCompassLayerManager()
    val layerSheet = MapLayersSheetState(NavigationToolRegistration.MAP_ID)

    // Compass layers
    private val beaconCompassLayer = BeaconCompassLayer()
    private val astronomyCompassLayer = MarkerCompassLayer()
    private val navigationCompassLayer = NavigationCompassLayer()

    private val updateAstronomyLayerCommand = UpdateAstronomyLayerCommand(
        astronomyCompassLayer,
        userPrefs,
        viewModel.sensors.gps
    ) { viewModel.declination }

    private val baseDistanceUnits = userPrefs.baseDistanceUnits
    private var lastErrorCodes: List<String> = emptyList()
    private var lastAttributionLayerKey: Int? = null
    private var lastAttributionRadarVisible: Boolean? = null
    private var scope: CoroutineScope? = null

    init {
        val sensors = viewModel.sensors
        views.linearCompass.setCompassLayers(
            listOf(
                astronomyCompassLayer,
                beaconCompassLayer,
                navigationCompassLayer
            )
        )
        views.radarCompass.setCompassLayers(
            listOfNotNull(
                astronomyCompassLayer,
                if (viewModel.isNearbyLinearOnly) null else beaconCompassLayer,
                navigationCompassLayer
            )
        )

        views.accuracyView.setSensors(sensors.gps, sensors.compass)

        val map = views.radarCompassMap
        map.isPanEnabled = false
        map.isFlingEnabled = false
        // No disc of its own: the radar's paths and places draw straight onto the dial.
        map.backgroundColorOverride = null
        map.minScale = 0.001f
        map.mapCenter = sensors.gps.location
        map.metersPerProjectedUnit = 1.0
        map.latitudeScaleFactor = { 1f }
        map.projection = AzimuthalEquidistantProjection(sensors.gps.location)
        map.resolutionPixels = userPrefs.navigation.radarCompassScale
        map.setOnScaleChangeListener(true) { resolutionPixels ->
            viewModel.setRadarScale(resolutionPixels)
        }

        if (!viewModel.hasCompass) {
            views.radarCompass.shouldDrawDial = userPrefs.navigation.showDialTicksWhenNoCompass
            views.radarCompass.shouldDrawAzimuthIndicator = false
            views.navigationTitle.isTitleVisible = false
            views.northReferenceIndicator.isVisible = false
        } else {
            views.radarCompass.shouldDrawAzimuthIndicator = userPrefs.navigation.showAzimuthIndicator
        }
    }

    fun resume(scope: CoroutineScope) {
        this.scope = scope
        views.radarCompassMap.useDensityPixelsForZoom = !userPrefs.navigation.highDetailMode
        layers.resume(views.radarCompassMap, scope)
        views.radarCompass.bindMapView(views.radarCompassMap)

        // Show the north reference indicator
        views.northReferenceIndicator.showDetailsOnClick = true
        views.northReferenceIndicator.useTrueNorth = viewModel.useTrueNorth
        views.northReferenceIndicator.showLabel = true
        lastErrorCodes = emptyList()
    }

    fun pause() {
        layerSheet.setOnDismissListener(null)
        layerSheet.dismiss()
        layers.pause(views.radarCompassMap)
        scope = null
    }

    fun refresh(errors: NavigatorUserErrors) {
        val sensors = viewModel.sensors
        updateNavigationSheet()
        // Speed only while moving: a standing 0.0 km/h is noise.
        views.speed.title = viewModel.movingSpeed()?.let { formatService.formatSpeed(it) } ?: ""
        updateCompassBearing()

        if (viewModel.shouldInvalidateCompass()) {
            views.radarCompass.invalidate()
        }

        views.altitude.title = formatService.formatDistance(
            Distance.meters(sensors.altimeter.altitude).convertTo(baseDistanceUnits)
        )
        updateLocation()

        if (viewModel.shouldUpdateAstronomy()) {
            scope?.launch {
                if (sensors.gps.location != Coordinate.zero) {
                    updateAstronomyLayerCommand.execute()
                }
            }
        }

        updateMapAttributionIfNeeded()
        updateCompassStyle()
        updateQuickActionAvailability()

        val codes = viewModel.diagnosticCodes
        if (codes != lastErrorCodes) {
            lastErrorCodes = codes
            errors.update(codes)
        }
    }

    private fun updateNavigationSheet() {
        val sensors = viewModel.sensors
        val shown = viewModel.sheetDestination()
        if (shown != null) {
            // The heading too, so the card's arrow turns with you (it pointed at 0° forever without it).
            views.navigationSheet.updateNavigationSensorValues(
                sensors.gps.location,
                sensors.altimeter.altitude,
                sensors.speedometer.speed.value,
                viewModel.declination,
                sensors.compass.rawBearing
            )
            views.navigationSheet.show(shown.first, shown.second)
        } else {
            views.navigationSheet.hide()
        }
    }

    private fun updateMapAttributionIfNeeded() {
        val isRadarVisible = views.radarCompass.isVisible
        if (lastAttributionLayerKey == layers.key && lastAttributionRadarVisible == isRadarVisible) {
            return
        }

        lastAttributionLayerKey = layers.key
        lastAttributionRadarVisible = isRadarVisible

        scope?.launch {
            if (isRadarVisible) {
                val attribution = views.radarCompassMap.getAttribution(context)
                views.mapAttribution.text = attribution
                views.mapAttribution.isVisible = attribution != null
            } else {
                views.mapAttribution.isVisible = false
            }
        }
    }

    private fun updateCompassStyle() {
        val style = viewModel.compassStyle()
        views.linearCompass.isInvisible = style != CompassStyle.Linear
        views.radarCompass.isInvisible = style != CompassStyle.Radar
        views.radarCompassMap.isInvisible = style != CompassStyle.Radar
    }

    private fun updateQuickActionAvailability() {
        // TODO: Extract this logic to the flashlight (if camera is in use)
        if (userPrefs.navigation.rightButton == ToolIds.QUICK_ACTION_FLASHLIGHT) {
            views.navigationTitle.rightButton.isClickable = !views.linearCompass.isCameraActive
        }
        if (userPrefs.navigation.leftButton == ToolIds.QUICK_ACTION_FLASHLIGHT) {
            views.navigationTitle.leftButton.isClickable = !views.linearCompass.isCameraActive
        }
    }

    private fun updateCompassBearing() {
        val compass = viewModel.sensors.compass
        val bearing = compass.rawBearing
        val declination = viewModel.declination

        // Azimuth
        if (viewModel.hasCompass) {
            views.navigationTitle.title = formatService.formatDegrees(bearing, replace360 = true)
            // Spelled out: the dial already shows the letters, the readout says it the way a person would.
            views.navigationTitle.direction = context.getString(
                when (CompassDirection.nearest(bearing)) {
                    CompassDirection.North -> R.string.direction_north_full
                    CompassDirection.NorthEast -> R.string.direction_north_east_full
                    CompassDirection.East -> R.string.direction_east_full
                    CompassDirection.SouthEast -> R.string.direction_south_east_full
                    CompassDirection.South -> R.string.direction_south_full
                    CompassDirection.SouthWest -> R.string.direction_south_west_full
                    CompassDirection.West -> R.string.direction_west_full
                    CompassDirection.NorthWest -> R.string.direction_north_west_full
                }
            )
        }

        // Compass
        listOf<ICompassView>(
            views.radarCompass,
            views.linearCompass
        ).forEach {
            it.azimuth = bearing
            it.declination = declination
        }

        val actualBearing = if (viewModel.useTrueNorth) {
            compass.bearing
        } else {
            compass.bearing.withDeclination(declination)
        }
        views.radarCompassMap.userAzimuth = actualBearing
        views.radarCompassMap.mapAzimuth = actualBearing.value
    }

    private fun updateLocation() {
        val gps = viewModel.sensors.gps
        val location = gps.location

        viewModel.updateLocation()

        // Readable at a glance: hemispheres instead of minus signs, 4 decimals (about 10 m) for decimal degrees; other
        // formats (UTM, MGRS, DMS…) as the person chose them.
        views.navigationTitle.subtitle = if (userPrefs.coordinateFormat == BuiltInCoordinateFormat.DecimalDegrees) {
            val lat = "%.4f° %s".format(kotlin.math.abs(location.latitude), if (location.latitude >= 0) "N" else "S")
            val lon = "%.4f° %s".format(kotlin.math.abs(location.longitude), if (location.longitude >= 0) "E" else "W")
            "$lat  $lon"
        } else {
            formatService.formatLocation(location)
        }

        views.radarCompassMap.userLocationAccuracy = gps.horizontalAccuracy?.let { Distance.meters(it) }

        // Compass center point
        listOf<ICompassView>(
            views.radarCompass,
            views.linearCompass
        ).forEach {
            it.compassCenter = location
        }

        views.radarCompassMap.mapCenter = location
        views.radarCompassMap.userLocation = location
        views.radarCompassMap.projection = AzimuthalEquidistantProjection(location)
    }

    fun updateCompassLayers(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            val destBearing = viewModel.getDestinationBearing()
            val destinationBeacon = (viewModel.currentDestination as? Destination.Beacon)?.beacon
            val destColor = destinationBeacon?.color ?: AppColor.Blue.color

            val direction = destBearing?.let {
                MappableBearing(it, destColor)
            }

            // Update beacon layers
            beaconCompassLayer.setBeacons(viewModel.nearbyBeacons)
            beaconCompassLayer.highlight(destinationBeacon)

            // Destination
            if (destinationBeacon != null) {
                navigationCompassLayer.setDestination(destinationBeacon)
            } else if (direction != null) {
                navigationCompassLayer.setDestination(direction)
            } else {
                navigationCompassLayer.setDestination(null as MappableBearing?)
            }
        }
    }
}
