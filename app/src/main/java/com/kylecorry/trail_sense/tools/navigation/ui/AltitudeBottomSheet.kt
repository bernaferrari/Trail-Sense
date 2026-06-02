package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.andromeda.views.chart.data.AreaChartLayer
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.shared.debugging.DebugElevationsCommand
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.chart.label.HourChartLabelFormatter
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.WeatherRepo
import java.time.Duration
import java.time.Instant

class AltitudeBottomSheet : BottomSheetDialogFragment() {

    private val pathService by lazy { PathService.getInstance(requireContext()) }
    private val weatherRepo by lazy { WeatherRepo.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private var altitudeViews: AltitudeViews? = null
    private val binding: AltitudeViews?
        get() = altitudeViews
    private var units = DistanceUnits.Meters
    private var backtrackReadings = listOf<Reading<Float>>()
    private var weatherReadings = listOf<Reading<Float>>()
    private var startTime: Instant = Instant.now()

    var currentAltitude: Reading<Float>? = null

    private val maxHistoryDuration = Duration.ofDays(1)
    private val maxFilterHistoryDuration = maxHistoryDuration.plusHours(6)
    private var historyDuration = maxHistoryDuration

    private val elevationLine by lazy {
        val color = Resources.getPrimaryColor(requireContext())
        AreaChartLayer(
            emptyList(),
            color,
            color.withAlpha(50)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createAltitudeViews(requireContext())
        altitudeViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    AltitudeHost(
                        views = views,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        altitudeViews = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = binding ?: return
        units = prefs.baseDistanceUnits
        views.chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true
        )

        views.chart.configureXAxis(
            labelCount = 7,
            drawGridLines = true,
            labelFormatter = HourChartLabelFormatter(requireContext()) { startTime }
        )

        views.chart.emptyText = requireContext().getString(R.string.no_data)

        views.chart.plot(elevationLine)

        observe(getBacktrackReadings()) {
            backtrackReadings = it
            updateChart()
        }
        observe(getWeatherReadings()) {
            weatherReadings = it
            updateChart()
        }

        views.altitudeHistoryLength.setOnClickListener {
            CustomUiUtils.pickDuration(
                requireContext(),
                historyDuration,
                getString(R.string.elevation_history_length)
            ) {
                if (it != null) {
                    historyDuration = it
                    updateChart()
                }
            }
        }
    }

    private fun updateChart(readings: List<Reading<Float>>) {
        val views = binding ?: return

        startTime = readings.firstOrNull()?.time ?: Instant.now()

        val data = Chart.getDataFromReadings(readings) {
            Distance.meters(it).convertTo(units).value
        }

        val margin = Distance.meters(10f).convertTo(units).value
        val minRange = Distance.meters(100f).convertTo(units).value
        val range = Chart.getYRange(data, margin, minRange)
        views.chart.configureYAxis(
            minimum = range.start,
            maximum = range.end,
            labelCount = 5,
            drawGridLines = true
        )


        elevationLine.data = data
        elevationLine.fillTo = range.start

        views.altitudeHistoryLength.text =
            getString(R.string.last_duration, formatService.formatDuration(historyDuration))
    }

    private fun updateChart() {
        inBackground {
            val filteredReadings = onDefault {
                val readings =
                    (backtrackReadings + weatherReadings + listOfNotNull(currentAltitude)).sortedBy { it.time }
                        .filter {
                            Duration.between(
                                it.time,
                                Instant.now()
                            ) < maxFilterHistoryDuration
                        }


                val smoothed = DataUtils.smoothTemporal(readings, 0.3f)

                onIO {
                    DebugElevationsCommand(requireContext(), readings, smoothed).execute()
                }

                smoothed.filter {
                    Duration.between(it.time, Instant.now()).abs() <= historyDuration
                }
            }

            onMain { updateChart(filteredReadings) }
        }
    }

    private fun getWeatherReadings(): LiveData<List<Reading<Float>>> {
        return weatherRepo.getAllLive().map { readings ->
            readings.mapNotNull { reading ->
                if (reading.value.altitude == 0f) {
                    return@mapNotNull null
                }
                Reading(reading.value.altitude, reading.time)
            }
        }
    }

    private fun getBacktrackReadings(): LiveData<List<Reading<Float>>> {
        return pathService.getRecentAltitudesLive(
            Instant.now().minus(maxFilterHistoryDuration)
        )
    }

}

private data class AltitudeViews(
    val root: View,
    val altitudeHistoryLength: TextView,
    val chart: Chart
)

@Composable
private fun AltitudeHost(
    views: AltitudeViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createAltitudeViews(context: Context): AltitudeViews {
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val title = TextView(context).apply {
        text = context.getString(R.string.elevation_history)
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
    }
    root.addView(
        title,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val historyLength = TextView(context).apply {
        id = R.id.altitude_history_length
        gravity = android.view.Gravity.CENTER_VERTICAL
        setCompoundDrawables(24.dp(context), right = R.drawable.ic_drop_down)
    }
    root.addView(
        historyLength,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 8.dp(context)
        }
    )

    val chart = Chart(context).apply {
        id = R.id.chart
    }
    root.addView(
        chart,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            context.resources.getDimensionPixelSize(R.dimen.chart_height)
        ).apply {
            topMargin = 16.dp(context)
        }
    )

    return AltitudeViews(root, historyLength, chart)
}

private fun Int.dp(context: Context): Int {
    return Resources.dp(context, toFloat()).toInt()
}

@Preview(showBackground = true)
@Composable
private fun AltitudeHostPreview() {
    TrailSenseTheme {
        AltitudeHost(
            views = createAltitudeViews(LocalContext.current)
        )
    }
}
