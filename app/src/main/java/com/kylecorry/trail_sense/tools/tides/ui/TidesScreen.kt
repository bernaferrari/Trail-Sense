@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.tides.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.commands.CurrentTideCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.DailyTideCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.LoadTideTableCommand
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@Composable
fun TidesToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { TidesController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                Lifecycle.Event.ON_DESTROY -> controller.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onPause()
        }
    }

    TidesScreen(
        title = controller.getCurrentTideTitle(),
        subtitle = controller.getTableSubtitle(),
        isRising = controller.current?.rising ?: true,
        isLoading = controller.isLoading,
        displayDate = controller.displayDate,
        daily = controller.daily,
        tides = controller.getDisplayTides(),
        onDateChange = controller::onDisplayDateSelected,
        onOpenTideList = controller::openTideList
    )
}

private class TidesController(private val host: TrailSenseDestinationHost) {

    private val formatService by lazy { FormatService.getInstance(host.requireContext()) }
    private val tideService by lazy { TideService(host.requireContext()) }
    private val prefs by lazy { UserPreferences(host.requireContext()) }
    private val units by lazy { prefs.baseDistanceUnits }
    private val currentTideCommand by lazy { CurrentTideCommand(tideService) }
    private val dailyTideCommand by lazy { DailyTideCommand(tideService) }
    private val loadTideCommand by lazy { LoadTideTableCommand(host.requireContext()) }

    private var table by mutableStateOf<TideTable?>(null)
    var current by mutableStateOf<CurrentTideData?>(null)
        private set
    var daily by mutableStateOf<DailyTideData?>(null)
        private set
    var displayDate by mutableStateOf(LocalDate.now())
        private set
    var isLoading by mutableStateOf(true)
        private set

    private var currentTideJob: Job? = null
    private var dailyTideJob: Job? = null
    private var currentRefreshJob: Job? = null

    fun onResume() {
        if (table == null) {
            displayDate = LocalDate.now()
            ShowTideDisclaimerCommand(host.requireContext(), host.findNavController()) {
                loadTideTable()
            }.execute()
        } else {
            startCurrentRefresh()
        }
    }

    fun onPause() {
        currentTideJob?.cancel()
        dailyTideJob?.cancel()
        currentRefreshJob?.cancel()
    }

    fun openTideList() {
        table = null
        host.findNavController().navigate(R.id.action_tides_to_tideList)
    }

    fun onDisplayDateSelected(date: LocalDate) {
        displayDate = date
        updateDailyTide()
    }

    private fun loadTideTable(newTable: TideTable? = null) {
        isLoading = true
        daily = null
        current = null

        host.lifecycleScope.launch {
            val updatedTable = newTable ?: loadTideCommand.execute()
            val locationSubsystem = LocationSubsystem.getInstance(host.requireContext())
            if (updatedTable?.isAutomaticNearbyTide == true &&
                locationSubsystem.locationAge > Duration.ofMinutes(30)
            ) {
                locationSubsystem.updateLocation()
            }

            table = updatedTable
            isLoading = false

            if (updatedTable == null) {
                val cancelled = CoroutineAlerts.dialog(
                    host.requireContext(),
                    host.getString(R.string.no_tides),
                    host.getString(R.string.calibrate_new_tide)
                )
                if (!cancelled) {
                    host.findNavController().navigate(R.id.action_tides_to_tideList)
                }
                return@launch
            }

            updateDailyTide()
            updateCurrentTide()
            startCurrentRefresh()
        }
    }

    private fun updateDailyTide() {
        val currentTable = table ?: return
        val date = displayDate
        dailyTideJob?.cancel()
        dailyTideJob = host.lifecycleScope.launch {
            daily = dailyTideCommand.execute(currentTable, date)
        }
    }

    private fun updateCurrentTide() {
        val currentTable = table ?: return
        currentTideJob?.cancel()
        currentTideJob = host.lifecycleScope.launch {
            current = currentTideCommand.execute(currentTable)
        }
    }

    private fun startCurrentRefresh() {
        currentRefreshJob?.cancel()
        currentRefreshJob = host.lifecycleScope.launch {
            while (isActive) {
                updateCurrentTide()
                delay(Duration.ofMinutes(1).toMillis())
            }
        }
    }

    fun getDisplayTides(): List<Tide> {
        val currentDaily = daily ?: return emptyList()
        val currentTable = table
        return currentDaily.tides.map {
            val matchingTide = currentTable?.tides?.firstOrNull { tide ->
                Duration.between(tide.time, it.time).abs() < Duration.ofMinutes(2)
            }

            it.copy(height = matchingTide?.height)
        }
    }

    @SuppressLint("SetTextI18n")
    fun getCurrentTideTitle(): String {
        val current = current ?: return host.getString(R.string.tides)
        val name = TideFormatter(host.requireContext()).getTideTypeName(current.type)
        val waterLevel = if (current.waterLevel != null) {
            val height = Distance.meters(current.waterLevel).convertTo(units)
            " (${formatService.formatDistance(height, 2, true)})"
        } else {
            ""
        }
        return name + waterLevel
    }

    fun getTableSubtitle(): String {
        val table = table ?: return ""
        return table.name
            ?: table.location?.let { formatService.formatLocation(it) }
            ?: host.getString(android.R.string.untitled)
    }
}

@Composable
private fun TidesScreen(
    title: String,
    subtitle: String,
    isRising: Boolean,
    isLoading: Boolean,
    displayDate: LocalDate,
    daily: DailyTideData?,
    tides: List<Tide>,
    onDateChange: (LocalDate) -> Unit,
    onOpenTideList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TrailSenseSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            TidesHero(
                title = title,
                subtitle = subtitle,
                isRising = isRising,
                isLoading = isLoading,
                onOpenTideList = onOpenTideList
            )

            TrailSenseCard {
                DatePickerInput(
                    date = displayDate,
                    onDateChange = onDateChange
                )
                Spacer(modifier = Modifier.height(TrailSenseSpacing.md))
                TideChartView(
                    daily = daily,
                    displayDate = displayDate
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
                SectionLabel(text = stringResource(R.string.tides))
                TrailSenseCard {
                    TideList(tides = tides)
                }
            }

            Text(
                text = stringResource(R.string.disclaimer_estimated_tide),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(R.dimen.default_bottom_margin))
                    .testTag("tide_disclaimer"),
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun TonalIconChip(
    painter: androidx.compose.ui.graphics.painter.Painter,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun TidesHero(
    title: String,
    subtitle: String,
    isRising: Boolean,
    isLoading: Boolean,
    onOpenTideList: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = TrailSenseSpacing.lg,
                        end = TrailSenseSpacing.sm,
                        top = TrailSenseSpacing.lg,
                        bottom = TrailSenseSpacing.lg
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(
                        if (isRising) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = TrailSenseSpacing.md)
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.testTag("tide_title"),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                IconButton(
                    onClick = onOpenTideList,
                    modifier = Modifier.testTag("tide_table_btn")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tide_table),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("loading"),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DatePickerInput(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember(context) { FormatService.getInstance(context) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onDateChange(date.minusDays(1)) },
            modifier = Modifier.testTag("prev_date")
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(180f)
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = formatter.formatRelativeDate(date),
            modifier = Modifier.testTag("tide_list_date"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Box(
            modifier = Modifier
                .padding(start = TrailSenseSpacing.lg)
                .size(48.dp)
                .combinedClickable(
                    onClick = {
                        CustomUiUtils.pickDate(context, date, null) {
                            if (it != null) {
                                onDateChange(it)
                            }
                        }
                    },
                    onLongClick = { onDateChange(LocalDate.now()) }
                )
                .testTag("date_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_date),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = { onDateChange(date.plusDays(1)) },
            modifier = Modifier.testTag("next_date")
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TideChartView(
    daily: DailyTideData?,
    displayDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val dailyData = daily
    val levelColor = if (UserPreferences(LocalContext.current).useDynamicColors) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(AppColor.Blue.color)
    }
    val grid = MaterialTheme.colorScheme.outlineVariant
    val highlightColor = MaterialTheme.colorScheme.onSurface
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.chart_height))
            .testTag("chart"),
        contentAlignment = Alignment.Center
    ) {
        if (dailyData == null || dailyData.waterLevels.isEmpty()) {
            Text(
                text = stringResource(R.string.no_data),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Box
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val startTime = dailyData.waterLevels.firstOrNull()?.time ?: Instant.now()
            val totalRange = dailyData.waterLevelRange.end - dailyData.waterLevelRange.start
            val rangeDelta = (totalRange * 0.25f).coerceAtLeast(0.2f)
            val minimum = dailyData.waterLevelRange.start - rangeDelta
            val maximum = dailyData.waterLevelRange.end + rangeDelta
            val valueRange = (maximum - minimum).takeIf { it > 0f } ?: 1f

            fun x(time: Instant): Float {
                val hours = Duration.between(startTime, time).toMillis() / 3_600_000f
                val totalHours = 24f
                return (hours / totalHours).coerceIn(0f, 1f) * size.width
            }

            fun y(value: Float): Float {
                val normalized = ((value - minimum) / valueRange).coerceIn(0f, 1f)
                return size.height - normalized * size.height
            }

            repeat(7) { index ->
                val x = index / 6f * size.width
                drawLine(
                    color = grid,
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val path = Path()
            dailyData.waterLevels.forEachIndexed { index, reading ->
                val pointX = x(reading.time)
                val pointY = y(reading.value)
                if (index == 0) {
                    path.moveTo(pointX, size.height)
                    path.lineTo(pointX, pointY)
                } else {
                    path.lineTo(pointX, pointY)
                }
            }
            val last = dailyData.waterLevels.last()
            path.lineTo(x(last.time), size.height)
            path.close()

            drawPath(path, levelColor.copy(alpha = 0.2f))

            dailyData.waterLevels.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = levelColor,
                    start = androidx.compose.ui.geometry.Offset(x(start.time), y(start.value)),
                    end = androidx.compose.ui.geometry.Offset(x(end.time), y(end.value)),
                    strokeWidth = 2.dp.toPx()
                )
            }

            val currentLevel = dailyData.waterLevels.minByOrNull {
                Duration.between(Instant.now(), it.time).abs()
            }
            if (currentLevel != null && displayDate == LocalDate.now()) {
                drawCircle(
                    color = highlightColor,
                    radius = 4.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(
                        x(currentLevel.time),
                        y(currentLevel.value)
                    )
                )
            }
        }
    }
}

@Composable
private fun TideList(
    tides: List<Tide>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tide_list"),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
    ) {
        if (tides.isEmpty()) {
            Text(
                text = stringResource(R.string.no_data),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TrailSenseSpacing.md),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        tides.forEach { tide ->
            TideRow(tide)
        }
    }
}

@Composable
private fun TideRow(tide: Tide) {
    val context = LocalContext.current
    val formatter = remember(context) { FormatService.getInstance(context) }
    val units = remember(context) { UserPreferences(context).baseDistanceUnits }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (tide.height == null) {
                    Alerts.dialog(
                        context,
                        context.getString(R.string.disclaimer_estimated_tide_title),
                        context.getString(R.string.disclaimer_estimated_tide),
                        cancelText = null
                    )
                }
            }
            .padding(vertical = TrailSenseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TonalIconChip(
            painter = painterResource(
                if (tide.isHigh) R.drawable.ic_tide_high else R.drawable.ic_tide_low
            )
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = TrailSenseSpacing.md)
        ) {
            Text(
                text = stringResource(if (tide.isHigh) R.string.high_tide else R.string.low_tide),
                modifier = Modifier.testTag("tide_event_title"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (tide.height == null) {
                    stringResource(R.string.estimated)
                } else {
                    formatter.formatDistance(
                        Distance.meters(tide.height!!).convertTo(units),
                        2,
                        true
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatter.formatTime(tide.time.toLocalTime(), includeSeconds = false),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TidesScreenPreview() {
    TrailSenseTheme {
        TidesScreen(
            title = "High tide",
            subtitle = "Harbor",
            isRising = true,
            isLoading = false,
            displayDate = LocalDate.now(),
            daily = null,
            tides = emptyList(),
            onDateChange = {},
            onOpenTideList = {}
        )
    }
}
