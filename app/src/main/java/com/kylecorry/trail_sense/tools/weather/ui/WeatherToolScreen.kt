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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
            tendencyDirection = tendencyDirection
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
    tendencyDirection: Int = 0
) {
    // The four live readings become a connected stat grid under the hero; the alert becomes a banner; the rest stay rows.
    val alert = weatherItems.firstOrNull { it.id == WEATHER_ALERT_ID }
    val statIds = listOf(1L, 2L, 5L, 6L)
    val stats = statIds.mapNotNull { id -> weatherItems.firstOrNull { it.id == id } }
    val rows = weatherItems.filter { it.id != WEATHER_ALERT_ID && it.id !in statIds }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("weather_list"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    top = TrailSenseSpacing.lg,
                    bottom = TrailSenseSpacing.lg
                ),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                item(key = "weather_hero") {
                    WeatherHeroCard(
                        state = views.weatherTitle,
                        alert = alert,
                        isUpdating = isWeatherUpdating,
                        modifier = Modifier.fillMaxWidth()
                    )
                }


                // Pressure leads as one full-width card holding its value, where it is heading and its history;
                // the other readings sit two-up under it.
                item(key = "weather_chart") {
                    val pressure = weatherItems.firstOrNull { it.id == 1L }
                    val tendency = weatherItems.firstOrNull { it.id == 2L }
                    WeatherPressureCard(
                        pressure = pressure,
                        tendency = tendency,
                        tendencyDirection = tendencyDirection,
                        data = pressureChartData,
                        markerText = pressureMarkerText,
                        formatService = formatService,
                        onMarkerSelected = onPressureMarkerSelected
                    )
                }

                val others = stats.filter { it.id != 1L && it.id != 2L }
                if (others.isNotEmpty()) {
                    item(key = "weather_stats") {
                        TrailSenseStatGrid(
                            stats = others.map { it.toStat(tendencyDirection) },
                            columns = 2
                        )
                    }
                }

                if (rows.isNotEmpty()) {
                    item(key = "weather_readouts") {
                        Column {
                            TrailSenseGroupHeader(stringResource(R.string.weather))
                            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                                rows.forEachIndexed { index, item ->
                                    WeatherListItemRow(item, TrailSenseGroupPosition.at(index, rows.size))
                                }
                            }
                        }
                    }
                }
            }

            WeatherMonitorPlayBar(
                state = monitorState,
                frequency = monitorFrequency,
                formatService = formatService,
                onSubtitleClick = onChangeMonitorFrequency,
                onPlayClick = onToggleMonitor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = TrailSenseSpacing.lg,
                        end = TrailSenseSpacing.lg,
                        bottom = TrailSenseSpacing.lg
                    )
            )
        }
    }
}

private const val WEATHER_ALERT_ID = 6293L

/** A reading as a stat tile: the pressure is the highlighted one, the tendency says in colour where it is heading. */
private fun WeatherListItem.toStat(tendencyDirection: Int): TrailSenseStat {
    val tone = when (id) {
        1L -> TrailSenseTone.Primary
        2L -> when {
            tendencyDirection >= 1 -> TrailSenseTone.Good
            tendencyDirection <= -2 -> TrailSenseTone.Caution
            else -> TrailSenseTone.Neutral
        }

        5L -> TrailSenseTone.Tertiary
        else -> TrailSenseTone.Neutral
    }
    return TrailSenseStat(
        label = title,
        value = trailingText ?: "—",
        supportingText = subtitle,
        tone = tone,
        icon = icon,
        onClick = onClick
    )
}

@Composable
private fun WeatherAlertBanner(item: WeatherListItem) {
    val colors = trailSenseToneColors(TrailSenseTone.Caution)
    Surface(
        onClick = { item.onClick?.invoke() },
        enabled = item.onClick != null,
        shape = RoundedCornerShape(TrailSenseShape.groupOuter),
        color = colors.container,
        contentColor = colors.content,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weather_item_${item.id}")
    ) {
        Row(
            Modifier.padding(TrailSenseSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrailSenseShapeBadge(
                icon = TrailIcons.Alert,
                shape = MaterialShapes.SoftBurst.toShape(),
                size = 52.dp,
                containerColor = colors.accent,
                contentColor = colors.onAccent
            )
            Spacer(Modifier.width(TrailSenseSpacing.lg))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.content.copy(alpha = 0.8f)
                )
                Text(
                    item.trailingText ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
            }
            if (item.onClick != null) {
                Icon(TrailIcons.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

/**
 * Pressure as the screen's second anchor: a full-width primary card with the reading at display size, the tendency
 * as a toned chip, and the history chart inside it. With fewer than two readings it says plainly that the chart
 * fills in as the monitor records.
 */
@Composable
private fun WeatherPressureCard(
    pressure: WeatherListItem?,
    tendency: WeatherListItem?,
    tendencyDirection: Int,
    data: WeatherPressureChartData,
    markerText: String?,
    formatService: FormatService,
    onMarkerSelected: (Duration?, Float?) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(TrailSenseShape.groupOuter),
        color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (pressure != null) Modifier.testTag("weather_item_1") else Modifier)
    ) {
        Column(Modifier.padding(TrailSenseSpacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(TrailIcons.Barometer, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(TrailSenseSpacing.xs))
                Text(
                    stringResource(R.string.pressure),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = TrailSenseSpacing.xs)
            ) {
                Text(
                    pressure?.trailingText ?: "--",
                    style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (tendency?.trailingText != null) {
                    TrailSenseValueChip(
                        text = tendency.trailingText,
                        icon = tendency.icon,
                        tone = when {
                            tendencyDirection >= 1 -> TrailSenseTone.Good
                            tendencyDirection <= -2 -> TrailSenseTone.Caution
                            else -> TrailSenseTone.Secondary
                        },
                        strong = tendencyDirection != 0,
                        modifier = Modifier.testTag("weather_item_2")
                    )
                }
            }
            Spacer(Modifier.height(TrailSenseSpacing.md))
            if (data.data.size >= 2) {
                WeatherPressureChart(
                    data = data.data,
                    raw = data.raw,
                    forecast = data.forecast,
                    onMarkerSelected = onMarkerSelected,
                    ringColor = scheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                        .testTag("chart")
                )
                WeatherChartAxis(data, formatService)
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(scheme.surfaceContainerLowest.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                        .padding(TrailSenseSpacing.md)
                        .testTag("chart"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkyFocalShape(
                        icon = TrailIcons.Chart,
                        shape = MaterialShapes.Cookie9Sided.toShape(),
                        size = 48.dp,
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                        spin = false
                    )
                    Spacer(Modifier.width(TrailSenseSpacing.md))
                    Column {
                        Text(
                            stringResource(R.string.sky_pressure_chart_waiting),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.weather_chart_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = markerText != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                SkyPill(
                    text = markerText ?: "",
                    icon = TrailIcons.Barometer,
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                    modifier = Modifier
                        .padding(top = TrailSenseSpacing.md)
                        .testTag("pressure_marker")
                )
            }
        }
    }
}

@Composable
private fun WeatherChartEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TrailSenseSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
    ) {
        SkyFocalShape(
            icon = TrailIcons.Barometer,
            shape = MaterialShapes.Cookie9Sided.toShape(),
            size = 72.dp,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = stringResource(R.string.no_data),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.weather_chart_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
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
            .padding(top = TrailSenseSpacing.sm, start = 44.dp)
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
    modifier: Modifier = Modifier,
    ringColor: Color = MaterialTheme.colorScheme.surfaceContainerLow
) {
    val primary = MaterialTheme.colorScheme.primary
    val forecastColor = MaterialTheme.colorScheme.tertiary
    val rawColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val surface = ringColor
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
                // The value labels get their own column at the left; the plot keeps room for the now dot's halo.
                .padding(start = 44.dp, end = 12.dp)
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
            drawSkyGrid(grid, lines = 3, top = pad, bottom = size.height - pad)

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
            // The recorded pressure: a deep wash under the line, then the line itself.
            if (chartPoints.data.size >= 2) {
                val line = pathOf(chartPoints.data)
                val area = Path().apply {
                    addPath(line)
                    lineTo(at(chartPoints.data.last()).x, size.height)
                    lineTo(at(chartPoints.data.first()).x, size.height)
                    close()
                }
                drawPath(
                    area,
                    Brush.verticalGradient(listOf(primary.copy(alpha = 0.34f), primary.copy(alpha = 0.02f)))
                )
                drawPath(
                    line,
                    color = primary,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Now: a ringed dot at the end of the record.
            chartPoints.data.lastOrNull()?.let {
                val o = at(it)
                drawCircle(primary.copy(alpha = 0.18f), radius = 12.dp.toPx(), center = o)
                drawCircle(surface, radius = 6.dp.toPx(), center = o)
                drawCircle(primary, radius = 4.dp.toPx(), center = o)
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
                drawCircle(primary.copy(alpha = 0.22f), radius = 12.dp.toPx(), center = o)
                drawCircle(primary, radius = 6.dp.toPx(), center = o)
            }
        }

        // The values of the grid lines, so a steady pressure reads as steady rather than as a broken flat line.
        val decimals = when {
            chartPoints.yRange < 2f -> 2
            chartPoints.yRange < 30f -> 0
            else -> 0
        }
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(40.dp)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            listOf(1f, 0.5f, 0f).forEach { fraction ->
                val value = chartPoints.yMinimum + chartPoints.yRange * fraction
                Text(
                    String.format(java.util.Locale.getDefault(), "%.${decimals}f", value),
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = textColor
                )
            }
        }
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
        " · ${
            formatService.formatDuration(
                it,
                short = true,
                includeSeconds = true
            )
        }"
    } ?: "")

    // The monitor as one tonal bar that fills with the accent while it runs; its badge turns into a turning sun.
    val container by androidx.compose.animation.animateColorAsState(
        if (isOn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "monitorBar"
    )
    val content = if (isOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier.testTag("weather_play_bar"),
        shape = RoundedCornerShape(TrailSenseShape.groupOuter),
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrailSenseSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkyFocalShape(
                icon = TrailIcons.Weather,
                shape = if (isOn) MaterialShapes.Sunny.toShape() else MaterialShapes.Cookie6Sided.toShape(),
                size = 44.dp,
                containerColor = if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (isOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                spin = isOn
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = TrailSenseSize.minimumTouchTarget)
                    .clickable(onClick = onSubtitleClick)
                    .padding(horizontal = TrailSenseSpacing.md)
                    .testTag("play_bar_title"),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.weather_monitor),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFeatureSettings = "tnum"
                    ),
                    color = content.copy(alpha = 0.75f)
                )
            }
            FilledIconButton(
                onClick = onPlayClick,
                colors = IconButtonDefaults.filledIconButtonColors(),
                shape = if (isOn) RoundedCornerShape(16.dp) else RoundedCornerShape(50),
                modifier = Modifier
                    .size(width = 64.dp, height = 52.dp)
                    .testTag("play_btn")
            ) {
                Icon(
                    imageVector = if (isOn) TrailIcons.BaselineStop24 else TrailIcons.BaselinePlayArrow24,
                    contentDescription = stringResource(
                        if (isOn) R.string.stop else R.string.start
                    )
                )
            }
        }
    }
}

@Composable
private fun WeatherListItemRow(item: WeatherListItem, position: TrailSenseGroupPosition) {
    SkyValueRow(
        title = item.title,
        subtitle = item.subtitle,
        value = item.trailingText,
        icon = item.icon,
        accent = item.iconTint?.let { Color(it) }?.takeIf { it.hasHue() } ?: TrailSenseAccents.Sky,
        badgeShape = when (item.id) {
            3L -> MaterialShapes.Circle.toShape()
            4L -> MaterialShapes.Arch.toShape()
            7L -> MaterialShapes.Puffy.toShape()
            9L -> MaterialShapes.Pill.toShape()
            else -> MaterialShapes.Cookie6Sided.toShape()
        },
        position = position,
        onClick = item.onClick,
        modifier = Modifier.testTag("weather_item_${item.id}")
    )
}

/** Grey tints (the old secondary text colour) carry no meaning, so those rows fall back to the sky accent. */
private fun Color.hasHue(): Boolean {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    return alpha > 0.9f && max - min > 0.12f
}

/** Tone of the hero for a forecast: storms warn, clear skies are warm, rain is cool. */
private fun weatherTone(condition: WeatherCondition?): TrailSenseTone = when (condition) {
    WeatherCondition.Clear -> TrailSenseTone.Tertiary
    WeatherCondition.Storm, WeatherCondition.Thunderstorm -> TrailSenseTone.Error
    WeatherCondition.Wind -> TrailSenseTone.Caution
    WeatherCondition.Overcast -> TrailSenseTone.Secondary
    WeatherCondition.Precipitation, WeatherCondition.Rain, WeatherCondition.Snow -> TrailSenseTone.Primary
    null -> TrailSenseTone.Primary
}

@Composable
private fun WeatherHeroCard(
    state: WeatherTitleState,
    alert: WeatherListItem?,
    isUpdating: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = trailSenseToneColors(weatherTone(state.condition))
    val container by androidx.compose.animation.animateColorAsState(
        colors.container,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "weatherHero"
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(TrailSenseShape.hero),
        color = container,
        contentColor = colors.content
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = state.onTitleClick != null) { state.onTitleClick?.invoke() }
                .padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The forecast's shaped mark, flanked by the two quick actions.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                state.leftButton.Content(offContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.72f))
                SkyFocalShape(
                    // The clear-sky glyph is a bare disc; a sun with rays reads as the sun inside the shape.
                    icon = if (state.condition == WeatherCondition.Clear) {
                        androidx.compose.material.icons.Icons.Rounded.WbSunny
                    } else {
                        state.icon ?: TrailIcons.Weather
                    },
                    shape = when (state.condition) {
                        WeatherCondition.Clear -> MaterialShapes.Sunny.toShape()
                        WeatherCondition.Storm, WeatherCondition.Thunderstorm -> MaterialShapes.Burst.toShape()
                        WeatherCondition.Overcast, WeatherCondition.Precipitation,
                        WeatherCondition.Rain, WeatherCondition.Snow -> MaterialShapes.Puffy.toShape()

                        WeatherCondition.Wind -> MaterialShapes.Flower.toShape()
                        null -> MaterialShapes.Cookie12Sided.toShape()
                    },
                    size = 132.dp,
                    containerColor = colors.accent,
                    contentColor = colors.onAccent,
                    spin = state.condition == WeatherCondition.Clear ||
                            state.condition == WeatherCondition.Wind ||
                            state.condition == null
                )
                state.rightButton.Content(offContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.72f))
            }
            Spacer(Modifier.height(TrailSenseSpacing.lg))
            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (state.subtitle.isNotEmpty()) {
                Text(
                    text = state.subtitle,
                    modifier = Modifier.padding(top = TrailSenseSpacing.xs),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.content.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
            // An alert rides in the hero as a strong chip; tap it for what it means.
            if (alert != null) {
                Surface(
                    onClick = { alert.onClick?.invoke() },
                    shape = RoundedCornerShape(50),
                    color = trailSenseToneColors(TrailSenseTone.Caution).accent,
                    contentColor = trailSenseToneColors(TrailSenseTone.Caution).onAccent,
                    modifier = Modifier
                        .padding(top = TrailSenseSpacing.md)
                        .testTag("weather_item_${alert.id}")
                ) {
                    Row(
                        Modifier.padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(TrailIcons.Alert, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(TrailSenseSpacing.sm))
                        Text(
                            listOfNotNull(alert.title, alert.trailingText).joinToString(" · "),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    }
                }
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
                        color = colors.content
                    )
                    Text(
                        text = stringResource(R.string.updating_weather),
                        modifier = Modifier.padding(start = TrailSenseSpacing.sm),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.content.copy(alpha = 0.8f)
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
