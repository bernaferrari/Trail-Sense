@file:Suppress("FunctionNaming", "LongMethod")

package com.kylecorry.trail_sense.tools.pedometer.ui

import android.os.Bundle
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.DrawableRes
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
import androidx.compose.ui.res.painterResource
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
            icon = R.drawable.ic_speed
        ),
        PedometerDataPoint(
            tag = "pedometer_average_speed",
            title = averageSpeed,
            label = stringResource(R.string.average_speed),
            icon = R.drawable.ic_speed
        )
    )
    val paceData = listOf(
        PedometerDataPoint(
            tag = "pedometer_pace",
            title = currentPace,
            label = stringResource(R.string.current_pace),
            icon = R.drawable.ic_tool_clock
        ),
        PedometerDataPoint(
            tag = "pedometer_average_pace",
            title = averagePace,
            label = stringResource(R.string.average_pace),
            icon = R.drawable.ic_tool_clock
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
                    painter = painterResource(R.drawable.ic_tool_clock),
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
                modifier = Modifier.testTag("pedometer_title")
            )
        }

        trailSenseSection(key = "controls") {
            row { position ->
                PedometerStartStopRow(
                    isOn = isOn,
                    onClick = onTogglePedometer,
                    position = position,
                    modifier = Modifier.testTag("pedometer_play_bar")
                )
            }
            row { position ->
                TrailSenseSwitchRow(
                    label = stringResource(R.string.distance_alert),
                    checked = prefs.pedometer.alertDistance != null,
                    onCheckedChange = { onToggleDistanceAlert() },
                    position = position,
                    modifier = Modifier.testTag("pedometer_alert_btn")
                )
            }
            row { position ->
                TrailSenseListItem(
                    title = stringResource(R.string.pedometer_reset_steps),
                    icon = painterResource(R.drawable.ic_baseline_refresh_24),
                    position = position,
                    onClick = onReset,
                    modifier = Modifier.testTag("reset_btn")
                )
            }
        }

        trailSenseSection(key = "pace", title = speedTitle) {
            row { position ->
                PedometerMetricPair(
                    points = speedData,
                    position = position,
                    modifier = Modifier.testTag("beacon_grid")
                )
            }
            row { position ->
                PedometerMetricPair(points = paceData, position = position)
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
                        icon = R.drawable.steps
                    ),
                    PedometerDataPoint(
                        tag = "hourly_steps_total_distance",
                        title = formatStepsDistance(totalSteps),
                        label = stringResource(R.string.distance),
                        icon = R.drawable.ruler
                    )
                ),
                position = position
            )
        }
        row { position ->
            TrailSenseGroupCard(position = position) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.chart_height))
                        .testTag("hourly_steps_chart"),
                    factory = { viewContext ->
                        Chart(viewContext).apply {
                            tag = HourlyStepsChart(this) { hour ->
                                selectedStart = hour.startTime
                            }
                        }
                    },
                    update = { chart ->
                        val hourlyChart = chart.tag as? HourlyStepsChart ?: return@AndroidView
                        hourlyChart.plot(hourlySteps)
                        hourlyChart.highlight(selectedHour)
                    }
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
                            icon = R.drawable.steps
                        ),
                        PedometerDataPoint(
                            tag = "hourly_steps_details_distance",
                            title = formatStepsDistance(selectedHour.steps),
                            label = stringResource(R.string.distance),
                            icon = R.drawable.ruler
                        )
                    ),
                    position = position,
                    modifier = Modifier.testTag("hourly_steps_details_grid")
                )
            }
        }
    }
}

/** The one fact of the screen: today's steps, big, with the distance they make beside it. */
@Composable
private fun PedometerHeroCard(
    steps: Long,
    distance: String,
    subtitle: String?,
    sessionTime: String?,
    isOn: Boolean,
    modifier: Modifier = Modifier
) {
    val container by animateColorAsState(
        if (isOn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "pedometerHeroContainer"
    )
    val content by animateColorAsState(
        if (isOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "pedometerHeroContent"
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
            Row(verticalAlignment = Alignment.Bottom) {
                PedometerHeroMetric(
                    value = DecimalFormatter.format(steps, 0),
                    label = stringResource(R.string.steps),
                    style = MaterialTheme.typography.displayMedium,
                    valueModifier = Modifier.testTag("pedometer_steps"),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(TrailSenseSpacing.lg))
                PedometerHeroMetric(
                    value = distance,
                    label = stringResource(R.string.distance),
                    style = MaterialTheme.typography.headlineMedium,
                    horizontalAlignment = Alignment.End
                )
            }
            if (subtitle != null || sessionTime != null) {
                Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = content.copy(alpha = 0.85f)
                        )
                    }
                    if (sessionTime != null) {
                        Text(
                            text = sessionTime,
                            modifier = Modifier.testTag("pedometer_session_time"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = content.copy(alpha = 0.7f)
                        )
                    }
                }
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
                                painter = painterResource(point.icon),
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

/** Pedometer on or off, with the start/stop button at the end of the row. */
@Composable
private fun PedometerStartStopRow(
    isOn: Boolean,
    onClick: () -> Unit,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier
) {
    val wellColor by animateColorAsState(
        if (isOn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "pedometerWell"
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = trailSenseGroupShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(TrailSenseShape.iconContainer),
                color = wellColor,
                contentColor = if (isOn) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(TrailSenseSize.leadingIconContainer)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.steps),
                        contentDescription = null,
                        modifier = Modifier.size(TrailSenseSize.leadingIcon)
                    )
                }
            }
            Spacer(Modifier.width(TrailSenseSpacing.md))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                Text(
                    text = stringResource(R.string.pedometer),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(if (isOn) R.string.on else R.string.off),
                    modifier = Modifier.testTag("play_bar_title"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(TrailSenseSpacing.md))
            FilledIconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("play_btn"),
                shape = if (isOn) MaterialTheme.shapes.large else CircleShape
            ) {
                Icon(
                    painter = painterResource(
                        if (isOn) R.drawable.ic_baseline_stop_24 else R.drawable.ic_baseline_play_arrow_24
                    ),
                    contentDescription = stringResource(if (isOn) R.string.stop else R.string.start)
                )
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
                    painter = painterResource(R.drawable.ic_menu_dots),
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
    @DrawableRes val icon: Int
)
