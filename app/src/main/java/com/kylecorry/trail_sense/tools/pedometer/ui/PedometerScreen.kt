@file:Suppress("FunctionNaming", "LongMethod")

package com.kylecorry.trail_sense.tools.pedometer.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.andromeda.pickers.Pickers
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow
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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(TrailSenseSpacing.screen),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
            // Hero step count + distance
            PedometerHeroCard(
                steps = steps,
                distance = title,
                subtitle = subtitle,
                sessionTime = sessionTime,
                modifier = Modifier.testTag("pedometer_title")
            )

            // Speed metrics
            Text(
                text = stringResource(R.string.pedometer),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = TrailSenseSpacing.xs)
            )
            PedometerMetricRow(speedData, modifier = Modifier.testTag("beacon_grid"))
            PedometerMetricRow(paceData)

            // Options group
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column {
                    TrailSenseSwitchRow(
                        label = stringResource(R.string.distance_alert),
                        checked = prefs.pedometer.alertDistance != null,
                        onCheckedChange = { onToggleDistanceAlert() },
                        modifier = Modifier.testTag("pedometer_alert_btn")
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = TrailSenseSpacing.lg,
                                vertical = TrailSenseSpacing.xs
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.steps),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onReset,
                            modifier = Modifier.testTag("reset_btn")
                        ) {
                            Text(stringResource(R.string.reset))
                        }
                    }
                }
            }

            HourlyStepsHistory(
                prefs = prefs,
                formatService = formatService,
                paceCalculator = paceCalculator,
                refreshKey = refreshKey,
                onShowSessions = {
                    scope.launch {
                        sessions = withContext(Dispatchers.IO) {
                            stepTracker.getAllStepTrackingPeriods()
                                .sortedByDescending { it.startTime }
                        }
                        showSessions = true
                    }
                }
            )

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

            // Primary start / stop control
            PedometerStartStopButton(
                state = state,
                onClick = onTogglePedometer,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pedometer_play_bar")
            )
        }
        }
    }
}

@Composable
private fun HourlyStepsHistory(
    prefs: UserPreferences,
    formatService: FormatService,
    paceCalculator: StrideLengthPaceCalculator,
    refreshKey: Int,
    onShowSessions: () -> Unit
) {
    val context = LocalContext.current
    val stepTracker = remember { getAppService<StepTrackerService>() }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var hourlySteps by remember { mutableStateOf<List<HourlyStepCount>>(emptyList()) }
    val selectedStart = remember { mutableStateOf<Instant?>(null) }
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
        selectedStart.value = null
    }

    val selectedHour = hourlySteps.firstOrNull { it.startTime == selectedStart.value }
    val totalSteps = hourlySteps.sumOf { it.steps }
    fun formatStepsDistance(steps: Long): String {
        val distance = getDistance(steps, paceCalculator, prefs)
        return formatService.formatDistance(
            distance,
            Units.getDecimalPlaces(distance.units),
            false
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.history),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = TrailSenseSpacing.xs)
            )
            IconButton(
                onClick = onShowSessions,
                modifier = Modifier.testTag("pedometer_sessions_btn")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_tool_clock),
                    contentDescription = stringResource(R.string.history),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hourly_steps_date"),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                IconButton(
                    onClick = { selectDate(selectedDate.plusDays(1)) },
                    enabled = selectedDate.isBefore(today),
                    modifier = Modifier.testTag("hourly_steps_next")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.next)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            PedometerDataPointCard(
                point = PedometerDataPoint(
                    tag = "hourly_steps_total_steps",
                    title = DecimalFormatter.format(totalSteps, 0),
                    label = stringResource(R.string.steps),
                    icon = R.drawable.steps
                ),
                modifier = Modifier.weight(1f)
            )
            PedometerDataPointCard(
                point = PedometerDataPoint(
                    tag = "hourly_steps_total_distance",
                    title = formatStepsDistance(totalSteps),
                    label = stringResource(R.string.distance),
                    icon = R.drawable.ruler
                ),
                modifier = Modifier.weight(1f)
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.chart_height))
                    .padding(TrailSenseSpacing.sm)
                    .testTag("hourly_steps_chart"),
                factory = { viewContext ->
                    Chart(viewContext).apply {
                        tag = HourlyStepsChart(this) { hour ->
                            selectedStart.value = hour.startTime
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
        if (selectedHour != null) {
            Text(
                text = stringResource(
                    R.string.dash_separated_pair,
                    formatService.formatTime(selectedHour.startTime, false),
                    formatService.formatTime(selectedHour.endTime, false)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hourly_steps_details"),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hourly_steps_details_grid"),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
            ) {
                PedometerDataPointCard(
                    point = PedometerDataPoint(
                        tag = "hourly_steps_details_steps",
                        title = DecimalFormatter.format(selectedHour.steps, 0),
                        label = stringResource(R.string.steps),
                        icon = R.drawable.steps
                    ),
                    modifier = Modifier.weight(1f)
                )
                PedometerDataPointCard(
                    point = PedometerDataPoint(
                        tag = "hourly_steps_details_distance",
                        title = formatStepsDistance(selectedHour.steps),
                        label = stringResource(R.string.distance),
                        icon = R.drawable.ruler
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PedometerHeroCard(
    steps: Long,
    distance: String,
    subtitle: String?,
    sessionTime: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = TrailSenseSpacing.xl,
                vertical = TrailSenseSpacing.xxl
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.steps).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(TrailSenseSpacing.sm))
            Text(
                text = DecimalFormatter.format(steps, 0),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFeatureSettings = "tnum"
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("pedometer_steps")
            )
            Spacer(modifier = Modifier.height(TrailSenseSpacing.md))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = distance,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(
                        horizontal = TrailSenseSpacing.lg,
                        vertical = TrailSenseSpacing.sm
                    )
                )
            }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(TrailSenseSpacing.sm))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
            if (sessionTime != null) {
                Spacer(modifier = Modifier.height(TrailSenseSpacing.xs))
                Text(
                    text = sessionTime,
                    modifier = Modifier.testTag("pedometer_session_time"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PedometerMetricRow(
    points: List<PedometerDataPoint>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
    ) {
        points.forEach { point ->
            PedometerDataPointCard(
                point = point,
                modifier = Modifier.weight(1f)
            )
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

@Composable
private fun PedometerDataPointCard(
    point: PedometerDataPoint,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(point.tag),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(point.icon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = point.title,
                modifier = Modifier.padding(top = TrailSenseSpacing.md),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = point.label,
                modifier = Modifier.padding(top = TrailSenseSpacing.xxs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PedometerStartStopButton(
    state: FeatureState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOn = state == FeatureState.On
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrailSenseSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isOn) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.steps),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = if (isOn) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.width(TrailSenseSpacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.pedometer),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(if (isOn) R.string.on else R.string.off),
                    modifier = Modifier.testTag("play_bar_title"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(TrailSenseSpacing.md))
            PedometerPlayControl(
                isOn = isOn,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun PedometerPlayControl(
    isOn: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(56.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.testTag("play_btn")
        ) {
            Icon(
                painter = painterResource(
                    if (isOn) {
                        R.drawable.ic_baseline_stop_24
                    } else {
                        R.drawable.ic_baseline_play_arrow_24
                    }
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
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
