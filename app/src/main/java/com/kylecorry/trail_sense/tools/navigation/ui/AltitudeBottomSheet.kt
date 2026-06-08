package com.kylecorry.trail_sense.tools.navigation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

@Composable
fun AltitudeContent(
    historySummary: String,
    readings: List<Reading<Float>>,
    units: DistanceUnits,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(TrailSenseSpacing.lg)
        ) {
            Text(
                text = stringResource(R.string.elevation_history),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    top = TrailSenseSpacing.sm,
                    bottom = TrailSenseSpacing.sm
                )
            )

            TrailSenseListItem(
                title = historySummary,
                trailing = {
                    Icon(
                        painter = painterResource(R.drawable.ic_drop_down),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = onHistoryClick,
                animateEntrance = false,
                modifier = Modifier.testTag("altitude_history_length")
            )
            TrailSenseDivider()

            AltitudeChartView(
                readings = readings,
                units = units,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.chart_height))
                    .padding(top = TrailSenseSpacing.lg)
                    .testTag("chart")
            )
        }
    }
}

@Composable
private fun AltitudeChartView(
    readings: List<Reading<Float>>,
    units: DistanceUnits,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

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

        Canvas(modifier = Modifier.fillMaxSize()) {
            val startTime = readings.firstOrNull()?.time ?: Instant.now()
            val points = readings.map {
                Duration.between(startTime, it.time).toMillis() / 3_600_000f to
                        Distance.meters(it.value).convertTo(units).value
            }
            val margin = Distance.meters(10f).convertTo(units).value
            val minRange = Distance.meters(100f).convertTo(units).value
            val minX = points.minOfOrNull { it.first } ?: 0f
            val maxX = points.maxOfOrNull { it.first } ?: 1f
            val minYValue = points.minOfOrNull { it.second } ?: 0f
            val maxYValue = points.maxOfOrNull { it.second } ?: 0f
            val yMinimum = minYValue - margin
            val yMaximum = max(maxYValue + margin, yMinimum + minRange)
            val xRange = (maxX - minX).takeIf { it > 0f } ?: 1f
            val yRange = (yMaximum - yMinimum).takeIf { it > 0f } ?: 1f

            fun x(value: Float): Float {
                return ((value - minX) / xRange).coerceIn(0f, 1f) * size.width
            }

            fun y(value: Float): Float {
                return size.height - ((value - yMinimum) / yRange).coerceIn(0f, 1f) * size.height
            }

            repeat(5) { index ->
                val y = index / 4f * size.height
                drawLine(
                    color = grid,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            points.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = primary,
                    start = androidx.compose.ui.geometry.Offset(x(start.first), y(start.second)),
                    end = androidx.compose.ui.geometry.Offset(x(end.first), y(end.second)),
                    strokeWidth = 2.dp.toPx()
                )
            }

            points.forEach { point ->
                drawLine(
                    color = primary.copy(alpha = 0.2f),
                    start = androidx.compose.ui.geometry.Offset(x(point.first), y(point.second)),
                    end = androidx.compose.ui.geometry.Offset(x(point.first), y(yMinimum)),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AltitudeContentPreview() {
    TrailSenseTheme {
        AltitudeContent(
            historySummary = "Last 24 hours",
            readings = emptyList(),
            units = DistanceUnits.Meters,
            onHistoryClick = {}
        )
    }
}
