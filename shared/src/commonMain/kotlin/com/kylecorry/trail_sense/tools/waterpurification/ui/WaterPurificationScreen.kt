@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.waterpurification.ui

import com.kylecorry.trail_sense.shared.text.SystemString
import com.kylecorry.trail_sense.shared.text.systemString

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.main.getAppService
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

import com.kylecorry.andromeda.core.sensors.read

import com.kylecorry.andromeda.core.sensors.Altimeter
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularWavyProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.preferences.Preferences
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
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimer
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.math.PI
import kotlin.math.sin
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.waterpurification.domain.WaterService
import kotlinx.coroutines.withTimeoutOrNull

const val WATER_PURIFICATION_END_TIME_KEY = "water_purification_start_time"

/**
 * The boil timer: a pot of water is the hero. A thick wavy ring and the water inside it drain together as the
 * countdown runs, bubbles rise while it boils, the duration choice and one action sit under it, and the three
 * steps follow with the next one highlighted (all ticked once the timer runs).
 */
@Composable
internal fun WaterPurificationScreen(
    cache: Preferences,
    formatService: FormatService,
    getSelectedDuration: suspend (TimeSelection) -> Duration
) {
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
        getAppService<WaterPurificationTimer>().stop()
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
        TrailSenseChoice(TimeSelection.Auto, appString(Res.string.auto)),
        TrailSenseChoice(
            TimeSelection.LowAltitude,
            formatService.formatDuration(1.minutes, short = true)
        ),
        TrailSenseChoice(
            TimeSelection.HighAltitude,
            formatService.formatDuration(3.minutes, short = true)
        )
    )

    // One question: how much longer does the water need to boil?
    TrailSensePage(
        title = appString(Res.string.water_boil_timer),
        onBack = rememberTrailSenseBack(),
        backLabel = appString(Res.string.back)
    ) {
        item("dial") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BoilPot(
                    seconds = displayed?.inWholeSeconds,
                    progress = animatedProgress,
                    isRunning = isRunning
                )
                // The verdict, right under the pot.
                Text(
                    text = appString(if (isRunning) Res.string.water_boil_verdict_running else Res.string.water_boil_verdict_idle
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = TrailSenseSpacing.lg)
                )
            }
        }

        // The controls as one block: the duration choice and the action, close together under the verdict.
        item("controls") {
          Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)) {
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
                    label = appString(Res.string.duration)
                )
            }

            // The one action on the screen is primary; cancelling is a quiet tonal secondary.
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
                text = if (isRunning) systemString(SystemString.Cancel) else appString(Res.string.start),
                enabled = displayed != null,
                icon = if (isRunning) TrailIcons.BaselineStop24 else TrailIcons.BaselinePlayArrow24,
                style = if (isRunning) {
                    com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Tonal
                } else {
                    com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Filled
                },
                onClick = {
                    if (isRunning) {
                        stop()
                    } else {
                        val selectedDuration = duration ?: 3.minutes
                        cache.putInstant(
                            WATER_PURIFICATION_END_TIME_KEY,
                            kotlin.time.Clock.System.now().plus(selectedDuration)
                        )
                        getAppService<WaterPurificationTimer>().start(selectedDuration.inWholeSeconds)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
          }
        }

        trailSenseSection(key = "steps", title = getAppService<AppStrings>().get(Res.string.steps)) {
            val steps = listOf(
                getAppService<AppStrings>().get(Res.string.one) to getAppService<AppStrings>().get(Res.string.water_purification_step_1),
                getAppService<AppStrings>().get(Res.string.two) to getAppService<AppStrings>().get(Res.string.water_purification_step_2),
                getAppService<AppStrings>().get(Res.string.three) to getAppService<AppStrings>().get(Res.string.water_purification_step_3)
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
    val total = duration?.inWholeSeconds ?: return 1f
    if (total <= 0L) return 1f
    val left = remaining?.inWholeSeconds ?: return 1f
    return (left.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

@Composable
private fun BoilPot(
    seconds: Long?,
    progress: Float,
    isRunning: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    // The water only moves while it boils; idle, it lies still.
    val transition = rememberInfiniteTransition(label = "boil")
    val rolling by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "wave"
    )
    val phase = if (isRunning) rolling else 0f
    // Light water under dark digits: the number always reads, whatever the level.
    val ring = scheme.primary
    val ringColor by animateColorAsState(ring, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "ring")
    val water = scheme.primaryContainer
    val waterColor by animateColorAsState(water, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "water")
    val face = scheme.surfaceContainerLow
    val faceColor by animateColorAsState(face, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "face")
    val ink = scheme.onPrimaryContainer
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
            // The water: still when idle, churning while it boils, and it drains with the timer.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val level = size.height * (1f - (0.18f + 0.6f * progress))
                val amplitude = if (isRunning) 7.dp.toPx() else 3.dp.toPx()
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
                // A thin line along the surface so the water reads as liquid, not a flat disc.
                val surface = Path().apply {
                    var x = 0f
                    moveTo(0f, level + amplitude * 1.6f + amplitude * sin((0.35f - phase) * 2 * PI).toFloat())
                    while (x <= size.width) {
                        val y = level + amplitude * 1.6f +
                            amplitude * sin((x / wavelength - phase + 0.35f) * 2 * PI).toFloat()
                        lineTo(x, y)
                        x += 4f
                    }
                }
                drawPath(surface, ringColor.copy(alpha = 0.5f), style = Stroke(width = 2.dp.toPx()))
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
                    text = appString(if (isRunning) Res.string.water_boil_left else Res.string.water_boil_for),
                    style = MaterialTheme.typography.titleSmall,
                    color = inkColor.copy(alpha = 0.8f)
                )
                if (seconds == null) {
                    // Auto is still finding the elevation: a calm loading shape instead of a dash.
                    androidx.compose.material3.LoadingIndicator(
                        color = inkColor,
                        modifier = Modifier.size(64.dp)
                    )
                } else {
                    Text(
                        text = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = inkColor
                    )
                }
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
    TrailSenseGroupCard(
        position = position,
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
                    containerColor = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer
                )

                StepState.Next -> TrailSenseShapeBadge(
                    text = number,
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    size = 44.dp,
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer
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
                color = scheme.onSurface
            )
        }
    }
}

private fun getRemainingTime(cache: Preferences): Duration? {
    val lastEndTime =
        cache.getInstant(WATER_PURIFICATION_END_TIME_KEY) ?: return null
    val duration = (lastEndTime - (Clock.System.now()))
    return if (!duration.isNegative() && duration != Duration.ZERO) duration else null
}

internal enum class TimeSelection {
    Auto,
    LowAltitude,
    HighAltitude
}

/** The water boil timer: auto picks the time from the altitude, falling back to three minutes. */
@Composable
fun WaterPurificationToolScreen() {
    val altimeter = remember { getAppService<SensorService>().getAltimeter() }
    val formatService = remember { FormatService.getInstance() }
    val cache = remember { getAppService<Preferences>() }
    val boilTime = remember(altimeter) { BoilTime(altimeter) }
    WaterPurificationScreen(
        cache = cache,
        formatService = formatService,
        getSelectedDuration = boilTime::getDuration
    )
}

/** How long to boil for a selection: auto reads the altitude (up to 10 s), otherwise three minutes. */
private class BoilTime(private val altimeter: Altimeter) {
    private val waterService = WaterService()

    suspend fun getDuration(selection: TimeSelection): Duration = onDefault {
        if (selection == TimeSelection.Auto) {
            if (!altimeter.hasValidReading) {
                withTimeoutOrNull(10.seconds.inWholeMilliseconds) {
                    altimeter.read()
                }
            }

            if (altimeter.hasValidReading) {
                return@onDefault waterService.getPurificationTime(
                    Distance.from(altimeter.altitude, DistanceUnits.Meters)
                )
            }
        } else if (selection == TimeSelection.LowAltitude) {
            return@onDefault 1.minutes
        }

        3.minutes
    }
}
