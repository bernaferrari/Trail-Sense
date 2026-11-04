@file:Suppress("FunctionNaming", "LongMethod")

package com.kylecorry.trail_sense.tools.pedometer.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGridCellShape
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import android.os.Bundle
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.andromeda.sense.pedometer.Pedometer
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.luna.topics.generic.getOrNull
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseMetric
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.domain.HourlyStepCount
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.AveragePaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.CurrentPaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Pedometer: a hero with the steps and distance since the last reset, the controls as one group (on/off, distance
 * alert, reset), speed and pace as paired readings, then the hourly history of any day.
 */
@Composable
internal fun PedometerScreen(
    pedometer: PedometerSubsystem,
    prefs: UserPreferences,
    formatService: FormatService,
    onReset: () -> Unit,
    onTogglePedometer: () -> Unit,
    onToggleDistanceAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stepTracker = remember { getAppService<StepTrackerService>() }
    val paceCalculator = remember(prefs.pedometer.strideLength) {
        StrideLengthPaceCalculator(prefs.pedometer.strideLength)
    }
    val sessionFormatter = remember(paceCalculator, prefs) {
        PedometerSessionFormatter(context, paceCalculator, formatService, prefs)
    }
    val averageSpeedometer = remember(stepTracker, paceCalculator, prefs.pedometer.averagePaceTimeMode) {
        AveragePaceSpeedometer(stepTracker, paceCalculator, prefs.pedometer)
    }
    val instantSpeedometer = remember(paceCalculator) {
        CurrentPaceSpeedometer(Pedometer(context), paceCalculator)
    }
    var refreshKey by remember { mutableIntStateOf(0) }
    var openPeriod by remember { mutableStateOf<StepTrackingPeriod?>(null) }
    var showSessions by remember { mutableStateOf(false) }
    var sessions by remember { mutableStateOf<List<StepTrackingPeriod>>(emptyList()) }

    suspend fun reloadOpenPeriod() {
        openPeriod = withContext(Dispatchers.IO) {
            stepTracker.getOpenStepTrackingPeriod()
        }
    }

    DisposableEffect(averageSpeedometer, instantSpeedometer, stepTracker) {
        val listener = {
            refreshKey++
            true
        }
        val stepsListener: (Bundle) -> Unit = {
            scope.launch { reloadOpenPeriod() }
        }
        averageSpeedometer.start(listener)
        instantSpeedometer.start(listener)
        Tools.subscribe(PedometerToolRegistration.BROADCAST_STEPS_CHANGED, stepsListener)
        onDispose {
            averageSpeedometer.stop(listener)
            instantSpeedometer.stop(listener)
            Tools.unsubscribe(PedometerToolRegistration.BROADCAST_STEPS_CHANGED, stepsListener)
        }
    }

    LaunchedEffect(Unit) {
        reloadOpenPeriod()
        while (isActive) {
            refreshKey++
            delay(500)
            reloadOpenPeriod()
        }
    }

    // Force state reads to refresh on the timer.
    refreshKey.hashCode()

    val steps = openPeriod?.steps ?: 0L
    val distance = getDistance(steps, paceCalculator, prefs)
    val lastReset = openPeriod?.startTime?.toZonedDateTime()
    val state = pedometer.state.getOrNull() ?: FeatureState.Off
    val title = formatService.formatDistance(
        distance,
        Units.getDecimalPlaces(distance.units),
        false
    )
    val resetLabel = lastReset?.let { reset ->
        val dateString = if (reset.toLocalDate() == LocalDate.now()) {
            formatService.formatTime(reset.toLocalTime(), false)
        } else {
            formatService.formatRelativeDate(reset.toLocalDate())
        }
        dateString
    }
    val subtitle = resetLabel?.let { stringResource(R.string.since_time, it) }
    val sessionTime = resetLabel?.let { dateString ->
        val span = stringResource(R.string.dash_separated_pair, dateString, stringResource(R.string.now))
        val active = formatService.formatDuration(openPeriod?.activeTime ?: Duration.ZERO)
        stringResource(
            R.string.parenthesized_pair,
            span,
            stringResource(R.string.active_duration, active)
        )
    }
    val currentSpeed = speedText(instantSpeedometer, formatService)
    val averageSpeed = speedText(averageSpeedometer, formatService)
    val currentPace = paceText(instantSpeedometer, formatService)
    val averagePace = paceText(averageSpeedometer, formatService)
    val speedData = listOf(
        PedometerDataPoint(
            tag = "pedometer_speed",
            title = currentSpeed,
            label = stringResource(R.string.current_speed),
            icon = TrailIcons.Speed
        ),
        PedometerDataPoint(
            tag = "pedometer_average_speed",
            title = averageSpeed,
            label = stringResource(R.string.average_speed),
            icon = TrailIcons.Speed
        )
    )
    val paceData = listOf(
        PedometerDataPoint(
            tag = "pedometer_pace",
            title = currentPace,
            label = stringResource(R.string.current_pace),
            icon = TrailIcons.ToolClock
        ),
        PedometerDataPoint(
            tag = "pedometer_average_pace",
            title = averagePace,
            label = stringResource(R.string.average_pace),
            icon = TrailIcons.ToolClock
        )
    )
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val isOn = state == FeatureState.On
    val speedTitle = stringResource(R.string.speed)

    TrailSensePage(
        title = stringResource(R.string.pedometer),
        onBack = { dispatcher?.onBackPressed() },
        backLabel = stringResource(R.string.back),
        modifier = modifier,
        actions = {
            IconButton(
                onClick = {
                    scope.launch {
                        sessions = withContext(Dispatchers.IO) {
                            stepTracker.getAllStepTrackingPeriods()
                                .sortedByDescending { it.startTime }
                        }
                        showSessions = true
                    }
                },
                modifier = Modifier.testTag("pedometer_sessions_btn")
            ) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.ToolClock),
                    contentDescription = stringResource(R.string.history)
                )
            }
        }
    ) {
        item(key = "hero") {
            PedometerHeroCard(
                steps = steps,
                distance = title,
                subtitle = subtitle,
                sessionTime = sessionTime,
                isOn = isOn,
                onToggle = onTogglePedometer,
                onReset = onReset,
                modifier = Modifier.testTag("pedometer_title")
            )
        }

        trailSenseSection(key = "controls") {
            row { position ->
                TrailSenseSwitchRow(
                    label = stringResource(R.string.distance_alert),
                    checked = prefs.pedometer.alertDistance != null,
                    onCheckedChange = { onToggleDistanceAlert() },
                    position = position,
                    modifier = Modifier.testTag("pedometer_alert_btn")
                )
            }
        }

        trailSenseSection(key = "pace", title = speedTitle) {
            row { _ ->
                PedometerTiles(
                    rows = listOf(speedData, paceData),
                    highlightFirst = isOn,
                    modifier = Modifier.testTag("beacon_grid")
                )
            }
        }

        item(key = "history") {
            HourlyStepsHistory(
                prefs = prefs,
                formatService = formatService,
                paceCalculator = paceCalculator,
                refreshKey = refreshKey
            )
        }
    }

    if (showSessions) {
        PedometerSessionsDialog(
            sessions = sessions,
            formatter = sessionFormatter,
            onDismiss = { showSessions = false },
            onDelete = { period, title ->
                Alerts.dialog(
                    context,
                    context.getString(R.string.delete),
                    title
                ) { cancelled ->
                    if (!cancelled) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                stepTracker.deleteStepTrackingPeriod(period)
                            }
                            sessions = withContext(Dispatchers.IO) {
                                stepTracker.getAllStepTrackingPeriods()
                                    .sortedByDescending { it.startTime }
                            }
                            reloadOpenPeriod()
                        }
                    }
                }
            }
        )
    }
}

/**
 * Steps by hour for one day: a day picker, the day's totals, the chart, and the hour you tap on it. All one group so
 * the day reads as one object.
 */
@Composable
private fun HourlyStepsHistory(
    prefs: UserPreferences,
    formatService: FormatService,
    paceCalculator: StrideLengthPaceCalculator,
    refreshKey: Int
) {
    val context = LocalContext.current
    val stepTracker = remember { getAppService<StepTrackerService>() }
    // Saveable so the chosen day and hour survive the history scrolling out of the lazy page and back.
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var selectedStart by rememberSaveable { mutableStateOf<Instant?>(null) }
    var hourlySteps by remember { mutableStateOf<List<HourlyStepCount>>(emptyList()) }
    val today = LocalDate.now()
    val historyTick = if (selectedDate == today) refreshKey / 4 else 0

    LaunchedEffect(selectedDate, historyTick) {
        val date = selectedDate
        val updated = withContext(Dispatchers.IO) {
            stepTracker.getHourlyStepCounts(date)
        }
        if (date == selectedDate) {
            hourlySteps = updated
        }
    }

    fun selectDate(date: LocalDate) {
        val clamped = if (date.isAfter(today)) today else date
        if (clamped == selectedDate) {
            return
        }
        selectedDate = clamped
        selectedStart = null
    }

    val selectedHour = hourlySteps.firstOrNull { it.startTime == selectedStart }
    val totalSteps = hourlySteps.sumOf { it.steps }
    fun formatStepsDistance(steps: Long): String {
        val distance = getDistance(steps, paceCalculator, prefs)
        return formatService.formatDistance(
            distance,
            Units.getDecimalPlaces(distance.units),
            false
        )
    }

    TrailSenseRowGroup(
        title = stringResource(R.string.history),
        modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())
    ) {
        row { position ->
            TrailSenseGroupCard(
                position = position,
                contentPadding = PaddingValues(TrailSenseSpacing.xs),
                modifier = Modifier.testTag("hourly_steps_date")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { selectDate(selectedDate.minusDays(1)) },
                        modifier = Modifier.testTag("hourly_steps_previous")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.previous)
                        )
                    }
                    Text(
                        text = formatService.formatRelativeDate(selectedDate),
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.large)
                            .combinedClickable(
                                onClick = {
                                    Pickers.date(context, selectedDate) { picked ->
                                        if (picked != null) {
                                            selectDate(picked)
                                        }
                                    }
                                },
                                onLongClick = { selectDate(LocalDate.now()) }
                            )
                            .padding(vertical = TrailSenseSpacing.md),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Hidden rather than disabled on today: there's no tomorrow to go to.
                    Box(modifier = Modifier.size(TrailSenseSize.minimumTouchTarget)) {
                        if (selectedDate.isBefore(today)) {
                            IconButton(
                                onClick = { selectDate(selectedDate.plusDays(1)) },
                                modifier = Modifier.testTag("hourly_steps_next")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.next)
                                )
                            }
                        }
                    }
                }
            }
        }
        row { position ->
            PedometerMetricPair(
                points = listOf(
                    PedometerDataPoint(
                        tag = "hourly_steps_total_steps",
                        title = DecimalFormatter.format(totalSteps, 0),
                        label = stringResource(R.string.steps),
                        icon = TrailIcons.Steps
                    ),
                    PedometerDataPoint(
                        tag = "hourly_steps_total_distance",
                        title = formatStepsDistance(totalSteps),
                        label = stringResource(R.string.distance),
                        icon = TrailIcons.Ruler
                    )
                ),
                position = position
            )
        }
        row { position ->
            TrailSenseGroupCard(position = position) {
                HourlyStepsBars(
                    hours = hourlySteps,
                    selected = selectedHour,
                    onSelect = { selectedStart = if (it.startTime == selectedStart) null else it.startTime },
                    formatHour = { formatService.formatTime(it, includeSeconds = false, includeMinutes = false) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.chart_height))
                        .testTag("hourly_steps_chart")
                )
            }
        }
        if (selectedHour != null) {
            row { position ->
                PedometerMetricPair(
                    label = stringResource(
                        R.string.dash_separated_pair,
                        formatService.formatTime(selectedHour.startTime, false),
                        formatService.formatTime(selectedHour.endTime, false)
                    ),
                    labelModifier = Modifier.testTag("hourly_steps_details"),
                    points = listOf(
                        PedometerDataPoint(
                            tag = "hourly_steps_details_steps",
                            title = DecimalFormatter.format(selectedHour.steps, 0),
                            label = stringResource(R.string.steps),
                            icon = TrailIcons.Steps
                        ),
                        PedometerDataPoint(
                            tag = "hourly_steps_details_distance",
                            title = formatStepsDistance(selectedHour.steps),
                            label = stringResource(R.string.distance),
                            icon = TrailIcons.Ruler
                        )
                    ),
                    position = position,
                    modifier = Modifier.testTag("hourly_steps_details_grid")
                )
            }
        }
    }
}

/**
 * The one fact of the screen: today's steps, huge, on a saturated card that is primary while counting and quiet
 * while off. The badge walks while it counts; distance and session time sit as pills; start/stop and reset are the
 * card's connected actions.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PedometerHeroCard(
    steps: Long,
    distance: String,
    subtitle: String?,
    sessionTime: String?,
    isOn: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val container by animateColorAsState(
        if (isOn) scheme.primaryContainer else scheme.surfaceContainerHigh,
        effects,
        label = "pedometerHeroContainer"
    )
    val content by animateColorAsState(
        if (isOn) scheme.onPrimaryContainer else scheme.onSurface,
        effects,
        label = "pedometerHeroContent"
    )
    val stride by rememberInfiniteTransition(label = "stride").animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "strideTilt"
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        contentColor = content
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrailSenseShapeBadge(
                    icon = TrailIcons.Steps,
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    size = 56.dp,
                    containerColor = if (isOn) scheme.primary else scheme.secondaryContainer,
                    contentColor = if (isOn) scheme.onPrimary else scheme.onSecondaryContainer,
                    iconRotation = if (isOn) stride else 0f
                )
                Spacer(Modifier.width(TrailSenseSpacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.pedometer),
                        style = MaterialTheme.typography.labelLarge,
                        color = content.copy(alpha = 0.78f)
                    )
                    Text(
                        text = stringResource(if (isOn) R.string.on else R.string.off),
                        modifier = Modifier.testTag("play_bar_title"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Column {
                Text(
                    text = DecimalFormatter.format(steps, 0),
                    modifier = Modifier.testTag("pedometer_steps"),
                    style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.steps),
                    style = MaterialTheme.typography.titleMedium,
                    color = content.copy(alpha = 0.78f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)) {
                HeroPill(
                    icon = TrailIcons.Ruler,
                    text = distance,
                    container = if (isOn) scheme.primary else scheme.secondaryContainer,
                    content = if (isOn) scheme.onPrimary else scheme.onSecondaryContainer
                )
                if (sessionTime != null) {
                    HeroPill(
                        icon = TrailIcons.ToolClock,
                        text = sessionTime,
                        container = content.copy(alpha = 0.1f),
                        content = content,
                        modifier = Modifier.testTag("pedometer_session_time")
                    )
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = content.copy(alpha = 0.8f)
                )
            }
            Row(
                modifier = Modifier.testTag("pedometer_play_bar"),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                val outer = 28.dp
                val inner = 8.dp
                Button(
                    onClick = onToggle,
                    modifier = Modifier
                        .weight(1.4f)
                        .heightIn(min = 56.dp)
                        .testTag("play_btn"),
                    shape = RoundedCornerShape(topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOn) scheme.primary else scheme.primary,
                        contentColor = scheme.onPrimary
                    )
                ) {
                    Icon(
                        painter = rememberVectorPainter(
                            if (isOn) TrailIcons.BaselineStop24 else TrailIcons.BaselinePlayArrow24
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(TrailSenseSpacing.sm))
                    Text(
                        stringResource(if (isOn) R.string.stop else R.string.start),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                FilledTonalButton(
                    onClick = onReset,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .testTag("reset_btn"),
                    shape = RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = scheme.surfaceContainerLowest.copy(alpha = 0.72f),
                        contentColor = content
                    )
                ) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.BaselineRefresh24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(TrailSenseSpacing.sm))
                    Text(stringResource(R.string.reset), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun HeroPill(
    icon: ImageVector,
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    Surface(shape = CircleShape, color = container, contentColor = content, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.md, vertical = TrailSenseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(rememberVectorPainter(icon), contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(TrailSenseSpacing.xs))
            Text(
                text,
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/**
 * Readings as connected tiles, two to a row: icon and label over a large tabular value. The first tile can be the
 * highlighted one (the live speed), in the primary tone.
 */
@Composable
private fun PedometerTiles(
    rows: List<List<PedometerDataPoint>>,
    modifier: Modifier = Modifier,
    highlightFirst: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
        rows.forEachIndexed { rowIndex, points ->
            Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                points.forEachIndexed { columnIndex, point ->
                    val highlighted = highlightFirst && rowIndex == 0 && columnIndex == 0
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .testTag(point.tag),
                        shape = trailSenseGridCellShape(columnIndex, points.size, rowIndex, rows.size),
                        color = if (highlighted) scheme.primaryContainer else scheme.surfaceContainerLow,
                        contentColor = if (highlighted) scheme.onPrimaryContainer else scheme.onSurface
                    ) {
                        Column(
                            Modifier
                                .heightIn(min = 88.dp)
                                .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    rememberVectorPainter(point.icon),
                                    contentDescription = null,
                                    tint = if (highlighted) scheme.onPrimaryContainer else scheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(TrailSenseSpacing.xs))
                                Text(
                                    point.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (highlighted) scheme.onPrimaryContainer.copy(alpha = 0.8f) else scheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Text(
                                point.title,
                                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Steps by hour as rounded bars; tap one to select it (it fills primary, the rest stay soft). */
@Composable
private fun HourlyStepsBars(
    hours: List<com.kylecorry.trail_sense.tools.pedometer.domain.HourlyStepCount>,
    selected: com.kylecorry.trail_sense.tools.pedometer.domain.HourlyStepCount?,
    onSelect: (com.kylecorry.trail_sense.tools.pedometer.domain.HourlyStepCount) -> Unit,
    formatHour: (Instant) -> String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val bar = scheme.primary
    val soft = scheme.primary.copy(alpha = 0.32f)
    val track = scheme.onSurface.copy(alpha = 0.06f)
    val label = scheme.onSurfaceVariant
    val labelSize = with(androidx.compose.ui.platform.LocalDensity.current) { 11.sp.toPx() }
    val currentHours by rememberUpdatedState(hours)
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val list = currentHours
                if (list.isEmpty()) return@detectTapGestures
                val index = (offset.x / (size.width / list.size.toFloat())).toInt().coerceIn(0, list.lastIndex)
                onSelect(list[index])
            }
        }
    ) {
        if (hours.isEmpty()) return@Canvas
        val labelSpace = labelSize + 8.dp.toPx()
        val chartHeight = size.height - labelSpace
        val slot = size.width / hours.size
        val width = (slot * 0.64f).coerceAtLeast(2.dp.toPx())
        val max = (hours.maxOf { it.steps }.coerceAtLeast(100L)).toFloat()
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = label.toArgb()
            textSize = labelSize
            textAlign = android.graphics.Paint.Align.CENTER
            fontFeatureSettings = "tnum"
        }
        hours.forEachIndexed { index, hour ->
            val x = slot * index + (slot - width) / 2f
            drawRoundRect(track, Offset(x, 0f), androidx.compose.ui.geometry.Size(width, chartHeight), CornerRadius(width / 2f))
            val h = (hour.steps / max) * chartHeight
            if (h > 0f) {
                drawRoundRect(
                    if (selected == null || hour == selected) bar else soft,
                    Offset(x, chartHeight - h.coerceAtLeast(width)),
                    androidx.compose.ui.geometry.Size(width, h.coerceAtLeast(width)),
                    CornerRadius(width / 2f)
                )
            }
            if (index % 6 == 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    formatHour(hour.startTime),
                    slot * index + slot / 2f,
                    size.height - 2.dp.toPx(),
                    paint
                )
            }
        }
    }
}

@Composable
private fun PedometerHeroMetric(
    value: String,
    label: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    valueModifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = LocalContentColor.current.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            modifier = valueModifier,
            style = style.copy(fontFeatureSettings = "tnum"),
            maxLines = 1
        )
    }
}

/** Two readings side by side in one group card, each a label over a tabular value. */
@Composable
private fun PedometerMetricPair(
    points: List<PedometerDataPoint>,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    label: String? = null,
    labelModifier: Modifier = Modifier
) {
    TrailSenseGroupCard(position = position, modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                modifier = labelModifier.padding(bottom = TrailSenseSpacing.md),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)) {
            points.forEach { point ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(point.tag),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(TrailSenseShape.iconContainer),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(TrailSenseSize.compactIconContainer)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = rememberVectorPainter(point.icon),
                                contentDescription = null,
                                modifier = Modifier.size(TrailSenseSize.compactIcon)
                            )
                        }
                    }
                    Spacer(Modifier.width(TrailSenseSpacing.md))
                    TrailSenseMetric(
                        value = point.title,
                        label = point.label,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun PedometerSessionsDialog(
    sessions: List<StepTrackingPeriod>,
    formatter: PedometerSessionFormatter,
    onDismiss: () -> Unit,
    onDelete: (StepTrackingPeriod, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history)) },
        text = {
            if (sessions.isEmpty()) {
                Text(stringResource(R.string.no_pedometer_sessions))
            } else {
                Column(
                    modifier = Modifier
                        .height(360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
                ) {
                    sessions.forEach { period ->
                        PedometerSessionRow(
                            period = period,
                            formatter = formatter,
                            onDelete = onDelete
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}

@Composable
private fun PedometerSessionRow(
    period: StepTrackingPeriod,
    formatter: PedometerSessionFormatter,
    onDelete: (StepTrackingPeriod, String) -> Unit
) {
    val title = formatter.title(period)
    var menuOpen by remember(period.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatter.subtitle(period),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.MenuDots),
                    contentDescription = stringResource(R.string.delete)
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete)) },
                    onClick = {
                        menuOpen = false
                        onDelete(period, title)
                    }
                )
            }
        }
    }
}

@Composable
private fun speedText(speedometer: ISpeedometer, formatService: FormatService): String {
    return if (speedometer.hasValidReading) {
        formatService.formatSpeed(speedometer.speed.value)
    } else {
        stringResource(R.string.dash)
    }
}

@Composable
private fun paceText(speedometer: ISpeedometer, formatService: FormatService): String {
    return if (speedometer.hasValidReading) {
        formatService.formatPace(speedometer.speed.value)
    } else {
        stringResource(R.string.dash)
    }
}

private fun getDistance(
    steps: Long,
    paceCalculator: StrideLengthPaceCalculator,
    prefs: UserPreferences
): Distance {
    val distance = paceCalculator.distance(steps)
    return distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
}

private data class PedometerDataPoint(
    val tag: String,
    val title: String,
    val label: String,
    val icon: ImageVector
)
