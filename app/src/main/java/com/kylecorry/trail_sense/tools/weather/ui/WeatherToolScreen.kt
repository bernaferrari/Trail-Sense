@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.weather.ui

import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.WbSunny
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseValueChip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAccents
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStatGrid
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import com.kylecorry.sol.science.meteorology.WeatherCondition
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseIconWell
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseWellColors
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseWellColors
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.clip
import com.kylecorry.trail_sense.shared.views.compose.drawTrailSenseNowDot
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSmoothPath
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.widthIn
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
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
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons
import com.kylecorry.trail_sense.shared.permissions.RemoveBatteryRestrictionsAlerter
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.charts.TrailSenseLineChart
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max

@Composable
fun WeatherToolScreen(
    host: TrailSenseDestinationHost,
    modifier: Modifier = Modifier
) {
    val controller = remember(host) { WeatherController(host) }
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
    private val host: TrailSenseDestinationHost
) {
    private var weatherViews: WeatherViews? =
        createWeatherViews(host.requireContext())
    private val views: WeatherViews
        get() = weatherViews ?: error("Weather views are not ready")
    private val isViewReady: Boolean
        get() = weatherViews != null

    private var useSeaLevelPressure = false
    private var units = PressureUnits.Hpa

    private val prefs by lazy { UserPreferences(host.requireContext()) }
    private val temperatureUnits by lazy { prefs.temperatureUnits }

    private val formatService by lazy { FormatService.getInstance(host.requireContext()) }

    private var history: List<WeatherObservation> = emptyList()
    private var pressureForecast: List<Reading<Pressure>> = emptyList()
    private var rawHistory: List<Reading<Pressure>> = emptyList()
    private var weatherItems by mutableStateOf<List<WeatherListItem>>(emptyList())
    private var pressureChartData by mutableStateOf(WeatherPressureChartData())
    private var pressureMarkerText by mutableStateOf<String?>(null)
    private var isWeatherUpdating by mutableStateOf(false)
    private var tendencyDirection by mutableStateOf(0)
    private var weatherMonitorState by mutableStateOf(FeatureState.Off)
    private var weatherMonitorFrequency by mutableStateOf<Duration?>(null)
    private var showWeatherFrequencyPicker by mutableStateOf(false)
    private var hasLoaded by mutableStateOf(false)

    private val weatherSubsystem by lazy { WeatherSubsystem.getInstance(host.requireContext()) }
    private var weather: CurrentWeather? = null
    private val loadingIndicator by lazy {
        ResettableLoadingIndicator(
            StateLoadingIndicator { isWeatherUpdating = it }
        )
    }

    private val logger by lazy {
        WeatherLogger(
            host.requireContext(),
            Duration.ofSeconds(30),
            Duration.ofMillis(500),
            loadingIndicator
        )
    }

    private val weatherMonitorService by lazy {
        Tools.getService(host.requireContext(), WeatherToolRegistration.SERVICE_WEATHER_MONITOR)!!
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
            modifier = modifier,
            tendencyDirection = tendencyDirection,
            isEmpty = hasLoaded && pressureChartData.data.size < 2
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
            host,
            views.weatherTitle.leftButton,
            views.weatherTitle.rightButton,
            prefs.weather
        ).bind()

        host.lifecycleScope.launch {
            weatherSubsystem.weatherChanged.flow.collect {
                updateWeather()
            }
        }

        updateStatusBar()

        Tools.observe(host, WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_STATE_CHANGED) {
            updateStatusBar()
        }

        Tools.observe(host, WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_FREQUENCY_CHANGED) {
            updateStatusBar()
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
        ChangeWeatherFrequencyCommand(host.requireContext()) { updateStatusBar() }.execute(frequency)
    }

    private fun toggleWeatherMonitor() {
        host.inBackground {
            when (weatherMonitorService.getFeatureState()) {
                FeatureState.On -> weatherMonitorService.disable()
                FeatureState.Off -> {
                    weatherMonitorService.enable()
                    RequestRemoveBatteryRestrictionCommand(
                        host.requireContext(),
                        RemoveBatteryRestrictionsAlerter(host)
                    ).execute()
                }

                FeatureState.Unavailable -> host.toast(host.getString(R.string.weather_monitoring_disabled))
            }
        }
    }

    fun onResume() {
        loadingIndicator.reset()
        logger.start()
        useSeaLevelPressure = prefs.weather.useSeaLevelPressure
        units = prefs.pressureUnits
        ShowHistoricTemperatureDisclaimerCommand(host.requireContext()).execute()
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

        weatherItems = fields.mapNotNull { it.getListItem(host.requireContext()) }
        tendencyDirection = weather.pressureTendency.characteristic.name.let {
            when {
                it.startsWith("Rising") && it.endsWith("Fast") -> 2
                it.startsWith("Rising") -> 1
                it.startsWith("Falling") && it.endsWith("Fast") -> -2
                it.startsWith("Falling") -> -1
                else -> 0
            }
        }
    }


    private fun updateWeather() {
        host.inBackground {
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
                hasLoaded = true
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
            host.getString(
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
                    Alerts.dialog(
                        host.requireContext(),
                        host.getString(R.string.weather),
                        conditions,
                        cancelText = null
                    )
                }
            }
            views.weatherTitle.icon = formatService.getWeatherImage(prediction.primaryHourly)
            views.weatherTitle.condition = prediction.primaryHourly
            val arrival =
                formatService.formatWeatherArrival(weather.prediction.hourlyArrival)
            val then = host.getString(
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
        CustomUiUtils.showComposeContentDialog(
            context = host.requireContext(),
            title = host.getString(
                R.string.humidity_history,
                formatService.formatDuration(readingDuration, true)
            )
        ) {
            TrailSenseLineChart(
                readings = readings,
                minimum = 0f,
                maximum = 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.chart_height))
            )
        }
    }

    private fun showTemperatureForecast() {
        host.inBackground {
            ShowHighLowTemperatureDialogCommand(host.requireContext()).execute()
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
        CustomUiUtils.showComposeContentDialog(
            context = host.requireContext(),
            title = host.getString(
                R.string.temperature_history,
                formatService.formatDuration(readingDuration, true)
            )
        ) {
            TrailSenseLineChart(
                readings = readings,
                freezingValue = Temperature.celsius(0f).convertTo(temperatureUnits).value,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.chart_height))
            )
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
    var icon by mutableStateOf<ImageVector?>(null)
    var condition by mutableStateOf<WeatherCondition?>(null)
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
    modifier: Modifier = Modifier,
    tendencyDirection: Int = 0,
    isEmpty: Boolean = false
) {
    // One question: will the weather turn? The forecast answers it; it is the product's second hero moment and turns
    // errorContainer only for a storm. Pressure (the evidence) and the other readings sit calmly under it.
    val alert = weatherItems.firstOrNull { it.id == WEATHER_ALERT_ID }
    val pressure = weatherItems.firstOrNull { it.id == 1L }
    val tendency = weatherItems.firstOrNull { it.id == 2L }
    val rows = weatherItems.filter { it.id != WEATHER_ALERT_ID && it.id != 1L && it.id != 2L }

    val storm = isStorm(views.weatherTitle.condition)
    // A standard page: the large title, and the two quick actions (clouds, temperature) in the bar instead of
    // floating in the forecast's corners.
    TrailSensePage(
        title = stringResource(R.string.weather),
        modifier = modifier.testTag("weather_list"),
        actions = {
            listOf(views.weatherTitle.leftButton, views.weatherTitle.rightButton)
                .filter { it.isVisible }
                .forEach {
                    it.Content(
                        modifier = Modifier.size(48.dp),
                        offContainerColor = Color.Transparent,
                        offContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        }
    ) {
            // Before the monitor has two readings there is no forecast to give: the hero becomes the empty state,
            // whose one button starts the monitor. The pressure and readings the phone has now still sit below it.
            if (isEmpty) {
                item(key = "weather_empty") {
                    val isOn = monitorState == FeatureState.On
                    TrailSenseEmptyState(
                        icon = TrailIcons.Weather,
                        title = stringResource(R.string.weather_empty_title),
                        description = stringResource(
                            if (isOn) R.string.weather_empty_recording else R.string.weather_empty_description
                        ),
                        actionLabel = if (isOn) null else stringResource(R.string.weather_empty_action),
                        onAction = if (isOn) null else onToggleMonitor,
                        actionModifier = Modifier.testTag("play_btn"),
                        modifier = Modifier.testTag("chart")
                    )
                }
            } else {
                item(key = "weather_hero") {
                    WeatherHeroCard(
                        state = views.weatherTitle,
                        isUpdating = isWeatherUpdating,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // An alert is one plain row under the forecast: the warning role, and the error role only in a storm.
            if (alert != null) {
                item(key = "weather_alert") {
                    TrailSenseListItem(
                        title = alert.title,
                        subtitle = alert.trailingText,
                        icon = rememberVectorPainter(TrailIcons.Alert),
                        iconTint = if (storm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                        position = TrailSenseGroupPosition.Single,
                        onClick = alert.onClick,
                        modifier = Modifier.testTag("weather_item_${alert.id}")
                    )
                }
            }

            // Once it is on (or once there is a forecast), the monitor is one row: its state and how often it reads.
            if (!isEmpty || monitorState == FeatureState.On) {
                item(key = "weather_monitor") {
                    WeatherMonitorRow(
                        state = monitorState,
                        frequency = monitorFrequency,
                        formatService = formatService,
                        onSubtitleClick = onChangeMonitorFrequency,
                        onPlayClick = onToggleMonitor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item(key = "weather_chart") {
                WeatherPressureSection(
                    pressure = pressure,
                    tendency = tendency,
                    tendencyDirection = tendencyDirection,
                    data = pressureChartData,
                    markerText = pressureMarkerText,
                    formatService = formatService,
                    onMarkerSelected = onPressureMarkerSelected,
                    showChart = !isEmpty
                )
            }

            // The other readings as one headerless group: the page title already says "Weather".
            if (rows.isNotEmpty()) {
                item(key = "weather_readouts") {
                    Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                        rows.forEachIndexed { index, item ->
                            WeatherListItemRow(item, TrailSenseGroupPosition.at(index, rows.size))
                        }
                    }
                }
            }
    }
}

private const val WEATHER_ALERT_ID = 6293L

/**
 * Pressure on the plain page: the reading in emphasised type, one verdict line with the tendency under it, and the
 * history in the chart frame. With fewer than two readings it says plainly that the chart fills in.
 */
@Composable
private fun WeatherPressureSection(
    pressure: WeatherListItem?,
    tendency: WeatherListItem?,
    tendencyDirection: Int,
    data: WeatherPressureChartData,
    markerText: String?,
    formatService: FormatService,
    onMarkerSelected: (Duration?, Float?) -> Unit,
    showChart: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (pressure != null) Modifier.testTag("weather_item_1") else Modifier)
    ) {
        TrailSenseGroupHeader(stringResource(R.string.pressure))
        val reading = pressure?.trailingText
        val number = reading?.substringBeforeLast(' ')
        val unit = reading?.substringAfterLast(' ', "")?.takeIf { reading.contains(' ') }
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
        ) {
            // A small barometer whose needle points at the reading (low left, high right), when there is one.
            val hpa = reading?.let(::pressureHpa)
            if (hpa != null) {
                PressureGaugeIcon(
                    hpa,
                    scheme.primary,
                    Modifier
                        .padding(end = TrailSenseSpacing.sm)
                        .size(32.dp)
                        .align(Alignment.CenterVertically)
                )
            }
            Text(
                number ?: stringResource(R.string.sky_pressure_waiting),
                style = (if (number != null) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleLarge)
                    .copy(fontFeatureSettings = "tnum"),
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = scheme.onSurface,
                modifier = Modifier.alignByBaseline()
            )
            if (unit != null) {
                Text(
                    unit,
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = TrailSenseSpacing.xs)
                        .alignByBaseline()
                )
            }
        }
        if (tendency?.trailingText != null) {
            Text(
                stringResource(
                    when {
                        tendencyDirection > 0 -> R.string.sky_pressure_verdict_rising
                        tendencyDirection < 0 -> R.string.sky_pressure_verdict_falling
                        else -> R.string.sky_pressure_verdict_steady
                    }
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
                modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.xxs)
            )
            Text(
                tendency.trailingText,
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = scheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = TrailSenseSpacing.lg)
                    .testTag("weather_item_2")
            )
        }
        if (!showChart) return@Column
        Spacer(Modifier.height(TrailSenseSpacing.lg))
        if (data.data.size >= 2) {
            // A steady pressure has nothing to show in a tall box: the chart shrinks to a compact strip.
            val values = data.data.map { it.value.convertTo(PressureUnits.Hpa).value }
            val isFlat = (values.maxOrNull() ?: 0f) - (values.minOrNull() ?: 0f) < 1f
            WeatherPressureChart(
                data = data.data,
                raw = data.raw,
                forecast = data.forecast,
                onMarkerSelected = onMarkerSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isFlat) 112.dp else 168.dp)
                    .testTag("chart")
            )
            WeatherChartAxis(data, formatService)
        } else {
            Text(
                stringResource(R.string.weather_chart_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = TrailSenseSpacing.lg)
                    .testTag("chart")
            )
        }
        AnimatedVisibility(
            visible = markerText != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = markerText ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = TrailSenseSpacing.md)
                    .testTag("pressure_marker")
            )
        }
    }
}

/** The span of the chart under it: how long ago it starts, and now. */
@Composable
private fun WeatherChartAxis(data: WeatherPressureChartData, formatService: FormatService) {
    val start = (data.data.firstOrNull() ?: data.raw.firstOrNull())?.time ?: return
    val span = Duration.between(start, Instant.now())
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = TrailSenseSpacing.sm, start = com.kylecorry.trail_sense.shared.views.compose.TrailSenseChartGutter)
    ) {
        Text(
            stringResource(R.string.time_ago, formatService.formatDuration(span, short = true)),
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            stringResource(R.string.now),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
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
    val forecastColor = MaterialTheme.colorScheme.tertiary
    val rawColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val surface = MaterialTheme.colorScheme.background
    var selected by remember { mutableStateOf<WeatherPressureChartPoint?>(null) }
    var chartSize by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (data.isEmpty()) {
            Text(
                text = stringResource(R.string.no_data),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Box
        }

        val chartPoints = weatherPressureChartPoints(data, raw, forecast)
        // Word axes (High, Normal, Low for this window) live in the frame's gutter, so no line ever crosses them.
        com.kylecorry.trail_sense.shared.views.compose.TrailSenseChartFrame(
            yLabels = listOf(
                stringResource(R.string.sky_axis_high),
                stringResource(R.string.sky_axis_normal),
                stringResource(R.string.sky_axis_low)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = com.kylecorry.trail_sense.shared.views.compose.TrailSenseChartInset)
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
                val pad = 12.dp.toPx()
                val h = size.height - 2 * pad
                // No numeric grid and no wash: the gutter's words carry the scale, the panel is the only frame.

                fun at(point: WeatherPressureChartPoint) = Offset(
                    chartPoints.x(point.x, size.width),
                    pad + chartPoints.y(point.pressure, h)
                )

                fun pathOf(points: List<WeatherPressureChartPoint>): Path = Path().apply {
                    points.forEachIndexed { index, point ->
                        val o = at(point)
                        if (index == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
                    }
                }

                if (chartPoints.raw.size >= 2) {
                    drawPath(
                        pathOf(chartPoints.raw),
                        color = rawColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                // The forecast is the screen's one accent: a dotted tertiary line onward from now.
                if (chartPoints.forecast.size >= 2) {
                    drawPath(
                        pathOf(chartPoints.forecast),
                        color = forecastColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(2.dp.toPx(), 7.dp.toPx())
                            )
                        )
                    )
                }
                if (chartPoints.data.size >= 2) {
                    drawPath(
                        trailSenseSmoothPath(chartPoints.data.map(::at)),
                        color = primary,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                chartPoints.data.lastOrNull()?.let {
                    drawTrailSenseNowDot(at(it), primary, surface)
                }

                selected?.let {
                    val o = at(it)
                    drawLine(
                        primary.copy(alpha = 0.5f),
                        Offset(o.x, 0f),
                        Offset(o.x, size.height),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                        )
                    )
                    drawCircle(primary, radius = 6.dp.toPx(), center = o)
                }
            }
        }
    }
}

/**
 * The monitor as one family row: its state and how often it reads (tap the row to change that), and a switch as the
 * one trailing element.
 */
@Composable
private fun WeatherMonitorRow(
    state: FeatureState,
    frequency: Duration?,
    formatService: FormatService,
    onSubtitleClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOn = state == FeatureState.On
    val subtitle = stringResource(if (isOn) R.string.on else R.string.off) + (frequency?.let {
        " · ${formatService.formatDuration(it, short = true, includeSeconds = true)}"
    } ?: "")
    val label = stringResource(if (isOn) R.string.stop else R.string.start)

    Box(modifier.testTag("weather_play_bar")) {
    TrailSenseListItem(
        title = stringResource(R.string.weather_monitor),
        subtitle = subtitle,
        icon = rememberVectorPainter(TrailIcons.Update),
        position = TrailSenseGroupPosition.Single,
        onClick = onSubtitleClick,
        modifier = Modifier.testTag("play_bar_title"),
        trailing = {
            androidx.compose.material3.Switch(
                checked = isOn,
                onCheckedChange = { onPlayClick() },
                modifier = Modifier
                    .semantics { contentDescription = label }
                    .testTag("play_btn")
            )
        }
    )
    }
}

@Composable
private fun WeatherListItemRow(item: WeatherListItem, position: TrailSenseGroupPosition) {
    TrailSenseListItem(
        title = item.title,
        subtitle = item.subtitle,
        icon = item.icon?.let { rememberVectorPainter(it) },
        position = position,
        onClick = item.onClick,
        modifier = Modifier.testTag("weather_item_${item.id}"),
        trailing = item.trailingText?.let { value ->
            {
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 3,
                    modifier = Modifier.widthIn(max = 200.dp)
                )
            }
        }
    )
}

private fun isStorm(condition: WeatherCondition?): Boolean =
    condition == WeatherCondition.Storm || condition == WeatherCondition.Thunderstorm

/**
 * The forecast, straight on the page: its shape in primary, the condition in display type and one line under it.
 * Only for a storm does it gain a container: errorContainer, its shape bursting, the one moment this screen raises
 * its voice.
 */
@Composable
private fun WeatherHeroCard(
    state: WeatherTitleState,
    isUpdating: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val storm = isStorm(state.condition)
    val container by androidx.compose.animation.animateColorAsState(
        if (storm) scheme.errorContainer else Color.Transparent,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "weatherHero"
    )
    val content = if (storm) scheme.onErrorContainer else scheme.onSurface
    val secondaryContent = if (storm) scheme.onErrorContainer.copy(alpha = 0.8f) else scheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(TrailSenseShape.hero),
        color = container,
        contentColor = content
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = state.onTitleClick != null) { state.onTitleClick?.invoke() }
                .padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SkyFocalShape(
                // The clear-sky glyph is a bare disc; a sun with rays reads as the sun inside the shape.
                icon = if (state.condition == WeatherCondition.Clear) {
                    androidx.compose.material.icons.Icons.Rounded.WbSunny
                } else {
                    state.icon ?: TrailIcons.Weather
                },
                shape = if (storm) MaterialShapes.Burst.toShape() else MaterialShapes.Cookie12Sided.toShape(),
                size = 88.dp,
                containerColor = if (storm) scheme.error else scheme.primary,
                contentColor = if (storm) scheme.onError else scheme.onPrimary
            )
            Spacer(Modifier.height(TrailSenseSpacing.lg))
            Text(
                text = state.title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (state.subtitle.isNotEmpty()) {
                Text(
                    text = state.subtitle,
                    modifier = Modifier.padding(top = TrailSenseSpacing.xs),
                    style = MaterialTheme.typography.bodyLarge,
                    color = secondaryContent,
                    textAlign = TextAlign.Center
                )
            }
            AnimatedVisibility(visible = isUpdating) {
                Row(
                    modifier = Modifier
                        .padding(top = TrailSenseSpacing.md)
                        .testTag("weather_updating"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.CircularWavyProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = content
                    )
                    Text(
                        text = stringResource(R.string.updating_weather),
                        modifier = Modifier.padding(start = TrailSenseSpacing.sm),
                        style = MaterialTheme.typography.labelLarge,
                        color = secondaryContent
                    )
                }
            }
        }
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
    val minRange = Pressure.hpa(8f).convertTo(units).value
    val minX = allPoints.minOfOrNull { it.x } ?: 0f
    val maxX = allPoints.maxOfOrNull { it.x } ?: 1f
    val minYValue = allPoints.minOfOrNull { it.pressure } ?: 0f
    val maxYValue = allPoints.maxOfOrNull { it.pressure } ?: 1f
    // At least the minimum range, centered on the readings so a steady pressure sits mid-chart, not on the floor.
    val span = max(maxYValue - minYValue + 2 * margin, minRange)
    val mid = (minYValue + maxYValue) / 2
    val yMinimum = mid - span / 2
    val yMaximum = mid + span / 2
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

/** The reading in hPa from its formatted text ("1013.2 hPa", "29.92 in", "760 mm"...), or null when unknown. */
private fun pressureHpa(text: String): Float? {
    val number = text.substringBefore(' ').replace(",", ".").toFloatOrNull() ?: return null
    val unit = text.substringAfter(' ', "").lowercase()
    return when {
        unit.startsWith("in") -> number * 33.8639f
        unit.startsWith("mm") -> number * 1.33322f
        unit.startsWith("psi") -> number * 68.9476f
        else -> number
    }
}

/**
 * A tiny barometer whose needle points at the reading: low pressure (stormy, 970 hPa) to the left, high (fair,
 * 1050 hPa) to the right, with the arc filled up to the needle.
 */
@Composable
private fun PressureGaugeIcon(hpa: Float, color: Color, modifier: Modifier = Modifier) {
    val fraction = ((hpa - 970f) / (1050f - 970f)).coerceIn(0f, 1f)
    val animated by androidx.compose.animation.core.animateFloatAsState(
        fraction,
        MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "gauge"
    )
    Canvas(modifier) {
        val stroke = size.minDimension * 0.14f
        val inset = stroke / 2f + size.minDimension * 0.04f
        val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset + size.height * 0.08f)
        val start = 150f
        val sweep = 240f
        val cap = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawArc(color.copy(alpha = 0.28f), start, sweep, false, topLeft, arcSize, style = cap)
        drawArc(color, start, sweep * animated, false, topLeft, arcSize, style = cap)
        val center = androidx.compose.ui.geometry.Offset(topLeft.x + arcSize.width / 2f, topLeft.y + arcSize.height / 2f)
        val angle = Math.toRadians((start + sweep * animated).toDouble())
        val length = arcSize.width / 2f * 0.62f
        drawLine(
            color,
            center,
            androidx.compose.ui.geometry.Offset(
                center.x + (kotlin.math.cos(angle) * length).toFloat(),
                center.y + (kotlin.math.sin(angle) * length).toFloat()
            ),
            strokeWidth = stroke * 0.9f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawCircle(color, radius = stroke * 0.9f, center = center)
    }
}
