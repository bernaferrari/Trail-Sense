@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.triangulate.ui

import com.kylecorry.trail_sense.shared.navigation.Route

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.drawText
import androidx.compose.foundation.layout.offset
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.andromeda.preferences.putOrRemoveCoordinate
import com.kylecorry.andromeda.preferences.putOrRemoveFloat
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.AppUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.from
import com.kylecorry.trail_sense.shared.navigation.TrailSenseAppNavigation
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.camera.SightingCompassDialog
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.gps.GPSLocationSource
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.BearingInput as ComposeBearingInput
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.sol.units.DistanceUnits
import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDialog

@Composable
fun TriangulateToolScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val appNavigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel {
        TriangulateViewModel(
            preferences = PreferencesSubsystem.getInstance(appContext).preferences,
            sensorService = SensorService(appContext)
        )
    }
    val formatService = remember(appContext) { FormatService.getInstance(appContext) }
    val prefs = remember(appContext) { UserPreferences(appContext) }
    val hasCompass = remember(appContext) { SensorService(appContext).hasCompass() }

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }

    // The camera sighting is a rendering object, so it lives in the composition; its bearing follows the compass.
    var cameraBearingIndex by remember { mutableStateOf<Int?>(null) }
    val cameraSheet = remember(context, lifecycleOwner) {
        SightingCompassDialog(context, lifecycleOwner, lifecycleOwner.lifecycleScope) { bearing ->
            cameraBearingIndex?.let { viewModel.updateBearing(it, bearing, false) }
        }
    }
    DisposableEffect(cameraSheet) {
        onDispose { cameraSheet.dismiss() }
    }
    val compassBearing = viewModel.compassBearing
    LaunchedEffect(compassBearing) {
        if (compassBearing != null) {
            cameraSheet.bearing = compassBearing
        }
    }

    val shouldCalculateMyLocation = viewModel.shouldCalculateMyLocation
    val location = viewModel.result
    val hasLocation = location != null && !location.latitude.isNaN() && !location.longitude.isNaN()
    val recommendedMinDistance = remember(prefs) {
        if (prefs.distanceUnits == UserPreferences.DistanceUnits.Feet) {
            Distance.feet(100f)
        } else {
            Distance.meters(30f)
        }
    }

    fun locationTitle(index: Int): String {
        val distance = viewModel.distanceToResult(index, prefs.baseDistanceUnits)
        val formattedDistance = distance?.let {
            formatService.formatDistance(it, Units.getDecimalPlaces(it.units))
        }
        return buildString {
            append(context.getString(R.string.location_number, index))
            if (distance != null) {
                appendLine()
                append(
                    if (shouldCalculateMyLocation) {
                        context.getString(R.string.distance_away_from_self, formattedDistance)
                    } else {
                        context.getString(R.string.distance_away_from_destination, formattedDistance)
                    }
                )
            }
        }
    }

    TriangulateScreen(
        title = if (hasLocation) {
            formatService.formatLocation(location!!)
        } else {
            stringResource(R.string.could_not_triangulate)
        },
        actionsVisible = hasLocation,
        navigateVisible = !shouldCalculateMyLocation,
        updateGpsOverrideVisible = prefs.gps.locationSource == GPSLocationSource.Manual && shouldCalculateMyLocation,
        shouldCalculateMyLocation = shouldCalculateMyLocation,
        location1Title = locationTitle(1),
        location2Title = locationTitle(2),
        location1Instructions = if (shouldCalculateMyLocation) {
            stringResource(R.string.triangulate_self_location_1_instructions)
        } else {
            stringResource(R.string.triangulate_destination_location_1_instructions)
        },
        location2Instructions = if (shouldCalculateMyLocation) {
            stringResource(R.string.triangulate_self_location_2_instructions)
        } else {
            stringResource(
                R.string.triangulate_destination_location_2_instructions,
                formatService.formatDistance(
                    recommendedMinDistance,
                    Units.getDecimalPlaces(recommendedMinDistance.units)
                )
            )
        },
        location1Complete = viewModel.isComplete(1),
        location2Complete = viewModel.isComplete(2),
        location1 = viewModel.location1,
        location2 = viewModel.location2,
        bearing1 = viewModel.bearing1,
        bearing2 = viewModel.bearing2,
        bearing1TrueNorth = viewModel.bearing1TrueNorth,
        bearing2TrueNorth = viewModel.bearing2TrueNorth,
        compassBearingText = compassBearing?.let { formatService.formatDegrees(it, replace360 = true) } ?: "",
        compassBearing = compassBearing,
        result = location,
        hasCompass = hasCompass,
        onModeChanged = viewModel::changeMode,
        onCreateBeacon = {
            viewModel.result?.let { AppUtils.placeBeacon(context, GeoUri(it)) }
        },
        onNavigate = {
            viewModel.result?.let {
                Navigator.getInstance(context).navigateTo(
                    it,
                    context.getString(R.string.location),
                    BeaconOwner.Triangulate
                )
                TrailSenseAppNavigation(appNavigator).navigate(Route.Navigation())
            }
        },
        onShareLocation = {
            viewModel.result?.let {
                scope.launch { shareLocation(context, appNavigator, effects, it) }
            }
        },
        onUpdateGpsOverride = {
            viewModel.result?.let { coord ->
                prefs.gps.locationOverride = coord
                Alerts.toast(context, context.getString(R.string.location_override_updated))
            }
        },
        onLocation1Change = { viewModel.updateLocation(1, it) },
        onLocation2Change = { viewModel.updateLocation(2, it) },
        onBearing1Change = { bearing, trueNorth -> viewModel.updateBearing(1, bearing, trueNorth) },
        onBearing2Change = { bearing, trueNorth -> viewModel.updateBearing(2, bearing, trueNorth) },
        onCompassBearing1Click = { viewModel.setBearingFromCompass(1) },
        onCompassBearing2Click = { viewModel.setBearingFromCompass(2) },
        onCameraBearing1Click = {
            scope.launch {
                if (effects.requestPermission(AppPermission.Camera)) {
                    cameraBearingIndex = 1
                    cameraSheet.show()
                } else {
                    effects.explainPermissionDenied(AppPermission.Camera)
                }
            }
        },
        onCameraBearing2Click = {
            scope.launch {
                if (effects.requestPermission(AppPermission.Camera)) {
                    cameraBearingIndex = 2
                    cameraSheet.show()
                } else {
                    effects.explainPermissionDenied(AppPermission.Camera)
                }
            }
        },
        onReset = viewModel::reset,
        modifier = modifier.fillMaxSize()
    )
    cameraSheet.Content()
}

/**
 * Triangulation's state: the mode (my location or a destination), the two sightings (where and which way), the
 * live compass bearing while the page is resumed, and the answer. The sightings are kept in preferences when the
 * page pauses, so they survive leaving the tool.
 */
internal class TriangulateViewModel(
    private val preferences: IPreferences,
    sensorService: SensorService
) : ViewModel() {

    private val compass = sensorService.getCompass()

    var shouldCalculateMyLocation by mutableStateOf(false)
        private set
    var location1 by mutableStateOf<Coordinate?>(null)
        private set
    var location2 by mutableStateOf<Coordinate?>(null)
        private set
    var bearing1 by mutableStateOf<Float?>(null)
        private set
    var bearing2 by mutableStateOf<Float?>(null)
        private set
    var bearing1TrueNorth by mutableStateOf(false)
        private set
    var bearing2TrueNorth by mutableStateOf(false)
        private set
    var compassBearing by mutableStateOf<Float?>(null)
        private set
    var result by mutableStateOf<Coordinate?>(null)
        private set

    private val onCompass: () -> Boolean = {
        compassBearing = compass.rawBearing
        true
    }

    init {
        restoreState()
    }

    fun onResume() {
        compass.start(onCompass)
        restoreState()
    }

    fun onPause() {
        compass.stop(onCompass)
        saveState()
    }

    override fun onCleared() {
        compass.stop(onCompass)
    }

    fun changeMode(calculateMyLocation: Boolean) {
        shouldCalculateMyLocation = calculateMyLocation
        update()
    }

    fun updateLocation(index: Int, coordinate: Coordinate?) {
        if (index == 1) location1 = coordinate else location2 = coordinate
        update()
    }

    fun updateBearing(index: Int, bearing: Float?, trueNorth: Boolean) {
        if (index == 1) {
            bearing1 = bearing
            bearing1TrueNorth = trueNorth
        } else {
            bearing2 = bearing
            bearing2TrueNorth = trueNorth
        }
        update()
    }

    fun setBearingFromCompass(index: Int) {
        updateBearing(index, compass.rawBearing, false)
    }

    fun isComplete(index: Int): Boolean {
        val location = if (index == 1) location1 else location2
        val bearing = if (index == 1) bearing1 else bearing2
        return location != null && bearing != null
    }

    /** How far sighting [index] was taken from the answer, in [units] (or a more readable relative unit). */
    fun distanceToResult(index: Int, units: DistanceUnits): Distance? {
        val start = (if (index == 1) location1 else location2) ?: return null
        val destination = result ?: return null
        return Distance.meters(start.distanceTo(destination)).convertTo(units).toRelativeDistance()
    }

    fun reset() {
        location1 = null
        location2 = null
        bearing1 = null
        bearing2 = null
        bearing1TrueNorth = false
        bearing2TrueNorth = false

        preferences.remove("state_triangulate_self")
        preferences.remove("state_triangulate_bearing1")
        preferences.remove("state_triangulate_bearing2")
        preferences.remove("state_triangulate_location1")
        preferences.remove("state_triangulate_location2")
        preferences.remove("state_triangulate_true_north1")
        preferences.remove("state_triangulate_true_north2")

        update()
    }

    private fun update() {
        val location1 = location1
        val location2 = location2
        val direction1 = bearing1
        val direction2 = bearing2

        if (location1 == null || location2 == null || direction1 == null || direction2 == null) {
            result = null
            return
        }

        val declination1 =
            if (bearing1TrueNorth) 0f else Geophysics.getGeomagneticDeclination(location1)
        val declination2 =
            if (bearing2TrueNorth) 0f else Geophysics.getGeomagneticDeclination(location2)
        val bearing1 = Bearing.from(direction1).withDeclination(declination1)
        val bearing2 = Bearing.from(direction2).withDeclination(declination2)

        result = if (shouldCalculateMyLocation) {
            Geography.triangulateSelf(location1, bearing1, location2, bearing2)
        } else {
            Geography.triangulateDestination(location1, bearing1, location2, bearing2)
        }
    }

    private fun saveState() {
        preferences.putBoolean("state_triangulate_self", shouldCalculateMyLocation)
        preferences.putOrRemoveFloat("state_triangulate_bearing1", bearing1)
        preferences.putOrRemoveFloat("state_triangulate_bearing2", bearing2)
        preferences.putOrRemoveCoordinate("state_triangulate_location1", location1)
        preferences.putOrRemoveCoordinate("state_triangulate_location2", location2)
        preferences.putBoolean("state_triangulate_true_north1", bearing1TrueNorth)
        preferences.putBoolean("state_triangulate_true_north2", bearing2TrueNorth)
    }

    private fun restoreState() {
        shouldCalculateMyLocation = preferences.getBoolean("state_triangulate_self") ?: false
        bearing1 = preferences.getFloat("state_triangulate_bearing1")
        bearing2 = preferences.getFloat("state_triangulate_bearing2")
        bearing1TrueNorth = preferences.getBoolean("state_triangulate_true_north1") ?: false
        bearing2TrueNorth = preferences.getBoolean("state_triangulate_true_north2") ?: false
        location1 = preferences.getCoordinate("state_triangulate_location1")
        location2 = preferences.getCoordinate("state_triangulate_location2")
        update()
    }
}

/**
 * Triangulate: what is being found on top (a destination, or where I am), then the object: a plot of the sightings
 * whose lines cross as you take them. Under it, the answer (until there is one, the next step to take, in one line),
 * and the two sightings as two plain groups: where you stood, which way you looked, and the ways to take that bearing.
 * Reset lives in the top bar so it can't be hit by accident.
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
    compassBearing: Float?,
    result: Coordinate?,
    hasCompass: Boolean,
    onModeChanged: (Boolean) -> Unit,
    onCreateBeacon: () -> Unit,
    onNavigate: () -> Unit,
    onShareLocation: () -> Unit,
    onUpdateGpsOverride: () -> Unit,
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
    // Where do my two sightings meet? The plot shows the lines crossing, the answer under it says it, and the two
    // sightings below are the steps that draw them.
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

        item(key = "map") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SightingPlot(
                    location1 = location1,
                    location2 = location2,
                    bearing1 = bearing1,
                    bearing2 = bearing2,
                    trueNorth1 = bearing1TrueNorth,
                    trueNorth2 = bearing2TrueNorth,
                    result = result.takeIf { actionsVisible },
                    shouldCalculateMyLocation = shouldCalculateMyLocation,
                    liveBearing = compassBearing.takeIf { hasCompass },
                    activeSighting = activeSighting,
                    completed = completed,
                    modifier = Modifier.testTag("map")
                )
            }
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
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
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
                textAlign = TextAlign.Center,
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
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = scheme.onSurface
            )
            Text(
                // Both in but no meeting point: the line says so ("Could not triangulate").
                text = if (completed == 2) title else nextInstructions,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = scheme.onSurfaceVariant
            )
        }
    }
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
            compassBearing = 120f,
            result = null,
            hasCompass = true,
            onModeChanged = {},
            onCreateBeacon = {},
            onNavigate = {},
            onShareLocation = {},
            onUpdateGpsOverride = {},
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

/**
 * The screen's object, drawn like the boil timer's pot: a round plot of the ground, north up, inside a thick wavy
 * ring that fills with each sighting taken. Each sighting is a numbered point with its line drawn out along its
 * bearing; while one is being taken, a dashed line follows the compass from where you stand. Once the two lines
 * cross, the answer springs in where they meet as a cookie.
 */
@Composable
private fun SightingPlot(
    location1: Coordinate?,
    location2: Coordinate?,
    bearing1: Float?,
    bearing2: Float?,
    trueNorth1: Boolean,
    trueNorth2: Boolean,
    result: Coordinate?,
    shouldCalculateMyLocation: Boolean,
    liveBearing: Float?,
    activeSighting: Int,
    completed: Int,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val density = androidx.compose.ui.platform.LocalDensity.current
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    val north = stringResource(R.string.direction_north)

    val progress by animateFloatAsState(
        targetValue = completed / 2f,
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "sightingsTaken"
    )
    // Each line draws out along its bearing once it is taken.
    val reach1 by animateFloatAsState(
        targetValue = if (bearing1 != null) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "sighting1Reach"
    )
    val reach2 by animateFloatAsState(
        targetValue = if (bearing2 != null) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "sighting2Reach"
    )
    val solved = result != null && !result.latitude.isNaN() && !result.longitude.isNaN()
    val answerScale by animateFloatAsState(
        targetValue = if (solved) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "answerScale"
    )

    val plot = remember(location1, location2, result, solved) {
        SightingProjection(listOfNotNull(location1, location2, result?.takeIf { solved }))
    }
    // The line runs the way the bearing points from the sighting; finding yourself, you stand back along it.
    fun lineBearing(point: Coordinate?, bearing: Float, trueNorth: Boolean): Float {
        val declination = if (trueNorth || point == null) 0f else Geophysics.getGeomagneticDeclination(point)
        val corrected = Bearing.from(bearing).withDeclination(declination)
        return (if (shouldCalculateMyLocation) corrected.inverse() else corrected).value
    }
    val line1 = bearing1?.let { lineBearing(location1, it, trueNorth1) }
    val line2 = bearing2?.let { lineBearing(location2, it, trueNorth2) }
    val livePoint = if (activeSighting == 2) location2 else location1
    val live = liveBearing?.takeIf {
        activeSighting != 0 && (if (activeSighting == 2) bearing2 else bearing1) == null
    }?.let { lineBearing(livePoint, it, false) }

    val ring = scheme.primary
    val ink = scheme.onSurface
    val faint = scheme.outlineVariant
    val face = scheme.surfaceContainerLow
    val stroke = with(density) { 12.dp.toPx() }

    Box(
        modifier = modifier
            .padding(vertical = TrailSenseSpacing.sm)
            .size(264.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.CircularWavyProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = ring,
            trackColor = scheme.surfaceVariant,
            stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            trackStroke = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            wavelength = 36.dp
        )
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .clip(CircleShape)
                .background(face)
        ) {
            val radiusPx = with(density) { maxWidth.toPx() } / 2f
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                fun screen(p: Offset) = Offset(center.x + p.x * radiusPx, center.y - p.y * radiusPx)
                fun direction(bearing: Float): Offset {
                    val rad = Math.toRadians(bearing.toDouble())
                    return Offset(kotlin.math.sin(rad).toFloat(), kotlin.math.cos(rad).toFloat())
                }

                // The ground: a faint ring and cross, and north at the top.
                drawCircle(faint, radius = radiusPx * 0.62f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                drawLine(faint, Offset(center.x, 0f), Offset(center.x, size.height), 1.dp.toPx())
                drawLine(faint, Offset(0f, center.y), Offset(size.width, center.y), 1.dp.toPx())

                fun start(point: Coordinate?) = screen(point?.let { plot.project(it) } ?: Offset.Zero)

                // The compass, live, from where the sighting is being taken.
                if (live != null) {
                    val from = start(livePoint)
                    val dir = direction(live)
                    drawLine(
                        ring.copy(alpha = 0.6f),
                        from,
                        Offset(from.x + dir.x * size.width * 1.2f, from.y - dir.y * size.width * 1.2f),
                        strokeWidth = 3.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(8.dp.toPx(), 8.dp.toPx())
                        )
                    )
                }

                // The sightings taken: solid lines out along their bearings.
                listOf(
                    Triple(location1, line1, reach1),
                    Triple(location2, line2, reach2)
                ).forEach { (point, bearing, reach) ->
                    if (bearing == null || reach <= 0f) return@forEach
                    val from = start(point)
                    val dir = direction(bearing)
                    val length = size.width * 1.2f * reach
                    drawLine(
                        ring,
                        from,
                        Offset(from.x + dir.x * length, from.y - dir.y * length),
                        strokeWidth = 4.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }

                // Where each sighting was taken: a numbered point.
                listOf(location1 to "1", location2 to "2").forEach { (point, number) ->
                    point ?: return@forEach
                    val at = screen(plot.project(point))
                    drawCircle(face, radius = 10.dp.toPx(), center = at)
                    drawCircle(ink, radius = 7.dp.toPx(), center = at)
                    val label = measurer.measure(number, labelStyle.copy(color = ink))
                    drawText(label, topLeft = Offset(at.x + 10.dp.toPx(), at.y - label.size.height - 4.dp.toPx()))
                }

                // North last, on a small disc of the face, so a sighting line pointing north passes under it
                // instead of striking through the letter.
                val northLabel = measurer.measure(north, labelStyle.copy(color = scheme.onSurfaceVariant))
                val northCenter = Offset(center.x, 10.dp.toPx() + northLabel.size.height / 2f)
                drawCircle(face, radius = maxOf(northLabel.size.width, northLabel.size.height) / 2f + 3.dp.toPx(), center = northCenter)
                drawText(
                    northLabel,
                    topLeft = Offset(center.x - northLabel.size.width / 2f, 10.dp.toPx())
                )
            }

            // The answer, where the lines meet.
            if (solved && result != null) {
                val at = plot.project(result)
                val markerSize = 36.dp
                Surface(
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    color = scheme.primary,
                    contentColor = scheme.onPrimary,
                    modifier = Modifier
                        .offset {
                            androidx.compose.ui.unit.IntOffset(
                                (radiusPx + at.x * radiusPx - markerSize.toPx() / 2f).toInt(),
                                (radiusPx - at.y * radiusPx - markerSize.toPx() / 2f).toInt()
                            )
                        }
                        .size(markerSize)
                        .graphicsLayer {
                            scaleX = answerScale
                            scaleY = answerScale
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (shouldCalculateMyLocation) TrailIcons.Location else TrailIcons.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Lays the points out flat around their middle, north up, scaled so the farthest sits at 62% of the radius (the faint
 * ring). One point alone sits at the centre.
 */
private class SightingProjection(points: List<Coordinate>) {
    private val originLatitude = points.map { it.latitude }.average().takeIf { !it.isNaN() } ?: 0.0
    private val originLongitude = points.map { it.longitude }.average().takeIf { !it.isNaN() } ?: 0.0
    private val metersPerDegreeLongitude = 111_320.0 * kotlin.math.cos(Math.toRadians(originLatitude))
    private val scale: Float

    init {
        val extent = points.maxOfOrNull { local(it).getDistance() } ?: 0f
        scale = 0.62f / extent.coerceAtLeast(25f)
    }

    private fun local(point: Coordinate): Offset = Offset(
        ((point.longitude - originLongitude) * metersPerDegreeLongitude).toFloat(),
        ((point.latitude - originLatitude) * 110_540.0).toFloat()
    )

    /** The point in the plot's units: -1..1 across the face, y up. */
    fun project(point: Coordinate): Offset {
        val p = local(point)
        return Offset(p.x * scale, p.y * scale)
    }
}
