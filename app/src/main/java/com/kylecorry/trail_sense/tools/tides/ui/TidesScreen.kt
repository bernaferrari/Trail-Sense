@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.tides.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.font.FontWeight
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import android.annotation.SuppressLint
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
        currentType = controller.current?.type,
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

    fun getCurrentTideIcon(): ImageVector {
        val current = current ?: return TrailIcons.TideHalf
        return TideFormatter(host.requireContext()).getTideTypeIcon(current.type)
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
    currentType: TideType? = null,
    icon: ImageVector,
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

    // One accent for the whole screen: the tide now sets the hero, the chart and the highlighted next tide.
    val heroTone = when (currentType) {
        TideType.High -> TrailSenseTone.Primary
        TideType.Low -> TrailSenseTone.Tertiary
        else -> TrailSenseTone.Primary
    }
    val heroColors = trailSenseToneColors(heroTone)

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
            val colors = trailSenseToneColors(heroTone)
            TrailSenseSpotlight(
                modifier = Modifier
                    .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())
                    .testTag("tide_title"),
                eyebrow = subtitle.ifBlank { null },
                // Before the first estimate the hero says what it is doing rather than showing a dash.
                title = if (title == "--") stringResource(R.string.sky_tides_loading) else title,
                titleStyle = if (title == "--") MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                supportingText = isRising?.let { stringResource(if (it) R.string.tides_rising else R.string.tides_falling) },
                tone = heroTone,
                badge = {
                    // The tide's mark on a wavy cookie, with an arrow for which way the water is going.
                    Box {
                        TrailSenseShapeBadge(
                            icon = icon,
                            shape = MaterialShapes.Cookie12Sided.toShape(),
                            size = 72.dp,
                            containerColor = colors.accent,
                            contentColor = colors.onAccent
                        )
                        if (isRising != null) {
                            TrailSenseShapeBadge(
                                icon = if (isRising) TrailIcons.ArrowUp else TrailIcons.ArrowDown,
                                shape = MaterialShapes.Circle.toShape(),
                                size = 28.dp,
                                containerColor = colors.container,
                                contentColor = colors.accent,
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }
                    }
                },
                trailing = if (isLoading) {
                    {
                        androidx.compose.material3.CircularWavyProgressIndicator(
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("loading"),
                            color = colors.accent
                        )
                    }
                } else {
                    null
                }
            ) {
                if (nextTide != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.72f),
                        contentColor = colors.content
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                rememberVectorPainter(TrailIcons.ToolClock),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = colors.accent
                            )
                            Spacer(Modifier.width(TrailSenseSpacing.md))
                            Text(
                                nextTide,
                                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // The day and its water as one surface: the date heads the chart.
        trailSenseSection(key = "day") {
            row { position ->
                TrailSenseGroupCard(position = position) {
                    DatePickerInput(
                        date = displayDate,
                        onDateChange = onDateChange,
                        modifier = Modifier.padding(bottom = TrailSenseSpacing.sm)
                    )
                    TideChartView(daily = daily, displayDate = displayDate, accent = heroColors.accent)
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
                val now = Instant.now()
                val next = tides.firstOrNull { it.time.toInstant().isAfter(now) }
                tides.forEach { tide ->
                    row { position -> TideRow(tide = tide, position = position, isNext = tide == next, highlight = heroColors) }
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
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    val formatter = remember(context) { FormatService.getInstance(context) }
    val dailyData = daily
    val levelColor = accent
    val lowColor = accent
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val labelPaint = remember(labelColor) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = context.resources.displayMetrics.scaledDensity * 11f
        }
    }
    val markRing = MaterialTheme.colorScheme.surfaceContainerLow
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
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
                } else {
                    TideGhostWave(accent = levelColor, modifier = Modifier.fillMaxSize())
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

                val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx()))
                chartAxisHours.forEach { hour ->
                    val x = hour / 24f * size.width
                    drawLine(
                        color = grid,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash,
                        cap = StrokeCap.Round
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

                drawPath(
                    path,
                    Brush.verticalGradient(
                        0f to levelColor.copy(alpha = 0.42f),
                        1f to levelColor.copy(alpha = 0.04f)
                    )
                )

                val line = Path()
                dailyData.waterLevels.forEachIndexed { index, reading ->
                    if (index == 0) line.moveTo(x(reading.time), y(reading.value))
                    else line.lineTo(x(reading.time), y(reading.value))
                }
                drawPath(
                    line,
                    levelColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Highs and lows as dots on the curve, in their own colours.
                dailyData.tides.forEach { tide ->
                    val reading = dailyData.waterLevels.minByOrNull {
                        Duration.between(tide.time.toInstant(), it.time).abs()
                    } ?: return@forEach
                    val center = Offset(x(reading.time), y(reading.value))
                    drawCircle(color = markRing, radius = 7.dp.toPx(), center = center)
                    drawCircle(color = if (tide.isHigh) levelColor else lowColor, radius = 4.5.dp.toPx(), center = center)
                    // Each high and low says when it is: above a high, below a low.
                    val label = formatter.formatTime(tide.time.toLocalTime(), includeSeconds = false)
                    val labelY = if (tide.isHigh) center.y - 12.dp.toPx() else center.y + 20.dp.toPx()
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        center.x.coerceIn(24.dp.toPx(), size.width - 24.dp.toPx()),
                        labelY.coerceIn(12.dp.toPx(), size.height - 4.dp.toPx()),
                        labelPaint
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
private fun TideRow(
    tide: Tide,
    position: TrailSenseGroupPosition,
    isNext: Boolean = false,
    highlight: com.kylecorry.trail_sense.shared.views.compose.TrailSenseToneColors? = null
) {
    val context = LocalContext.current
    val formatter = remember(context) { FormatService.getInstance(context) }
    val units = remember(context) { UserPreferences(context).baseDistanceUnits }
    val height = tide.height
    val scheme = MaterialTheme.colorScheme
    val accent = if (tide.isHigh) scheme.primary else scheme.tertiary

    TidesGroupRow(
        position = position,
        containerColor = if (isNext) {
            highlight?.container ?: if (tide.isHigh) scheme.primaryContainer else scheme.tertiaryContainer
        } else {
            scheme.surfaceContainerLow
        },
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
            PlacesBadge(
                containerColor = if (isNext) highlight?.accent ?: accent else if (tide.isHigh) scheme.primaryContainer else scheme.tertiaryContainer,
                contentColor = if (isNext) {
                    highlight?.onAccent ?: if (tide.isHigh) scheme.onPrimary else scheme.onTertiary
                } else {
                    if (tide.isHigh) scheme.onPrimaryContainer else scheme.onTertiaryContainer
                },
                icon = if (tide.isHigh) TrailIcons.ArrowUp else TrailIcons.ArrowDown,
                shape = if (tide.isHigh) MaterialShapes.Sunny.toShape() else MaterialShapes.Cookie6Sided.toShape(),
                modifier = Modifier.padding(start = TrailSenseSpacing.sm)
            )
        },
        trailing = {
            Text(
                text = formatter.formatTime(tide.time.toLocalTime(), includeSeconds = false),
                modifier = Modifier.padding(end = TrailSenseSpacing.md),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }
    ) {
        Text(
            text = stringResource(if (tide.isHigh) R.string.high_tide else R.string.low_tide),
            modifier = Modifier.testTag("tide_event_title"),
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface
        )
        Text(
            text = listOfNotNull(
                if (isNext) stringResource(R.string.next) else null,
                if (height == null) {
                    stringResource(R.string.estimated)
                } else {
                    formatter.formatDistance(Distance.meters(height).convertTo(units), 2, true)
                }
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant
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
            icon = TrailIcons.TideHigh,
            isLoading = false,
            displayDate = LocalDate.now(),
            daily = null,
            tides = emptyList(),
            onDateChange = {},
            onOpenTideList = {}
        )
    }
}

/**
 * The chart while the estimate loads: a dashed ghost of a tide curve whose phase drifts slowly, over a calm wash,
 * so the card reads as "water is coming" rather than an empty frame.
 */
@Composable
private fun TideGhostWave(accent: Color, modifier: Modifier = Modifier) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "ghostTide")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(4_000, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "ghostTidePhase"
    )
    Canvas(modifier) {
        val mid = size.height * 0.5f
        val amp = size.height * 0.22f
        val wave = androidx.compose.ui.graphics.Path()
        val steps = 64
        for (i in 0..steps) {
            val x = size.width * i / steps
            val y = mid + amp * kotlin.math.sin(i / steps.toFloat() * 4 * Math.PI.toFloat() + phase)
            if (i == 0) wave.moveTo(x, y) else wave.lineTo(x, y)
        }
        val area = androidx.compose.ui.graphics.Path().apply {
            addPath(wave)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            area,
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(accent.copy(alpha = 0.14f), accent.copy(alpha = 0f)))
        )
        drawPath(
            wave,
            accent.copy(alpha = 0.45f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 8.dp.toPx()))
            )
        )
    }
}
