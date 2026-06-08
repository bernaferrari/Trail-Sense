package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.flatten
import com.kylecorry.trail_sense.shared.views.Toolbar
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

class NavigationSheetView(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {


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
    private val toolbar: Toolbar
    private val distanceDataView: NavigationDataPointState
    private val bearingDataView: NavigationDataPointState
    private val elevationDataView: NavigationDataPointState
    private val etaDataView: NavigationDataPointState

    init {
        val root = LinearLayout(context).apply {
            id = R.id.navigation_sheet
            background = ColorDrawable(
                Resources.getAndroidColorAttr(
                    context,
                    com.google.android.material.R.attr.colorSurfaceContainer
                )
            )
            orientation = LinearLayout.VERTICAL
        }
        addView(
            root,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        toolbar = Toolbar(context, null).apply {
            id = R.id.navigation_sheet_title
            leftButton.setIconResource(R.drawable.ic_tool_notes)
            rightButton.setIconResource(R.drawable.ic_cancel)
        }
        root.addView(
            toolbar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val dataRow = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        root.addView(
            dataRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = Resources.dp(context, 8f).toInt()
                marginEnd = Resources.dp(context, 8f).toInt()
            }
        )

        distanceDataView = addNavigationDataPoint(dataRow, R.id.navigation_distance, R.drawable.ruler)
        bearingDataView = addNavigationDataPoint(
            dataRow,
            R.id.navigation_bearing,
            R.drawable.ic_compass_icon
        )
        elevationDataView = addNavigationDataPoint(
            dataRow,
            R.id.navigation_elevation,
            R.drawable.ic_altitude
        )
        etaDataView = addNavigationDataPoint(dataRow, R.id.navigation_eta, R.drawable.ic_tool_clock)

        bearingDataView.setShowDescription(false)
        toolbar.leftButton.flatten()
    }

    private fun addNavigationDataPoint(
        row: LinearLayout,
        id: Int,
        icon: Int
    ): NavigationDataPointState {
        val dataPoint = NavigationDataPointState(icon)
        val view = ComposeView(context).apply {
            this.id = id
            dataPoint.view = this
            setContent {
                TrailSenseTheme {
                    dataPoint.Content()
                }
            }
        }
        row.addView(
            view,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = Resources.dp(context, 8f).toInt()
                marginEnd = Resources.dp(context, 8f).toInt()
            }
        )
        return dataPoint
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
        toolbar.title.maxWidth = Resources.dp(context, 250f).toInt()
        toolbar.rightButton.isVisible = isNavigating
        toolbar.rightButton.setOnClickListener {
            requestCancelNavigation()
        }

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
        toolbar.title.text = context.getString(R.string.bearing)
        toolbar.subtitle.isVisible = false
        toolbar.leftButton.isVisible = false
        toolbar.title.setOnClickListener(null)
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
        toolbar.title.text = beacon.name
        toolbar.subtitle.isVisible = true

        val hasComment = !beacon.comment.isNullOrEmpty()
        toolbar.leftButton.isVisible = hasComment

        toolbar.leftButton.setOnClickListener {
            Alerts.dialog(context, beacon.name, beacon.comment, cancelText = null)
        }

        val elevationDistance = beacon.elevation?.let {
            Distance.meters(it).convertTo(prefs.baseDistanceUnits)
        }

        toolbar.subtitle.text = formatter.join(
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

        toolbar.title.setOnClickListener {
            openBeacon(beacon.id)
        }

        toolbar.subtitle.setOnClickListener {
            openBeacon(beacon.id)
        }
    }

    private fun openBeacon(id: Long) {
        findNavController().navigateWithAnimation(
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

private class NavigationDataPointState(
    private val icon: Int
) {
    lateinit var view: View
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    private var showDescriptionState by mutableStateOf(true)

    var isVisible: Boolean
        get() = view.isVisible
        set(value) {
            view.isVisible = value
        }

    fun setShowDescription(show: Boolean) {
        showDescriptionState = show
    }

    @Composable
    fun Content() {
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
}
