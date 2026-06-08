package com.kylecorry.trail_sense.tools.navigation.ui

import androidx.compose.ui.platform.ViewCompositionStrategy
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.map
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.shared.debugging.DebugElevationsCommand
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.WeatherRepo
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

class AltitudeBottomSheet(
    private val fragment: Fragment,
    var currentAltitude: Reading<Float>? = null
) {

    private val pathService by lazy { PathService.getInstance(fragment.requireContext()) }
    private val weatherRepo by lazy { WeatherRepo.getInstance(fragment.requireContext()) }
    private val prefs by lazy { UserPreferences(fragment.requireContext()) }
    private val formatService by lazy { FormatService.getInstance(fragment.requireContext()) }
    private var setHistorySummary: ((String) -> Unit)? = null
    private var setChartReadings: ((List<Reading<Float>>) -> Unit)? = null
    private var units = DistanceUnits.Meters
    private var backtrackReadings = listOf<Reading<Float>>()
    private var weatherReadings = listOf<Reading<Float>>()

    private val maxHistoryDuration = Duration.ofDays(1)
    private val maxFilterHistoryDuration = maxHistoryDuration.plusHours(6)
    private var historyDuration = maxHistoryDuration

    fun show(): BottomSheetDialog {
        units = prefs.baseDistanceUnits
        val context = fragment.requireContext()
        val dialog = BottomSheetDialog(context)
        dialog.setContentView(
            TrailSenseComposeRootView(
                context,
                ViewCompositionStrategy.DisposeOnDetachedFromWindow
            ) {
                val initialUnits = remember {
                    prefs.baseDistanceUnits.also { units = it }
                }
                var historySummary by remember {
                    mutableStateOf(formatHistoryDuration())
                }
                var chartReadings by remember {
                    mutableStateOf<List<Reading<Float>>>(emptyList())
                }
                var showHistoryDurationPicker by remember { mutableStateOf(false) }
                val historyDurationTitle = stringResource(R.string.elevation_history_length)

                DisposableEffect(Unit) {
                    setHistorySummary = { historySummary = it }
                    setChartReadings = { chartReadings = it }
                    onDispose {
                        setHistorySummary = null
                        setChartReadings = null
                    }
                }

                AltitudeContent(
                    historySummary = historySummary,
                    readings = chartReadings,
                    units = initialUnits,
                    onHistoryClick = {
                        showHistoryDurationPicker = true
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (showHistoryDurationPicker) {
                    DurationPickerDialog(
                        title = historyDurationTitle,
                        duration = historyDuration,
                        onDurationPicked = {
                            if (it != null) {
                                historyDuration = it
                                updateChart()
                            }
                            showHistoryDurationPicker = false
                        },
                        onDismiss = {
                            showHistoryDurationPicker = false
                        }
                    )
                }
            }
        )

        val backtrackLiveData = getBacktrackReadings()
        val weatherLiveData = getWeatherReadings()
        val backtrackObserver = Observer<List<Reading<Float>>> {
            backtrackReadings = it
            updateChart()
        }
        val weatherObserver = Observer<List<Reading<Float>>> {
            weatherReadings = it
            updateChart()
        }

        backtrackLiveData.observe(fragment.viewLifecycleOwner, backtrackObserver)
        weatherLiveData.observe(fragment.viewLifecycleOwner, weatherObserver)
        dialog.setOnDismissListener {
            backtrackLiveData.removeObserver(backtrackObserver)
            weatherLiveData.removeObserver(weatherObserver)
            setHistorySummary = null
            setChartReadings = null
        }
        dialog.show()
        return dialog
    }

    private fun updateChart(readings: List<Reading<Float>>) {
        setChartReadings?.invoke(readings)
        setHistorySummary?.invoke(formatHistoryDuration())
    }

    private fun updateChart() {
        fragment.inBackground {
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
                    DebugElevationsCommand(fragment.requireContext(), readings, smoothed).execute()
                }

                smoothed.filter {
                    Duration.between(it.time, Instant.now()).abs() <= historyDuration
                }
            }

            onMain { updateChart(filteredReadings) }
        }
    }

    private fun formatHistoryDuration(): String {
        return fragment.getString(R.string.last_duration, formatService.formatDuration(historyDuration))
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
