@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.waterpurification.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.icons.trail.BaselinePlayArrow24
import com.kylecorry.trail_sense.shared.icons.trail.BaselineStop24
import com.kylecorry.trail_sense.shared.icons.trail.Check
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import kotlin.math.PI
import kotlin.math.sin

const val WATER_PURIFICATION_END_TIME_KEY = "water_purification_start_time"

/**
 * The boil timer: a pot of water is the hero. A thick wavy ring and the water inside it drain together as the
 * countdown runs, bubbles rise while it boils, the duration choice and one tall action sit under it, and the three
 * steps follow with the next one highlighted (all ticked once the timer runs).
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
    // Full when idle; while running it empties as the time runs out.
    val animatedProgress by animateFloatAsState(
        targetValue = remainingFraction(remaining, duration),
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
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
                BoilPot(
                    seconds = displayed?.seconds,
                    progress = animatedProgress,
                    isRunning = isRunning
                )
            }
        }

        item("duration") {
            AnimatedVisibility(
                visible = !isRunning,
                enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
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
            val scheme = MaterialTheme.colorScheme
            // The one action on the screen is warm; cancelling is a quiet secondary.
            val container by animateColorAsState(
                if (isRunning) scheme.secondaryContainer else scheme.tertiary,
                MaterialTheme.motionScheme.defaultEffectsSpec(),
                label = "action"
            )
            Button(
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
                shapes = ButtonDefaults.shapesFor(ButtonDefaults.LargeContainerHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = container,
                    contentColor = if (isRunning) scheme.onSecondaryContainer else scheme.onTertiary
                ),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
            ) {
                Icon(
                    if (isRunning) TrailIcons.BaselineStop24 else TrailIcons.BaselinePlayArrow24,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(TrailSenseSpacing.sm))
                Text(
                    stringResource(if (isRunning) android.R.string.cancel else R.string.start),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        trailSenseSection(key = "steps", title = context.getString(R.string.steps)) {
            val steps = listOf(
                context.getString(R.string.one) to context.getString(R.string.water_purification_step_1),
                context.getString(R.string.two) to context.getString(R.string.water_purification_step_2),
                context.getString(R.string.three) to context.getString(R.string.water_purification_step_3)
            )
            steps.forEachIndexed { index, (number, text) ->
                row { position ->
                    WaterStep(
                        position = position,
                        number = number,
                        text = text,
                        state = when {
                            isRunning -> StepState.Done
                            index == steps.lastIndex -> StepState.Next
                            else -> StepState.Todo
                        },
                        index = index
                    )
                }
            }
        }
    }
}

/**
 * Fraction of the selected duration still remaining (0f..1f). Used to drive the ring and the water. Returns full
 * when idle so the pot reads as "ready".
 */
private fun remainingFraction(remaining: Duration?, duration: Duration?): Float {
    val total = duration?.seconds ?: return 1f
    if (total <= 0L) return 1f
    val left = remaining?.seconds ?: return 1f
    return (left.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

@Composable
private fun BoilPot(
    seconds: Long?,
    progress: Float,
    isRunning: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "boil")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (isRunning) 1400 else 4200, easing = LinearEasing), RepeatMode.Restart),
        label = "wave"
    )
    // Light water under dark digits: the number always reads, whatever the level.
    val ring = scheme.primary
    val ringColor by animateColorAsState(ring, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "ring")
    val water = if (isRunning) scheme.tertiaryContainer else scheme.primaryContainer
    val waterColor by animateColorAsState(water, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "water")
    val face = scheme.surfaceContainerLow
    val faceColor by animateColorAsState(face, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "face")
    val ink = if (isRunning) scheme.onTertiaryContainer else scheme.onPrimaryContainer
    val inkColor by animateColorAsState(ink, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "ink")

    Box(
        modifier = Modifier
            .padding(vertical = TrailSenseSpacing.sm)
            .size(288.dp)
            .testTag("water_boil_dial"),
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = ringColor,
            trackColor = scheme.surfaceVariant,
            stroke = Stroke(width = with(androidx.compose.ui.platform.LocalDensity.current) { 12.dp.toPx() }, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            trackStroke = Stroke(width = with(androidx.compose.ui.platform.LocalDensity.current) { 12.dp.toPx() }, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            wavelength = 36.dp
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Surface(color = faceColor, modifier = Modifier.fillMaxSize()) {}
            // The water: its surface rolls gently when idle and churns while it boils, and it drains with the timer.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val level = size.height * (1f - (0.18f + 0.6f * progress))
                val amplitude = if (isRunning) 7.dp.toPx() else 4.dp.toPx()
                val wavelength = size.width / 1.5f
                val path = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(0f, level)
                    var x = 0f
                    while (x <= size.width) {
                        val y = level + amplitude * sin((x / wavelength + phase) * 2 * PI).toFloat()
                        lineTo(x, y)
                        x += 4f
                    }
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(path, waterColor.copy(alpha = 0.6f))
                // A second, offset wave in front for depth.
                val front = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(0f, level + amplitude)
                    var x = 0f
                    while (x <= size.width) {
                        val y = level + amplitude * 1.6f +
                            amplitude * sin((x / wavelength - phase + 0.35f) * 2 * PI).toFloat()
                        lineTo(x, y)
                        x += 4f
                    }
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(front, waterColor)
                if (isRunning && (seconds ?: Long.MAX_VALUE) <= 10L) {
                    // The last ten seconds: bubbles rise through the water.
                    listOf(0.16f, 0.3f, 0.44f, 0.58f, 0.72f, 0.86f).forEachIndexed { i, fx ->
                        val t = (phase + i * 0.27f) % 1f
                        val y = size.height - (size.height - level) * t
                        drawCircle(
                            ringColor.copy(alpha = 0.45f * (1f - t)),
                            radius = (3f + i % 2 * 2f).dp.toPx(),
                            center = Offset(size.width * fx, y)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(if (isRunning) R.string.water_boil_left else R.string.water_boil_for),
                    style = MaterialTheme.typography.titleSmall,
                    color = inkColor.copy(alpha = 0.8f)
                )
                Text(
                    text = seconds?.let { "%d:%02d".format(it / 60, it % 60) } ?: stringResource(R.string.dash),
                    style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = inkColor
                )
            }
        }
    }
}

private enum class StepState { Todo, Next, Done }

@Composable
private fun WaterStep(
    position: TrailSenseGroupPosition,
    number: String,
    text: String,
    state: StepState,
    index: Int = 0
) {
    val scheme = MaterialTheme.colorScheme
    val status = trailSenseStatusColors()
    val rowColor by animateColorAsState(
        when (state) {
            StepState.Next -> scheme.primaryContainer
            StepState.Done -> scheme.surfaceContainerLow
            StepState.Todo -> scheme.surfaceContainerLow
        },
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "stepRow"
    )
    TrailSenseGroupCard(
        position = position,
        color = rowColor,
        contentPadding = PaddingValues(horizontal = TrailSenseSpacing.lg, vertical = 0.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(contentAlignment = Alignment.Center) {
            // The rail: a line through the badges joins the steps into one sequence.
            val rail = scheme.outlineVariant
            Canvas(Modifier.size(width = 44.dp, height = 72.dp)) {
                val x = size.width / 2f
                // Only between badges, and inside the row: the 2 dp gaps break it like stitches.
                val badgeHalf = 22.dp.toPx()
                if (!position.isFirst) {
                    drawLine(rail, Offset(x, 0f), Offset(x, size.height / 2f - badgeHalf), 3.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
                }
                if (!position.isLast) {
                    drawLine(rail, Offset(x, size.height / 2f + badgeHalf), Offset(x, size.height), 3.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
                }
            }
            when (state) {
                StepState.Done -> TrailSenseShapeBadge(
                    icon = TrailIcons.Check,
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    size = 44.dp,
                    containerColor = status.goodContainer,
                    contentColor = status.onGoodContainer
                )

                StepState.Next -> TrailSenseShapeBadge(
                    text = number,
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    size = 44.dp
                )

                StepState.Todo -> TrailSenseShapeBadge(
                    text = number,
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    size = 44.dp,
                    containerColor = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer
                )
            }
          }
            Spacer(modifier = Modifier.width(TrailSenseSpacing.lg))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (state == StepState.Next) FontWeight.SemiBold else FontWeight.Normal,
                color = if (state == StepState.Next) scheme.onPrimaryContainer else scheme.onSurface
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
