@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.waterpurification.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChip
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

const val WATER_PURIFICATION_END_TIME_KEY = "water_purification_start_time"

@Composable
internal fun WaterPurificationScreen(
    cache: IPreferences,
    formatService: FormatService,
    getSelectedDuration: suspend (TimeSelection) -> Duration
) {
    val context = LocalContext.current
    var selectedTime by remember { mutableStateOf(TimeSelection.Auto) }
    var duration by remember { mutableStateOf<Duration?>(null) }
    var remaining by remember { mutableStateOf<Duration?>(null) }
    val isRunning = remaining != null

    LaunchedEffect(selectedTime) {
        duration = null
        duration = getSelectedDuration(selectedTime)
    }

    LaunchedEffect(Unit) {
        while (true) {
            remaining = getRemainingTime(cache)
            delay(250)
        }
    }

    fun stop() {
        cache.remove(WATER_PURIFICATION_END_TIME_KEY)
        WaterPurificationTimerService.stop(context)
        remaining = null
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
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
                    .padding(horizontal = TrailSenseSpacing.screen, vertical = TrailSenseSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl)
            ) {
            // Instructions card
            StepsCard()

            // Central timer readout with circular progress
            val displayed = remaining ?: duration
            // Progress fraction of the remaining run. When idle/selecting we show
            // a full ring; while running we deplete from full to empty.
            val progressTarget = remainingFraction(remaining, duration)
            val animatedProgress by animateFloatAsState(
                targetValue = progressTarget,
                animationSpec = TrailSenseAnimation.standardSpring,
                label = "purificationProgress"
            )

            TimerDial(
                seconds = displayed?.seconds,
                progress = animatedProgress,
                loading = displayed == null
            )

            // Duration selection (only when not running)
            if (!isRunning) {
                Text(
                    text = stringResource(R.string.duration),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        TrailSenseSpacing.xs,
                        Alignment.CenterHorizontally
                    )
                ) {
                    TrailSenseChip(
                        text = stringResource(R.string.auto),
                        selected = selectedTime == TimeSelection.Auto,
                        onClick = { selectedTime = TimeSelection.Auto }
                    )
                    TrailSenseChip(
                        text = formatService.formatDuration(Duration.ofMinutes(1), short = true),
                        selected = selectedTime == TimeSelection.LowAltitude,
                        onClick = { selectedTime = TimeSelection.LowAltitude }
                    )
                    TrailSenseChip(
                        text = formatService.formatDuration(Duration.ofMinutes(3), short = true),
                        selected = selectedTime == TimeSelection.HighAltitude,
                        onClick = { selectedTime = TimeSelection.HighAltitude }
                    )
                }
            }

            Spacer(modifier = Modifier.height(TrailSenseSpacing.xs))

            // Primary start / stop action
            if (displayed != null) {
                TrailSenseButton(
                    text = stringResource(if (isRunning) android.R.string.cancel else R.string.start),
                    onClick = {
                        if (isRunning) {
                            stop()
                        } else {
                            val selectedDuration = duration ?: Duration.ofMinutes(3)
                            cache.putInstant(
                                WATER_PURIFICATION_END_TIME_KEY,
                                Instant.now().plus(selectedDuration)
                            )
                            WaterPurificationTimerService.start(
                                context,
                                selectedDuration.seconds
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        }
    }
}

/**
 * Fraction of the selected duration still remaining (0f..1f). Used to drive the
 * circular progress ring. Returns a full ring when idle so the dial reads as
 * "ready".
 */
private fun remainingFraction(remaining: Duration?, duration: Duration?): Float {
    val total = duration?.seconds ?: return 1f
    if (total <= 0L) return 1f
    val left = remaining?.seconds ?: return 1f
    return (left.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

@Composable
private fun TimerDial(
    seconds: Long?,
    progress: Float,
    loading: Boolean
) {
    Box(
        modifier = Modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Track ring
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeWidth = 12.dp
        )
        // Active progress ring
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 12.dp
        )

        if (loading || seconds == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = seconds.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFeatureSettings = "tnum"
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.seconds),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StepsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            WaterStep(stringResource(R.string.one), stringResource(R.string.water_purification_step_1))
            WaterStep(stringResource(R.string.two), stringResource(R.string.water_purification_step_2))
            WaterStep(stringResource(R.string.three), stringResource(R.string.water_purification_step_3))
        }
    }
}

@Composable
private fun WaterStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.width(TrailSenseSpacing.lg))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getRemainingTime(cache: IPreferences): Duration? {
    val lastEndTime =
        cache.getInstant(WATER_PURIFICATION_END_TIME_KEY) ?: return null
    val duration = Duration.between(Instant.now(), lastEndTime)
    return if (!duration.isNegative && !duration.isZero) duration else null
}

internal enum class TimeSelection {
    Auto,
    LowAltitude,
    HighAltitude
}
