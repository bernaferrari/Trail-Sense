package com.kylecorry.trail_sense.tools.weather.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator
import com.kylecorry.trail_sense.shared.alerts.ResettableLoadingIndicator
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.getFeatureState
import com.kylecorry.trail_sense.tools.tools.infrastructure.observe
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherLogger
import com.kylecorry.trail_sense.tools.weather.infrastructure.commands.ChangeWeatherFrequencyCommand
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import com.kylecorry.trail_sense.tools.weather.quickactions.WeatherQuickActionBinder
import com.kylecorry.trail_sense.tools.weather.ui.charts.HumidityChart
import com.kylecorry.trail_sense.tools.weather.ui.charts.PressureChart
import com.kylecorry.trail_sense.tools.weather.ui.charts.TemperatureChart
import com.kylecorry.trail_sense.tools.weather.ui.dialogs.ShowHighLowTemperatureDialogCommand
import com.kylecorry.trail_sense.tools.weather.ui.dialogs.ShowHistoricTemperatureDisclaimerCommand
import com.kylecorry.trail_sense.tools.weather.ui.fields.AlertWeatherField
import com.kylecorry.trail_sense.tools.weather.ui.fields.CloudWeatherField
import com.kylecorry.trail_sense.tools.weather.ui.fields.FrontWeatherField
import com.kylecorry.trail_sense.tools.weather.ui.fields.HistoricTemperatureRangeWeatherField
import com.kylecorry.trail_sense.tools.weather.ui.fields.HumidityWeatherField
import com.kylecorry.trail_sense.tools.weather.ui.fields.PressureSystemWeatherField
import com.kylecorry.trail_sense.tools.weather.ui.fields.PressureTendencyWeatherField
import com.kylecorry.trail_sense.tools.weather.ui.fields.PressureWeatherField
import com.kylecorry.trail_sense.tools.weather.ui.fields.TemperatureWeatherField
import com.kylecorry.trail_sense.tools.weather.ui.fields.WeatherListItem
import java.time.Duration
import java.time.Instant

class WeatherFragment : AndromedaFragment() {

    private var weatherViews: WeatherViews? = null
    private val binding: WeatherViews
        get() = weatherViews ?: error("Weather views are not ready")
    private val isViewReady: Boolean
        get() = weatherViews != null && view != null

    private var useSeaLevelPressure = false
    private var units = PressureUnits.Hpa

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val temperatureUnits by lazy { prefs.temperatureUnits }

    private lateinit var chart: PressureChart

    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    private var history: List<WeatherObservation> = emptyList()
    private var pressureForecast: List<Reading<Pressure>> = emptyList()
    private var rawHistory: List<Reading<Pressure>> = emptyList()
    private var weatherItems by mutableStateOf<List<WeatherListItem>>(emptyList())
    private var pressureMarkerText by mutableStateOf<String?>(null)
    private var isWeatherUpdating by mutableStateOf(false)
    private var weatherMonitorState by mutableStateOf(FeatureState.Off)
    private var weatherMonitorFrequency by mutableStateOf<Duration?>(null)

    private val weatherSubsystem by lazy { WeatherSubsystem.getInstance(requireContext()) }
    private var weather: CurrentWeather? = null
    private val loadingIndicator by lazy {
        ResettableLoadingIndicator(
            StateLoadingIndicator { isWeatherUpdating = it }
        )
    }

    private val logger by lazy {
        WeatherLogger(
            requireContext(),
            Duration.ofSeconds(30),
            Duration.ofMillis(500),
            loadingIndicator
        )
    }

    private val weatherMonitorService by lazy {
        Tools.getService(requireContext(), WeatherToolRegistration.SERVICE_WEATHER_MONITOR)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createWeatherViews(requireContext())
        weatherViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    WeatherHost(
                        views = views,
                        weatherItems = weatherItems,
                        pressureMarkerText = pressureMarkerText,
                        isWeatherUpdating = isWeatherUpdating,
                        monitorState = weatherMonitorState,
                        monitorFrequency = weatherMonitorFrequency,
                        formatService = formatService,
                        onChangeMonitorFrequency = ::changeWeatherFrequency,
                        onToggleMonitor = ::toggleWeatherMonitor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        weatherViews = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        WeatherQuickActionBinder(
            this,
            binding.weatherTitle,
            prefs.weather
        ).bind()

        chart = PressureChart(binding.chart) { timeAgo, pressure ->
            if (timeAgo == null || pressure == null) {
                pressureMarkerText = null
            } else {
                val formatted = formatService.formatPressure(
                    Pressure.from(pressure, units),
                    Units.getDecimalPlaces(units)
                )
                pressureMarkerText = getString(
                    R.string.pressure_reading_time_ago,
                    formatted,
                    formatService.formatDuration(timeAgo, false)
                )
            }
        }

        observe(weatherSubsystem.weatherChanged) {
            updateWeather()
        }

        updateStatusBar()

        Tools.observe(this, WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_STATE_CHANGED) {
            updateStatusBar()
            true
        }

        Tools.observe(this, WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_FREQUENCY_CHANGED) {
            updateStatusBar()
            true
        }

    }

    private fun updateStatusBar() {
        weatherMonitorState = weatherMonitorService.getFeatureState()
        weatherMonitorFrequency = weatherMonitorService.getFrequency()
    }

    private fun changeWeatherFrequency() {
        ChangeWeatherFrequencyCommand(requireContext()) { updateStatusBar() }.execute()
    }

    private fun toggleWeatherMonitor() {
        inBackground {
            when (weatherMonitorService.getFeatureState()) {
                FeatureState.On -> weatherMonitorService.disable()
                FeatureState.Off -> {
                    weatherMonitorService.enable()
                    RequestRemoveBatteryRestrictionCommand(this@WeatherFragment).execute()
                }

                FeatureState.Unavailable -> toast(getString(R.string.weather_monitoring_disabled))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadingIndicator.reset()
        logger.start()
        useSeaLevelPressure = prefs.weather.useSeaLevelPressure
        units = prefs.pressureUnits
        ShowHistoricTemperatureDisclaimerCommand(requireContext()).execute()
        updateWeather()
    }

    override fun onPause() {
        super.onPause()
        logger.stop()
        loadingIndicator.hide()
    }

    private fun updateList() {
        if (!isViewReady) return
        val weather = weather ?: return

        val fields = listOf(
            // Alerts
            AlertWeatherField(weather.prediction.alerts),

            // Pressure
            PressureWeatherField(weather.observation?.pressure),
            PressureTendencyWeatherField(weather.pressureTendency),

            // Temperature
            TemperatureWeatherField(weather.observation?.temperature) { showTemperatureChart() },
            HistoricTemperatureRangeWeatherField(
                weather.prediction.temperature?.low,
                weather.prediction.temperature?.high
            ) { showTemperatureForecast() },

            // Humidity
            HumidityWeatherField(weather.observation?.humidity) { showHumidityChart() },
            CloudWeatherField(weather.clouds),

            // System
            PressureSystemWeatherField(weather.observation?.pressure),
            FrontWeatherField(weather.prediction.front),
        )

        weatherItems = fields.mapNotNull { it.getListItem(requireContext()) }
    }


    private fun updateWeather() {
        inBackground {
            val nextHistory = onIO {
                weatherSubsystem.getHistory().filter {
                    Duration.between(it.time, Instant.now()) <= prefs.weather.pressureHistory
                }
            }

            val nextRawHistory = loadRawWeatherReadings()
            val nextWeather = onIO { weatherSubsystem.getWeather() }

            val nextForecast = if (prefs.weather.showPressureForecast) {
                listOfNotNull(nextHistory.lastOrNull()?.pressureReading()) +
                        onIO { weatherSubsystem.getPressureForecast() }
            } else {
                emptyList()
            }

            onMain {
                history = nextHistory
                rawHistory = nextRawHistory
                weather = nextWeather
                pressureForecast = nextForecast
                displayPressureChart(history, rawHistory, pressureForecast)
                updateList()
            }
            updateForecast()
        }
    }

    private suspend fun loadRawWeatherReadings(): List<Reading<Pressure>> {
        if (isDebug()) {
            if (prefs.weather.useSeaLevelPressure) {
                val raw = weatherSubsystem.getRawHistory(true).filter {
                    Duration.between(
                        it.time,
                        Instant.now()
                    ) <= prefs.weather.pressureHistory
                }
                val useTemperature = prefs.weather.seaLevelFactorInTemp
                return raw.map {
                    Reading(it.value.seaLevel(useTemperature), it.time)
                }
            }
        }
        return emptyList()
    }

    private fun displayPressureChart(
        readings: List<WeatherObservation>,
        rawReadings: List<Reading<Pressure>>,
        forecast: List<Reading<Pressure>>
    ) {
        val displayReadings =
            readings.map { it.pressureReading() }.map { it.copy(value = it.value.convertTo(units)) }
        if (displayReadings.isNotEmpty()) {
            val displayForecast = forecast.map { it.copy(value = it.value.convertTo(units)) }
            chart.plot(
                displayReadings,
                rawReadings.map { it.copy(value = it.value.convertTo(units)) }.ifEmpty { null },
                displayForecast
            )
        }
    }

    private suspend fun updateForecast() {
        if (!isViewReady) return
        val weather = weather ?: return
        val prediction = weather.prediction
        onMain {
            binding.weatherTitle.title.text = formatService.formatWeather(prediction.primaryHourly)
            binding.weatherTitle.title.setOnClickListener {
                if (prediction.hourly.isNotEmpty()) {
                    val conditions = prediction.hourly.joinToString("\n") {
                        formatService.formatWeather(it)
                    }
                    dialog(getString(R.string.weather), conditions, cancelText = null)
                }
            }
            binding.weatherTitle.title.setCompoundDrawables(
                size = Resources.dp(requireContext(), 24f).toInt(),
                left = formatService.getWeatherImage(prediction.primaryHourly)
            )
            val arrival =
                formatService.formatWeatherArrival(weather.prediction.hourlyArrival)
            val then = getString(
                R.string.then_weather,
                formatService.formatWeather(prediction.primaryDaily).lowercase()
            )

            val hourlySameAsDaily = prediction.primaryDaily == prediction.primaryHourly

            binding.weatherTitle.subtitle.text =
                if (arrival.isNotEmpty() && (prediction.primaryDaily == null || hourlySameAsDaily)) {
                    arrival
                } else if (arrival.isNotEmpty()) {
                    "$arrival, $then"
                } else if (!hourlySameAsDaily) {
                    then
                } else {
                    ""
                }
            binding.weatherTitle.subtitle.isVisible =
                binding.weatherTitle.subtitle.text.isNotEmpty()
        }
    }

    private fun showHumidityChart() {
        val readings =
            history.filter { it.humidity != null }.map { Reading(it.humidity!!, it.time) }
        if (readings.size < 2) {
            return
        }
        val readingDuration = Duration.between(readings.first().time, Instant.now())
        CustomUiUtils.showChart(
            this, getString(
                R.string.humidity_history,
                formatService.formatDuration(readingDuration, true)
            )
        ) {
            val chart = HumidityChart(it)
            chart.plot(readings)
        }
    }

    private fun showTemperatureForecast() {
        inBackground {
            ShowHighLowTemperatureDialogCommand(this@WeatherFragment).execute()
        }
    }

    private fun showTemperatureChart() {
        val readings = history.map {
            Reading(it.temperature.convertTo(temperatureUnits).value, it.time)
        }
        if (readings.size < 2) {
            return
        }
        val readingDuration = Duration.between(readings.first().time, Instant.now())
        CustomUiUtils.showChart(
            this, getString(
                R.string.temperature_history,
                formatService.formatDuration(readingDuration, true)
            )
        ) {
            val chart = TemperatureChart(it)
            chart.plot(readings)
        }
    }

}

data class WeatherViews(
    val weatherTitle: Toolbar,
    val chart: Chart
)

@Composable
private fun WeatherHost(
    views: WeatherViews,
    weatherItems: List<WeatherListItem>,
    pressureMarkerText: String?,
    isWeatherUpdating: Boolean,
    monitorState: FeatureState,
    monitorFrequency: Duration?,
    formatService: FormatService,
    onChangeMonitorFrequency: () -> Unit,
    onToggleMonitor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { views.weatherTitle }
            )

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.chart_height))
                    .padding(horizontal = 16.dp)
                    .testTag("chart"),
                factory = { views.chart }
            )

            if (pressureMarkerText != null) {
                Text(
                    text = pressureMarkerText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("pressure_marker"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp)
                    .testTag("weather_list")
            ) {
                items(weatherItems, key = { it.id }) { item ->
                    WeatherListItemRow(item)
                    TrailSenseDivider()
                }
            }

            if (isWeatherUpdating) {
                UpdatingWeather(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            WeatherMonitorPlayBar(
                state = monitorState,
                frequency = monitorFrequency,
                formatService = formatService,
                onSubtitleClick = onChangeMonitorFrequency,
                onPlayClick = onToggleMonitor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun UpdatingWeather(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.testTag("weather_updating"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_update),
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.updating_weather),
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeatherMonitorPlayBar(
    state: FeatureState,
    frequency: Duration?,
    formatService: FormatService,
    onSubtitleClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOn = state == FeatureState.On
    val subtitle = stringResource(if (isOn) R.string.on else R.string.off) + (frequency?.let {
        " ${stringResource(R.string.dash)} ${
            formatService.formatDuration(
                it,
                short = true,
                includeSeconds = true
            )
        }"
    } ?: "")

    Surface(
        modifier = modifier.testTag("weather_play_bar"),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_weather),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
                    .testTag("play_bar_title")
            ) {
                Text(
                    text = stringResource(R.string.weather_monitor),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    modifier = Modifier.clickable(onClick = onSubtitleClick),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier.testTag("play_btn")
            ) {
                Icon(
                    painter = painterResource(
                        if (isOn) R.drawable.ic_baseline_stop_24 else R.drawable.ic_baseline_play_arrow_24
                    ),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun WeatherListItemRow(item: WeatherListItem) {
    TrailSenseListItem(
        title = item.title,
        subtitle = item.subtitle,
        icon = item.icon?.let { painterResource(it) },
        iconTint = item.iconTint?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
        trailing = item.trailingText?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        onClick = item.onClick,
        animateEntrance = false,
        modifier = Modifier
            .padding(horizontal = TrailSenseSpacing.lg)
            .testTag("weather_item_${item.id}")
    )
}

private fun createWeatherViews(context: Context): WeatherViews {
    val title = Toolbar(context, null).apply {
        id = R.id.weather_title
    }

    val chart = Chart(context).apply {
        id = R.id.chart
    }

    return WeatherViews(
        weatherTitle = title,
        chart = chart
    )
}

private fun Int.dp(context: Context): Int {
    return Resources.dp(context, toFloat()).toInt()
}

@Preview(showBackground = true)
@Composable
private fun WeatherHostPreview() {
    TrailSenseTheme {
        WeatherHost(
            views = createWeatherViews(LocalContext.current),
            weatherItems = emptyList(),
            pressureMarkerText = null,
            isWeatherUpdating = false,
            monitorState = FeatureState.Off,
            monitorFrequency = Duration.ofMinutes(15),
            formatService = FormatService.getInstance(LocalContext.current),
            onChangeMonitorFrequency = {},
            onToggleMonitor = {}
        )
    }
}

private class StateLoadingIndicator(
    private val setLoading: (Boolean) -> Unit
) : ILoadingIndicator {
    override fun show() {
        setLoading(true)
    }

    override fun hide() {
        setLoading(false)
    }
}
