package com.kylecorry.trail_sense.tools.navigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    private var updateHistoryLengthText: ((String) -> Unit)? = null
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
                    var historyLengthText by remember { mutableStateOf(getHistoryLengthText()) }
                    DisposableEffect(Unit) {
                        updateHistoryLengthText = { historyLengthText = it }
                        onDispose {
                            updateHistoryLengthText = null
                            chart = null
                        }
                    }
                    AltitudeSheetContent(
                        historyLengthText = historyLengthText,
                        onHistoryLengthClick = ::pickHistoryDuration,
                        onChartReady = {
                            chart = it
                            configureChart(it)
                            updateChart()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        chart = null
        updateHistoryLengthText = null
        super.onDestroyView()
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

    private fun configureChart(chart: Chart) {
        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true
        )

        chart.configureXAxis(
            labelCount = 7,
            drawGridLines = true,
            labelFormatter = HourChartLabelFormatter(requireContext()) { startTime }
        )

        chart.emptyText = requireContext().getString(R.string.no_data)
        chart.plot(elevationLine)
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
        updateHistoryLengthText?.invoke(getHistoryLengthText())
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

    private fun getHistoryLengthText(): String {
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
private fun AltitudeSheetContent(
    historyLengthText: String,
    onHistoryLengthClick: () -> Unit,
    onChartReady: (Chart) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.elevation_history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable(onClick = onHistoryLengthClick)
                    .testTag("altitude_history_length"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = historyLengthText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(dimensionResource(R.dimen.chart_height))
                    .testTag("chart"),
                factory = { context ->
                    Chart(context).apply {
                        id = R.id.chart
                        onChartReady(this)
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AltitudeSheetContentPreview() {
    TrailSenseTheme {
        AltitudeSheetContent(
            historyLengthText = "Last 24 hours",
            onHistoryLengthClick = {},
            onChartReady = {}
        )
    }
}
