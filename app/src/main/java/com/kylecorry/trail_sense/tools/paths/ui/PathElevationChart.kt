package com.kylecorry.trail_sense.tools.paths.ui

import androidx.annotation.ColorInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import kotlin.math.absoluteValue
import kotlin.math.max

/**
 * A path's elevation profile: elevation over distance, the area under each segment tinted by how steep it is, with
 * [highlighted] ringed. A tap near a point reports it through [onPointClick]. [path] is in walking order.
 */
@Composable
fun PathElevationChart(
    path: List<PathPoint>,
    @ColorInt color: Int,
    highlighted: PathPoint?,
    baseDistanceUnits: DistanceUnits,
    formatter: FormatService,
    onPointClick: (PathPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val plot = remember(path, color, baseDistanceUnits) { ElevationPlot.from(path, color, baseDistanceUnits) }
    val highlight = remember(plot, highlighted) { highlighted?.let { plot.positionOf(it) } }
    val currentOnPointClick = rememberUpdatedState(onPointClick)

    // Theme colors so the chart sits calmly in its group card: quiet grid, muted labels, solid highlight ring.
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val highlightColor = MaterialTheme.colorScheme.onSurface
    val highlightRing = MaterialTheme.colorScheme.surfaceContainerLow
    val status = trailSenseStatusColors()
    val steepColors = mapOf(
        Steepness.Low to status.good,
        Steepness.Medium to status.caution,
        Steepness.High to MaterialTheme.colorScheme.error
    )
    val noData = stringResource(R.string.no_data)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = textColor, fontSize = 10.sp)
    val noDataStyle = TextStyle(color = textColor, fontSize = 14.sp)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(plot) {
                detectTapGestures { offset ->
                    plot.findNearestPoint(this, offset, size.width.toFloat(), size.height.toFloat())?.let {
                        currentOnPointClick.value(it)
                    }
                }
            }
    ) {
        val chartPoints = plot.elevations
        if (chartPoints.isEmpty()) {
            drawLabel(textMeasurer, noData, noDataStyle, size.width / 2f, size.height / 2f, LabelAlign.Center)
            return@Canvas
        }

        val bounds = plot.bounds(this, size.width, size.height)

        repeat(3) { idx ->
            val fraction = idx / 2f
            val y = bounds.bottom - (bounds.bottom - bounds.top) * fraction
            val value = plot.yRange.start + (plot.yRange.end - plot.yRange.start) * fraction
            drawLine(
                gridColor,
                Offset(bounds.left, y),
                Offset(bounds.right, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx())),
                cap = StrokeCap.Round
            )
            drawLabel(
                textMeasurer,
                formatElevation(formatter, baseDistanceUnits, value),
                labelStyle,
                bounds.left - 6.dp.toPx(),
                y + 3.dp.toPx(),
                LabelAlign.Right
            )
        }

        repeat(4) { idx ->
            val fraction = idx / 3f
            val x = bounds.left + (bounds.right - bounds.left) * fraction
            val value = bounds.xRange.start + (bounds.xRange.end - bounds.xRange.start) * fraction
            drawLabel(
                textMeasurer,
                formatDistance(formatter, baseDistanceUnits, value),
                labelStyle,
                x,
                size.height - 8.dp.toPx(),
                LabelAlign.Center
            )
        }

        chartPoints.zipWithNext().forEach { (start, end) ->
            val p1 = plot.toScreen(start.position, bounds)
            val p2 = plot.toScreen(end.position, bounds)
            val area = Path().apply {
                moveTo(p1.x, bounds.bottom)
                lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p2.x, bounds.bottom)
                close()
            }
            drawPath(
                area,
                (steepColors[end.steepness] ?: Color(end.steepness.color)).copy(alpha = 0.32f)
            )
            drawLine(
                Color(start.lineColor),
                p1,
                p2,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        highlight?.let {
            val point = plot.toScreen(it, bounds)
            drawCircle(color = highlightRing, radius = 9.dp.toPx(), center = point)
            drawCircle(color = highlightColor, radius = 6.dp.toPx(), center = point)
        }
    }
}

private enum class LabelAlign {
    Center,
    Right
}

/** Draws [text] with its baseline at [y], centred on or ending at [x]. */
private fun DrawScope.drawLabel(
    textMeasurer: TextMeasurer,
    text: String,
    style: TextStyle,
    x: Float,
    y: Float,
    align: LabelAlign
) {
    val layout = textMeasurer.measure(text, style)
    val left = when (align) {
        LabelAlign.Center -> x - layout.size.width / 2f
        LabelAlign.Right -> x - layout.size.width
    }
    drawText(layout, topLeft = Offset(left, y - layout.firstBaseline))
}

private fun formatElevation(formatter: FormatService, units: DistanceUnits, value: Float): String {
    val distance = Distance.from(value, DistanceUnits.Meters).convertTo(units)
    return formatter.formatDistance(
        distance,
        Units.getDecimalPlaces(distance.units),
        false
    )
}

private fun formatDistance(formatter: FormatService, units: DistanceUnits, value: Float): String {
    val distance = Distance.from(value, DistanceUnits.Meters)
        .convertTo(units)
        .toRelativeDistance()
    return formatter.formatDistance(
        distance,
        Units.getDecimalPlaces(distance.units),
        false
    )
}

private class ChartBounds(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
    val xRange: Range<Float>
)

/** The plotted points of a path: distance along it against elevation, and which path point each came from. */
private class ElevationPlot(
    private val path: List<PathPoint>,
    private val elevationIndex: List<Int>,
    val elevations: List<ElevationPoint>,
    val yRange: Range<Float>
) {

    fun positionOf(point: PathPoint): Vector2? {
        val idx = path.indexOf(point)
        if (idx == -1) {
            return null
        }
        val pointIndex = elevationIndex.indexOf(idx)
        if (pointIndex == -1) {
            return null
        }
        return elevations.getOrNull(pointIndex)?.position
    }

    fun bounds(density: Density, width: Float, height: Float): ChartBounds = with(density) {
        ChartBounds(
            left = 58.dp.toPx(),
            right = width - 12.dp.toPx(),
            top = 12.dp.toPx(),
            bottom = height - 28.dp.toPx(),
            xRange = Range(0f, elevations.maxOfOrNull { it.position.x }?.coerceAtLeast(1f) ?: 1f)
        )
    }

    fun toScreen(point: Vector2, bounds: ChartBounds): Offset {
        val xRange = bounds.xRange
        val x = bounds.left + ((point.x - xRange.start) / (xRange.end - xRange.start)) * (bounds.right - bounds.left)
        val y = bounds.bottom - ((point.y - yRange.start) / (yRange.end - yRange.start)) * (bounds.bottom - bounds.top)
        return Offset(x.coerceIn(bounds.left, bounds.right), y.coerceIn(bounds.top, bounds.bottom))
    }

    fun findNearestPoint(density: Density, offset: Offset, width: Float, height: Float): PathPoint? {
        if (elevations.isEmpty()) {
            return null
        }
        val bounds = bounds(density, width, height)
        val nearest = elevations
            .mapIndexed { index, point -> index to toScreen(point.position, bounds) }
            .minByOrNull { (_, point) -> (point.x - offset.x).absoluteValue }
            ?: return null

        if ((nearest.second.x - offset.x).absoluteValue > with(density) { 32.dp.toPx() }) {
            return null
        }

        val pathIndex = elevationIndex.getOrNull(nearest.first) ?: return null
        return path.getOrNull(pathIndex)
    }

    companion object {
        fun from(path: List<PathPoint>, @ColorInt color: Int, units: DistanceUnits): ElevationPlot {
            val indices = mutableListOf<Int>()
            val points = mutableListOf<ElevationPoint>()
            var distance = 0f
            path.forEachIndexed { index, point ->
                if (index != 0) {
                    distance += path[index - 1].coordinate.distanceTo(point.coordinate)
                }

                val elevation = point.elevation
                if (elevation != null) {
                    points.add(ElevationPoint(Vector2(distance, elevation), getSteepness(point.slope), color))
                    indices.add(index)
                }
            }
            val margin = Distance.meters(10f).convertTo(units).value
            val minRange = Distance.meters(100f).convertTo(units).value
            val yRange = getRange(
                points.minOfOrNull { it.position.y } ?: 0f,
                points.maxOfOrNull { it.position.y } ?: 0f,
                margin,
                minRange
            )
            return ElevationPlot(path, indices, points, yRange)
        }

        private fun getRange(minimum: Float, maximum: Float, margin: Float, minRange: Float): Range<Float> {
            val center = (minimum + maximum) / 2f
            val range = max(maximum - minimum + margin * 2, minRange)
            return Range(center - range / 2f, center + range / 2f)
        }

        private fun getSteepness(slope: Float): Steepness {
            return when {
                slope.absoluteValue <= 10f -> Steepness.Low
                slope.absoluteValue <= 25f -> Steepness.Medium
                else -> Steepness.High
            }
        }
    }
}

private data class ElevationPoint(
    val position: Vector2,
    val steepness: Steepness,
    @ColorInt val lineColor: Int
)

private enum class Steepness(@ColorInt val color: Int) {
    Low(AppColor.Green.color),
    Medium(AppColor.Yellow.color),
    High(AppColor.Red.color)
}
