@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.cliffheight.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.icons.trail.BaselinePlayArrow24
import com.kylecorry.trail_sense.shared.icons.trail.BaselineStop24
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.cliffheight.domain.CliffHeightService
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils

/**
 * Cliff height, in the boiler's shape: the cliff itself is the object. A stone falls down its face in real time
 * (free fall, so it speeds up) and a measuring line grows from the ledge behind it, with the height large in the
 * open air beside the cliff. When the stone lands, a ring of dust spreads on the ground. Under it sit one status
 * line, one button (Start, Stop, Again) and the safety note as one quiet line. The guide sits in the top bar.
 */
@Composable
internal fun CliffHeightScreen(
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val service = remember { CliffHeightService() }
    val formatter = remember { FormatService.getInstance(context) }
    val prefs = remember { UserPreferences(context) }
    val gps = remember { SensorService(context).getGPS() }
    val units = remember { prefs.baseDistanceUnits }
    val location = remember { gps.location.takeIf { it != Coordinate.zero } }
    val singlePressDuration = remember { Duration.ofMillis(200) }
    var startTime by remember { mutableStateOf<Instant?>(null) }
    var running by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var heightValue by remember { mutableStateOf("--") }
    var elapsed by remember { mutableStateOf("") }
    var fall by remember { mutableFloatStateOf(0f) }
    val impact = remember { Animatable(0f) }

    LaunchedEffect(running, startTime, units, location) {
        while (running) {
            title = formatHeight(service, formatter, startTime, units, location)
            heightValue = formatHeightValue(service, startTime, units, location)
            elapsed = formatElapsed(startTime)
            fall = startTime?.let { Duration.between(it, Instant.now()).toMillis() / 1000f } ?: 0f
            delay(16)
        }
    }

    // The landing: dust spreads once from where the stone hit.
    LaunchedEffect(running) {
        if (!running && startTime != null) {
            impact.snapTo(0f)
            impact.animateTo(1f, tween(700))
        } else {
            impact.snapTo(0f)
        }
    }

    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.tool_cliff_height_title),
        modifier = modifier,
        actions = {
            IconButton(onClick = onOpenGuide) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = stringResource(R.string.instructions)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                CliffWell(
                    running = running,
                    seconds = fall,
                    impact = impact.value,
                    heightValue = heightValue,
                    unit = formatter.getDistanceUnitName(units, true),
                    title = title
                )

                // The verdict, right under the cliff.
                Text(
                    text = when {
                        running -> stringResource(R.string.cliff_falling, elapsed)
                        startTime != null -> stringResource(R.string.cliff_fell_for, elapsed)
                        else -> stringResource(R.string.cliff_ready)
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                DropTimerButton(
                    running = running,
                    hasDropped = startTime != null,
                    onPress = {
                        if (!running) {
                            startTime = Instant.now()
                            fall = 0f
                            running = true
                            val released = tryAwaitRelease()
                            if (released &&
                                Duration.between(startTime, Instant.now()) > singlePressDuration
                            ) {
                                running = false
                            }
                        } else {
                            running = false
                        }
                    }
                )

                Text(
                    text = stringResource(R.string.tool_cliff_height_disclaimer),
                    modifier = Modifier
                        .padding(horizontal = TrailSenseSpacing.sm)
                        .testTag("tide_disclaimer"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DropTimerButton(
    running: Boolean,
    hasDropped: Boolean,
    onPress: suspend PressGestureScope.(Offset) -> Unit
) {
    // Timed from the press itself (not the release), so the drop starts the instant the stone is let go.
    TrailSenseButton(
        text = stringResource(
            when {
                running -> R.string.stop
                hasDropped -> R.string.cliff_again
                else -> R.string.start
            }
        ),
        onClick = {},
        icon = if (running) TrailIcons.BaselineStop24 else TrailIcons.BaselinePlayArrow24,
        style = if (running) TrailSenseButtonStyle.Tonal else TrailSenseButtonStyle.Filled,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(running) {
                detectTapGestures(onPress = onPress)
            }
            .testTag("start_btn")
    )
}

/**
 * The object: the cliff in a rounded well. The face rises on the left with the ground at its foot; the stone falls
 * beside it and a primary measuring line with ticks grows from the ledge down to it, so the height is drawn, not
 * only stated. The number sits in the open air on the right. The well turns tertiary-tinted while the stone falls.
 */
@Composable
private fun CliffWell(
    running: Boolean,
    seconds: Float,
    impact: Float,
    heightValue: String,
    unit: String,
    title: String
) {
    val scheme = MaterialTheme.colorScheme
    val air by animateColorAsState(
        if (running) scheme.tertiaryContainer.copy(alpha = 0.35f) else scheme.surfaceContainerLow,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "air"
    )
    val rock = scheme.surfaceContainerHighest
    val ground = scheme.secondaryContainer
    val stone by animateColorAsState(
        if (running) scheme.tertiary else scheme.primary,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "stone"
    )
    val tape = scheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(MaterialTheme.shapes.extraLarge),
        contentAlignment = Alignment.CenterEnd
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(air)
            val top = size.height * 0.16f
            val bottom = size.height * 0.86f
            val cliffWidth = size.width * 0.3f
            val drop = bottom - top
            // The cliff: a rocky face from the ledge down to the ground.
            val cliff = Path().apply {
                moveTo(0f, top)
                lineTo(cliffWidth, top)
                lineTo(cliffWidth * 0.9f, top + drop * 0.22f)
                lineTo(cliffWidth * 0.97f, top + drop * 0.45f)
                lineTo(cliffWidth * 0.86f, top + drop * 0.7f)
                lineTo(cliffWidth * 0.93f, bottom)
                lineTo(0f, bottom)
                close()
            }
            drawPath(cliff, rock)
            drawRect(ground, topLeft = Offset(0f, bottom), size = androidx.compose.ui.geometry.Size(size.width, size.height - bottom))

            // Free fall: the fraction of the drop covered grows with the square of the time (a 4 s fall fills it).
            fun yAt(t: Float): Float {
                val fraction = ((t / 4f) * (t / 4f)).coerceIn(0f, 1f)
                return top + drop * fraction
            }
            val radius = 11.dp.toPx()
            val x = cliffWidth + 22.dp.toPx()
            val stoneY = (yAt(seconds) - radius).coerceIn(top - radius, bottom - radius)

            // The measuring line: from the ledge to the stone, with a tick every tenth of the drop.
            val tapeX = x + radius + 12.dp.toPx()
            if (seconds > 0f) {
                drawLine(tape, Offset(tapeX, top), Offset(tapeX, stoneY + radius), 3.dp.toPx(), StrokeCap.Round)
                drawLine(tape, Offset(tapeX - 7.dp.toPx(), top), Offset(tapeX + 7.dp.toPx(), top), 3.dp.toPx(), StrokeCap.Round)
                var tick = top + drop / 10f
                while (tick < stoneY + radius) {
                    drawLine(tape.copy(alpha = 0.6f), Offset(tapeX, tick), Offset(tapeX + 6.dp.toPx(), tick), 2.dp.toPx(), StrokeCap.Round)
                    tick += drop / 10f
                }
            }

            // Ghost stones a beat behind, so the speed shows.
            if (running) {
                listOf(0.36f, 0.24f, 0.12f).forEachIndexed { i, lag ->
                    if (seconds > lag) {
                        drawCircle(
                            stone.copy(alpha = 0.16f + i * 0.12f),
                            radius * (0.6f + i * 0.12f),
                            Offset(x, (yAt(seconds - lag) - radius).coerceAtMost(bottom - radius))
                        )
                    }
                }
            }
            drawCircle(stone, radius, Offset(x, stoneY))

            // The landing: a ring of dust spreads and fades on the ground.
            if (impact > 0f && impact < 1f) {
                drawCircle(
                    stone.copy(alpha = 0.5f * (1f - impact)),
                    radius * (1f + impact * 2.5f),
                    Offset(x, stoneY),
                    style = Stroke(2.dp.toPx())
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth(0.52f)
                .padding(end = TrailSenseSpacing.xl)
                .testTag("cliff_height_title")
                .semantics(mergeDescendants = true) { contentDescription = title },
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = stringResource(R.string.height),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurfaceVariant
            )
            Text(
                text = heightValue,
                style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                maxLines = 1
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

private fun formatHeight(
    service: CliffHeightService,
    formatter: FormatService,
    startTime: Instant?,
    units: DistanceUnits,
    location: Coordinate?
): String {
    val start = startTime ?: return ""
    val height = service.getCliffHeight(start, Instant.now(), location)
    val converted = height.convertTo(units)
    return formatter.formatDistance(converted, 2)
}

private fun formatHeightValue(
    service: CliffHeightService,
    startTime: Instant?,
    units: DistanceUnits,
    location: Coordinate?
): String {
    val start = startTime ?: return "--"
    val height = service.getCliffHeight(start, Instant.now(), location)
    return DecimalFormatter.format(height.convertTo(units).value, 2)
}

private fun formatElapsed(startTime: Instant?): String {
    val start = startTime ?: return ""
    val millis = Duration.between(start, Instant.now()).toMillis().coerceAtLeast(0)
    return String.format("%.1fs", millis / 1000f)
}

/** The cliff height tool, which shows its disclaimer once on the way in. */
@Composable
fun CliffHeightToolScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    LaunchedEffect(Unit) {
        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.disclaimer_message_title),
            context.getString(R.string.tool_cliff_height_disclaimer),
            "cache_dialog_tool_cliff_height",
            considerShownIfCancelled = true,
            cancelText = null
        )
    }
    CliffHeightScreen(
        onOpenGuide = {
            UserGuideUtils.showGuide(
                context,
                navigator,
                R.raw.guide_tool_cliff_height
            )
        }
    )
}
