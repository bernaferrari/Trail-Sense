package com.kylecorry.trail_sense.tools.navigation.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.*
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
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
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.sensors.NavigationSensorValues
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.domain.guidance.Guidance
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.math.absoluteValue

class NavigationSheetView(private val context: Context) {

    private val prefs = DependencyRegistry.get<UserPreferences>()
    private val formatter = DependencyRegistry.get<FormatService>()
    private val navigator = DependencyRegistry.get<Navigator>()
    private val hasCompass = DependencyRegistry.get<SensorService>().hasCompass()
    private val navigationService = NavigationService()
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private var destination: Destination? = null
    private var isNavigating: Boolean = false
    private var sensorValues: NavigationSensorValues? = null
    private var useTrueNorthOverride: Boolean? = null
    private var sheetVisible by mutableStateOf(false)
    private var model by mutableStateOf(SheetModel())
    private var titleAction: (() -> Unit)? = null

    var onOpenBeacon: ((Long) -> Unit)? = null
    var onOpenPath: ((Long) -> Unit)? = null

    init {
        scope.launch {
            navigator.isRestoringNavigation.collect {
                updateNavigation()
            }
        }
    }

    var isVisible: Boolean
        get() = sheetVisible
        set(value) {
            sheetVisible = value
        }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!sheetVisible) {
            return
        }
        val state = model
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.titleIcon != null) {
                        Icon(
                            painter = rememberVectorPainter(state.titleIcon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(24.dp)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .clickable(enabled = titleAction != null) { titleAction?.invoke() },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (!state.subtitle.isNullOrEmpty()) {
                            Text(
                                text = state.subtitle,
                                modifier = Modifier.padding(top = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    if (state.showDirection) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = rememberVectorPainter(state.arrowIcon),
                                contentDescription = null,
                                tint = state.arrowColor,
                                modifier = Modifier
                                    .size(28.dp)
                                    .rotate(state.arrowRotation)
                            )
                            Text(
                                text = state.bearingText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    if (state.showCancel) {
                        IconButton(onClick = ::requestCancelNavigation) {
                            Icon(
                                imageVector = TrailIcons.Cancel,
                                contentDescription = stringResource(R.string.stop),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                if (state.showProgress) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                if (state.stats.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        state.stats.forEach { stat ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (stat.valueIcon != null) {
                                        Icon(
                                            painter = rememberVectorPainter(stat.valueIcon),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .padding(end = 4.dp)
                                                .size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = stat.value,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (stat.labelIcon != null) {
                                        Icon(
                                            painter = rememberVectorPainter(stat.labelIcon),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .padding(end = 4.dp)
                                                .size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = stat.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

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

    fun updateNavigationSensorValues(values: NavigationSensorValues) {
        sensorValues = values
        updateNavigation()
    }

    fun updateNavigationSensorValues(
        location: Coordinate,
        elevation: Float,
        speed: Float,
        declination: Float,
        bearing: Float = 0f
    ) {
        updateNavigationSensorValues(
            NavigationSensorValues(
                location,
                null,
                Distance.meters(elevation),
                null,
                Bearing.from(bearing),
                declination,
                Speed.from(speed, DistanceUnits.Meters, TimeUnits.Seconds),
                Speed.from(speed, DistanceUnits.Meters, TimeUnits.Seconds)
            )
        )
    }

    fun requestCancelNavigation() {
        if (!isNavigating) {
            return
        }
        scope.launch {
            val restore = navigator.cancelAllNavigation()
            val content = findActivity()?.findViewById<View>(android.R.id.content) ?: return@launch
            Snackbar.make(
                content,
                context.getString(R.string.navigation_cancelled),
                Snackbar.LENGTH_LONG
            ).setAction(context.getString(R.string.undo)) {
                CoroutineScope(Dispatchers.Default).launch { restore() }
            }.show()
        }
    }

    private fun updateNavigation() {
        if (navigator.isRestoringNavigation.value) {
            showLoading()
            return
        }
        val values = sensorValues
        val destination = destination
        if (destination == null || values == null) {
            sheetVisible = false
            titleAction = null
            return
        }
        sheetVisible = true
        val guidance = navigator.getGuidance(
            destination,
            values.location,
            values.elevation,
            values.declination,
            useTrueNorthOverride
        )
        val direction = directionState(guidance, values)
        when (destination) {
            is Destination.Beacon -> updateBeaconNavigation(destination, guidance, values, direction)
            is Destination.Bearing -> updateBearingNavigation(guidance, values, direction)
            is Destination.Path -> updatePathNavigation(destination, guidance, values, direction)
        }
    }

    private fun showLoading() {
        sheetVisible = true
        titleAction = null
        model = SheetModel(title = context.getString(R.string.loading))
    }

    private fun updateBeaconNavigation(
        destination: Destination.Beacon,
        guidance: Guidance,
        values: NavigationSensorValues,
        direction: DirectionState
    ) {
        val beacon = destination.beacon
        titleAction = { onOpenBeacon?.invoke(beacon.id) }
        val stats = mutableListOf<SheetStat>()
        guidance.remainingDistance?.let {
            stats += SheetStat(formatRelativeDistance(it), context.getString(R.string.navigation_remaining))
        }
        etaStat(guidance, values)?.let { stats += it }
        val gain = guidance.remainingElevationGain
        val loss = guidance.remainingElevationLoss
        when {
            gain != null -> stats += SheetStat(
                formatElevationChange(gain),
                context.getString(R.string.navigation_remaining_gain),
                valueIcon = TrailIcons.TriangleUp
            )

            loss != null -> stats += SheetStat(
                formatElevationChange(loss),
                context.getString(R.string.navigation_remaining_loss),
                valueIcon = TrailIcons.TriangleDown
            )
        }
        model = direction.asModel(
            title = beacon.name,
            titleIcon = TrailIcons.Location,
            titleDescription = titled(R.string.beacon, beacon.name),
            subtitle = formatter.join(
                formatter.formatLocation(beacon.coordinate),
                beacon.elevation?.let {
                    context.getString(
                        R.string.navigation_elevation_format,
                        formatter.formatElevation(Distance.meters(it))
                    ).replace(' ', NON_BREAKING_SPACE)
                },
                separator = FormatService.Separator.Space
            ),
            showCancel = isNavigating,
            stats = stats
        )
    }

    private fun updateBearingNavigation(
        guidance: Guidance,
        values: NavigationSensorValues,
        direction: DirectionState
    ) {
        titleAction = null
        val stats = mutableListOf<SheetStat>()
        guidance.traveledDistance?.let {
            stats += SheetStat(formatRelativeDistance(it), context.getString(R.string.navigation_traveled))
        }
        if (hasCompass) {
            stats += SheetStat(
                formatBearing(values.bearing.value),
                context.getString(R.string.navigation_facing)
            )
        }
        val title = formatBearing(guidance.targetBearing.value)
        model = direction.asModel(
            title = title,
            titleIcon = TrailIcons.CompassIcon,
            titleDescription = titled(R.string.bearing, title),
            showCancel = isNavigating,
            stats = stats
        )
    }

    private fun updatePathNavigation(
        destination: Destination.Path,
        guidance: Guidance,
        values: NavigationSensorValues,
        direction: DirectionState
    ) {
        val name = destination.path.name ?: context.getString(R.string.path)
        titleAction = { onOpenPath?.invoke(destination.path.id) }
        val stats = mutableListOf<SheetStat>()
        guidance.remainingDistance?.let {
            stats += SheetStat(formatRelativeDistance(it), context.getString(R.string.navigation_remaining))
        }
        etaStat(guidance, values)?.let { stats += it }
        val gain = guidance.remainingElevationGain ?: Distance.meters(0f)
        val loss = guidance.remainingElevationLoss ?: Distance.meters(0f)
        val gainText = formatElevationChange(gain)
        val lossText = formatElevationChange(loss)
        if (gain.value > 0 || loss.value > 0) {
            stats += SheetStat(
                gainText,
                lossText,
                valueIcon = TrailIcons.TriangleUp,
                labelIcon = TrailIcons.TriangleDown
            )
        }
        model = direction.asModel(
            title = name,
            titleIcon = TrailIcons.ToolBacktrack,
            titleDescription = titled(R.string.path, name),
            showCancel = isNavigating,
            showProgress = true,
            progress = (guidance.progress ?: 0f).coerceIn(0f, 1f),
            stats = stats
        )
    }

    private fun etaStat(guidance: Guidance, values: NavigationSensorValues): SheetStat? {
        val duration = navigationService.eta(guidance, values.speed) ?: return null
        val arrival = formatter.formatTime(
            ZonedDateTime.now().plus(duration).toLocalTime(),
            includeSeconds = false
        )
        return SheetStat(
            formatter.formatDuration(duration, duration.toHours() >= LONG_DURATION_HOURS),
            context.getString(R.string.navigation_arrive_format, arrival)
        )
    }

    private fun directionState(guidance: Guidance, values: NavigationSensorValues): DirectionState {
        val bearingText = formatBearing(guidance.targetBearing.value)
        if (!hasCompass) {
            return DirectionState(
                arrowIcon = TrailIcons.CompassUnavailable,
                arrowRotation = 0f,
                arrowColor = Color(Resources.androidTextColorSecondary(context)),
                bearingText = bearingText
            )
        }
        val turn = deltaAngle(values.bearing.value, guidance.targetBearing.value)
        return DirectionState(
            arrowIcon = TrailIcons.NavigationArrow,
            arrowRotation = turn,
            arrowColor = Color(directionColor(turn.absoluteValue)),
            bearingText = bearingText
        )
    }

    private fun directionColor(offset: Float): Int {
        val fraction = ((offset - ON_TARGET_DEGREES) / (WRONG_DIRECTION_DEGREES - ON_TARGET_DEGREES))
            .coerceIn(0f, 1f)
        return ColorUtils.blendARGB(AppColor.Green.color, AppColor.Red.color, fraction)
    }

    private fun formatBearing(degrees: Float): String {
        return formatter.formatDegrees(degrees, replace360 = true) + " " +
                formatter.formatDirection(CompassDirection.nearest(degrees))
    }

    private fun formatRelativeDistance(distance: Distance): String {
        val relative = distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        val decimalPlaces = if (relative.value >= LARGE_DISTANCE_THRESHOLD) {
            0
        } else {
            Units.getDecimalPlaces(relative.units)
        }
        return formatter.formatDistance(relative, decimalPlaces, false)
    }

    private fun formatElevationChange(distance: Distance): String {
        val converted = Distance.from(
            distance.convertTo(prefs.baseDistanceUnits).value.absoluteValue,
            prefs.baseDistanceUnits
        )
        return formatter.formatDistance(converted, Units.getDecimalPlaces(converted.units), false)
    }

    private fun titled(@StringRes typeRes: Int, text: String): String {
        return "${context.getString(typeRes)}: $text"
    }

    private fun findActivity(): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) {
                return current
            }
            current = current.baseContext
        }
        return null
    }

    private data class SheetStat(
        val value: String,
        val label: String,
        val valueIcon: ImageVector? = null,
        val labelIcon: ImageVector? = null
    )

    private data class DirectionState(
        val arrowIcon: ImageVector,
        val arrowRotation: Float,
        val arrowColor: Color,
        val bearingText: String
    ) {
        fun asModel(
            title: String,
            titleIcon: ImageVector,
            titleDescription: String,
            subtitle: String? = null,
            showCancel: Boolean,
            showProgress: Boolean = false,
            progress: Float = 0f,
            stats: List<SheetStat>
        ): SheetModel {
            return SheetModel(
                title = title,
                titleIcon = titleIcon,
                titleDescription = titleDescription,
                subtitle = subtitle,
                showCancel = showCancel,
                showDirection = true,
                arrowIcon = arrowIcon,
                arrowRotation = arrowRotation,
                arrowColor = arrowColor,
                bearingText = bearingText,
                showProgress = showProgress,
                progress = progress,
                stats = stats
            )
        }
    }

    private data class SheetModel(
        val title: String = "",
        val titleIcon: ImageVector? = null,
        val titleDescription: String? = null,
        val subtitle: String? = null,
        val showCancel: Boolean = false,
        val showDirection: Boolean = false,
        val arrowIcon: ImageVector = TrailIcons.NavigationArrow,
        val arrowRotation: Float = 0f,
        val arrowColor: Color = Color.Unspecified,
        val bearingText: String = "",
        val showProgress: Boolean = false,
        val progress: Float = 0f,
        val stats: List<SheetStat> = emptyList()
    )

    companion object {
        private const val ON_TARGET_DEGREES = 5f
        private const val WRONG_DIRECTION_DEGREES = 45f
        private const val LARGE_DISTANCE_THRESHOLD = 100f
        private const val LONG_DURATION_HOURS = 100L
        private const val NON_BREAKING_SPACE = ' '
    }
}
