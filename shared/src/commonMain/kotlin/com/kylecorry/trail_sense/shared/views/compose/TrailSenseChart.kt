package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The width of a chart's word gutter: the labels sit here, never on the plot. */
val TrailSenseChartGutter: Dp = 56.dp

/** The inset inside the plot panel, so end dots and their halos never clip. */
val TrailSenseChartInset: Dp = 14.dp

/**
 * The family's chart frame (WiFi Weakspot's signal charts): a rounded, softly tinted plot panel, word labels
 * ([yLabels], top to bottom, e.g. High / Normal / Low) in a gutter at its left where no line can cross them, and
 * optional [xLabels] spread under it with the last one ("Now", "latest") emphasised. [plot] draws inside the panel;
 * keep [TrailSenseChartInset] free at its edges for end dots.
 */
@Composable
fun TrailSenseChartFrame(
    yLabels: List<String>,
    modifier: Modifier = Modifier,
    xLabels: List<String> = emptyList(),
    tint: Color = MaterialTheme.colorScheme.primary,
    plot: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier) {
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier
                    .width(TrailSenseChartGutter)
                    .fillMaxHeight()
                    .padding(end = TrailSenseSpacing.sm, top = TrailSenseChartInset - 8.dp, bottom = TrailSenseChartInset - 8.dp),
                verticalArrangement = if (yLabels.size > 1) Arrangement.SpaceBetween else Arrangement.Center,
                horizontalAlignment = Alignment.End
            ) {
                yLabels.forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.End
                    )
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(TrailSenseShape.groupInner * 3))
                    .background(
                        Brush.verticalGradient(
                            listOf(tint.copy(alpha = 0.10f), scheme.tertiary.copy(alpha = 0.04f))
                        )
                    ),
                content = plot
            )
        }
        if (xLabels.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = TrailSenseChartGutter, top = TrailSenseSpacing.sm),
                horizontalArrangement = if (xLabels.size > 1) Arrangement.SpaceBetween else Arrangement.End
            ) {
                xLabels.forEachIndexed { index, label ->
                    val last = index == xLabels.lastIndex
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = if (last) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (last) tint else scheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * A smooth line through [points] (sorted by x) that never overshoots them: a monotone cubic (Fritsch–Carlson), so
 * a steady reading stays flat and a peak stays where it was measured.
 */
fun trailSenseSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    if (points.size == 2) {
        path.lineTo(points[1].x, points[1].y)
        return path
    }
    val n = points.size
    val slopes = FloatArray(n - 1) { i ->
        val dx = points[i + 1].x - points[i].x
        if (dx == 0f) 0f else (points[i + 1].y - points[i].y) / dx
    }
    val tangents = FloatArray(n)
    tangents[0] = slopes[0]
    tangents[n - 1] = slopes[n - 2]
    for (i in 1 until n - 1) {
        tangents[i] = if (slopes[i - 1] * slopes[i] <= 0f) 0f else (slopes[i - 1] + slopes[i]) / 2f
    }
    for (i in 0 until n - 1) {
        if (slopes[i] == 0f) {
            tangents[i] = 0f
            tangents[i + 1] = 0f
            continue
        }
        val a = tangents[i] / slopes[i]
        val b = tangents[i + 1] / slopes[i]
        val h = a * a + b * b
        if (h > 9f) {
            val t = 3f / kotlin.math.sqrt(h)
            tangents[i] = t * a * slopes[i]
            tangents[i + 1] = t * b * slopes[i]
        }
    }
    for (i in 0 until n - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val dx = (p1.x - p0.x) / 3f
        path.cubicTo(p0.x + dx, p0.y + tangents[i] * dx, p1.x - dx, p1.y - tangents[i + 1] * dx, p1.x, p1.y)
    }
    return path
}

/** The area under [line] down to [bottom], between [startX] and [endX], for a gradient wash under a series. */
fun trailSenseAreaUnder(line: Path, startX: Float, endX: Float, bottom: Float): Path = Path().apply {
    addPath(line)
    lineTo(endX, bottom)
    lineTo(startX, bottom)
    close()
}

/**
 * "Now" (or a selected point): WiFi Weakspot's inspect marker. A thin vertical line across the plot, from [top] to
 * [bottom], and a small dot where it meets the line, with no halo.
 */
fun DrawScope.drawTrailSenseNowDot(center: Offset, color: Color, ring: Color, top: Float = 0f, bottom: Float = size.height) {
    drawLine(color.copy(alpha = 0.65f), Offset(center.x, top), Offset(center.x, bottom), strokeWidth = 1.dp.toPx())
    drawCircle(ring, radius = 6.dp.toPx(), center = center)
    drawCircle(color, radius = 4.5.dp.toPx(), center = center)
}
