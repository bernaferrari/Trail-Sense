@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.settings.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.roundToInt

/** How a live reading is doing. Shown as a tone change on the hero, never as a border. */
enum class CalibrationTone { Neutral, Good, Moderate, Poor }

fun Quality.toCalibrationTone(): CalibrationTone = when (this) {
    Quality.Good -> CalibrationTone.Good
    Quality.Moderate -> CalibrationTone.Moderate
    Quality.Poor -> CalibrationTone.Poor
    else -> CalibrationTone.Neutral
}

/**
 * Back from a calibration or tool settings page, the same way the system back gesture goes (so an
 * [androidx.activity.compose.BackHandler] on the page still gets a say).
 */
@Composable
fun rememberGoBack(): () -> Unit {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    return remember(dispatcher) { { dispatcher?.onBackPressed() } }
}

/**
 * Splits a formatted reading like "1,013.2 hPa" or "-12 ft" into its number and its unit so the number can be set
 * large and the unit small beside it. Anything that isn't "number, space, unit" (like "45°" or a coordinate) stays
 * whole.
 */
fun splitReading(text: String): Pair<String, String?> {
    val match = READING_PATTERN.matchEntire(text.trim()) ?: return text to null
    return match.groupValues[1] to match.groupValues[2]
}

private val READING_PATTERN = Regex("""^([+\-−]?[\d.,  ']*\d)[\s  ]+([^\d\s].*)$""")

/**
 * The lead of a calibration page: what the sensor reads right now, large and in tabular figures, with how much to
 * trust it as a chip. The card's tone follows the status so a poor reading is noticed without a single border.
 */
@Composable
fun CalibrationHero(
    label: String,
    value: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    status: String? = null,
    tone: CalibrationTone = CalibrationTone.Neutral,
    supportingText: String? = null,
    valueStyle: TextStyle = MaterialTheme.typography.displayMedium,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val (targetContainer, targetContent) = when (tone) {
        CalibrationTone.Neutral, CalibrationTone.Good -> scheme.primaryContainer to scheme.onPrimaryContainer
        CalibrationTone.Moderate -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        CalibrationTone.Poor -> scheme.errorContainer to scheme.onErrorContainer
    }
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val container by animateColorAsState(targetContainer, effects, label = "heroContainer")
    val contentColor by animateColorAsState(targetContent, effects, label = "heroContent")
    val (number, unit) = splitReading(value)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec()),
        shape = RoundedCornerShape(TrailSenseShape.hero),
        color = container,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.12f),
                    contentColor = contentColor,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.width(TrailSenseSpacing.md))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (status != null) {
                    Spacer(Modifier.width(TrailSenseSpacing.sm))
                    CalibrationStatusChip(status, contentColor)
                }
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "$label $value"
                }
            ) {
                Text(
                    text = number,
                    style = valueStyle.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Medium
                )
                if (unit != null) {
                    Spacer(Modifier.width(TrailSenseSpacing.sm))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor.copy(alpha = 0.72f),
                        modifier = Modifier.padding(bottom = TrailSenseSpacing.sm)
                    )
                }
            }
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
            content?.invoke(this)
        }
    }
}

/** The calibration's status as one quiet line beside the value, not a pill. */
@Composable
private fun CalibrationStatusChip(text: String, contentColor: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = contentColor
    )
}

/**
 * A slider in a group: its title, the value it's set to, and an optional line on what it does. Whole numbers only,
 * without tick marks (a thousand of them read as a smear, not as steps).
 */
@Composable
fun CalibrationSliderRow(
    title: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    position: TrailSenseGroupPosition,
    valueLabel: String = value.toString(),
    summary: String? = null
) {
    TrailSenseGroupCard(
        position = position,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = TrailSenseSpacing.lg,
            end = TrailSenseSpacing.lg,
            top = TrailSenseSpacing.md,
            bottom = TrailSenseSpacing.xs
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (summary != null) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(range.first, range.last)) },
            valueRange = range.first.toFloat()..range.last.toFloat()
        )
    }
}

/**
 * The recent history of a calibrated reading (accent) over what the sensor reported before calibration (quiet), in a
 * group card. [data] is null until the first load so the card holds its space instead of flashing "no data".
 */
@Composable
fun CalibrationHistoryChart(
    data: List<Reading<Float>>?,
    raw: List<Reading<Float>>,
    margin: Float,
    minRange: Float,
    emptyText: String,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier
) {
    val line = MaterialTheme.colorScheme.primary
    val rawLine = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    TrailSenseGroupCard(position = position, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                data == null -> Unit
                data.isEmpty() -> Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> Canvas(modifier = Modifier.fillMaxSize()) {
                    val start = data.first().time
                    fun hours(time: Instant) = Duration.between(start, time).toMillis() / 3_600_000f
                    val points = data.map { hours(it.time) to it.value }
                    val rawPoints = raw.map { hours(it.time) to it.value }
                    val all = points + rawPoints
                    val minX = all.minOf { it.first }
                    val maxX = all.maxOf { it.first }
                    val yMin = all.minOf { it.second } - margin
                    val yMax = max(all.maxOf { it.second } + margin, yMin + minRange)
                    val xRange = (maxX - minX).takeIf { it > 0f } ?: 1f
                    val yRange = (yMax - yMin).takeIf { it > 0f } ?: 1f

                    fun x(value: Float) = ((value - minX) / xRange).coerceIn(0f, 1f) * size.width
                    fun y(value: Float) =
                        size.height - ((value - yMin) / yRange).coerceIn(0f, 1f) * size.height

                    repeat(4) { index ->
                        val gy = (index + 0.5f) / 4f * size.height
                        drawLine(grid, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx())
                    }

                    fun series(values: List<Pair<Float, Float>>, color: Color) {
                        if (values.size < 2) return
                        val path = Path()
                        values.forEachIndexed { index, (px, py) ->
                            if (index == 0) path.moveTo(x(px), y(py)) else path.lineTo(x(px), y(py))
                        }
                        drawPath(
                            path,
                            color,
                            style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    series(rawPoints, rawLine)
                    series(points, line)
                }
            }
        }
    }
}
