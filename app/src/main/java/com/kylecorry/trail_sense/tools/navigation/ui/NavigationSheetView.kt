package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.NavigationSensorValues
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.DataPointContent
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NavigationSheetView(private val context: Context) {


    private val prefs = DependencyRegistry.get<UserPreferences>()
    private val formatter = DependencyRegistry.get<FormatService>()
    private val navigator = DependencyRegistry.get<Navigator>()

    private val useTrueNorth = prefs.compass.useTrueNorth

    private val navigationService = NavigationService()

    private var destination: Destination? = null
    private var isNavigating: Boolean = false
    private var sensorValues: NavigationSensorValues? = null
    private var useTrueNorthOverride: Boolean? = null

    // VIEWS
    private val toolbar: NavigationSheetToolbarState
    private val distanceDataView: NavigationDataPointState
    private val bearingDataView: NavigationDataPointState
    private val elevationDataView: NavigationDataPointState
    private val etaDataView: NavigationDataPointState

    init {
        toolbar = NavigationSheetToolbarState()
        distanceDataView = NavigationDataPointState(R.drawable.ruler)
        bearingDataView = addNavigationDataPoint(
            R.drawable.ic_compass_icon
        )
        elevationDataView = addNavigationDataPoint(
            R.drawable.ic_altitude
        )
        etaDataView = NavigationDataPointState(R.drawable.ic_tool_clock)

        bearingDataView.setShowDescription(false)
    }

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

    private fun addNavigationDataPoint(icon: Int): NavigationDataPointState {
        return NavigationDataPointState(icon)
    }

    @Composable
    private fun Content() {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                toolbar.Content()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    distanceDataView.Content(Modifier.weight(1f))
                    bearingDataView.Content(Modifier.weight(1f))
                    elevationDataView.Content(Modifier.weight(1f))
                    etaDataView.Content(Modifier.weight(1f))
                }
            }
        }
    }


    // TODO: Listen for navigation and automatically hide/show with override ability (for navigation tool)
    fun hide() {
        isVisible = false
        destination = null
        updateNavigation()
    }

    fun show(destination: Destination, isNavigating: Boolean = false) {
        this.destination = destination
        this.isNavigating = isNavigating
        updateNavigation()
    }

    fun setTrueNorthOverride(useTrueNorth: Boolean?) {
        useTrueNorthOverride = useTrueNorth
        updateNavigation()
    }


    // TODO: This should be tracked by a service (for background navigation)
    fun updateNavigationSensorValues(values: NavigationSensorValues) {
        sensorValues = values
        updateNavigation()
    }

    fun updateNavigationSensorValues(
        location: Coordinate,
        elevation: Float,
        speed: Float,
        declination: Float
    ) {
        val values = NavigationSensorValues(
            location,
            null,
            Distance.meters(elevation),
            null,
            Bearing.from(0f),
            declination,
            Speed.from(speed, DistanceUnits.Meters, TimeUnits.Seconds),
            Speed.from(speed, DistanceUnits.Meters, TimeUnits.Seconds)
        )
        updateNavigationSensorValues(values)
    }

    fun requestCancelNavigation() {
        if (!isNavigating) {
            return
        }
        Alerts.dialog(
            context,
            context.getString(R.string.cancel_navigation_question),
            okText = context.getString(R.string.yes),
            cancelText = context.getString(R.string.no)
        ) { cancelled ->
            if (!cancelled) {
                CoroutineScope(Dispatchers.Default).launch {
                    navigator.cancelAllNavigation()
                }
            }
        }
    }

    private fun updateNavigation() {
        val values = sensorValues
        val destination = destination
        if (destination == null || values == null) {
            isVisible = false
            return
        }
        isVisible = true
        toolbar.rightVisible = isNavigating
        toolbar.onRightClick = ::requestCancelNavigation

        if (destination is Destination.Beacon) {
            updateBeaconNavigation(destination, values)
        } else if (destination is Destination.Bearing) {
            updateBearingNavigation(destination, values)
        }
    }

    private fun updateBearingNavigation(
        destination: Destination.Bearing,
        values: NavigationSensorValues
    ) {
        val bearing = navigator.getBearing(values.location, destination)
        updateDestinationDirection(bearing.value, true)
        updateDestinationElevation(null, null)
        etaDataView.isVisible = false
        toolbar.title = context.getString(R.string.bearing)
        toolbar.subtitleVisible = false
        toolbar.leftVisible = false
        toolbar.onTitleClick = null
        toolbar.onSubtitleClick = null
    }

    private fun updateBeaconNavigation(
        destination: Destination.Beacon,
        values: NavigationSensorValues
    ) {
        val beacon = destination.beacon
        val vector = navigationService.navigate(
            values.location,
            values.elevation.meters().value,
            beacon,
            values.declination,
            useTrueNorthOverride ?: useTrueNorth
        )

        updateDestinationDirection(vector.direction.value)
        updateDestinationElevation(beacon.elevation, vector.altitudeChange)
        updateDestinationEta(
            values.location, values.elevation.meters().value, values.speed.convertTo(
                DistanceUnits.Meters, TimeUnits.Seconds
            ).value, beacon
        )

        // TODO: These don't change
        toolbar.title = beacon.name
        toolbar.subtitleVisible = true

        val hasComment = !beacon.comment.isNullOrEmpty()
        toolbar.leftVisible = hasComment

        toolbar.onLeftClick = {
            Alerts.dialog(context, beacon.name, beacon.comment, cancelText = null)
        }

        val elevationDistance = beacon.elevation?.let {
            Distance.meters(it).convertTo(prefs.baseDistanceUnits)
        }

        toolbar.subtitle = formatter.join(
            *listOfNotNull(
                formatter.formatLocation(beacon.coordinate),
                if (elevationDistance != null) context.getString(
                    R.string.elevation_value,
                    formatter.formatDistance(
                        elevationDistance,
                        Units.getDecimalPlaces(elevationDistance.units)
                    )
                ) else null
            ).toTypedArray(),
            separator = FormatService.Separator.NewLine
        )

        toolbar.onTitleClick = {
            openBeacon(beacon.id)
        }

        toolbar.onSubtitleClick = {
            openBeacon(beacon.id)
        }
    }

    private fun openBeacon(id: Long) {
        view.findNavController().navigateWithAnimation(
            R.id.beaconDetailsFragment,
            Bundle().apply {
                putLong("beacon_id", id)
            }
        )
    }

    private fun updateDestinationDirection(azimuth: Float, isTitle: Boolean = false) {
        val value = formatter.formatDegrees(
            azimuth,
            replace360 = true
        ) + " " + formatter.formatDirection(CompassDirection.nearest(azimuth))
        distanceDataView.isVisible = !isTitle
        bearingDataView.isVisible = isTitle
        if (isTitle) {
            bearingDataView.title = value
        } else {
            distanceDataView.description = value
        }
    }

    private fun updateDestinationEta(
        location: Coordinate,
        elevation: Float,
        speed: Float,
        beacon: Beacon
    ) {
        etaDataView.isVisible = true
        val d = Distance.meters(location.distanceTo(beacon.coordinate))
            .convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        distanceDataView.title =
            formatter.formatDistance(d, Units.getDecimalPlaces(d.units), false)

        // ETA
        val eta = navigationService.eta(location, elevation, speed, beacon)
        etaDataView.title = formatter.formatDuration(eta, false)
        etaDataView.description = formatter.formatTime(
            ZonedDateTime.now().plus(eta).toLocalTime(),
            includeSeconds = false
        )
    }

    private fun updateDestinationElevation(destinationElevation: Float?, elevationChange: Float?) {
        val hasElevation = elevationChange != null && destinationElevation != null
        elevationDataView.isVisible = hasElevation
        if (hasElevation) {
            val direction = when {
                elevationChange > 0 -> context.getString(R.string.increase)
                else -> ""
            }

            val elevationChangeDist =
                Distance.meters(elevationChange).convertTo(prefs.baseDistanceUnits)

            elevationDataView.setShowDescription(false)
            elevationDataView.title = context.getString(
                R.string.elevation_diff_format,
                direction,
                formatter.formatDistance(
                    elevationChangeDist, Units.getDecimalPlaces(elevationChangeDist.units),
                    false
                )
            )
        }
    }
}

private class NavigationSheetToolbarState {
    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var leftVisible by mutableStateOf(false)
    var rightVisible by mutableStateOf(false)
    var subtitleVisible by mutableStateOf(true)
    var onLeftClick by mutableStateOf<(() -> Unit)?>(null)
    var onRightClick by mutableStateOf<(() -> Unit)?>(null)
    var onTitleClick by mutableStateOf<(() -> Unit)?>(null)
    var onSubtitleClick by mutableStateOf<(() -> Unit)?>(null)

    @Composable
    fun Content() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leftVisible) {
                IconButton(onClick = { onLeftClick?.invoke() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tool_notes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    modifier = Modifier
                        .widthIn(max = 250.dp)
                        .clickable(enabled = onTitleClick != null) {
                            onTitleClick?.invoke()
                        },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (subtitleVisible && subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable(enabled = onSubtitleClick != null) {
                                onSubtitleClick?.invoke()
                            },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (rightVisible) {
                IconButton(onClick = { onRightClick?.invoke() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_cancel),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

private class NavigationDataPointState(
    private val icon: Int
) {
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    private var showDescriptionState by mutableStateOf(true)

    var isVisible by mutableStateOf(true)

    fun setShowDescription(show: Boolean) {
        showDescriptionState = show
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (isVisible) {
            Box(modifier = modifier) {
                DataPointContent(
                    title = title,
                    description = description,
                    icon = icon,
                    tint = MaterialTheme.colorScheme.onSurface,
                    showDescription = showDescriptionState,
                    gravity = Gravity.CENTER,
                    onDescriptionClick = null
                )
            }
        } else {
            Spacer(modifier = modifier)
        }
    }
}
