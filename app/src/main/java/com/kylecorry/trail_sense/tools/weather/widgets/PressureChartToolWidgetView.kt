package com.kylecorry.trail_sense.tools.weather.widgets

import android.content.Context
import android.view.View
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
import com.kylecorry.trail_sense.tools.tools.widgets.ChartToolWidgetViewBase
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration
import java.time.Instant
import kotlin.math.max

class PressureChartToolWidgetView : ChartToolWidgetViewBase() {

    override suspend fun getContent(
        context: Context,
        prefs: WidgetPreferences?
    ): ToolWidgetContent {
        val weather = WeatherSubsystem.getInstance(context)
        val userPrefs = UserPreferences(context)
        val units = userPrefs.pressureUnits

        val history = weather.getHistory()
        val displayReadings = history.filter {
            Duration.between(
                it.time,
                Instant.now()
            ) <= userPrefs.weather.pressureHistory
        }
            .map { it.pressureReading() }
            .map { it.copy(value = it.value.convertTo(units)) }


        var bitmap: android.graphics.Bitmap? = null
        onMain {
            bitmap = renderChart(context, createPressureChartView(context, displayReadings))
        }

        return ToolWidgetContent.Chart(
            bitmap = bitmap,
            clickAction = WidgetClickAction.StartActivity(NavigationUtils.toolIntent(context, Tools.WEATHER))
        )
    }

    private fun createPressureChartView(context: Context, readings: List<Reading<Pressure>>): View {
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
                    text = context.getString(R.string.pressure),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                WidgetPressureChart(
                    readings = readings,
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
private fun WidgetPressureChart(
    readings: List<Reading<Pressure>>,
    noDataText: String,
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
                text = noDataText,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Box
        }

        val chartPoints = pressureChartPoints(readings)
        Canvas(modifier = Modifier.fillMaxSize()) {
            repeat(5) { index ->
                val y = index / 4f * size.height
                drawLine(
                    color = grid,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            chartPoints.data.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = primary,
                    start = Offset(
                        chartPoints.x(start.x, size.width),
                        chartPoints.y(start.pressure, size.height)
                    ),
                    end = Offset(
                        chartPoints.x(end.x, size.width),
                        chartPoints.y(end.pressure, size.height)
                    ),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

private fun pressureChartPoints(data: List<Reading<Pressure>>): PressureChartPoints {
    val startTime = data.firstOrNull()?.time ?: Instant.now()
    val points = data.map {
        PressureChartPoint(
            Duration.between(startTime, it.time).toMillis() / 3_600_000f,
            it.value.value
        )
    }
    val units = data.firstOrNull()?.value?.units ?: PressureUnits.Hpa
    val margin = Pressure.hpa(1f).convertTo(units).value
    val minRange = Pressure.hpa(40f).convertTo(units).value
    val minX = points.minOfOrNull { it.x } ?: 0f
    val maxX = points.maxOfOrNull { it.x } ?: 1f
    val minYValue = points.minOfOrNull { it.pressure } ?: 0f
    val maxYValue = points.maxOfOrNull { it.pressure } ?: 1f
    val yMinimum = minYValue - margin
    val yMaximum = max(maxYValue + margin, yMinimum + minRange)
    return PressureChartPoints(
        data = points,
        minX = minX,
        xRange = (maxX - minX).takeIf { it > 0f } ?: 1f,
        yMinimum = yMinimum,
        yRange = (yMaximum - yMinimum).takeIf { it > 0f } ?: 1f
    )
}

private data class PressureChartPoint(
    val x: Float,
    val pressure: Float
)

private data class PressureChartPoints(
    val data: List<PressureChartPoint>,
    val minX: Float,
    val xRange: Float,
    val yMinimum: Float,
    val yRange: Float
) {
    fun x(value: Float, width: Float): Float {
        return ((value - minX) / xRange).coerceIn(0f, 1f) * width
    }

    fun y(value: Float, height: Float): Float {
        return height - ((value - yMinimum) / yRange).coerceIn(0f, 1f) * height
    }
}
