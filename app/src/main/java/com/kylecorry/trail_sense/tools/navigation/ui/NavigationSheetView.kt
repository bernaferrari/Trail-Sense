package com.kylecorry.trail_sense.tools.navigation.ui

import com.kylecorry.trail_sense.tools.beacons.ui.polygon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGridCellShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors
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

    /**
     * The destination card: a direction badge whose arrow turns toward the destination (it morphs from Cookie9 to
     * Cookie12 with one haptic tick the moment you face it), the name with its bearing, the distance as the card's
     * one number, and arrival and climb as one quiet line. Each value appears once.
     */
    @OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun Content(modifier: Modifier = Modifier) {
        AnimatedVisibility(
            visible = sheetVisible,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            SheetCard(model, modifier)
        }
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun SheetCard(state: SheetModel, modifier: Modifier) {
        val status = trailSenseStatusColors()
        val scheme = MaterialTheme.colorScheme
        val off = state.offCourse
        val badgeContainer by animateColorAsState(
            when (off) {
                null -> scheme.primary
                else -> lerp(status.good, scheme.tertiary, off)
            },
            label = "badgeContainer"
        )
        val badgeContent by animateColorAsState(
            when (off) {
                null -> scheme.onPrimary
                else -> lerp(status.goodContainer, scheme.onTertiary, off)
            },
            label = "badgeContent"
        )
        // Turn the short way round, so the arrow never spins a full circle when the bearing wraps past 180°.
        var target by remember { mutableFloatStateOf(state.arrowRotation) }
        target += ((state.arrowRotation - target + 540f) % 360f) - 180f
        // A needle that settles: a soft spring with a touch of overshoot as you turn.
        val rotation by animateFloatAsState(
            target,
            androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            ),
            label = "arrow"
        )
        // A soft haptic tick the moment you face the destination.
        val facing = off != null && off < 0.08f
        val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
        androidx.compose.runtime.LaunchedEffect(facing) {
            if (facing) {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.SegmentTick)
            }
        }
        val badgeScale by animateFloatAsState(
            if (facing) 1.08f else 1f,
            MaterialTheme.motionScheme.fastSpatialSpec(),
            label = "facing"
        )

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            // A calm surface: the compass above is the focal point, this card is what you are walking to.
            shape = RoundedCornerShape(28.dp),
            color = scheme.surfaceContainerHigh,
            contentColor = scheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.showDirection) {
                        Surface(
                            shape = (if (facing) MaterialShapes.Cookie12Sided else (state.badgeShape?.polygon() ?: MaterialShapes.Cookie9Sided)).toShape(),
                            color = badgeContainer,
                            contentColor = badgeContent,
                            modifier = Modifier
                                .size(56.dp)
                                .graphicsLayer {
                                    scaleX = badgeScale
                                    scaleY = badgeScale
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = state.arrowIcon,
                                    contentDescription = state.bearingText,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .rotate(rotation)
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .clickable(enabled = titleAction != null) { titleAction?.invoke() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // No leading pin: the arrow badge already says "this is where you're going".
                            Text(
                                text = state.title,
                                modifier = Modifier.semantics {
                                    state.titleDescription?.let { contentDescription = it }
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = scheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // The bearing, unless the destination's name already is the bearing.
                        if (state.showDirection && state.showBearing && state.bearingText.isNotEmpty()) {
                            Text(
                                text = state.bearingText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                color = scheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    // The distance: the one number on the card.
                    state.distance?.let { distance ->
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(end = if (state.showCancel) 4.dp else 0.dp)
                        ) {
                            Text(
                                text = distance.value,
                                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = distance.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    if (state.showCancel) {
                        FilledTonalIconButton(
                            onClick = ::requestCancelNavigation,
                            // Top-aligned with the name, like a sheet's close, rather than floating mid-card.
                            modifier = Modifier.align(Alignment.Top),
                            colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = scheme.surfaceContainerHighest,
                                contentColor = scheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.stop)
                            )
                        }
                    }
                }
                if (state.showProgress) {
                    LinearWavyProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Everything else (arrival, climb) is one quiet line, not a second set of big readings.
                if (state.details.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(start = if (state.showDirection) 68.dp else 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        state.details.forEachIndexed { index, detail ->
                            if (index > 0) {
                                Text(
                                    text = "  ·  ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                            if (detail.icon != null) {
                                Icon(
                                    imageVector = detail.icon,
                                    contentDescription = detail.iconDescription,
                                    tint = scheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .size(12.dp)
                                )
                            }
                            Text(
                                text = detail.text,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
            is Destination.Bearing -> updateBearingNavigation(guidance, direction)
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
        val details = mutableListOf<SheetDetail>()
        etaDetail(guidance, values)?.let { details += it }
        val gain = guidance.remainingElevationGain
        val loss = guidance.remainingElevationLoss
        when {
            gain != null -> details += SheetDetail(
                formatElevationChange(gain),
                TrailIcons.TriangleUp,
                context.getString(R.string.navigation_remaining_gain)
            )

            loss != null -> details += SheetDetail(
                formatElevationChange(loss),
                TrailIcons.TriangleDown,
                context.getString(R.string.navigation_remaining_loss)
            )
        }
        // The beacon's coordinates and elevation live on its details page (tap the name); the card keeps only
        // what you need while walking.
        model = direction.asModel(
            title = beacon.name,
            titleDescription = titled(R.string.beacon, beacon.name),
            showCancel = isNavigating,
            distance = guidance.remainingDistance?.let {
                SheetStat(formatRelativeDistance(it), context.getString(R.string.navigation_remaining))
            },
            details = details,
            badgeShape = beacon.shape
        )
    }

    private fun updateBearingNavigation(
        guidance: Guidance,
        direction: DirectionState
    ) {
        titleAction = null
        val title = formatBearing(guidance.targetBearing.value)
        // The name is the bearing, so the bearing line is not repeated; "facing" is the heading at the top of the
        // screen, so it is not repeated either.
        model = direction.asModel(
            title = title,
            titleDescription = titled(R.string.bearing, title),
            showBearing = false,
            showCancel = isNavigating,
            distance = guidance.traveledDistance?.let {
                SheetStat(formatRelativeDistance(it), context.getString(R.string.navigation_traveled))
            },
            details = emptyList()
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
        val details = mutableListOf<SheetDetail>()
        etaDetail(guidance, values)?.let { details += it }
        val gain = guidance.remainingElevationGain ?: Distance.meters(0f)
        val loss = guidance.remainingElevationLoss ?: Distance.meters(0f)
        if (gain.value > 0 || loss.value > 0) {
            details += SheetDetail(
                formatElevationChange(gain),
                TrailIcons.TriangleUp,
                context.getString(R.string.navigation_remaining_gain)
            )
            details += SheetDetail(
                formatElevationChange(loss),
                TrailIcons.TriangleDown,
                context.getString(R.string.navigation_remaining_loss)
            )
        }
        model = direction.asModel(
            title = name,
            titleDescription = titled(R.string.path, name),
            showCancel = isNavigating,
            showProgress = true,
            progress = (guidance.progress ?: 0f).coerceIn(0f, 1f),
            distance = guidance.remainingDistance?.let {
                SheetStat(formatRelativeDistance(it), context.getString(R.string.navigation_remaining))
            },
            details = details
        )
    }

    private fun etaDetail(guidance: Guidance, values: NavigationSensorValues): SheetDetail? {
        val duration = navigationService.eta(guidance, values.speed) ?: return null
        val arrival = formatter.formatTime(
            ZonedDateTime.now().plus(duration).toLocalTime(),
            includeSeconds = false
        )
        return SheetDetail(
            formatter.join(
                context.getString(R.string.navigation_arrive_format, arrival),
                formatter.formatDuration(duration, duration.toHours() >= LONG_DURATION_HOURS),
                separator = FormatService.Separator.Dot
            )
        )
    }

    private fun directionState(guidance: Guidance, values: NavigationSensorValues): DirectionState {
        val bearingText = formatBearing(guidance.targetBearing.value)
        if (!hasCompass) {
            return DirectionState(
                arrowIcon = TrailIcons.CompassUnavailable,
                arrowRotation = 0f,
                arrowColor = Color(Resources.androidTextColorSecondary(context)),
                bearingText = bearingText,
                offCourse = null
            )
        }
        val turn = deltaAngle(values.bearing.value, guidance.targetBearing.value)
        return DirectionState(
            arrowIcon = TrailIcons.NavigationArrow,
            arrowRotation = turn,
            arrowColor = Color(directionColor(turn.absoluteValue)),
            bearingText = bearingText,
            offCourse = ((turn.absoluteValue - ON_TARGET_DEGREES) / (WRONG_DIRECTION_DEGREES - ON_TARGET_DEGREES))
                .coerceIn(0f, 1f)
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
        val label: String
    )

    /** One item of the card's quiet details line; [icon] stands for its label, read out as [iconDescription]. */
    private data class SheetDetail(
        val text: String,
        val icon: ImageVector? = null,
        val iconDescription: String? = null
    )

    private data class DirectionState(
        val arrowIcon: ImageVector,
        val arrowRotation: Float,
        val arrowColor: Color,
        val bearingText: String,
        val offCourse: Float?
    ) {
        fun asModel(
            title: String,
            titleDescription: String,
            showBearing: Boolean = true,
            showCancel: Boolean,
            showProgress: Boolean = false,
            progress: Float = 0f,
            distance: SheetStat?,
            details: List<SheetDetail>,
            badgeShape: com.kylecorry.trail_sense.tools.beacons.domain.BeaconShape? = null
        ): SheetModel {
            return SheetModel(
                badgeShape = badgeShape,
                title = title,
                titleDescription = titleDescription,
                showCancel = showCancel,
                showDirection = true,
                showBearing = showBearing,
                arrowIcon = arrowIcon,
                arrowRotation = arrowRotation,
                arrowColor = arrowColor,
                bearingText = bearingText,
                offCourse = offCourse,
                showProgress = showProgress,
                progress = progress,
                distance = distance,
                details = details
            )
        }
    }

    private data class SheetModel(
        val title: String = "",
        val titleDescription: String? = null,
        val showCancel: Boolean = false,
        val showBearing: Boolean = true,
        val showDirection: Boolean = false,
        val arrowIcon: ImageVector = TrailIcons.NavigationArrow,
        val arrowRotation: Float = 0f,
        val arrowColor: Color = Color.Unspecified,
        val bearingText: String = "",
        /** 0 on course, 1 heading the wrong way; null without a compass. */
        val offCourse: Float? = null,
        val showProgress: Boolean = false,
        val progress: Float = 0f,
        val distance: SheetStat? = null,
        val details: List<SheetDetail> = emptyList(),
        /** The beacon's own shape for the badge (null: the default cookie). */
        val badgeShape: com.kylecorry.trail_sense.tools.beacons.domain.BeaconShape? = null
    )

    companion object {
        private const val ON_TARGET_DEGREES = 5f
        private const val WRONG_DIRECTION_DEGREES = 45f
        private const val LARGE_DISTANCE_THRESHOLD = 100f
        private const val LONG_DURATION_HOURS = 100L
        private const val NON_BREAKING_SPACE = ' '
    }
}
