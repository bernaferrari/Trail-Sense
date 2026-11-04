package com.kylecorry.trail_sense.tools.paths.ui

import android.content.Context
import android.graphics.Paint
import androidx.annotation.ColorInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import kotlin.math.absoluteValue
import kotlin.math.max

class PathElevationChart(private val context: Context) {

    private val formatter = FormatService.getInstance(context)
    private val baseDistanceUnits = com.kylecorry.trail_sense.shared.UserPreferences(context).baseDistanceUnits

    private var renderVersion by mutableIntStateOf(0)
    private var path = emptyList<PathPoint>()
    private var elevationIndex = emptyList<Int>()
    private var elevations = emptyList<ElevationPoint>()
    private var highlight: Vector2? = null
    private var yRange = Range(0f, 100f)
    private var listener: (PathPoint) -> Unit = {}

    fun setOnPointClickListener(listener: (point: PathPoint) -> Unit) {
        this.listener = listener
    }

    fun plot(path: List<PathPoint>, @ColorInt color: Int) {
        val points = getElevationPlotPoints(path)
        val margin = Distance.meters(10f).convertTo(baseDistanceUnits).value
        val minRange = Distance.meters(100f).convertTo(baseDistanceUnits).value
        yRange = getRange(
            points.minOfOrNull { it.position.y } ?: 0f,
            points.maxOfOrNull { it.position.y } ?: 0f,
            margin,
            minRange
        )
        elevations = points.map { it.copy(lineColor = color) }
        highlight = null
        renderVersion++
    }

    fun highlight(point: PathPoint) {
        val idx = path.indexOf(point)
        if (idx == -1) {
            return
        }
        val pointIndex = elevationIndex.indexOf(idx)
        if (pointIndex == -1) {
            return
        }
        tryOrNothing {
            highlight = elevations[pointIndex].position
        }
        renderVersion++
    }

    fun removeHighlight() {
        highlight = null
        renderVersion++
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        // Theme colors so the chart sits calmly in its group card: quiet grid, muted labels, solid highlight ring.
        val textColor = MaterialTheme.colorScheme.onSurfaceVariant
        val gridColor = MaterialTheme.colorScheme.outlineVariant
        val highlightColor = MaterialTheme.colorScheme.onSurface
        val highlightRing = MaterialTheme.colorScheme.surfaceContainerLow
        val status = com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors()
        val steepColors = mapOf(
            Steepness.Low to status.good,
            Steepness.Medium to status.caution,
            Steepness.High to MaterialTheme.colorScheme.error
        )
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor.toArgb()
            textSize = Resources.sp(context, 10f)
            textAlign = Paint.Align.RIGHT
        }
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(renderVersion) {
                    detectTapGestures { offset ->
                        findNearestPoint(offset, size.width.toFloat(), size.height.toFloat())?.let {
                            listener(it)
                        }
                    }
                }
        ) {
            renderVersion
            val chartPoints = elevations
            if (chartPoints.isEmpty()) {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor.toArgb()
                    textSize = Resources.sp(context, 14f)
                    textAlign = Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText(
                    context.getString(R.string.no_data),
                    size.width / 2f,
                    size.height / 2f,
                    paint
                )
                return@Canvas
            }

            val left = 58.dp.toPx()
            val right = size.width - 12.dp.toPx()
            val top = 12.dp.toPx()
            val bottom = size.height - 28.dp.toPx()
            val xRange = Range(0f, chartPoints.maxOfOrNull { it.position.x }?.coerceAtLeast(1f) ?: 1f)

            repeat(3) { idx ->
                val fraction = idx / 2f
                val y = bottom - (bottom - top) * fraction
                val value = yRange.start + (yRange.end - yRange.start) * fraction
                drawLine(
                    gridColor,
                    Offset(left, y),
                    Offset(right, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(3.dp.toPx(), 5.dp.toPx())
                    ),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawContext.canvas.nativeCanvas.drawText(
                    formatElevation(value),
                    left - 6.dp.toPx(),
                    y + 3.dp.toPx(),
                    labelPaint
                )
            }

            labelPaint.textAlign = Paint.Align.CENTER
            repeat(4) { idx ->
                val fraction = idx / 3f
                val x = left + (right - left) * fraction
                val value = xRange.start + (xRange.end - xRange.start) * fraction
                drawContext.canvas.nativeCanvas.drawText(
                    formatDistance(value),
                    x,
                    size.height - 8.dp.toPx(),
                    labelPaint
                )
            }

            chartPoints.zipWithNext().forEach { (start, end) ->
                val p1 = toScreen(start.position, xRange, left, right, top, bottom)
                val p2 = toScreen(end.position, xRange, left, right, top, bottom)
                val area = Path().apply {
                    moveTo(p1.x, bottom)
                    lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p2.x, bottom)
                    close()
                }
                drawPath(
                    area,
                    (steepColors[end.steepness] ?: Color(getColor(end.steepness))).copy(alpha = 0.32f)
                )
                drawLine(
                    Color(start.lineColor),
                    p1,
                    p2,
                    strokeWidth = 3.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            highlight?.let {
                val point = toScreen(it, xRange, left, right, top, bottom)
                drawCircle(color = highlightRing, radius = 9.dp.toPx(), center = point)
                drawCircle(color = highlightColor, radius = 6.dp.toPx(), center = point)
            }
        }
    }

    private fun findNearestPoint(offset: Offset, width: Float, height: Float): PathPoint? {
        val points = elevations
        if (points.isEmpty()) {
            return null
        }
        val left = Resources.dp(context, 58f)
        val right = width - Resources.dp(context, 12f)
        val top = Resources.dp(context, 12f)
        val bottom = height - Resources.dp(context, 28f)
        val xRange = Range(0f, points.maxOfOrNull { it.position.x }?.coerceAtLeast(1f) ?: 1f)
        val nearest = points
            .mapIndexed { index, point ->
                index to toScreen(point.position, xRange, left, right, top, bottom)
            }
            .minByOrNull { (_, point) -> (point.x - offset.x).absoluteValue }
            ?: return null

        if ((nearest.second.x - offset.x).absoluteValue > Resources.dp(context, 32f)) {
            return null
        }

        val pathIndex = elevationIndex.getOrNull(nearest.first) ?: return null
        return path.getOrNull(pathIndex)
    }

    private fun getElevationPlotPoints(path: List<PathPoint>): List<ElevationPoint> {
        this.path = path
        val indices = mutableListOf<Int>()
        val points = mutableListOf<ElevationPoint>()
        var distance = 0f
        path.forEachIndexed { index, point ->
            if (index != 0) {
                distance += path[index - 1].coordinate.distanceTo(point.coordinate)
            }

            if (point.elevation != null) {
                points.add(
                    ElevationPoint(
                        Vector2(distance, point.elevation),
                        getSteepness(point.slope),
                        AppColor.Blue.color
                    )
                )
                indices.add(index)
            }
        }
        elevationIndex = indices
        return points
    }

    private fun toScreen(
        point: Vector2,
        xRange: Range<Float>,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float
    ): Offset {
        val x = left + ((point.x - xRange.start) / (xRange.end - xRange.start)) * (right - left)
        val y = bottom - ((point.y - yRange.start) / (yRange.end - yRange.start)) * (bottom - top)
        return Offset(x.coerceIn(left, right), y.coerceIn(top, bottom))
    }

    private fun formatElevation(value: Float): String {
        val distance = Distance.from(value, DistanceUnits.Meters).convertTo(baseDistanceUnits)
        return formatter.formatDistance(
            distance,
            Units.getDecimalPlaces(distance.units),
            false
        )
    }

    private fun formatDistance(value: Float): String {
        val distance = Distance.from(value, DistanceUnits.Meters)
            .convertTo(baseDistanceUnits)
            .toRelativeDistance()
        return formatter.formatDistance(
            distance,
            Units.getDecimalPlaces(distance.units),
            false
        )
    }

    private fun getRange(minimum: Float, maximum: Float, margin: Float, minRange: Float): Range<Float> {
        val center = (minimum + maximum) / 2f
        val range = max(maximum - minimum + margin * 2, minRange)
        return Range(center - range / 2f, center + range / 2f)
    }

    @ColorInt
    private fun getColor(steepness: Steepness): Int {
        return when (steepness) {
            Steepness.Low -> AppColor.Green.color
            Steepness.Medium -> AppColor.Yellow.color
            Steepness.High -> AppColor.Red.color
        }
    }

    private fun getSteepness(slope: Float): Steepness {
        return when {
            slope.absoluteValue <= 10f -> Steepness.Low
            slope.absoluteValue <= 25f -> Steepness.Medium
            else -> Steepness.High
        }
    }

    private data class ElevationPoint(
        val position: Vector2,
        val steepness: Steepness,
        @ColorInt val lineColor: Int
    )

    private enum class Steepness {
        Low,
        Medium,
        High
    }
}
