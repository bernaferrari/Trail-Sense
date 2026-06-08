package com.kylecorry.trail_sense.tools.pedometer.ui

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.sense.pedometer.Pedometer
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.topics.generic.getOrNull
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.alertNoActivityRecognitionPermission
import com.kylecorry.trail_sense.shared.permissions.requestActivityRecognition
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.AveragePaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.CurrentPaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDate

class FragmentToolPedometer : TrailSenseComposeFragment() {

    private val pedometer by lazy { PedometerSubsystem.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    @Composable
    override fun FragmentContent() {
        PedometerScreen(
            pedometer = pedometer,
            prefs = prefs,
            formatService = formatService,
            onReset = ::resetCounter,
            onTogglePedometer = ::togglePedometer,
            onToggleDistanceAlert = ::toggleDistanceAlert
        )
    }

    private fun resetCounter(counter: StepCounter) {
        Alerts.dialog(requireContext(), getString(R.string.reset_distance_title)) {
            if (!it) {
                counter.reset()
            }
        }
    }

    private fun togglePedometer() {
        when (pedometer.state.getOrNull()) {
            FeatureState.On -> pedometer.disable()
            FeatureState.Off -> startStepCounter()
            else -> {
                if (pedometer.isDisabledDueToPermissions()) {
                    startStepCounter()
                }
            }
        }
    }

    private fun toggleDistanceAlert() {
        val alertDistance = prefs.pedometer.alertDistance
        if (alertDistance == null) {
            val units = formatService.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
            CustomUiUtils.pickDistance(
                requireContext(),
                units,
                title = getString(R.string.distance_alert),
            ) { distance, _ ->
                if (distance != null) {
                    prefs.pedometer.alertDistance = distance
                }
            }
        } else {
            val distance = alertDistance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
            Alerts.dialog(
                requireContext(),
                getString(R.string.distance_alert),
                getString(
                    R.string.remove_distance_alert,
                    formatService.formatDistance(
                        distance,
                        Units.getDecimalPlaces(distance.units),
                        false
                    )
                )
            ) {
                if (!it) {
                    prefs.pedometer.alertDistance = null
                }
            }
        }
        binding.pedometerAveragePace.title = if (averageSpeedometer.hasValidReading) {
            formatService.formatPace(speed.value)
        } else {
            getString(R.string.dash)
        }
    }

    private fun startStepCounter() {
        requestActivityRecognition { hasPermission ->
            if (hasPermission) {
                pedometer.enable()
            } else {
                pedometer.disable()
                alertNoActivityRecognitionPermission()
            }
        }
    }
}

@Composable
private fun PedometerScreen(
    pedometer: PedometerSubsystem,
    prefs: UserPreferences,
    formatService: FormatService,
    onReset: (StepCounter) -> Unit,
    onTogglePedometer: () -> Unit,
    onToggleDistanceAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val counter = remember { StepCounter(PreferencesSubsystem.getInstance(context).preferences) }
    val paceCalculator = remember(prefs.pedometer.strideLength) {
        StrideLengthPaceCalculator(prefs.pedometer.strideLength)
    }
    val averageSpeedometer = remember(counter, paceCalculator) {
        AveragePaceSpeedometer(counter, paceCalculator)
    }
    val instantSpeedometer = remember(paceCalculator) {
        CurrentPaceSpeedometer(Pedometer(context), paceCalculator)
    }
    var refreshKey by remember { mutableIntStateOf(0) }

    DisposableEffect(averageSpeedometer, instantSpeedometer) {
        val listener = {
            refreshKey++
            true
        }
        averageSpeedometer.start(listener)
        instantSpeedometer.start(listener)
        onDispose {
            averageSpeedometer.stop(listener)
            instantSpeedometer.stop(listener)
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            refreshKey++
            delay(500)
        }
    }

    // Force state reads to refresh on the timer.
    refreshKey.hashCode()

    val distance = getDistance(counter.steps, paceCalculator, prefs)
    val lastReset = counter.startTime?.toZonedDateTime()
    val state = pedometer.state.getOrNull() ?: FeatureState.Off
    val title = formatService.formatDistance(
        distance,
        Units.getDecimalPlaces(distance.units),
        false
    )
    val subtitle = lastReset?.let {
        val dateString = if (it.toLocalDate() == LocalDate.now()) {
            formatService.formatTime(it.toLocalTime(), false)
        } else {
            formatService.formatRelativeDate(it.toLocalDate())
        }
        stringResource(R.string.since_time, dateString)
    }
    val averageSpeed = if (averageSpeedometer.hasValidReading) {
        formatService.formatSpeed(averageSpeedometer.speed.value)
    } else {
        stringResource(R.string.dash)
    }
    val currentSpeed = if (averageSpeedometer.hasValidReading) {
        formatService.formatSpeed(instantSpeedometer.speed.value)
    } else {
        stringResource(R.string.dash)
    }
    val data = listOf(
        PedometerDataPoint(
            tag = "pedometer_steps",
            title = DecimalFormatter.format(counter.steps, 0),
            label = stringResource(R.string.steps),
            icon = R.drawable.steps
        ),
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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PedometerHeader(
                title = title,
                subtitle = subtitle,
                isAlertEnabled = prefs.pedometer.alertDistance != null,
                onToggleDistanceAlert = onToggleDistanceAlert,
                modifier = Modifier.testTag("pedometer_title")
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    .height(260.dp)
                    .testTag("beacon_grid"),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(data) {
                    PedometerDataPointCard(it)
                }
            }
            Button(
                onClick = { onReset(counter) },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp)
                    .testTag("reset_btn")
            ) {
                Text(stringResource(R.string.reset))
            }
            Spacer(modifier = Modifier.weight(1f))
            PedometerPlayBar(
                state = state,
                onClick = onTogglePedometer,
                modifier = Modifier.testTag("pedometer_play_bar")
            )
        }
    }
}

@Composable
private fun PedometerHeader(
    title: String,
    subtitle: String?,
    isAlertEnabled: Boolean,
    onToggleDistanceAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = onToggleDistanceAlert,
                modifier = Modifier.testTag("pedometer_alert_btn")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_notification),
                    contentDescription = null,
                    tint = if (isAlertEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun PedometerDataPointCard(
    point: PedometerDataPoint
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(point.tag),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(point.icon),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = point.title,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = point.label,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PedometerPlayBar(
    state: FeatureState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOn = state == FeatureState.On
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.steps),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.pedometer),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(if (isOn) R.string.on else R.string.off),
                    modifier = Modifier.testTag("play_bar_title"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                    tint = MaterialTheme.colorScheme.primary
                )
            }
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
