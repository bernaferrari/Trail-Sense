@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.cliffheight.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.ToolCliffHeight
import com.kylecorry.trail_sense.shared.icons.trail.BaselineStop24
import com.kylecorry.trail_sense.shared.icons.trail.BaselinePlayArrow24
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseReading
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSensePressScale
import com.kylecorry.trail_sense.tools.cliffheight.domain.CliffHeightService
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/**
 * Cliff height: the height is the hero with the fall time under it, one big start/reset button below, and the
 * how-to as a calm group at the bottom. The guide sits in the top bar.
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
    var lastFall by remember { mutableStateOf(0f) }

    LaunchedEffect(running, startTime, units, location) {
        while (running) {
            title = formatHeight(service, formatter, startTime, units, location)
            heightValue = formatHeightValue(service, startTime, units, location)
            elapsed = formatElapsed(startTime)
            lastFall = startTime?.let { Duration.between(it, Instant.now()).toMillis() / 1000f } ?: 0f
            delay(16)
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
                CliffHero(
                    running = running,
                    hasDropped = startTime != null,
                    heightValue = heightValue,
                    unit = formatter.getDistanceUnitName(units, true),
                    title = title,
                    elapsed = elapsed,
                    elapsedSeconds = startTime?.let {
                        if (running) {
                            Duration.between(it, Instant.now()).toMillis() / 1000f
                        } else {
                            null
                        }
                    } ?: lastFall
                )

                DropTimerButton(
                    running = running,
                    onPress = {
                        if (!running) {
                            startTime = Instant.now()
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

                TrailSenseRowGroup(title = stringResource(R.string.instructions)) {
                    row { position ->
                        TrailSenseGroupCard(position = position) {
                            Row(verticalAlignment = Alignment.Top) {
                                TrailSenseShapeBadge(
                                    icon = TrailIcons.ToolCliffHeight,
                                    shape = MaterialShapes.Cookie6Sided.toShape(),
                                    size = 44.dp,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(Modifier.width(TrailSenseSpacing.lg))
                                Text(
                                    text = stringResource(R.string.tool_cliff_height_disclaimer),
                                    modifier = Modifier.testTag("tide_disclaimer"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DropTimerButton(
    running: Boolean,
    onPress: suspend androidx.compose.foundation.gestures.PressGestureScope.(androidx.compose.ui.geometry.Offset) -> Unit
) {
    // Timed from the press itself (not the release), so the drop starts the instant the stone is let go.
    com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
        text = stringResource(if (running) R.string.reset else R.string.start),
        onClick = {},
        icon = if (running) TrailIcons.BaselineStop24 else TrailIcons.BaselinePlayArrow24,
        style = if (running) {
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Tonal
        } else {
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Filled
        },
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(running) {
                detectTapGestures(onPress = onPress)
            }
            .testTag("start_btn")
    )
}

/**
 * The hero: a saturated card with a cliff on its left edge and a stone falling down it in real time (free fall, so
 * it speeds up), the height large beside it and the fall time as a pill. Primary at rest, tertiary while falling.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CliffHero(
    running: Boolean,
    hasDropped: Boolean,
    heightValue: String,
    unit: String,
    title: String,
    elapsed: String,
    elapsedSeconds: Float
) {
    val tone = if (running) TrailSenseTone.Tertiary else TrailSenseTone.Primary
    val colors = trailSenseToneColors(tone)
    val container by animateColorAsState(colors.container, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "cliff")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        contentColor = colors.content
    ) {
        Row(
            modifier = Modifier
                .height(260.dp)
                .padding(end = TrailSenseSpacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CliffDrop(
                seconds = elapsedSeconds,
                ink = colors.content,
                stone = colors.accent,
                modifier = Modifier
                    .width(112.dp)
                    .fillMaxHeight()
            )
            Spacer(Modifier.width(TrailSenseSpacing.lg))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
            ) {
                TrailSenseReading(
                    value = heightValue,
                    unit = unit,
                    label = stringResource(R.string.height),
                    color = colors.content,
                    unitColor = colors.content.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.displayMedium,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .testTag("cliff_height_title")
                        .semantics { contentDescription = title }
                )
                Surface(
                    shape = CircleShape,
                    color = colors.accent,
                    contentColor = colors.onAccent
                ) {
                    Text(
                        text = when {
                            running -> stringResource(R.string.cliff_falling, elapsed)
                            hasDropped -> stringResource(R.string.cliff_fell_for, elapsed)
                            else -> stringResource(R.string.cliff_ready)
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.md, vertical = TrailSenseSpacing.sm)
                    )
                }
            }
        }
    }
}

@Composable
private fun CliffDrop(
    seconds: Float,
    ink: Color,
    stone: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val top = 34.dp.toPx()
        val bottom = size.height - 18.dp.toPx()
        val cliffWidth = size.width * 0.42f
        // The cliff: a block rising from the bottom with a ledge at the top, and the ground at its foot.
        val cliff = Path().apply {
            moveTo(0f, top)
            lineTo(cliffWidth, top)
            lineTo(cliffWidth * 0.92f, top + (bottom - top) * 0.35f)
            lineTo(cliffWidth, top + (bottom - top) * 0.62f)
            lineTo(cliffWidth * 0.88f, bottom)
            lineTo(0f, bottom)
            close()
        }
        drawPath(cliff, ink.copy(alpha = 0.16f))
        drawLine(ink.copy(alpha = 0.3f), Offset(0f, bottom), Offset(size.width, bottom), 3.dp.toPx(), StrokeCap.Round)

        // Free fall: the fraction of the drop covered grows with the square of the time (a 4 s fall fills it).
        fun yAt(t: Float): Float {
            val fraction = ((t / 4f) * (t / 4f)).coerceIn(0f, 1f)
            return top - 10.dp.toPx() + (bottom - top) * fraction
        }
        val x = cliffWidth + 24.dp.toPx()
        val radius = 10.dp.toPx()
        // Ghost stones a beat behind, so the speed shows.
        listOf(0.36f, 0.24f, 0.12f).forEachIndexed { i, lag ->
            if (seconds > lag) {
                drawCircle(stone.copy(alpha = 0.16f + i * 0.12f), radius * (0.6f + i * 0.12f), Offset(x, yAt(seconds - lag)))
            }
        }
        drawCircle(stone, radius, Offset(x, yAt(seconds).coerceAtMost(bottom - radius)))
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
