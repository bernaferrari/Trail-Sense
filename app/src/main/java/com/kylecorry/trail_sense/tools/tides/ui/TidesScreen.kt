@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.tides.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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
fun TidesToolScreen(fragment: Fragment) {
    val controller = remember(fragment) { TidesController(fragment) }
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

private class TidesController(private val fragment: Fragment) {

    private val formatService by lazy { FormatService.getInstance(fragment.requireContext()) }
    private val tideService by lazy { TideService(fragment.requireContext()) }
    private val prefs by lazy { UserPreferences(fragment.requireContext()) }
    private val units by lazy { prefs.baseDistanceUnits }
    private val currentTideCommand by lazy { CurrentTideCommand(tideService) }
    private val dailyTideCommand by lazy { DailyTideCommand(tideService) }
    private val loadTideCommand by lazy { LoadTideTableCommand(fragment.requireContext()) }

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
            ShowTideDisclaimerCommand(fragment) {
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
        fragment.findNavController().navigate(R.id.action_tides_to_tideList)
    }

    fun onDisplayDateSelected(date: LocalDate) {
        displayDate = date
        updateDailyTide()
    }

    private fun loadTideTable(newTable: TideTable? = null) {
        isLoading = true
        daily = null
        current = null

        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val updatedTable = newTable ?: loadTideCommand.execute()
            val locationSubsystem = LocationSubsystem.getInstance(fragment.requireContext())
            if (updatedTable?.isAutomaticNearbyTide == true &&
                locationSubsystem.locationAge > Duration.ofMinutes(30)
            ) {
                locationSubsystem.updateLocation()
            }

            table = updatedTable
            isLoading = false

            if (updatedTable == null) {
                val cancelled = CoroutineAlerts.dialog(
                    fragment.requireContext(),
                    fragment.getString(R.string.no_tides),
                    fragment.getString(R.string.calibrate_new_tide)
                )
                if (!cancelled) {
                    fragment.findNavController().navigate(R.id.action_tides_to_tideList)
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
        dailyTideJob = fragment.viewLifecycleOwner.lifecycleScope.launch {
            daily = dailyTideCommand.execute(currentTable, date)
        }
    }

    private fun updateCurrentTide() {
        val currentTable = table ?: return
        currentTideJob?.cancel()
        currentTideJob = fragment.viewLifecycleOwner.lifecycleScope.launch {
            current = currentTideCommand.execute(currentTable)
        }
    }

    private fun startCurrentRefresh() {
        currentRefreshJob?.cancel()
        currentRefreshJob = fragment.viewLifecycleOwner.lifecycleScope.launch {
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
        val current = current ?: return fragment.getString(R.string.tides)
        val name = TideFormatter(fragment.requireContext()).getTideTypeName(current.type)
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
            ?: fragment.getString(android.R.string.untitled)
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
        Column(modifier = Modifier.fillMaxSize()) {
            TidesHeader(
                title = title,
                subtitle = subtitle,
                isRising = isRising,
                isLoading = isLoading,
                onOpenTideList = onOpenTideList
            )
            DatePickerInput(
                date = displayDate,
                onDateChange = onDateChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
            TideChartView(
                daily = daily,
                displayDate = displayDate,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            TideList(
                tides = tides,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            Text(
                text = stringResource(R.string.disclaimer_estimated_tide),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = dimensionResource(R.dimen.default_bottom_margin))
                    .testTag("tide_disclaimer"),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TidesHeader(
    title: String,
    subtitle: String,
    isRising: Boolean,
    isLoading: Boolean,
    onOpenTideList: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(
                        if (isRising) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.testTag("tide_title"),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onOpenTideList,
                    modifier = Modifier.testTag("tide_table_btn")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tide_table),
                        contentDescription = null
                    )
                }
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("loading")
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
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
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
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tide_list")
    ) {
        items(tides, key = { it.time.toInstant().toEpochMilli() }) { tide ->
            TideRow(tide)
            HorizontalDivider()
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(if (tide.isHigh) R.drawable.ic_tide_high else R.drawable.ic_tide_low),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
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
