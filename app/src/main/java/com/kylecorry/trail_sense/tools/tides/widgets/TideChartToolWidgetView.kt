package com.kylecorry.trail_sense.tools.tides.widgets

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.tools.tides.subsystem.TidesSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.widgets.ChartToolWidgetViewBase
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences
import java.time.Duration
import java.time.Instant

class TideChartToolWidgetView : ChartToolWidgetViewBase() {

    override suspend fun getPopulatedView(
        context: Context,
        prefs: WidgetPreferences?
    ): RemoteViews {
        val tides = TidesSubsystem.getInstance(context)
        val tide = tides.getNearestTide()
        val currentWaterLevel = tide?.today?.waterLevels?.minByOrNull {
            Duration.between(Instant.now(), it.time).abs()
        }

        val views = getView(context, prefs)
        onMain {
            renderChart(
                context,
                views,
                createTideChartView(
                    context,
                    if (tide == null) context.getString(R.string.no_tides) else tide.table.name ?: "",
                    tide?.today?.waterLevels.orEmpty(),
                    tide?.today?.waterLevelRange,
                    currentWaterLevel
                )
            )
        }

        views.setViewVisibility(TITLE_TEXTVIEW, View.GONE)

        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.TIDES)
        )
        return views
    }

    private fun createTideChartView(
        context: Context,
        title: String,
        waterLevels: List<Reading<Float>>,
        range: Range<Float>?,
        currentWaterLevel: Reading<Float>?
    ): View {
        return TrailSenseComposeRootView(
            context,
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                WidgetTideChart(
                    waterLevels = waterLevels,
                    range = range,
                    currentWaterLevel = currentWaterLevel,
                    noDataText = context.getString(R.string.no_data),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WidgetTideChart(
    waterLevels: List<Reading<Float>>,
    range: Range<Float>?,
    currentWaterLevel: Reading<Float>?,
    noDataText: String,
    modifier: Modifier = Modifier
) {
    val levelColor = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val highlightColor = MaterialTheme.colorScheme.onSurface
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val waterLevelRange = range
        if (waterLevels.isEmpty() || waterLevelRange == null) {
            Text(
                text = noDataText,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Box
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val startTime = waterLevels.firstOrNull()?.time ?: Instant.now()
            val totalRange = waterLevelRange.end - waterLevelRange.start
            val rangeDelta = (totalRange * 0.25f).coerceAtLeast(0.2f)
            val minimum = waterLevelRange.start - rangeDelta
            val maximum = waterLevelRange.end + rangeDelta
            val valueRange = (maximum - minimum).takeIf { it > 0f } ?: 1f

            fun x(time: Instant): Float {
                val hours = Duration.between(startTime, time).toMillis() / 3_600_000f
                return (hours / 24f).coerceIn(0f, 1f) * size.width
            }

            fun y(value: Float): Float {
                val normalized = ((value - minimum) / valueRange).coerceIn(0f, 1f)
                return size.height - normalized * size.height
            }

            repeat(7) { index ->
                val x = index / 6f * size.width
                drawLine(
                    color = grid,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val path = Path()
            waterLevels.forEachIndexed { index, reading ->
                val pointX = x(reading.time)
                val pointY = y(reading.value)
                if (index == 0) {
                    path.moveTo(pointX, size.height)
                    path.lineTo(pointX, pointY)
                } else {
                    path.lineTo(pointX, pointY)
                }
            }
            val last = waterLevels.last()
            path.lineTo(x(last.time), size.height)
            path.close()
            drawPath(path, levelColor.copy(alpha = 0.2f))

            waterLevels.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = levelColor,
                    start = Offset(x(start.time), y(start.value)),
                    end = Offset(x(end.time), y(end.value)),
                    strokeWidth = 2.dp.toPx()
                )
            }

            currentWaterLevel?.let {
                drawCircle(
                    color = highlightColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x(it.time), y(it.value))
                )
            }
        }
    }
}
