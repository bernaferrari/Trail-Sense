package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.andromeda.views.chart.data.AreaChartLayer
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.WeatherRepo
import java.time.Duration
import java.time.Instant

class AltitudeBottomSheet : BottomSheetDialogFragment() {

    private val pathService by lazy { PathService.getInstance(requireContext()) }
    private val weatherRepo by lazy { WeatherRepo.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private var chart: Chart? = null
    private var setHistorySummary: ((String) -> Unit)? = null
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
        units = prefs.baseDistanceUnits

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    var historySummary by remember {
                        mutableStateOf(formatHistoryDuration())
                    }

                    DisposableEffect(Unit) {
                        setHistorySummary = { historySummary = it }
                        onDispose {
                            setHistorySummary = null
                            chart = null
                        }
                    }

                    AltitudeContent(
                        historySummary = historySummary,
                        onHistoryClick = ::pickHistoryDuration,
                        onChartReady = {
                            chart = it
                            configureChart(requireContext(), it)
                            updateChart()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        units = prefs.baseDistanceUnits

        observe(getBacktrackReadings()) {
            backtrackReadings = it
            updateChart()
        }
        observe(getWeatherReadings()) {
            weatherReadings = it
            updateChart()
        }
    }

    override fun onDestroyView() {
        setHistorySummary = null
        chart = null
        super.onDestroyView()
    }

    private fun pickHistoryDuration() {
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

    private fun configureChart(context: Context, chart: Chart) {
        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true
        )

        chart.configureXAxis(
            labelCount = 7,
            drawGridLines = true,
            labelFormatter = HourChartLabelFormatter(context) { startTime }
        )

        chart.emptyText = context.getString(R.string.no_data)
        chart.plot(elevationLine)
    }

    private fun updateChart(readings: List<Reading<Float>>) {
        val chart = chart ?: return

        startTime = readings.firstOrNull()?.time ?: Instant.now()

        val data = Chart.getDataFromReadings(readings) {
            Distance.meters(it).convertTo(units).value
        }

        val margin = Distance.meters(10f).convertTo(units).value
        val minRange = Distance.meters(100f).convertTo(units).value
        val range = Chart.getYRange(data, margin, minRange)
        chart.configureYAxis(
            minimum = range.start,
            maximum = range.end,
            labelCount = 5,
            drawGridLines = true
        )

        elevationLine.data = data
        elevationLine.fillTo = range.start

        setHistorySummary?.invoke(formatHistoryDuration())
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

    private fun formatHistoryDuration(): String {
        return getString(R.string.last_duration, formatService.formatDuration(historyDuration))
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

@Composable
private fun AltitudeContent(
    historySummary: String,
    onHistoryClick: () -> Unit,
    onChartReady: (Chart) -> Unit,
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
                onChartReady = onChartReady,
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
    onChartReady: (Chart) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            Chart(context).apply {
                id = R.id.chart
                onChartReady(this)
            }
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun AltitudeContentPreview() {
    TrailSenseTheme {
        AltitudeContent(
            historySummary = "Last 24 hours",
            onHistoryClick = {},
            onChartReady = {}
        )
    }
}
