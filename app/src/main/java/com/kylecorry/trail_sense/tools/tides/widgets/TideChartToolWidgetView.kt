package com.kylecorry.trail_sense.tools.tides.widgets

import kotlin.time.Clock

import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetChart

import androidx.compose.ui.graphics.drawscope.DrawScope

import androidx.compose.material3.ColorScheme

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
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
import com.kylecorry.trail_sense.tools.tools.widgets.ChartToolWidgetViewBase
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences
import kotlin.time.Instant

class TideChartToolWidgetView : ChartToolWidgetViewBase() {

    override suspend fun getContent(
        context: Context,
        prefs: WidgetPreferences?
    ): ToolWidgetContent {
        val tides = TidesSubsystem.getInstance(context)
        val tide = tides.getNearestTide()
        val currentWaterLevel = tide?.today?.waterLevels?.minByOrNull {
            (it.time - (kotlin.time.Clock.System.now())).absoluteValue
        }

        val title = if (tide == null) context.getString(R.string.no_tides) else tide.table.name ?: ""
        val levels = tide?.today?.waterLevels.orEmpty()
        val range = tide?.today?.waterLevelRange
        val hasData = levels.isNotEmpty() && range != null
        return ToolWidgetContent.Chart(
            title = title,
            drawing = if (hasData) TideWidgetChart(levels, range!!, currentWaterLevel) else null,
            emptyText = if (hasData) null else context.getString(R.string.no_data),
            clickAction = WidgetClickAction.StartActivity(NavigationUtils.toolIntent(context, Tools.TIDES))
        )
    }
}

/** Today's water level as a filled curve over the hours, with now marked. */
private class TideWidgetChart(
    private val waterLevels: List<Reading<Float>>,
    private val waterLevelRange: Range<Float>,
    private val currentWaterLevel: Reading<Float>?
) : WidgetChart {
    override fun DrawScope.draw(scheme: ColorScheme) {
        val startTime = waterLevels.firstOrNull()?.time ?: kotlin.time.Clock.System.now()
        val totalRange = waterLevelRange.end - waterLevelRange.start
        val rangeDelta = (totalRange * 0.25f).coerceAtLeast(0.2f)
        val minimum = waterLevelRange.start - rangeDelta
        val maximum = waterLevelRange.end + rangeDelta
        val valueRange = (maximum - minimum).takeIf { it > 0f } ?: 1f

        fun x(time: kotlin.time.Instant): Float {
            val hours = (time - startTime).inWholeMilliseconds / 3_600_000f
            return (hours / 24f).coerceIn(0f, 1f) * size.width
        }

        fun y(value: Float): Float {
            val normalized = ((value - minimum) / valueRange).coerceIn(0f, 1f)
            return size.height - normalized * size.height
        }

        repeat(7) { index ->
            val x = index / 6f * size.width
            drawLine(
                color = scheme.outlineVariant,
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
        drawPath(path, scheme.primary.copy(alpha = 0.2f))

        waterLevels.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = scheme.primary,
                start = Offset(x(start.time), y(start.value)),
                end = Offset(x(end.time), y(end.value)),
                strokeWidth = 2.dp.toPx()
            )
        }

        currentWaterLevel?.let {
            drawCircle(
                color = scheme.onSurface,
                radius = 4.dp.toPx(),
                center = Offset(x(it.time), y(it.value))
            )
        }
    }
}
