package com.kylecorry.trail_sense.tools.triangulate.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.pickers.Pickers
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
import com.kylecorry.trail_sense.shared.camera.SightingCompassBottomSheetFragment
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.BearingInput as ComposeBearingInput
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
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

class FragmentToolTriangulate : TrailSenseComposeFragment() {

    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val appNavigation by lazy { NavControllerAppNavigation(findNavController()) }
    private val navigator by lazy { Navigator.getInstance(requireContext()) }

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

    private val compass by lazy { SensorService(requireContext()).getCompass() }
    private val hasCompass by lazy { SensorService(requireContext()).hasCompass() }
    private var cameraSheet: SightingCompassBottomSheetFragment? = null
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
    override fun FragmentContent() {
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
            modifier = Modifier.fillMaxSize()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            AppUtils.placeBeacon(requireContext(), GeoUri(it))
        }
    }

    private fun navigateToLocation() {
        location?.let {
            navigator.navigateTo(it, getString(R.string.location), BeaconOwner.Triangulate)
            appNavigation.navigate(R.id.action_navigation)
        }
    }

    private fun shareLocation() {
        location?.let {
            Share.shareLocation(this, it)
        }
    }

    private fun updateGpsOverride() {
        location?.let { coord ->
            prefs.locationOverride = coord
            Alerts.toast(requireContext(), getString(R.string.location_override_updated))
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
        requestCamera { hasPermission ->
            if (hasPermission) {
                cameraBearingIndex = index
                if (cameraSheet == null) {
                    cameraSheet = SightingCompassBottomSheetFragment {
                        cameraBearingIndex?.let { selectedIndex ->
                            updateBearing(selectedIndex, it, false)
                        }
                    }
                }
                cameraSheet?.show(this)
            } else {
                alertNoCameraPermission()
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
                    color = Resources.getPrimaryMarkerColor(requireContext()),
                    strokeColor = Color.WHITE
                )
            },
            location2?.let {
                GeoJsonFeature.point(
                    it,
                    id = 2,
                    name = "2",
                    color = Resources.getPrimaryMarkerColor(requireContext()),
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
            append(getString(R.string.location_number, locationIdx))
            if (distance != null) {
                appendLine()
                scale(0.75f) {
                    append(
                        if (shouldCalculateMyLocation) {
                            getString(R.string.distance_away_from_self, formattedDistance)
                        } else {
                            getString(R.string.distance_away_from_destination, formattedDistance)
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
            location1Instructions = getString(R.string.triangulate_self_location_1_instructions)
            location2Instructions = getString(R.string.triangulate_self_location_2_instructions)
        } else {
            location1Instructions = getString(R.string.triangulate_destination_location_1_instructions)
            location2Instructions = getString(
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
                color = Resources.getPrimaryMarkerColor(requireContext()),
                lineStyle = LineStyle.Arrow
            )
        } else {
            null
        }
    }

    override fun onResume() {
        super.onResume()
        compass.start {
            val bearing = compass.rawBearing
            compassBearingText = formatService.formatDegrees(bearing, replace360 = true)
            cameraSheet?.bearing = bearing
            true
        }
        restoreState()
        updateTimer.once(500)
    }

    override fun onPause() {
        super.onPause()
        updateTimer.stop()
        compass.stop(null)
        saveState()
    }

    override fun onDestroyView() {
        map = null
        cameraSheet = null
        cameraBearingIndex = null
        isViewCreated = false
        isSetup = false
        super.onDestroyView()
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
            title = getString(R.string.could_not_triangulate)
            actionsVisible = false
        } else {
            title = formatService.formatLocation(location)
            actionsVisible = true
        }

        navigateVisible = !shouldCalculateMyLocation
        updateGpsOverrideVisible = !prefs.useAutoLocation && shouldCalculateMyLocation

        updateMap()
        updateDistances()
    }

    private fun saveState() {
        if (!isBound) {
            return
        }

        val preferences = PreferencesSubsystem.getInstance(requireContext()).preferences
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

        val preferences = PreferencesSubsystem.getInstance(requireContext()).preferences
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

        val preferences = PreferencesSubsystem.getInstance(requireContext()).preferences
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
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = TrailSenseSpacing.xxxl)
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md)
                    .testTag("triangulate_title"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (actionsVisible) {
                TriangulateActions(
                    navigateVisible = navigateVisible,
                    updateGpsOverrideVisible = updateGpsOverrideVisible,
                    onCreateBeacon = onCreateBeacon,
                    onNavigate = onNavigate,
                    onShareLocation = onShareLocation,
                    onUpdateGpsOverride = onUpdateGpsOverride
                )
            }

            TriangulateMap(
                onMapReady = onMapReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = TrailSenseSpacing.sm)
                    .padding(top = TrailSenseSpacing.sm)
            )

            Text(
                text = stringResource(R.string.location_to_triangulate),
                modifier = Modifier.padding(
                    start = TrailSenseSpacing.sm,
                    end = TrailSenseSpacing.sm,
                    top = TrailSenseSpacing.lg
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            TriangulateModeSelector(
                shouldCalculateMyLocation = shouldCalculateMyLocation,
                onModeChanged = onModeChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.sm, vertical = TrailSenseSpacing.sm)
            )

            TriangulateLocationCard(
                title = location1Title.ifBlank { stringResource(R.string.location_number, 1) },
                instructions = location1Instructions,
                isComplete = location1Complete,
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
                onCameraBearingClick = onCameraBearing1Click,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.sm)
                    .padding(top = TrailSenseSpacing.lg)
            )

            TriangulateLocationCard(
                title = location2Title.ifBlank { stringResource(R.string.location_number, 2) },
                instructions = location2Instructions,
                isComplete = location2Complete,
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
                onCameraBearingClick = onCameraBearing2Click,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.sm)
                    .padding(top = TrailSenseSpacing.lg)
            )

            TrailSenseButton(
                text = stringResource(R.string.reset),
                onClick = onReset,
                modifier = Modifier
                    .padding(top = TrailSenseSpacing.lg)
                    .align(Alignment.CenterHorizontally)
                    .testTag("reset_btn")
            )
        }
    }
}

@Composable
private fun TriangulateActions(
    navigateVisible: Boolean,
    updateGpsOverrideVisible: Boolean,
    onCreateBeacon: () -> Unit,
    onNavigate: () -> Unit,
    onShareLocation: () -> Unit,
    onUpdateGpsOverride: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = TrailSenseSpacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TriangulateActionChip(
            label = stringResource(R.string.beacon),
            icon = R.drawable.ic_location,
            onClick = onCreateBeacon
        )
        if (navigateVisible) {
            TriangulateActionChip(
                label = stringResource(R.string.navigate),
                icon = R.drawable.ic_beacon,
                onClick = onNavigate
            )
        }
        TriangulateActionChip(
            label = stringResource(R.string.share_ellipsis),
            icon = R.drawable.ic_share,
            onClick = onShareLocation
        )
        if (updateGpsOverrideVisible) {
            TriangulateActionChip(
                label = stringResource(R.string.update_gps_override),
                icon = R.drawable.satellite,
                onClick = onUpdateGpsOverride
            )
        }
    }
}

@Composable
private fun TriangulateActionChip(
    label: String,
    icon: Int,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = Modifier.padding(end = TrailSenseSpacing.sm)
    )
}

@Composable
private fun TriangulateMap(
    onMapReady: (MapView) -> Unit,
    modifier: Modifier = Modifier
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    AndroidView(
        modifier = modifier.testTag("map"),
        factory = { context ->
            val view = MapView(context).apply {
                id = R.id.map
                background = ContextCompat.getDrawable(context, R.drawable.rounded_rectangle)
                backgroundTintList = ColorStateList.valueOf(Resources.color(context, R.color.colorSecondary))
                backgroundColorOverride = Resources.color(context, R.color.colorSecondary)
                onMapReady(this)
            }
            mapView = view
            view.view
        },
        update = {
            mapView?.let(onMapReady)
        }
    )
}

@Composable
private fun TriangulateModeSelector(
    shouldCalculateMyLocation: Boolean,
    onModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        FilterChip(
            selected = !shouldCalculateMyLocation,
            onClick = { onModeChanged(false) },
            label = { Text(stringResource(R.string.destination)) },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.size(TrailSenseSpacing.sm))
        FilterChip(
            selected = shouldCalculateMyLocation,
            onClick = { onModeChanged(true) },
            label = { Text(stringResource(R.string.my_location)) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TriangulateLocationCard(
    title: String,
    instructions: String,
    isComplete: Boolean,
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
    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!isExpanded) }
                .padding(TrailSenseSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                tint = if (isComplete) ComposeColor(AppColor.Green.color) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = TrailSenseSpacing.sm),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(if (isExpanded) 180f else 0f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.lg)
                    .padding(bottom = TrailSenseSpacing.lg)
            ) {
                Text(
                    text = instructions,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CoordinateInput(
                    coordinate = coordinate,
                    onCoordinateChange = onCoordinateChange,
                    tag = coordinateTag,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TrailSenseSpacing.xl)
                )
                BearingInput(
                    bearing = bearing,
                    trueNorth = trueNorth,
                    compassBearingText = compassBearingText,
                    hasCompass = hasCompass,
                    tag = bearingTag,
                    onBearingChange = onBearingChange,
                    onCompassBearingClick = onCompassBearingClick,
                    onCameraBearingClick = onCameraBearingClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TrailSenseSpacing.xl)
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

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoadingLocation) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(40.dp)
                    .testTag("gps_loading")
            )
        } else {
            androidx.compose.material3.IconButton(
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
                modifier = Modifier.testTag("gps_btn")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_location),
                    contentDescription = stringResource(R.string.gps)
                )
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        OutlinedTextField(
            value = coordinateText,
            onValueChange = {
                coordinateText = it
                onCoordinateChange(Coordinate.parse(it))
            },
            modifier = Modifier
                .weight(1f)
                .testTag(tag),
            label = { Text(stringResource(R.string.location)) },
            isError = coordinateText.isNotBlank() && parsedCoordinate == null,
            supportingText = {
                if (coordinateText.isNotBlank() && parsedCoordinate == null) {
                    Text(stringResource(R.string.coordinate_input_invalid_location))
                }
            },
            singleLine = true,
            enabled = !isLoadingLocation
        )
        Spacer(modifier = Modifier.size(8.dp))
        androidx.compose.material3.IconButton(
            onClick = { showHelp = true },
            modifier = Modifier.testTag("coordinate_input_help_btn")
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_help),
                contentDescription = stringResource(R.string.location_input_help_title)
            )
        }
    }

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
