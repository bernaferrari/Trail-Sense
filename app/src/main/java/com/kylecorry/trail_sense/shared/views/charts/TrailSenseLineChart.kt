package com.kylecorry.trail_sense.shared.views.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import java.time.Duration
import java.time.Instant
import kotlin.math.max

@Composable
fun TrailSenseLineChart(
    readings: List<Reading<Float>>,
    modifier: Modifier = Modifier,
    minimum: Float? = null,
    maximum: Float? = null,
    freezingValue: Float? = null
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val freezingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (readings.isEmpty()) {
            Text(
                text = stringResource(R.string.no_data),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Box
        }

        val points = trailSenseLineChartPoints(readings, minimum, maximum, freezingValue)
        Canvas(modifier = Modifier.fillMaxSize()) {
            repeat(5) { index ->
                val y = index / 4f * size.height
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            freezingValue?.let {
                val y = points.y(it, size.height)
                if (y in 0f..size.height) {
                    drawRect(
                        color = freezingColor,
                        topLeft = Offset(0f, y),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height - y)
                    )
                }
            }

            drawLineSeries(points.points, lineColor, points, 2.dp.toPx())
        }
    }
}

private fun trailSenseLineChartPoints(
    readings: List<Reading<Float>>,
    minimum: Float?,
    maximum: Float?,
    freezingValue: Float?
): TrailSenseLineChartPoints {
    val startTime = readings.firstOrNull()?.time ?: Instant.now()
    val points = readings.map {
            TrailSenseLineChartPoint(
            Duration.between(startTime, it.time).toMillis() / 3_600_000f,
            it.value
        )
    }
    val minX = points.minOfOrNull { it.x } ?: 0f
    val maxX = points.maxOfOrNull { it.x } ?: 1f
    val minYValue = listOfNotNull(points.minOfOrNull { it.y }, minimum, freezingValue).min()
    val maxYValue = listOfNotNull(points.maxOfOrNull { it.y }, maximum, freezingValue).max()
    val margin = 5f
    val yMinimum = minimum ?: (minYValue - margin)
    val yMaximum = maximum ?: max(maxYValue + margin, yMinimum + 10f)
    return TrailSenseLineChartPoints(
        points = points,
        minX = minX,
        xRange = (maxX - minX).takeIf { it > 0f } ?: 1f,
        yMinimum = yMinimum,
        yRange = (yMaximum - yMinimum).takeIf { it > 0f } ?: 1f
    )
}

private fun DrawScope.drawLineSeries(
    points: List<TrailSenseLineChartPoint>,
    color: Color,
    chartPoints: TrailSenseLineChartPoints,
    strokeWidth: Float
) {
    points.zipWithNext().forEach { (start, end) ->
        drawLine(
            color = color,
            start = Offset(
                chartPoints.x(start.x, size.width),
                chartPoints.y(start.y, size.height)
            ),
            end = Offset(
                chartPoints.x(end.x, size.width),
                chartPoints.y(end.y, size.height)
            ),
            strokeWidth = strokeWidth
        )
    }
}

private data class TrailSenseLineChartPoint(
    val x: Float,
    val y: Float
)

private data class TrailSenseLineChartPoints(
    val points: List<TrailSenseLineChartPoint>,
    val minX: Float,
    val xRange: Float,
    val yMinimum: Float,
    val yRange: Float
) {
    fun x(value: Float, width: Float): Float {
        return (value - minX) / xRange * width
    }

    fun y(value: Float, height: Float): Float {
        return height - (value - yMinimum) / yRange * height
    }
}
