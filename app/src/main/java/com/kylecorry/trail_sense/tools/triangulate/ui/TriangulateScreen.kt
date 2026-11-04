@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.triangulate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBearingDialog
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
import androidx.compose.material3.Button
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
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesKeyValue
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDialog

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
 * Triangulate: what is being found on top (a destination, or where I am), then the answer as the screen's one
 * emphasised thing (until there is one, the next step to take), the map of both sightings under it, and the two
 * sightings as two plain groups: where you stood, which way you looked, and the ways to take that bearing. Reset lives
 * in the top bar so it can't be hit by accident.
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
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Where do my two sightings meet? The answer at the top says it; the map and the two sightings support it.
    // The map is held here, outside the lazy list, so scrolling it off screen never trades it for a fresh, empty one.
    val mapView = rememberTriangulateMap(onMapReady)
    val hasSighting = location1 != null || location2 != null
    val completed = listOf(location1Complete, location2Complete).count { it }
    // The sighting still to take carries the screen's one filled button (its compass bearing); none once both are in.
    val activeSighting = when {
        !location1Complete -> 1
        !location2Complete -> 2
        else -> 0
    }

    TrailSensePage(
        title = stringResource(R.string.tool_triangulate_title),
        backLabel = stringResource(R.string.back),
        modifier = modifier,
        actions = {
            IconButton(onClick = onReset, modifier = Modifier.testTag("reset_btn")) {
                Icon(
                    imageVector = TrailIcons.BaselineRefresh24,
                    contentDescription = stringResource(R.string.reset)
                )
            }
        }
    ) {
        item(key = "mode") {
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

        item(key = "result") {
            TriangulateAnswer(
                title = title,
                solved = actionsVisible,
                shouldCalculateMyLocation = shouldCalculateMyLocation,
                completed = completed,
                nextInstructions = if (activeSighting == 2) location2Instructions else location1Instructions,
                navigateVisible = navigateVisible,
                onNavigate = onNavigate,
                onCreateBeacon = onCreateBeacon
            )
        }

        // The map only appears once there's something to put on it.
        item(key = "map") {
            AnimatedVisibility(
                visible = hasSighting,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                        fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                        fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
            ) {
                val shape = RoundedCornerShape(TrailSenseShape.groupOuter)
                Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainer) {
                    mapView.Content(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(shape)
                            .testTag("map")
                    )
                }
            }
        }

        // What else to do with the answer, besides the main button: quiet rows.
        if (actionsVisible) {
            trailSenseSection(key = "actions") {
                if (navigateVisible) {
                    row { position ->
                        TrailSenseListItem(
                            title = stringResource(R.string.create_beacon),
                            icon = rememberVectorPainter(TrailIcons.Beacon),
                            position = position,
                            onClick = onCreateBeacon
                        )
                    }
                }
                row { position ->
                    TrailSenseListItem(
                        title = stringResource(R.string.share_ellipsis),
                        icon = rememberVectorPainter(TrailIcons.Share),
                        position = position,
                        onClick = onShareLocation
                    )
                }
                if (updateGpsOverrideVisible) {
                    row { position ->
                        TrailSenseListItem(
                            title = stringResource(R.string.update_gps_override),
                            icon = rememberVectorPainter(TrailIcons.Satellite),
                            position = position,
                            onClick = onUpdateGpsOverride
                        )
                    }
                }
            }
        }

        item(key = "location_1") {
            TriangulateSighting(
                number = 1,
                title = location1Title,
                isActive = activeSighting == 1,
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
            TriangulateSighting(
                number = 2,
                title = location2Title,
                isActive = activeSighting == 2,
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
 * The answer as plain type. Solved: the coordinate as the display value, what it is as the verdict, and one filled
 * main action (navigate to a destination, or save where you are as a beacon). Until then: the next step and how to
 * take it.
 */
@Composable
private fun TriangulateAnswer(
    title: String,
    solved: Boolean,
    shouldCalculateMyLocation: Boolean,
    completed: Int,
    nextInstructions: String,
    navigateVisible: Boolean,
    onNavigate: () -> Unit,
    onCreateBeacon: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("triangulate_title")
            .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
    ) {
        if (solved) {
            val display = MaterialTheme.typography.displaySmall
            Text(
                text = title,
                style = display.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                maxLines = 1,
                // A long coordinate format steps down to fit on one line rather than wrapping or cutting off.
                autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = display.fontSize)
            )
            Text(
                text = stringResource(if (shouldCalculateMyLocation) R.string.my_location else R.string.destination),
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(TrailSenseSpacing.md))
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
                text = stringResource(if (navigateVisible) R.string.navigate else R.string.create_beacon),
                onClick = if (navigateVisible) onNavigate else onCreateBeacon,
                icon = if (navigateVisible) TrailIcons.NavigationArrow else TrailIcons.Beacon,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = stringResource(
                    when (completed) {
                        0 -> R.string.places_triangulate_take_first
                        1 -> R.string.places_triangulate_take_second
                        else -> R.string.places_triangulate_check
                    }
                ),
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface
            )
            Text(
                // Both in but no meeting point: the line says so ("Could not triangulate").
                text = if (completed == 2) title else nextInstructions,
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun rememberTriangulateMap(onMapReady: (MapView) -> Unit): MapView {
    val context = LocalContext.current
    val background = MaterialTheme.colorScheme.surfaceContainer.toArgb()
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
 * One sighting as a plain group under its own header ("Location 1"), with how far it is from the answer as the
 * footer: where you stood, which way you looked (tap to type it), and the two ways to take the bearing live.
 */
@Composable
private fun TriangulateSighting(
    number: Int,
    title: String,
    isActive: Boolean,
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
    val lines = title.ifBlank { stringResource(R.string.location_number, number) }.split('\n', limit = 2)
    var showManualEntry by remember { mutableStateOf(false) }

    TrailSenseRowGroup(
        modifier = modifier,
        title = lines.first(),
        footer = lines.getOrNull(1)?.takeIf { it.isNotBlank() }
    ) {
        row { position ->
            CoordinateInput(
                coordinate = coordinate,
                onCoordinateChange = onCoordinateChange,
                tag = coordinateTag,
                position = position
            )
        }
        row { position ->
            TriangulateBearingRow(
                bearing = bearing,
                trueNorth = trueNorth,
                position = position,
                tag = bearingTag,
                onClick = { showManualEntry = true },
                onClear = { onBearingChange(null, false) }
            )
        }
        if (hasCompass) {
            row { position ->
                TrailSenseGroupCard(
                    position = position,
                    contentPadding = PaddingValues(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md)
                ) {
                    TriangulateBearingButtons(
                        isActive = isActive,
                        compassBearingText = compassBearingText,
                        onCompassClick = onCompassBearingClick,
                        onCameraClick = onCameraBearingClick
                    )
                }
            }
        }
    }

    if (showManualEntry) {
        PlacesBearingDialog(
            initialBearing = bearing,
            initialTrueNorth = trueNorth,
            onDismiss = { showManualEntry = false },
            onConfirm = { selectedBearing, selectedTrueNorth ->
                showManualEntry = false
                onBearingChange(selectedBearing, selectedTrueNorth)
            }
        )
    }
}

/**
 * The bearing as a family row: a well whose arrow points the way you looked, "Bearing", and the value with its north
 * reference. Tapping it types one in; once set, its one trailing element clears it.
 */
@Composable
private fun TriangulateBearingRow(
    bearing: Float?,
    trueNorth: Boolean,
    position: TrailSenseGroupPosition,
    tag: String,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    val scheme = MaterialTheme.colorScheme
    val rotation by animateFloatAsState(
        targetValue = bearing ?: 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "triangulateBearing"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = trailSenseGroupShape(position),
        color = scheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clickable(onClickLabel = stringResource(R.string.enter_manually), onClick = onClick)
                .testTag("manual_bearing")
                .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowWell {
                Icon(
                    imageVector = if (bearing != null) TrailIcons.NavigationArrow else TrailIcons.CompassIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(if (bearing != null) rotation else 0f)
                )
            }
            Spacer(Modifier.width(TrailSenseSpacing.md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                Text(
                    text = stringResource(R.string.bearing),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface
                )
                Text(
                    text = if (bearing != null) {
                        "${formatter.formatDegrees(bearing, replace360 = true)} · " +
                                stringResource(if (trueNorth) R.string.true_north else R.string.magnetic_north)
                    } else {
                        stringResource(R.string.direction_not_set)
                    },
                    modifier = Modifier.testTag("bearing"),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                    color = scheme.onSurfaceVariant
                )
            }
            if (bearing != null) {
                Spacer(Modifier.width(TrailSenseSpacing.md))
                IconButton(onClick = onClear, modifier = Modifier.testTag("clear_btn")) {
                    Icon(
                        TrailIcons.Cancel,
                        contentDescription = stringResource(android.R.string.cancel),
                        tint = scheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Taking the bearing live: from the compass (the value it would record; filled on the sighting still to take) or
 * through the camera, as two equal medium buttons.
 */
@Composable
private fun TriangulateBearingButtons(
    isActive: Boolean,
    compassBearingText: String,
    onCompassClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    val compassLabel = stringResource(R.string.places_triangulate_compass)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
    ) {
        Box(Modifier.weight(1f).testTag("compass_btn")) {
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
                text = compassBearingText,
                onClick = onCompassClick,
                icon = TrailIcons.CompassIcon,
                style = if (isActive) {
                    com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Filled
                } else {
                    com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Tonal
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "$compassLabel $compassBearingText" }
                    .testTag("compass_bearing")
            )
        }
        com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
            text = stringResource(R.string.camera),
            onClick = onCameraClick,
            icon = TrailIcons.Camera,
            style = com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Tonal,
            modifier = Modifier
                .weight(1f)
                .testTag("camera_btn")
        )
    }
}

/**
 * Where you stood, typed in place on the group's surface. The well in front fills it in (from the GPS or a beacon);
 * the help button explains the formats.
 */
@Composable
private fun CoordinateInput(
    coordinate: Coordinate?,
    onCoordinateChange: (Coordinate?) -> Unit,
    tag: String,
    position: TrailSenseGroupPosition,
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
    val autofillLabel = stringResource(R.string.autofill_source)
    TrailSenseInputField(
        value = coordinateText,
        onValueChange = {
            coordinateText = it
            onCoordinateChange(Coordinate.parse(it))
        },
        label = stringResource(R.string.location),
        modifier = modifier
            .heightIn(min = 72.dp)
            .testTag(tag),
        position = position,
        keyboardType = KeyboardType.Text,
        enabled = !isLoadingLocation,
        isError = isInvalid,
        errorText = stringResource(R.string.coordinate_input_invalid_location),
        leadingIcon = {
            if (isLoadingLocation) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("gps_loading"),
                        strokeWidth = 3.dp
                    )
                }
            } else {
                com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowWell(
                    modifier = Modifier
                        .padding(start = TrailSenseSpacing.xs)
                        .testTag("gps_btn")
                        .semantics { contentDescription = autofillLabel },
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
                    }
                ) {
                    Icon(
                        imageVector = TrailIcons.Location,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        trailingIcon = {
            IconButton(
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
        TrailSenseDialog(
            onDismissRequest = { showHelp = false },
            title = stringResource(R.string.location_input_help_title),
            confirmText = stringResource(android.R.string.ok)
        ) {
            Text(stringResource(R.string.location_input_help))
        }
    }
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
            onReset = {}
        )
    }
}

