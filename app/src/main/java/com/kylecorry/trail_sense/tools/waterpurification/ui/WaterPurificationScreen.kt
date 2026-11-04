@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.waterpurification.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseReading
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

const val WATER_PURIFICATION_END_TIME_KEY = "water_purification_start_time"

/**
 * The boil timer: the countdown is the hero, the duration choice and the one action sit right under it, and the
 * three steps follow as a reference group. The duration choice folds away while the timer runs.
 */
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

    val displayed = remaining ?: duration
    // Full ring when idle; while running it empties as the time runs out.
    val animatedProgress by animateFloatAsState(
        targetValue = remainingFraction(remaining, duration),
        animationSpec = TrailSenseAnimation.standardSpring,
        label = "purificationProgress"
    )
    val durationOptions = listOf(
        TrailSenseChoice(TimeSelection.Auto, stringResource(R.string.auto)),
        TrailSenseChoice(
            TimeSelection.LowAltitude,
            formatService.formatDuration(Duration.ofMinutes(1), short = true)
        ),
        TrailSenseChoice(
            TimeSelection.HighAltitude,
            formatService.formatDuration(Duration.ofMinutes(3), short = true)
        )
    )

    TrailSensePage(
        title = stringResource(R.string.water_boil_timer),
        onBack = rememberTrailSenseBack(),
        backLabel = stringResource(R.string.back)
    ) {
        item("dial") {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimerDial(
                    seconds = displayed?.seconds,
                    progress = animatedProgress
                )
            }
        }

        item("duration") {
            AnimatedVisibility(
                visible = !isRunning,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                TrailSenseConnectedChoice(
                    options = durationOptions,
                    selected = selectedTime,
                    onSelect = { selectedTime = it },
                    label = stringResource(R.string.duration)
                )
            }
        }

        item("action") {
            // Holds its place while the duration is worked out, so nothing jumps when it appears.
            TrailSenseButton(
                text = stringResource(if (isRunning) android.R.string.cancel else R.string.start),
                enabled = displayed != null,
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

        trailSenseSection(key = "steps", title = context.getString(R.string.steps)) {
            row { position ->
                WaterStep(position, context.getString(R.string.one), context.getString(R.string.water_purification_step_1))
            }
            row { position ->
                WaterStep(position, context.getString(R.string.two), context.getString(R.string.water_purification_step_2))
            }
            row { position ->
                WaterStep(position, context.getString(R.string.three), context.getString(R.string.water_purification_step_3))
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
    progress: Float
) {
    Box(
        modifier = Modifier
            .padding(vertical = TrailSenseSpacing.lg)
            .size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeWidth = 12.dp
        )
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 12.dp
        )
        // The dash holds the number's place while the auto duration reads the altimeter.
        TrailSenseReading(
            value = seconds?.toString() ?: stringResource(R.string.dash),
            label = stringResource(R.string.seconds)
        )
    }
}

@Composable
private fun WaterStep(
    position: com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition,
    number: String,
    text: String
) {
    TrailSenseGroupCard(
        position = position,
        contentPadding = PaddingValues(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
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
