@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.triangulate.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.shared.views.compose.trailSensePressScale
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlightAction
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBearingField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.BaselineRefresh24
import com.kylecorry.trail_sense.shared.icons.trail.Location
import com.kylecorry.trail_sense.shared.icons.trail.Beacon
import com.kylecorry.trail_sense.shared.icons.trail.Share
import com.kylecorry.trail_sense.shared.icons.trail.Satellite
import com.kylecorry.trail_sense.shared.icons.trail.Help
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.andromeda.preferences.putOrRemoveCoordinate
import com.kylecorry.andromeda.preferences.putOrRemoveFloat
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.AppUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryMarkerColor
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.from
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.ConfigurableGeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayers
import com.kylecorry.trail_sense.shared.navigation.NavControllerAppNavigation
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.camera.SightingCompassDialog
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.gps.GPSLocationSource
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.BearingInput as ComposeBearingInput
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import com.kylecorry.trail_sense.tools.map.map_layers.ScaleBarLayer
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TriangulateToolScreen(
    host: TrailSenseDestinationHost,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember(host) {
        TriangulateController(host)
    }

    DisposableEffect(lifecycleOwner, controller) {
        controller.onViewCreated()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            controller.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onPause()
            controller.onDestroyView()
        }
    }

    controller.Content(modifier)
}

private class TriangulateController(
    private val host: TrailSenseDestinationHost
) {

    private val formatService by lazy { FormatService.getInstance(host.requireContext()) }
    private val prefs by lazy { UserPreferences(host.requireContext()) }
    private val appNavigation by lazy { NavControllerAppNavigation(host.findNavController()) }
    private val navigator by lazy { Navigator.getInstance(host.requireContext()) }

    private var location: Coordinate? = null

    private var shouldCalculateMyLocation by mutableStateOf(false)
    private var title by mutableStateOf("")
    private var actionsVisible by mutableStateOf(false)
    private var navigateVisible by mutableStateOf(true)
    private var updateGpsOverrideVisible by mutableStateOf(false)
    private var location1Title by mutableStateOf("")
    private var location2Title by mutableStateOf("")
    private var location1Instructions by mutableStateOf("")
    private var location2Instructions by mutableStateOf("")
    private var location1Complete by mutableStateOf(false)
    private var location2Complete by mutableStateOf(false)
    private var location1Expanded by mutableStateOf(true)
    private var location2Expanded by mutableStateOf(false)
    private var location1Coordinate by mutableStateOf<Coordinate?>(null)
    private var location2Coordinate by mutableStateOf<Coordinate?>(null)
    private var bearing1Value by mutableStateOf<Float?>(null)
    private var bearing2Value by mutableStateOf<Float?>(null)
    private var bearing1TrueNorth by mutableStateOf(false)
    private var bearing2TrueNorth by mutableStateOf(false)
    private var compassBearingText by mutableStateOf("")

    private var map: MapView? = null
    private var isViewCreated = false
    private var isSetup = false

    private val isBound: Boolean
        get() = map != null

    private val compass by lazy { SensorService(host.requireContext()).getCompass() }
    private val hasCompass by lazy { SensorService(host.requireContext()).hasCompass() }
    private var cameraSheet: SightingCompassDialog? = null
    private var cameraBearingIndex: Int? = null

    private val layer = ConfigurableGeoJsonLayer()
    private val scaleBarLayer = ScaleBarLayer()

    private val radius = Distance.meters(100f)

    private val recommendedMinDistance by lazy {
        if (prefs.distanceUnits == UserPreferences.DistanceUnits.Feet) {
            Distance.feet(100f)
        } else {
            Distance.meters(30f)
        }
    }

    private val updateTimer = CoroutineTimer {
        update()
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        TriangulateScreen(
            title = title,
            actionsVisible = actionsVisible,
            navigateVisible = navigateVisible,
            updateGpsOverrideVisible = updateGpsOverrideVisible,
            shouldCalculateMyLocation = shouldCalculateMyLocation,
            location1Title = location1Title,
            location2Title = location2Title,
            location1Instructions = location1Instructions,
            location2Instructions = location2Instructions,
            location1Complete = location1Complete,
            location2Complete = location2Complete,
            location1Expanded = location1Expanded,
            location2Expanded = location2Expanded,
            location1 = location1Coordinate,
            location2 = location2Coordinate,
            bearing1 = bearing1Value,
            bearing2 = bearing2Value,
            bearing1TrueNorth = bearing1TrueNorth,
            bearing2TrueNorth = bearing2TrueNorth,
            compassBearingText = compassBearingText,
            hasCompass = hasCompass,
            onModeChanged = {
                shouldCalculateMyLocation = it
                update()
            },
            onCreateBeacon = ::createBeacon,
            onNavigate = ::navigateToLocation,
            onShareLocation = ::shareLocation,
            onUpdateGpsOverride = ::updateGpsOverride,
            onMapReady = {
                map = it
                maybeSetup()
            },
            onLocation1Change = ::updateLocation1Coordinate,
            onLocation2Change = ::updateLocation2Coordinate,
            onBearing1Change = { bearing, trueNorth -> updateBearing(1, bearing, trueNorth) },
            onBearing2Change = { bearing, trueNorth -> updateBearing(2, bearing, trueNorth) },
            onCompassBearing1Click = { setBearingFromCompass(1) },
            onCompassBearing2Click = { setBearingFromCompass(2) },
            onCameraBearing1Click = { setBearingFromCamera(1) },
            onCameraBearing2Click = { setBearingFromCamera(2) },
            onLocation1ExpandedChange = { location1Expanded = it },
            onLocation2ExpandedChange = { location2Expanded = it },
            onReset = ::reset,
            modifier = modifier.fillMaxSize()
        )
        cameraSheet?.Content()
    }

    fun onViewCreated() {
        isViewCreated = true
        maybeSetup()
    }

    private fun maybeSetup() {
        if (!isViewCreated || isSetup || !isBound) {
            return
        }

        isSetup = true
        layer.renderer.configureLineStringRenderer(shouldRenderWithDrawLines = true)
        layer.renderer.configurePointRenderer(shouldRenderLabels = true)
        map?.setLayers(listOf(layer, scaleBarLayer))
        update()
    }

    private fun createBeacon() {
        location?.let {
            AppUtils.placeBeacon(host.requireContext(), GeoUri(it))
        }
    }

    private fun navigateToLocation() {
        location?.let {
            navigator.navigateTo(it, host.getString(R.string.location), BeaconOwner.Triangulate)
            appNavigation.navigate(R.id.action_navigation)
        }
    }

    private fun shareLocation() {
        location?.let {
            Share.shareLocation(host, it)
        }
    }

    private fun updateGpsOverride() {
        location?.let { coord ->
            prefs.gps.locationOverride = coord
            Alerts.toast(host.requireContext(), host.getString(R.string.location_override_updated))
        }
    }

    private fun updateLocation1Coordinate(coordinate: Coordinate?) {
        location1Coordinate = coordinate
        update()
    }

    private fun updateLocation2Coordinate(coordinate: Coordinate?) {
        location2Coordinate = coordinate
        update()
    }

    private fun updateBearing(index: Int, bearing: Float?, trueNorth: Boolean) {
        if (index == 1) {
            bearing1Value = bearing
            bearing1TrueNorth = trueNorth
        } else {
            bearing2Value = bearing
            bearing2TrueNorth = trueNorth
        }
        update()
    }

    private fun setBearingFromCompass(index: Int) {
        updateBearing(index, compass.rawBearing, false)
    }

    private fun setBearingFromCamera(index: Int) {
        host.requestCamera { hasPermission ->
            if (hasPermission) {
                cameraBearingIndex = index
                if (cameraSheet == null) {
                    cameraSheet = SightingCompassDialog(
                        host.requireContext(),
                        host.lifecycleOwner,
                        host.lifecycleScope
                    ) {
                        cameraBearingIndex?.let { selectedIndex ->
                            updateBearing(selectedIndex, it, false)
                        }
                    }
                }
                cameraSheet?.show()
            } else {
                host.alertNoCameraPermission()
            }
        }
    }

    private fun updateMap() {
        val map = map ?: return
        val location1 = location1Coordinate
        val location2 = location2Coordinate
        val destination = location

        val fences = listOfNotNull(
            location1,
            location2,
            destination
        ).map {
            Geofence(it, radius)
        }

        val bounds = CoordinateBounds.from(fences)

        map.fitIntoView(bounds)

        val points = listOfNotNull(
            location1?.let {
                GeoJsonFeature.point(
                    it,
                    id = 1,
                    name = "1",
                    color = Resources.getPrimaryMarkerColor(host.requireContext()),
                    strokeColor = Color.WHITE
                )
            },
            location2?.let {
                GeoJsonFeature.point(
                    it,
                    id = 2,
                    name = "2",
                    color = Resources.getPrimaryMarkerColor(host.requireContext()),
                    strokeColor = Color.WHITE
                )
            },
            destination?.let {
                GeoJsonFeature.point(
                    it,
                    id = 3,
                    color = AppColor.Green.color,
                    strokeColor = Color.WHITE
                )
            }
        )

        val path1 = getPath(1)
        val path2 = getPath(2)
        val paths = listOfNotNull(path1, path2)

        layer.setData(GeoJsonFeatureCollection(points + paths))
    }

    private fun updateDistances() {
        if (!isBound) {
            return
        }

        location1Title = getLocationTitle(1).toString()
        location2Title = getLocationTitle(2).toString()
    }

    private fun getLocationTitle(locationIdx: Int): CharSequence {
        val distance = getDistanceToDestination(locationIdx)
        val formattedDistance = distance?.let {
            formatService.formatDistance(it, Units.getDecimalPlaces(it.units))
        }
        return buildSpannedString {
            append(host.getString(R.string.location_number, locationIdx))
            if (distance != null) {
                appendLine()
                scale(0.75f) {
                    append(
                        if (shouldCalculateMyLocation) {
                            host.getString(R.string.distance_away_from_self, formattedDistance)
                        } else {
                            host.getString(R.string.distance_away_from_destination, formattedDistance)
                        }
                    )
                }
            }
        }
    }

    private fun updateInstructions() {
        if (!isBound) {
            return
        }

        if (shouldCalculateMyLocation) {
            location1Instructions = host.getString(R.string.triangulate_self_location_1_instructions)
            location2Instructions = host.getString(R.string.triangulate_self_location_2_instructions)
        } else {
            location1Instructions = host.getString(R.string.triangulate_destination_location_1_instructions)
            location2Instructions = host.getString(
                R.string.triangulate_destination_location_2_instructions,
                formatService.formatDistance(
                    recommendedMinDistance,
                    Units.getDecimalPlaces(recommendedMinDistance.units)
                )
            )
        }
    }

    private fun updateCompletionState() {
        if (!isBound) {
            return
        }

        location1Complete = isComplete(1)
        location2Complete = isComplete(2)
    }

    private fun getDistanceToDestination(locationIdx: Int): Distance? {
        val start =
            (if (locationIdx == 1) location1Coordinate else location2Coordinate)
                ?: return null
        val destination = location ?: return null
        return Distance.meters(start.distanceTo(destination)).convertTo(prefs.baseDistanceUnits)
            .toRelativeDistance()
    }

    private fun getPath(locationIdx: Int): GeoJsonFeature? {
        val destination = location
        val start = if (locationIdx == 1) location1Coordinate else location2Coordinate
        val direction = if (locationIdx == 1) bearing1Value else bearing2Value
        val trueNorth = if (locationIdx == 1) bearing1TrueNorth else bearing2TrueNorth

        val end = if (start != null && direction != null) {
            val declination = if (trueNorth == true) 0f else Geophysics.getGeomagneticDeclination(start)
            val bearing = Bearing.from(direction).withDeclination(declination)
            destination ?: start.plus(
                Distance.kilometers(1f),
                if (shouldCalculateMyLocation) bearing.inverse() else bearing
            )
        } else {
            null
        }

        return if (start != null && end != null) {
            val pts = listOf(start, end)
            GeoJsonFeature.lineString(
                if (shouldCalculateMyLocation) pts.reversed() else pts,
                locationIdx.toLong(),
                color = Resources.getPrimaryMarkerColor(host.requireContext()),
                lineStyle = LineStyle.Arrow
            )
        } else {
            null
        }
    }

    fun onResume() {
        compass.start {
            val bearing = compass.rawBearing
            compassBearingText = formatService.formatDegrees(bearing, replace360 = true)
            cameraSheet?.bearing = bearing
            true
        }
        restoreState()
        updateTimer.once(500)
    }

    fun onPause() {
        updateTimer.stop()
        compass.stop(null)
        saveState()
    }

    fun onDestroyView() {
        map = null
        cameraSheet?.dismiss()
        cameraSheet = null
        cameraBearingIndex = null
        isViewCreated = false
        isSetup = false
    }

    private fun update() {
        if (!isBound) {
            return
        }

        updateInstructions()
        updateCompletionState()

        val location1 = location1Coordinate
        val location2 = location2Coordinate
        val direction1 = bearing1Value
        val direction2 = bearing2Value

        if (location1 == null || location2 == null || direction1 == null || direction2 == null) {
            setLocation(null)
            return
        }

        val declination1 =
            if (bearing1TrueNorth) 0f else Geophysics.getGeomagneticDeclination(location1)
        val declination2 =
            if (bearing2TrueNorth) 0f else Geophysics.getGeomagneticDeclination(location2)
        val bearing1 = Bearing.from(direction1).withDeclination(declination1)
        val bearing2 = Bearing.from(direction2).withDeclination(declination2)

        val location = if (shouldCalculateMyLocation) {
            Geography.triangulateSelf(location1, bearing1, location2, bearing2)
        } else {
            Geography.triangulateDestination(location1, bearing1, location2, bearing2)
        }
        setLocation(location)
    }

    private fun setLocation(location: Coordinate?) {
        this.location = location
        if (location == null || location.latitude.isNaN() || location.longitude.isNaN()) {
            title = host.getString(R.string.could_not_triangulate)
            actionsVisible = false
        } else {
            title = formatService.formatLocation(location)
            actionsVisible = true
        }

        navigateVisible = !shouldCalculateMyLocation
        updateGpsOverrideVisible =
            prefs.gps.locationSource == GPSLocationSource.Manual && shouldCalculateMyLocation

        updateMap()
        updateDistances()
    }

    private fun saveState() {
        if (!isBound) {
            return
        }

        val preferences = PreferencesSubsystem.getInstance(host.requireContext()).preferences
        preferences.putBoolean("state_triangulate_self", shouldCalculateMyLocation)
        preferences.putOrRemoveFloat("state_triangulate_bearing1", bearing1Value)
        preferences.putOrRemoveFloat("state_triangulate_bearing2", bearing2Value)
        preferences.putOrRemoveCoordinate(
            "state_triangulate_location1",
            location1Coordinate
        )
        preferences.putOrRemoveCoordinate(
            "state_triangulate_location2",
            location2Coordinate
        )
        preferences.putBoolean("state_triangulate_true_north1", bearing1TrueNorth)
        preferences.putBoolean("state_triangulate_true_north2", bearing2TrueNorth)
    }

    private fun restoreState() {
        if (!isBound) {
            return
        }

        val preferences = PreferencesSubsystem.getInstance(host.requireContext()).preferences
        shouldCalculateMyLocation = preferences.getBoolean("state_triangulate_self") ?: false
        bearing1Value = preferences.getFloat("state_triangulate_bearing1")
        bearing2Value = preferences.getFloat("state_triangulate_bearing2")
        bearing1TrueNorth = preferences.getBoolean("state_triangulate_true_north1") ?: false
        bearing2TrueNorth = preferences.getBoolean("state_triangulate_true_north2") ?: false
        location1Coordinate = preferences.getCoordinate("state_triangulate_location1")
        location2Coordinate = preferences.getCoordinate("state_triangulate_location2")
        update()
    }

    private fun reset() {
        location1Coordinate = null
        location2Coordinate = null
        bearing1Value = null
        bearing2Value = null
        bearing1TrueNorth = false
        bearing2TrueNorth = false

        val preferences = PreferencesSubsystem.getInstance(host.requireContext()).preferences
        preferences.remove("state_triangulate_self")
        preferences.remove("state_triangulate_bearing1")
        preferences.remove("state_triangulate_bearing2")
        preferences.remove("state_triangulate_location1")
        preferences.remove("state_triangulate_location2")
        preferences.remove("state_triangulate_true_north1")
        preferences.remove("state_triangulate_true_north2")

        update()
    }

    private fun isComplete(locationIdx: Int): Boolean {
        val location =
            if (locationIdx == 1) location1Coordinate else location2Coordinate
        val bearing = if (locationIdx == 1) bearing1Value else bearing2Value
        return location != null && bearing != null
    }

}

/**
 * Triangulate: the answer first (the coordinate, with what you can do with it, over a map of both sightings), then
 * what you're finding, then the two sightings as expandable cards. Reset lives in the top bar so it can't be hit by
 * accident at the end of the form.
 */
@Composable
private fun TriangulateScreen(
    title: String,
    actionsVisible: Boolean,
    navigateVisible: Boolean,
    updateGpsOverrideVisible: Boolean,
    shouldCalculateMyLocation: Boolean,
    location1Title: String,
    location2Title: String,
    location1Instructions: String,
    location2Instructions: String,
    location1Complete: Boolean,
    location2Complete: Boolean,
    location1Expanded: Boolean,
    location2Expanded: Boolean,
    location1: Coordinate?,
    location2: Coordinate?,
    bearing1: Float?,
    bearing2: Float?,
    bearing1TrueNorth: Boolean,
    bearing2TrueNorth: Boolean,
    compassBearingText: String,
    hasCompass: Boolean,
    onModeChanged: (Boolean) -> Unit,
    onCreateBeacon: () -> Unit,
    onNavigate: () -> Unit,
    onShareLocation: () -> Unit,
    onUpdateGpsOverride: () -> Unit,
    onMapReady: (MapView) -> Unit,
    onLocation1Change: (Coordinate?) -> Unit,
    onLocation2Change: (Coordinate?) -> Unit,
    onBearing1Change: (Float?, Boolean) -> Unit,
    onBearing2Change: (Float?, Boolean) -> Unit,
    onCompassBearing1Click: () -> Unit,
    onCompassBearing2Click: () -> Unit,
    onCameraBearing1Click: () -> Unit,
    onCameraBearing2Click: () -> Unit,
    onLocation1ExpandedChange: (Boolean) -> Unit,
    onLocation2ExpandedChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The map is held here, outside the lazy list, so scrolling it off screen never trades it for a fresh, empty one.
    val mapView = rememberTriangulateMap(onMapReady)
    val modeTitle = stringResource(R.string.location_to_triangulate)
    val hasSighting = location1 != null || location2 != null
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    TrailSensePage(
        title = stringResource(R.string.tool_triangulate_title),
        backLabel = stringResource(R.string.back),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = TrailSenseSpacing.lg,
            end = TrailSenseSpacing.lg,
            top = TrailSenseSpacing.sm,
            bottom = TrailSenseSpacing.xxxl + navBottom
        ),
        actions = {
            IconButton(onClick = onReset, modifier = Modifier.testTag("reset_btn")) {
                Icon(
                    imageVector = TrailIcons.BaselineRefresh24,
                    contentDescription = stringResource(R.string.reset)
                )
            }
        }
    ) {
        item(key = "result") {
            TriangulateResultCard(
                title = title,
                actionsVisible = actionsVisible,
                navigateVisible = navigateVisible,
                updateGpsOverrideVisible = updateGpsOverrideVisible,
                shouldCalculateMyLocation = shouldCalculateMyLocation,
                completed = listOf(location1Complete, location2Complete).count { it },
                onCreateBeacon = onCreateBeacon,
                onNavigate = onNavigate,
                onShareLocation = onShareLocation,
                onUpdateGpsOverride = onUpdateGpsOverride
            )
        }

        // The map only appears once there's something to put on it, as a tinted card rather than a black void.
        item(key = "map") {
            AnimatedVisibility(
                visible = hasSighting,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                        fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                        fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    mapView.Content(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .testTag("map")
                    )
                }
            }
        }

        item(key = "mode") {
            Column {
                TrailSenseGroupHeader(modeTitle)
                TrailSenseConnectedChoice(
                    options = listOf(
                        TrailSenseChoice(false, stringResource(R.string.destination), TrailIcons.Flag),
                        TrailSenseChoice(true, stringResource(R.string.my_location), TrailIcons.Location)
                    ),
                    selected = shouldCalculateMyLocation,
                    onSelect = onModeChanged,
                    label = stringResource(R.string.location_to_triangulate)
                )
            }
        }

        item(key = "location_1") {
            TriangulateLocationCard(
                number = 1,
                title = location1Title,
                instructions = location1Instructions,
                isComplete = location1Complete,
                isActive = !location1Complete,
                isExpanded = location1Expanded,
                onExpandedChange = onLocation1ExpandedChange,
                coordinate = location1,
                bearing = bearing1,
                trueNorth = bearing1TrueNorth,
                compassBearingText = compassBearingText,
                hasCompass = hasCompass,
                coordinateTag = "location_1",
                bearingTag = "bearing_1",
                onCoordinateChange = onLocation1Change,
                onBearingChange = onBearing1Change,
                onCompassBearingClick = onCompassBearing1Click,
                onCameraBearingClick = onCameraBearing1Click
            )
        }

        item(key = "location_2") {
            TriangulateLocationCard(
                number = 2,
                title = location2Title,
                instructions = location2Instructions,
                isComplete = location2Complete,
                isActive = location1Complete && !location2Complete,
                isExpanded = location2Expanded,
                onExpandedChange = onLocation2ExpandedChange,
                coordinate = location2,
                bearing = bearing2,
                trueNorth = bearing2TrueNorth,
                compassBearingText = compassBearingText,
                hasCompass = hasCompass,
                coordinateTag = "location_2",
                bearingTag = "bearing_2",
                onCoordinateChange = onLocation2Change,
                onBearingChange = onBearing2Change,
                onCompassBearingClick = onCompassBearing2Click,
                onCameraBearingClick = onCameraBearing2Click
            )
        }
    }
}

/**
 * The answer as the screen's Spotlight: while sightings are missing, a secondary card counting them (two cookies
 * that fill in); once solved, the coordinate large on primary with what you can do with it.
 */
@Composable
private fun TriangulateResultCard(
    title: String,
    actionsVisible: Boolean,
    navigateVisible: Boolean,
    updateGpsOverrideVisible: Boolean,
    shouldCalculateMyLocation: Boolean,
    completed: Int,
    onCreateBeacon: () -> Unit,
    onNavigate: () -> Unit,
    onShareLocation: () -> Unit,
    onUpdateGpsOverride: () -> Unit
) {
    val tone = TrailSenseTone.Primary
    val colors = trailSenseToneColors(tone)
    TrailSenseSpotlight(
        modifier = Modifier.testTag("triangulate_title"),
        eyebrow = stringResource(if (shouldCalculateMyLocation) R.string.my_location else R.string.destination),
        // Until there's an answer the headline says what to do next; a failure is only the subtitle.
        title = when {
            actionsVisible -> title
            completed == 0 -> stringResource(R.string.places_triangulate_take_first)
            completed == 1 -> stringResource(R.string.places_triangulate_take_second)
            else -> stringResource(R.string.places_triangulate_check)
        },
        titleStyle = MaterialTheme.typography.headlineSmall,
        supportingText = when {
            actionsVisible -> null
            completed == 2 -> title
            else -> stringResource(R.string.places_triangulate_sightings, completed)
        },
        tone = tone,
        badge = {
            TrailSenseShapeBadge(
                icon = if (actionsVisible) TrailIcons.Location else TrailIcons.ToolTriangulate,
                shape = if (actionsVisible) MaterialShapes.Sunny.toShape() else MaterialShapes.Cookie9Sided.toShape(),
                size = 64.dp,
                containerColor = colors.accent,
                contentColor = colors.onAccent
            )
        },
        actions = if (actionsVisible) {
            listOfNotNull(
                TrailSenseSpotlightAction(stringResource(R.string.beacon), TrailIcons.Location, onClick = onCreateBeacon),
                if (navigateVisible) {
                    TrailSenseSpotlightAction(stringResource(R.string.navigate), TrailIcons.NavigationArrow, onClick = onNavigate)
                } else {
                    null
                },
                TrailSenseSpotlightAction(stringResource(R.string.share_ellipsis), TrailIcons.Share, onClick = onShareLocation)
            )
        } else {
            emptyList()
        }
    ) {
        if (actionsVisible && updateGpsOverrideVisible) {
            FilledTonalButton(
                onClick = onUpdateGpsOverride,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.72f),
                    contentColor = colors.content
                )
            ) {
                Icon(TrailIcons.Satellite, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(TrailSenseSpacing.sm))
                Text(stringResource(R.string.update_gps_override))
            }
        }
    }
}

@Composable
private fun rememberTriangulateMap(onMapReady: (MapView) -> Unit): MapView {
    val context = LocalContext.current
    val background = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
    val mapView = remember {
        MapView(context).apply {
            id = R.id.map
        }
    }
    SideEffect {
        mapView.backgroundTintList = ColorStateList.valueOf(background)
        mapView.backgroundColorOverride = background
    }
    SideEffect {
        onMapReady(mapView)
    }
    return mapView
}

/**
 * One sighting: a header that says where you are with it (its number, or a check once it has a location and a
 * bearing, plus how far it is from the answer), opening onto the instructions and the two inputs.
 */
@Composable
private fun TriangulateLocationCard(
    number: Int,
    title: String,
    instructions: String,
    isComplete: Boolean,
    isActive: Boolean = false,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    coordinate: Coordinate?,
    bearing: Float?,
    trueNorth: Boolean,
    compassBearingText: String,
    hasCompass: Boolean,
    coordinateTag: String,
    bearingTag: String,
    onCoordinateChange: (Coordinate?) -> Unit,
    onBearingChange: (Float?, Boolean) -> Unit,
    onCompassBearingClick: () -> Unit,
    onCameraBearingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The controller's title is "Location n" with the distance to the answer on a second line.
    val fallbackTitle = stringResource(R.string.location_number, number)
    val lines = title.ifBlank { fallbackTitle }.split('\n', limit = 2)
    val headline = lines.first()
    val distance = lines.getOrNull(1)?.takeIf { it.isNotBlank() }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "triangulateChevron"
    )
    val badgeColor by animateColorAsState(
        targetValue = if (isComplete) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "triangulateBadge"
    )

    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
        Surface(
            shape = trailSenseGroupShape(TrailSenseGroupPosition(isFirst = true, isLast = !isExpanded)),
            color = if (isActive) scheme.primaryContainer else scheme.surfaceContainerLow,
            contentColor = if (isActive) scheme.onPrimaryContainer else scheme.onSurface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 76.dp)
                    .clickable(
                        onClickLabel = headline,
                        onClick = { onExpandedChange(!isExpanded) }
                    )
                    .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlacesBadge(
                    containerColor = if (isComplete || isActive) scheme.primary else scheme.primaryContainer,
                    contentColor = if (isComplete || isActive) scheme.onPrimary else scheme.onPrimaryContainer,
                    icon = if (isComplete) Icons.Rounded.Check else null,
                    text = if (isComplete) null else number.toString(),
                    shape = MaterialShapes.Cookie4Sided.toShape(),
                    size = 44.dp
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = TrailSenseSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
                ) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isActive) scheme.onPrimaryContainer else scheme.onSurface
                    )
                    if (distance != null) {
                        Text(
                            text = distance,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                            color = scheme.primary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(chevronRotation),
                    tint = scheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                Surface(
                    shape = trailSenseGroupShape(TrailSenseGroupPosition(isFirst = false, isLast = false)),
                    color = scheme.surfaceContainerLow
                ) {
                    Text(
                        text = instructions,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md)
                    )
                }
                CoordinateInput(
                    coordinate = coordinate,
                    onCoordinateChange = onCoordinateChange,
                    tag = coordinateTag,
                    modifier = Modifier.fillMaxWidth()
                )
                PlacesBearingField(
                    bearing = bearing,
                    trueNorth = trueNorth,
                    compassBearingText = compassBearingText,
                    hasCompass = hasCompass,
                    onBearingChange = onBearingChange,
                    onCompassClick = onCompassBearingClick,
                    onCameraClick = onCameraBearingClick,
                    position = TrailSenseGroupPosition(isFirst = false, isLast = true),
                    tag = bearingTag
                )
            }
        }
    }
}

@Composable
private fun CoordinateInput(
    coordinate: Coordinate?,
    onCoordinateChange: (Coordinate?) -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    val sensors = remember { SensorSubsystem.getInstance(context) }
    val gps = remember { SensorService(context).getGPS() }
    val scope = rememberCoroutineScope()
    var coordinateText by remember(coordinate, formatter) {
        mutableStateOf(coordinate?.let { formatter.formatLocation(it) }.orEmpty())
    }
    val parsedCoordinate = remember(coordinateText) {
        Coordinate.parse(coordinateText)
    }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    fun autofillFromGps() {
        scope.launch {
            isLoadingLocation = true
            val updated = withContext(Dispatchers.IO) {
                sensors.getLocation(SensorSubsystem.SensorRefreshPolicy.Refresh)
            }
            coordinateText = formatter.formatLocation(updated)
            onCoordinateChange(updated)
            isLoadingLocation = false
        }
    }

    LaunchedEffect(coordinate) {
        val formatted = coordinate?.let { formatter.formatLocation(it) }.orEmpty()
        if (coordinateText != formatted) {
            coordinateText = formatted
        }
    }

    val isInvalid = coordinateText.isNotBlank() && parsedCoordinate == null
    TrailSenseInputField(
        value = coordinateText,
        onValueChange = {
            coordinateText = it
            onCoordinateChange(Coordinate.parse(it))
        },
        label = stringResource(R.string.location),
        modifier = modifier.testTag(tag),
        position = TrailSenseGroupPosition(isFirst = false, isLast = false),
        keyboardType = KeyboardType.Text,
        isError = isInvalid,
        errorText = stringResource(R.string.coordinate_input_invalid_location),
        enabled = !isLoadingLocation,
        leadingIcon = {
            if (isLoadingLocation) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("gps_loading"),
                    strokeWidth = 3.dp
                )
            } else {
                FilledTonalIconButton(
                    onClick = {
                        Pickers.item(
                            context,
                            context.getString(R.string.autofill_source),
                            listOf(
                                context.getString(R.string.gps),
                                context.getString(R.string.beacon)
                            ),
                            0
                        ) { selection ->
                            when (selection) {
                                0 -> autofillFromGps()
                                1 -> scope.launch {
                                    val beacon = BeaconPickers.pickBeacon(
                                        context,
                                        sort = ClosestBeaconSort(BeaconService(context), gps::location)
                                    ) ?: return@launch
                                    coordinateText = formatter.formatLocation(beacon.coordinate)
                                    onCoordinateChange(beacon.coordinate)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(start = TrailSenseSpacing.xs)
                        .testTag("gps_btn")
                ) {
                    Icon(
                        imageVector = TrailIcons.Location,
                        contentDescription = stringResource(R.string.gps)
                    )
                }
            }
        },
        trailingIcon = {
            androidx.compose.material3.IconButton(
                onClick = { showHelp = true },
                modifier = Modifier.testTag("coordinate_input_help_btn")
            ) {
                Icon(
                    imageVector = TrailIcons.Help,
                    contentDescription = stringResource(R.string.location_input_help_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            title = { Text(stringResource(R.string.location_input_help_title)) },
            text = { Text(stringResource(R.string.location_input_help)) }
        )
    }
}

@Composable
private fun BearingInput(
    bearing: Float?,
    trueNorth: Boolean,
    compassBearingText: String,
    hasCompass: Boolean,
    tag: String,
    onBearingChange: (Float?, Boolean) -> Unit,
    onCompassBearingClick: () -> Unit,
    onCameraBearingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ComposeBearingInput(
        bearing = bearing,
        trueNorth = trueNorth,
        compassBearingText = compassBearingText,
        hasCompass = hasCompass,
        onBearingChange = onBearingChange,
        onCompassClick = onCompassBearingClick,
        onCameraClick = onCameraBearingClick,
        modifier = modifier,
        tag = tag
    )
}

@Preview(showBackground = true)
@Composable
private fun TriangulateScreenPreview() {
    val context = LocalContext.current
    TrailSenseTheme {
        TriangulateScreen(
            title = context.getString(R.string.could_not_triangulate),
            actionsVisible = true,
            navigateVisible = true,
            updateGpsOverrideVisible = false,
            shouldCalculateMyLocation = false,
            location1Title = context.getString(R.string.location_number, 1),
            location2Title = context.getString(R.string.location_number, 2),
            location1Instructions = context.getString(R.string.triangulate_destination_location_1_instructions),
            location2Instructions = context.getString(
                R.string.triangulate_destination_location_2_instructions,
                "30 m"
            ),
            location1Complete = false,
            location2Complete = true,
            location1Expanded = true,
            location2Expanded = false,
            location1 = null,
            location2 = null,
            bearing1 = null,
            bearing2 = 135f,
            bearing1TrueNorth = false,
            bearing2TrueNorth = false,
            compassBearingText = "120 deg",
            hasCompass = true,
            onModeChanged = {},
            onCreateBeacon = {},
            onNavigate = {},
            onShareLocation = {},
            onUpdateGpsOverride = {},
            onMapReady = {},
            onLocation1Change = {},
            onLocation2Change = {},
            onBearing1Change = { _, _ -> },
            onBearing2Change = { _, _ -> },
            onCompassBearing1Click = {},
            onCompassBearing2Click = {},
            onCameraBearing1Click = {},
            onCameraBearing2Click = {},
            onLocation1ExpandedChange = {},
            onLocation2ExpandedChange = {},
            onReset = {}
        )
    }
}
