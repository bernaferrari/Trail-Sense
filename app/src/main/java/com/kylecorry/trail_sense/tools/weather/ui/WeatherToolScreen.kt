package com.kylecorry.trail_sense.tools.weather.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
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
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.permissions.RemoveBatteryRestrictionsAlerter
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
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
import kotlin.math.abs
import kotlin.math.max

@Composable
fun WeatherToolScreen(
    fragment: TrailSenseComposeDestinationFragment,
    modifier: Modifier = Modifier
) {
    val controller = remember(fragment) { WeatherController(fragment) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.onCreate()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onDestroy()
        }
    }

    controller.Content(modifier = modifier)
}

private class WeatherController(
    private val fragment: TrailSenseComposeDestinationFragment
) {
    private var weatherViews: WeatherViews? =
        createWeatherViews(fragment.requireContext())
    private val views: WeatherViews
        get() = weatherViews ?: error("Weather views are not ready")
    private val isViewReady: Boolean
        get() = weatherViews != null

    private var useSeaLevelPressure = false
    private var units = PressureUnits.Hpa

    private val prefs by lazy { UserPreferences(fragment.requireContext()) }
    private val temperatureUnits by lazy { prefs.temperatureUnits }

    private val formatService by lazy { FormatService.getInstance(fragment.requireContext()) }

    private var history: List<WeatherObservation> = emptyList()
    private var pressureForecast: List<Reading<Pressure>> = emptyList()
    private var rawHistory: List<Reading<Pressure>> = emptyList()
    private var weatherItems by mutableStateOf<List<WeatherListItem>>(emptyList())
    private var pressureChartData by mutableStateOf(WeatherPressureChartData())
    private var pressureMarkerText by mutableStateOf<String?>(null)
    private var isWeatherUpdating by mutableStateOf(false)
    private var weatherMonitorState by mutableStateOf(FeatureState.Off)
    private var weatherMonitorFrequency by mutableStateOf<Duration?>(null)
    private var showWeatherFrequencyPicker by mutableStateOf(false)

    private val weatherSubsystem by lazy { WeatherSubsystem.getInstance(fragment.requireContext()) }
    private var weather: CurrentWeather? = null
    private val loadingIndicator by lazy {
        ResettableLoadingIndicator(
            StateLoadingIndicator { isWeatherUpdating = it }
        )
    }

    private val logger by lazy {
        WeatherLogger(
            fragment.requireContext(),
            Duration.ofSeconds(30),
            Duration.ofMillis(500),
            loadingIndicator
        )
    }

    private val weatherMonitorService by lazy {
        Tools.getService(fragment.requireContext(), WeatherToolRegistration.SERVICE_WEATHER_MONITOR)!!
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        val weatherFrequencyTitle = stringResource(R.string.pref_weather_update_frequency_title)
        WeatherHost(
            views = views,
            weatherItems = weatherItems,
            pressureChartData = pressureChartData,
            pressureMarkerText = pressureMarkerText,
            isWeatherUpdating = isWeatherUpdating,
            monitorState = weatherMonitorState,
            monitorFrequency = weatherMonitorFrequency,
            formatService = formatService,
            onChangeMonitorFrequency = {
                showWeatherFrequencyPicker = true
            },
            onPressureMarkerSelected = ::onPressureMarkerSelected,
            onToggleMonitor = ::toggleWeatherMonitor,
            modifier = modifier
        )

        if (showWeatherFrequencyPicker) {
            DurationPickerDialog(
                title = weatherFrequencyTitle,
                duration = prefs.weather.weatherUpdateFrequency,
                message = stringResource(R.string.actual_frequency_disclaimer),
                hint = stringResource(R.string.frequency),
                onDurationPicked = {
                    if (it != null) {
                        changeWeatherFrequency(it)
                    }
                    showWeatherFrequencyPicker = false
                },
                onDismiss = {
                    showWeatherFrequencyPicker = false
                }
            )
        }
    }

    fun onCreate() {
        WeatherQuickActionBinder(
            fragment,
            views.weatherTitle.leftButton,
            views.weatherTitle.rightButton,
            prefs.weather
        ).bind()

        fragment.observe(weatherSubsystem.weatherChanged) {
            updateWeather()
        }

        updateStatusBar()

        Tools.observe(fragment, WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_STATE_CHANGED) {
            updateStatusBar()
            true
        }

        Tools.observe(fragment, WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_FREQUENCY_CHANGED) {
            updateStatusBar()
            true
        }

    }

    fun onDestroy() {
        weatherViews = null
    }

    private fun updateStatusBar() {
        weatherMonitorState = weatherMonitorService.getFeatureState()
        weatherMonitorFrequency = weatherMonitorService.getFrequency()
    }

    private fun changeWeatherFrequency(frequency: Duration) {
        ChangeWeatherFrequencyCommand(fragment.requireContext()) { updateStatusBar() }.execute(frequency)
    }

    private fun toggleWeatherMonitor() {
        fragment.inBackground {
            when (weatherMonitorService.getFeatureState()) {
                FeatureState.On -> weatherMonitorService.disable()
                FeatureState.Off -> {
                    weatherMonitorService.enable()
                    RequestRemoveBatteryRestrictionCommand(
                        fragment.requireContext(),
                        RemoveBatteryRestrictionsAlerter(fragment)
                    ).execute()
                }

                FeatureState.Unavailable -> fragment.toast(fragment.getString(R.string.weather_monitoring_disabled))
            }
        }
    }

    fun onResume() {
        loadingIndicator.reset()
        logger.start()
        useSeaLevelPressure = prefs.weather.useSeaLevelPressure
        units = prefs.pressureUnits
        ShowHistoricTemperatureDisclaimerCommand(fragment.requireContext()).execute()
        updateWeather()
    }

    fun onPause() {
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

        weatherItems = fields.mapNotNull { it.getListItem(fragment.requireContext()) }
    }


    private fun updateWeather() {
        fragment.inBackground {
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
        val displayForecast = forecast.map { it.copy(value = it.value.convertTo(units)) }
        pressureChartData = WeatherPressureChartData(
            data = displayReadings,
            raw = rawReadings.map { it.copy(value = it.value.convertTo(units)) },
            forecast = displayForecast
        )
    }

    private fun onPressureMarkerSelected(timeAgo: Duration?, pressure: Float?) {
        pressureMarkerText = if (timeAgo == null || pressure == null) {
            null
        } else {
            val formatted = formatService.formatPressure(
                Pressure.from(pressure, units),
                Units.getDecimalPlaces(units)
            )
            fragment.getString(
                R.string.pressure_reading_time_ago,
                formatted,
                formatService.formatDuration(timeAgo, false)
            )
        }
    }

    private suspend fun updateForecast() {
        if (!isViewReady) return
        val weather = weather ?: return
        val prediction = weather.prediction
        onMain {
            views.weatherTitle.title = formatService.formatWeather(prediction.primaryHourly)
            views.weatherTitle.onTitleClick = {
                if (prediction.hourly.isNotEmpty()) {
                    val conditions = prediction.hourly.joinToString("\n") {
                        formatService.formatWeather(it)
                    }
                    fragment.dialog(fragment.getString(R.string.weather), conditions, cancelText = null)
                }
            }
            views.weatherTitle.icon = formatService.getWeatherImage(prediction.primaryHourly)
            val arrival =
                formatService.formatWeatherArrival(weather.prediction.hourlyArrival)
            val then = fragment.getString(
                R.string.then_weather,
                formatService.formatWeather(prediction.primaryDaily).lowercase()
            )

            val hourlySameAsDaily = prediction.primaryDaily == prediction.primaryHourly

            views.weatherTitle.subtitle =
                if (arrival.isNotEmpty() && (prediction.primaryDaily == null || hourlySameAsDaily)) {
                    arrival
                } else if (arrival.isNotEmpty()) {
                    "$arrival, $then"
                } else if (!hourlySameAsDaily) {
                    then
                } else {
                    ""
                }
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
            fragment, fragment.getString(
                R.string.humidity_history,
                formatService.formatDuration(readingDuration, true)
            )
        ) {
            val chart = HumidityChart(it)
            chart.plot(readings)
        }
    }

    private fun showTemperatureForecast() {
        fragment.inBackground {
            ShowHighLowTemperatureDialogCommand(fragment.requireContext()).execute()
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
            fragment, fragment.getString(
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
    val weatherTitle: WeatherTitleState
)

class WeatherTitleState {
    val leftButton = ComposeQuickActionButtonView()
    val rightButton = ComposeQuickActionButtonView()
    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var icon by mutableStateOf<Int?>(null)
    var onTitleClick by mutableStateOf<(() -> Unit)?>(null)
}

@Composable
private fun WeatherHost(
    views: WeatherViews,
    weatherItems: List<WeatherListItem>,
    pressureChartData: WeatherPressureChartData,
    pressureMarkerText: String?,
    isWeatherUpdating: Boolean,
    monitorState: FeatureState,
    monitorFrequency: Duration?,
    formatService: FormatService,
    onChangeMonitorFrequency: () -> Unit,
    onPressureMarkerSelected: (Duration?, Float?) -> Unit,
    onToggleMonitor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            WeatherTitle(
                state = views.weatherTitle,
                modifier = Modifier.fillMaxWidth()
            )

            WeatherPressureChart(
                data = pressureChartData.data,
                raw = pressureChartData.raw,
                forecast = pressureChartData.forecast,
                onMarkerSelected = onPressureMarkerSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.chart_height))
                    .padding(horizontal = 16.dp)
                    .testTag("chart")
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
private fun WeatherPressureChart(
    data: List<Reading<Pressure>>,
    raw: List<Reading<Pressure>>,
    forecast: List<Reading<Pressure>>,
    onMarkerSelected: (Duration?, Float?) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val forecastColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val rawColor = Color(AppColor.Gray.color).copy(alpha = 0.2f)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val highlightColor = MaterialTheme.colorScheme.onSurface
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    var selected by remember { mutableStateOf<WeatherPressureChartPoint?>(null) }
    var chartSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (data.isEmpty()) {
            Text(
                text = stringResource(R.string.no_data),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Box
        }

        val chartPoints = weatherPressureChartPoints(data, raw, forecast)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { chartSize = it }
                .pointerInput(data, chartSize) {
                    detectTapGestures { tap ->
                        val points = weatherPressureChartPoints(data, raw, forecast)
                        val primaryPoints = points.data
                        if (primaryPoints.isEmpty() || chartSize.width <= 0) {
                            selected = null
                            onMarkerSelected(null, null)
                            return@detectTapGestures
                        }

                        val selectedPoint = primaryPoints.minByOrNull {
                            abs(points.x(it.x, chartSize.width.toFloat()) - tap.x)
                        }
                        selected = selectedPoint
                        if (selectedPoint == null) {
                            onMarkerSelected(null, null)
                        } else {
                            val timeAgo = Duration.between(
                                points.startTime.plusSeconds((selectedPoint.x * 60 * 60).toLong()),
                                Instant.now()
                            )
                            onMarkerSelected(timeAgo, selectedPoint.pressure)
                        }
                    }
                }
        ) {
            repeat(5) { index ->
                val y = index / 4f * size.height
                drawLine(
                    color = grid,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            fun drawSeries(points: List<WeatherPressureChartPoint>, color: Color, strokeWidth: Float) {
                points.zipWithNext().forEach { (start, end) ->
                    drawLine(
                        color = color,
                        start = Offset(
                            chartPoints.x(start.x, size.width),
                            chartPoints.y(start.pressure, size.height)
                        ),
                        end = Offset(
                            chartPoints.x(end.x, size.width),
                            chartPoints.y(end.pressure, size.height)
                        ),
                        strokeWidth = strokeWidth
                    )
                }
            }

            drawSeries(chartPoints.raw, rawColor, 2.dp.toPx())
            drawSeries(chartPoints.forecast, forecastColor, 2.dp.toPx())
            drawSeries(chartPoints.data, primary, 2.dp.toPx())

            selected?.let {
                drawCircle(
                    color = highlightColor,
                    radius = 4.dp.toPx(),
                    center = Offset(
                        chartPoints.x(it.x, size.width),
                        chartPoints.y(it.pressure, size.height)
                    )
                )
            }
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

@Composable
private fun WeatherTitle(
    state: WeatherTitleState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        state.leftButton.Content()

        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = state.onTitleClick != null) {
                    state.onTitleClick?.invoke()
                }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            state.icon?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(24.dp)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                if (state.subtitle.isNotEmpty()) {
                    Text(
                        text = state.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        state.rightButton.Content()
    }
}

private fun createWeatherViews(context: Context): WeatherViews {
    return WeatherViews(
        weatherTitle = WeatherTitleState()
    )
}

private fun weatherPressureChartPoints(
    data: List<Reading<Pressure>>,
    raw: List<Reading<Pressure>>,
    forecast: List<Reading<Pressure>>
): WeatherPressureChartPoints {
    val startTime = data.firstOrNull()?.time
        ?: raw.firstOrNull()?.time
        ?: forecast.firstOrNull()?.time
        ?: Instant.now()
    fun toPoints(readings: List<Reading<Pressure>>): List<WeatherPressureChartPoint> {
        return readings.map {
            WeatherPressureChartPoint(
                Duration.between(startTime, it.time).toMillis() / 3_600_000f,
                it.value.value
            )
        }
    }

    val points = toPoints(data)
    val rawPoints = toPoints(raw)
    val forecastPoints = toPoints(forecast)
    val allPoints = points + rawPoints + forecastPoints
    val units = data.firstOrNull()?.value?.units ?: raw.firstOrNull()?.value?.units ?: PressureUnits.Hpa
    val margin = Pressure.hpa(1f).convertTo(units).value
    val minRange = Pressure.hpa(40f).convertTo(units).value
    val minX = allPoints.minOfOrNull { it.x } ?: 0f
    val maxX = allPoints.maxOfOrNull { it.x } ?: 1f
    val minYValue = allPoints.minOfOrNull { it.pressure } ?: 0f
    val maxYValue = allPoints.maxOfOrNull { it.pressure } ?: 1f
    val yMinimum = minYValue - margin
    val yMaximum = max(maxYValue + margin, yMinimum + minRange)
    return WeatherPressureChartPoints(
        startTime = startTime,
        data = points,
        raw = rawPoints,
        forecast = forecastPoints,
        minX = minX,
        xRange = (maxX - minX).takeIf { it > 0f } ?: 1f,
        yMinimum = yMinimum,
        yRange = (yMaximum - yMinimum).takeIf { it > 0f } ?: 1f
    )
}

@Preview(showBackground = true)
@Composable
private fun WeatherHostPreview() {
    TrailSenseTheme {
        WeatherHost(
            views = createWeatherViews(LocalContext.current),
            weatherItems = emptyList(),
            pressureChartData = WeatherPressureChartData(),
            pressureMarkerText = null,
            isWeatherUpdating = false,
            monitorState = FeatureState.Off,
            monitorFrequency = Duration.ofMinutes(15),
            formatService = FormatService.getInstance(LocalContext.current),
            onChangeMonitorFrequency = {},
            onPressureMarkerSelected = { _, _ -> },
            onToggleMonitor = {}
        )
    }
}

private data class WeatherPressureChartData(
    val data: List<Reading<Pressure>> = emptyList(),
    val raw: List<Reading<Pressure>> = emptyList(),
    val forecast: List<Reading<Pressure>> = emptyList()
)

private data class WeatherPressureChartPoint(
    val x: Float,
    val pressure: Float
)

private data class WeatherPressureChartPoints(
    val startTime: Instant,
    val data: List<WeatherPressureChartPoint>,
    val raw: List<WeatherPressureChartPoint>,
    val forecast: List<WeatherPressureChartPoint>,
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
