package com.kylecorry.trail_sense.tools.tides.widgets

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.kylecorry.andromeda.core.ui.Views
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.tools.tides.subsystem.TidesSubsystem
import com.kylecorry.trail_sense.tools.tides.ui.TideChart
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
            val chart = Chart(context)
            val tideChart = TideChart(chart)
            if (tide != null) {
                tideChart.plot(
                    tide.today.waterLevels,
                    tide.today.waterLevelRange
                )

                currentWaterLevel?.let {
                    tideChart.highlight(it, tide.today.waterLevelRange)
                }
            }

            val text = createTideChartTitle(
                context,
                if (tide == null) context.getString(R.string.no_tides) else tide.table.name ?: ""
            )
            val layout = Views.linear(listOf(text, chart))

            renderChart(context, views, layout)
        }

        views.setViewVisibility(TITLE_TEXTVIEW, View.GONE)

        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.TIDES)
        )
        return views
    }

    private fun createTideChartTitle(context: Context, title: String): View {
        return TrailSenseComposeRootView(
            context,
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }.apply {
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
    }
}
