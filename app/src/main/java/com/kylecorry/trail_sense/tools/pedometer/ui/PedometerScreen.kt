@file:Suppress("FunctionNaming", "LongMethod")

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.sense.pedometer.Pedometer
import com.kylecorry.luna.topics.generic.getOrNull
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.AveragePaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.CurrentPaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDate

@Composable
internal fun PedometerScreen(
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
    val averageSpeedometer = remember(counter, paceCalculator, prefs.pedometer.averagePaceTimeMode) {
        AveragePaceSpeedometer(counter, paceCalculator, prefs.pedometer)
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
                steps = counter.steps,
                distance = title,
                subtitle = subtitle,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("beacon_grid"),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
            ) {
                data.forEach { point ->
                    PedometerDataPointCard(
                        point = point,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

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
                            onClick = { onReset(counter) },
                            modifier = Modifier.testTag("reset_btn")
                        ) {
                            Text(stringResource(R.string.reset))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(TrailSenseSpacing.xs))

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
private fun PedometerHeroCard(
    steps: Long,
    distance: String,
    subtitle: String?,
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
        }
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
