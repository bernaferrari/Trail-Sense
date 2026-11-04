@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.tides.ui

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import android.annotation.SuppressLint
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.CoroutineAlerts
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseHeroCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
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
import java.time.LocalTime
import java.time.ZonedDateTime

@Composable
fun TidesToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { TidesController(host) }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
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
        isRising = controller.current?.rising,
        nextTide = controller.getNextTideText(),
        icon = controller.getCurrentTideIcon(),
        isLoading = controller.isLoading,
        displayDate = controller.displayDate,
        daily = controller.daily,
        tides = controller.getDisplayTides(),
        onDateChange = controller::onDisplayDateSelected,
        onOpenTideList = controller::openTideList,
        onBack = { dispatcher?.onBackPressed() }
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
    var nextTide by mutableStateOf<Tide?>(null)
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
        nextTide = null

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
            nextTide = getNextTide(currentTable)
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

    /** The first high or low after now, looking into tomorrow when today's are past. */
    private suspend fun getNextTide(table: TideTable): Tide? = onDefault {
        val now = ZonedDateTime.now()
        val today = now.toLocalDate()
        listOf(today, today.plusDays(1)).firstNotNullOfOrNull { date ->
            tideService.getTides(table, date).firstOrNull { it.time.isAfter(now) }
        }
    }

    fun getNextTideText(): String? {
        val tide = nextTide ?: return null
        val name = host.getString(if (tide.isHigh) R.string.high_tide else R.string.low_tide)
        return host.getString(
            R.string.tides_next_tide,
            name,
            formatService.formatTime(tide.time.toLocalTime(), includeSeconds = false)
        )
    }

    @DrawableRes
    fun getCurrentTideIcon(): Int {
        val current = current ?: return R.drawable.ic_tide_half
        return TideFormatter(host.requireContext()).getTideTypeImage(current.type)
    }

    @SuppressLint("SetTextI18n")
    fun getCurrentTideTitle(): String {
        val current = current ?: return "--"
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

/**
 * Tides: the hero says the tide now (high, low or half, with the water level), whether it is rising, and when the
 * next tide turns; the table's name sits above it and the tide tables button in the top bar. Below, the day: a date
 * row (previous, pick or long-press for today, next) and the water level chart with a time axis, then that day's
 * highs and lows as grouped rows, and the estimate disclaimer as a quiet footer.
 */
@Composable
private fun TidesScreen(
    title: String,
    subtitle: String,
    isRising: Boolean?,
    nextTide: String?,
    @DrawableRes icon: Int,
    isLoading: Boolean,
    displayDate: LocalDate,
    daily: DailyTideData?,
    tides: List<Tide>,
    onDateChange: (LocalDate) -> Unit,
    onOpenTideList: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val supporting = listOfNotNull(
        isRising?.let { stringResource(if (it) R.string.tides_rising else R.string.tides_falling) },
        nextTide
    ).joinToString(" · ").ifBlank { null }

    TrailSensePage(
        title = stringResource(R.string.tides),
        onBack = onBack,
        backLabel = stringResource(R.string.back),
        modifier = modifier,
        actions = {
            IconButton(onClick = onOpenTideList, modifier = Modifier.testTag("tide_table_btn")) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.TideTable),
                    contentDescription = stringResource(R.string.tide_table)
                )
            }
        }
    ) {
        item(key = "hero") {
            TrailSenseHeroCard(
                modifier = Modifier
                    .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())
                    .testTag("tide_title"),
                eyebrow = subtitle.ifBlank { null },
                title = title,
                supportingText = supporting,
                icon = painterResource(icon),
                action = if (isLoading) {
                    {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("loading"),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                } else {
                    null
                }
            )
        }

        trailSenseSection(key = "day") {
            row { position ->
                TrailSenseGroupCard(
                    position = position,
                    contentPadding = PaddingValues(
                        horizontal = TrailSenseSpacing.xs,
                        vertical = TrailSenseSpacing.xs
                    )
                ) {
                    DatePickerInput(date = displayDate, onDateChange = onDateChange)
                }
            }
            row { position ->
                TrailSenseGroupCard(position = position) {
                    TideChartView(daily = daily, displayDate = displayDate)
                }
            }
        }

        item(key = "tides") {
            TrailSenseRowGroup(
                modifier = Modifier.testTag("tide_list"),
                title = stringResource(R.string.tides)
            ) {
                if (tides.isEmpty() && daily != null) {
                    row { position ->
                        TidesGroupRow(position = position) {
                            Text(
                                text = stringResource(R.string.no_tides),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                tides.forEach { tide ->
                    row { position -> TideRow(tide = tide, position = position) }
                }
            }
        }

        item(key = "disclaimer") {
            Text(
                text = stringResource(R.string.disclaimer_estimated_tide),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.lg)
                    .testTag("tide_disclaimer"),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    val dateLabel = stringResource(R.string.date)
    val todayLabel = stringResource(R.string.today)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onDateChange(date.minusDays(1)) },
            modifier = Modifier.testTag("prev_date")
        ) {
            Icon(
                painter = rememberVectorPainter(TrailIcons.KeyboardArrowRight),
                contentDescription = stringResource(R.string.previous),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(180f)
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = TrailSenseSize.minimumTouchTarget)
                .clip(RoundedCornerShape(TrailSenseShape.iconContainer))
                .combinedClickable(
                    onClickLabel = dateLabel,
                    onLongClickLabel = todayLabel,
                    role = Role.Button,
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
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = rememberVectorPainter(TrailIcons.Date),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(TrailSenseSpacing.sm))
            Text(
                text = formatter.formatRelativeDate(date),
                modifier = Modifier.testTag("tide_list_date"),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(
            onClick = { onDateChange(date.plusDays(1)) },
            modifier = Modifier.testTag("next_date")
        ) {
            Icon(
                painter = rememberVectorPainter(TrailIcons.KeyboardArrowRight),
                contentDescription = stringResource(R.string.next),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Hours marked on the chart's time axis; the grid lines sit on the same hours. */
private val chartAxisHours = listOf(0, 6, 12, 18, 24)

@Composable
private fun TideChartView(
    daily: DailyTideData?,
    displayDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember(context) { FormatService.getInstance(context) }
    val dailyData = daily
    val levelColor = if (UserPreferences(context).useDynamicColors) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(AppColor.Blue.color)
    }
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val highlightColor = MaterialTheme.colorScheme.onSurface
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisLabels = remember(formatter) {
        chartAxisHours.map { hour ->
            formatter.formatTime(LocalTime.of(hour % 24, 0), includeSeconds = false, includeMinutes = false)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.chart_height))
                .testTag("chart"),
            contentAlignment = Alignment.Center
        ) {
            if (dailyData == null || dailyData.waterLevels.isEmpty()) {
                if (dailyData != null) {
                    Text(
                        text = stringResource(R.string.no_data),
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
                    return (hours / 24f).coerceIn(0f, 1f) * size.width
                }

                fun y(value: Float): Float {
                    val normalized = ((value - minimum) / valueRange).coerceIn(0f, 1f)
                    return size.height - normalized * size.height
                }

                chartAxisHours.forEach { hour ->
                    val x = hour / 24f * size.width
                    drawLine(
                        color = grid,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                drawLine(
                    color = grid,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )

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
                        start = Offset(x(start.time), y(start.value)),
                        end = Offset(x(end.time), y(end.value)),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                val currentLevel = dailyData.waterLevels.minByOrNull {
                    Duration.between(Instant.now(), it.time).abs()
                }
                if (currentLevel != null && displayDate == LocalDate.now()) {
                    val center = Offset(x(currentLevel.time), y(currentLevel.value))
                    drawCircle(color = levelColor.copy(alpha = 0.24f), radius = 8.dp.toPx(), center = center)
                    drawCircle(color = highlightColor, radius = 4.dp.toPx(), center = center)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            axisLabels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = when (index) {
                        0 -> TextAlign.Start
                        axisLabels.lastIndex -> TextAlign.End
                        else -> TextAlign.Center
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = textColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TideRow(tide: Tide, position: TrailSenseGroupPosition) {
    val context = LocalContext.current
    val formatter = remember(context) { FormatService.getInstance(context) }
    val units = remember(context) { UserPreferences(context).baseDistanceUnits }
    val height = tide.height

    TidesGroupRow(
        position = position,
        onClick = if (height == null) {
            {
                Alerts.dialog(
                    context,
                    context.getString(R.string.disclaimer_estimated_tide_title),
                    context.getString(R.string.disclaimer_estimated_tide),
                    cancelText = null
                )
            }
        } else {
            null
        },
        leading = {
            TidesIconWell(
                rememberVectorPainter(if (tide.isHigh) TrailIcons.TideHigh else TrailIcons.TideLow)
            )
        },
        trailing = {
            Text(
                text = formatter.formatTime(tide.time.toLocalTime(), includeSeconds = false),
                modifier = Modifier.padding(end = TrailSenseSpacing.md),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    ) {
        Text(
            text = stringResource(if (tide.isHigh) R.string.high_tide else R.string.low_tide),
            modifier = Modifier.testTag("tide_event_title"),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (height == null) {
                stringResource(R.string.estimated)
            } else {
                formatter.formatDistance(Distance.meters(height).convertTo(units), 2, true)
            },
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
            title = "High (1.20 ft)",
            subtitle = "Harbor",
            isRising = true,
            nextTide = "Next: Low at 3:42 PM",
            icon = R.drawable.ic_tide_high,
            isLoading = false,
            displayDate = LocalDate.now(),
            daily = null,
            tides = emptyList(),
            onDateChange = {},
            onOpenTideList = {}
        )
    }
}
