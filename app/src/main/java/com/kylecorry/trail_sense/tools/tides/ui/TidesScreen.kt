@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.tides.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChartFrame
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChartGutter
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChartInset
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSmoothPath
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
        timeUntilNextTide = controller.getTimeUntilNextTide(),
        nextTideIsHigh = controller.nextTide?.isHigh,
        levelFraction = controller.levelFraction,
        turnRemaining = controller.turnRemaining,
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
    /** How far the water is between today's lowest (0) and highest (1) levels right now. */
    var levelFraction by mutableStateOf<Float?>(null)
        private set
    private var timeUntilNextTide by mutableStateOf<Duration?>(null)
    /** How much of the way from the last turn to the next one is still to go (1 just after a turn, 0 at it). */
    var turnRemaining by mutableStateOf<Float?>(null)
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
        levelFraction = null
        timeUntilNextTide = null
        turnRemaining = null

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
            val next = getNextTide(currentTable)
            val previous = getPreviousTide(currentTable)
            val now = ZonedDateTime.now()
            nextTide = next
            timeUntilNextTide = next?.let { Duration.between(now, it.time) }
            turnRemaining = if (next != null && previous != null) {
                val total = Duration.between(previous.time, next.time).toMillis().toFloat()
                val left = Duration.between(now, next.time).toMillis().toFloat()
                if (total > 0f) (left / total).coerceIn(0f, 1f) else null
            } else {
                null
            }
            levelFraction = getLevelFraction(currentTable)
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

    /** The last high or low before now, looking into yesterday when none has passed today. */
    private suspend fun getPreviousTide(table: TideTable): Tide? = onDefault {
        val now = ZonedDateTime.now()
        val today = now.toLocalDate()
        listOf(today, today.minusDays(1)).firstNotNullOfOrNull { date ->
            tideService.getTides(table, date).lastOrNull { !it.time.isAfter(now) }
        }
    }

    /** Where the water is now within today's range, from the day's estimated levels. */
    private suspend fun getLevelFraction(table: TideTable): Float? {
        val today = dailyTideCommand.execute(table, LocalDate.now())
        val now = Instant.now()
        val reading = today.waterLevels.minByOrNull { Duration.between(now, it.time).abs() } ?: return null
        val range = today.waterLevelRange.end - today.waterLevelRange.start
        if (range <= 0f) return null
        return ((reading.value - today.waterLevelRange.start) / range).coerceIn(0f, 1f)
    }

    /** The time until the water turns, like "2h 14m". */
    fun getTimeUntilNextTide(): String? {
        val duration = timeUntilNextTide?.takeIf { !it.isNegative } ?: return null
        return formatService.formatDuration(duration)
    }

    /**
     * The verdict: which way the water is going and until when ("Falling until 12:45 PM"). The next turn is implied
     * by the direction, so a "Low" tide that is still falling never reads as "Next: Low".
     */
    fun getNextTideText(): String? {
        val tide = nextTide ?: return null
        val time = formatService.formatTime(tide.time.toLocalTime(), includeSeconds = false)
        return when (current?.rising) {
            true -> host.getString(R.string.tides_rising_until, time)
            false -> host.getString(R.string.tides_falling_until, time)
            null -> host.getString(
                R.string.tides_next_tide,
                host.getString(if (tide.isHigh) R.string.high_tide else R.string.low_tide),
                time
            )
        }
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

    /**
     * Where the estimate comes from, said plainly: the built-in model for where you are ("Nearby" in the tide table
     * list), or the table you picked by name. Empty until the tide is known, so loading never shows a bare "Nearby".
     */
    fun getTableSubtitle(): String {
        val table = table ?: return ""
        if (current == null) return ""
        if (table.isAutomaticNearbyTide) {
            return host.getString(R.string.tides_source_nearby)
        }
        val name = table.name
            ?: table.location?.let { formatService.formatLocation(it) }
            ?: host.getString(android.R.string.untitled)
        return host.getString(R.string.tides_source_table, name)
    }
}

/**
 * Tides answers one question: what is the water doing now? Like the boil timer, the object is the thing itself: a
 * well of water at its level in today's range, the time until it turns inside it, and a ring that runs down to the
 * turn. Under it one verdict line ("Rising until 3:42 PM") and where the estimate comes from. Then the day: the date
 * with its arrows, the water level chart, and that day's highs and lows. Until the first estimate the well only waits.
 */
@Composable
private fun TidesScreen(
    title: String,
    subtitle: String,
    isRising: Boolean?,
    nextTide: String?,
    timeUntilNextTide: String?,
    nextTideIsHigh: Boolean?,
    levelFraction: Float?,
    turnRemaining: Float?,
    isLoading: Boolean,
    displayDate: LocalDate,
    daily: DailyTideData?,
    tides: List<Tide>,
    onDateChange: (LocalDate) -> Unit,
    onOpenTideList: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val waiting = isLoading || (title == "--" && timeUntilNextTide == null)
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
            TideHero(
                title = title,
                subtitle = subtitle,
                isRising = isRising,
                nextTide = nextTide,
                timeUntilNextTide = timeUntilNextTide,
                nextTideIsHigh = nextTideIsHigh,
                levelFraction = levelFraction,
                turnRemaining = turnRemaining,
                waiting = waiting
            )
        }

        // The day appears once it is known: while loading there is no empty chart frame to puzzle over.
        item(key = "day") {
            AnimatedVisibility(
                visible = daily != null,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)) {
                    Column {
                        TideDayHeader(
                            date = displayDate,
                            onDateChange = onDateChange,
                            modifier = Modifier.padding(bottom = TrailSenseSpacing.sm)
                        )
                        TideChartView(daily = daily, displayDate = displayDate)
                    }
                    Column {
                        // No header: the page title already says "Tides", and the date heads the chart above.
                        TrailSenseRowGroup(modifier = Modifier.testTag("tide_list")) {
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
                        Text(
                            text = stringResource(R.string.disclaimer_estimated_tide),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = TrailSenseSpacing.lg,
                                    end = TrailSenseSpacing.lg,
                                    top = TrailSenseSpacing.sm
                                )
                                .testTag("tide_disclaimer"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * The tide now: the well, then the verdict (which way the water goes, and until when) and the table it comes from.
 * While waiting: the well's ring spins, the line says it is reading the tides, and no table is named yet.
 */
@Composable
private fun TideHero(
    title: String,
    subtitle: String,
    isRising: Boolean?,
    nextTide: String?,
    timeUntilNextTide: String?,
    nextTideIsHigh: Boolean?,
    levelFraction: Float?,
    turnRemaining: Float?,
    waiting: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())
            .testTag("tide_title"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TideWell(
            fraction = levelFraction,
            turnRemaining = turnRemaining,
            isRising = isRising,
            waiting = waiting,
            label = if (!waiting && timeUntilNextTide != null) {
                stringResource(if (nextTideIsHigh == true) R.string.tides_until_high else R.string.tides_until_low)
            } else {
                null
            },
            // No turn within two days: the tide now is the answer.
            value = if (waiting) null else timeUntilNextTide ?: title
        )
        Text(
            text = if (waiting) stringResource(R.string.sky_tides_loading) else nextTide ?: title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = scheme.onSurface,
            modifier = Modifier.padding(
                top = TrailSenseSpacing.lg,
                start = TrailSenseSpacing.lg,
                end = TrailSenseSpacing.lg
            )
        )
        if (!waiting && subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = scheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = TrailSenseSpacing.xs, start = TrailSenseSpacing.lg, end = TrailSenseSpacing.lg)
                    .testTag("tide_source")
            )
        }
    }
}

/**
 * The screen's one live object, drawn like the boil timer's pot: a thick wavy ring that runs down to the next turn,
 * and inside it the water at its level in today's range (low at the bottom, high at the top), its surface a soft
 * double wave that drifts the way the water is going. The level moves on a spring; the reading sits on the water.
 */
@Composable
private fun TideWell(
    fraction: Float?,
    turnRemaining: Float?,
    isRising: Boolean?,
    waiting: Boolean,
    label: String?,
    value: String?,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val density = androidx.compose.ui.platform.LocalDensity.current
    val level by animateFloatAsState(
        targetValue = if (waiting) 0.12f else (fraction ?: 0.5f).coerceIn(0.08f, 0.92f),
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "tideLevel"
    )
    val ring by animateFloatAsState(
        targetValue = turnRemaining ?: 1f,
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "tideTurn"
    )
    // The surface drifts slowly, rightward while rising and leftward while falling: the motion says which way it goes.
    val transition = rememberInfiniteTransition(label = "tide")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "tideDrift"
    )
    val phase = when (isRising) {
        true -> -drift
        false -> drift
        null -> 0f
    }
    val direction = isRising?.let { stringResource(if (it) R.string.tides_rising else R.string.tides_falling) }
    val ringColor = scheme.primary
    val waterColor = scheme.primaryContainer
    val ink = scheme.onPrimaryContainer
    val stroke = with(density) { 12.dp.toPx() }

    Box(
        modifier = modifier
            .padding(vertical = TrailSenseSpacing.sm)
            .size(264.dp)
            .then(
                if (direction != null) Modifier.semantics { contentDescription = direction } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (waiting) {
            androidx.compose.material3.CircularWavyProgressIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("loading"),
                color = ringColor,
                trackColor = scheme.surfaceVariant,
                stroke = Stroke(width = stroke, cap = StrokeCap.Round),
                trackStroke = Stroke(width = stroke, cap = StrokeCap.Round),
                wavelength = 36.dp
            )
        } else {
            androidx.compose.material3.CircularWavyProgressIndicator(
                progress = { ring },
                modifier = Modifier.fillMaxSize(),
                color = ringColor,
                trackColor = scheme.surfaceVariant,
                stroke = Stroke(width = stroke, cap = StrokeCap.Round),
                trackStroke = Stroke(width = stroke, cap = StrokeCap.Round),
                wavelength = 36.dp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Surface(color = scheme.surfaceContainerLow, modifier = Modifier.fillMaxSize()) {}
            Canvas(Modifier.fillMaxSize()) {
                val surfaceY = size.height * (1f - level)
                val amplitude = 4.dp.toPx()
                val wavelength = size.width / 1.5f
                fun wave(offset: Float, lift: Float) = Path().apply {
                    moveTo(0f, size.height)
                    var x = 0f
                    while (x <= size.width) {
                        lineTo(x, surfaceY + lift + amplitude * sin((x / wavelength + offset) * 2 * PI).toFloat())
                        x += 4f
                    }
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(wave(phase, 0f), waterColor.copy(alpha = 0.6f))
                drawPath(wave(phase + 0.35f, amplitude * 1.4f), waterColor)
            }
            if (value != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // The reading sits at the middle: on the water once it is past half, on the bare face below that.
                    val onWater = level > 0.5f
                    if (label != null) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall,
                            color = (if (onWater) ink else scheme.onSurfaceVariant).copy(alpha = 0.85f)
                        )
                    }
                    Text(
                        text = value,
                        style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = if (onWater) ink else scheme.onSurface,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(
                            minFontSize = 22.sp,
                            maxFontSize = MaterialTheme.typography.displayMedium.fontSize
                        ),
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                }
            }
        }
    }
}

/** The day being shown, with its arrows: tap the date to pick one, long-press it for today. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TideDayHeader(
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
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = TrailSenseSize.minimumTouchTarget)
                .clip(RoundedCornerShape(TrailSenseShape.groupOuter))
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
                .padding(horizontal = TrailSenseSpacing.lg)
                .testTag("date_btn"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatter.formatRelativeDate(date),
                modifier = Modifier.testTag("tide_list_date"),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                painter = rememberVectorPainter(TrailIcons.KeyboardArrowRight),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = TrailSenseSpacing.xs)
                    .size(20.dp)
                    .rotate(90f)
            )
        }
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

/** Hours marked on the chart's time axis. */
private val chartAxisHours = listOf(0, 6, 12, 18, 24)

/**
 * The day's water in the chart frame: High / Low in the gutter, the level as one smooth primary line, the day's
 * highs and lows as small hollow marks (their times are in the list below), and now as the "now" dot.
 */
@Composable
private fun TideChartView(
    daily: DailyTideData?,
    displayDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember(context) { FormatService.getInstance(context) }
    val dailyData = daily
    val levelColor = MaterialTheme.colorScheme.primary
    val nowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    val ringColor = MaterialTheme.colorScheme.background
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
                .height(176.dp)
                .testTag("chart"),
            contentAlignment = Alignment.Center
        ) {
            TrailSenseChartFrame(
                yLabels = listOf(stringResource(R.string.high), stringResource(R.string.low)),
                modifier = Modifier.fillMaxSize()
            ) {
                if (dailyData == null || dailyData.waterLevels.isEmpty()) {
                    if (dailyData != null) {
                        Text(
                            text = stringResource(R.string.no_data),
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    return@TrailSenseChartFrame
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val startTime = dailyData.waterLevels.firstOrNull()?.time ?: Instant.now()
                    val totalRange = dailyData.waterLevelRange.end - dailyData.waterLevelRange.start
                    val rangeDelta = (totalRange * 0.1f).coerceAtLeast(0.1f)
                    val minimum = dailyData.waterLevelRange.start - rangeDelta
                    val maximum = dailyData.waterLevelRange.end + rangeDelta
                    val valueRange = (maximum - minimum).takeIf { it > 0f } ?: 1f
                    val inset = TrailSenseChartInset.toPx()
                    val plotWidth = size.width - 2 * inset
                    val plotHeight = size.height - 2 * inset

                    fun x(time: Instant): Float {
                        val hours = Duration.between(startTime, time).toMillis() / 3_600_000f
                        return inset + (hours / 24f).coerceIn(0f, 1f) * plotWidth
                    }

                    fun y(value: Float): Float {
                        val normalized = ((value - minimum) / valueRange).coerceIn(0f, 1f)
                        return inset + plotHeight - normalized * plotHeight
                    }

                    drawPath(
                        trailSenseSmoothPath(dailyData.waterLevels.map { Offset(x(it.time), y(it.value)) }),
                        levelColor,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    val currentLevel = dailyData.waterLevels.minByOrNull {
                        Duration.between(Instant.now(), it.time).abs()
                    }?.takeIf { displayDate == LocalDate.now() }
                    val nowCenter = currentLevel?.let { Offset(x(it.time), y(it.value)) }

                    // Now is a thin line across the plot (the family's inspect marker), not a haloed dot.
                    if (nowCenter != null) {
                        drawLine(
                            color = nowColor,
                            start = Offset(nowCenter.x, 0f),
                            end = Offset(nowCenter.x, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // A high or low right under now's small dot is left out, so now never looks like two dots.
                    val clearance = 8.dp.toPx()
                    dailyData.tides.forEach { tide ->
                        val reading = dailyData.waterLevels.minByOrNull {
                            Duration.between(tide.time.toInstant(), it.time).abs()
                        } ?: return@forEach
                        val center = Offset(x(reading.time), y(reading.value))
                        if (nowCenter != null && (center - nowCenter).getDistance() < clearance) return@forEach
                        drawCircle(color = ringColor, radius = 4.dp.toPx(), center = center)
                        drawCircle(
                            color = levelColor,
                            radius = 4.dp.toPx(),
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    if (nowCenter != null) {
                        drawCircle(color = levelColor, radius = 4.dp.toPx(), center = nowCenter)
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = TrailSenseChartGutter)
        ) {
            axisLabels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = when (index) {
                        0 -> TextAlign.Start
                        axisLabels.lastIndex -> TextAlign.End
                        else -> TextAlign.Center
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = textColor,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * One high or low: the family's 40 dp Cookie6 well (12 % primary) with its arrow, the tide and its measured height,
 * and the time as the one trailing element. A tide that has already passed steps back in colour.
 */
@Composable
private fun TideRow(
    tide: Tide,
    position: TrailSenseGroupPosition
) {
    val context = LocalContext.current
    val formatter = remember(context) { FormatService.getInstance(context) }
    val units = remember(context) { UserPreferences(context).baseDistanceUnits }
    val height = tide.height
    val scheme = MaterialTheme.colorScheme
    val isPast = tide.time.isBefore(ZonedDateTime.now())
    val accent = if (isPast) scheme.onSurfaceVariant else scheme.primary

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
            Surface(
                shape = MaterialShapes.Cookie6Sided.toShape(),
                color = accent.copy(alpha = 0.12f),
                contentColor = accent,
                modifier = Modifier
                    .padding(start = TrailSenseSpacing.sm)
                    .size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = rememberVectorPainter(if (tide.isHigh) TrailIcons.ArrowUp else TrailIcons.ArrowDown),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        trailing = {
            Text(
                text = formatter.formatTime(tide.time.toLocalTime(), includeSeconds = false),
                modifier = Modifier.padding(end = TrailSenseSpacing.md),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = if (isPast) scheme.onSurfaceVariant else scheme.onSurface
            )
        }
    ) {
        Text(
            text = stringResource(if (tide.isHigh) R.string.high_tide else R.string.low_tide),
            modifier = Modifier.testTag("tide_event_title"),
            style = MaterialTheme.typography.titleMedium,
            color = if (isPast) scheme.onSurfaceVariant else scheme.onSurface
        )
        // A measured height when the table has one; estimated tides say so once, in the footer below.
        if (height != null) {
            Text(
                text = formatter.formatDistance(Distance.meters(height).convertTo(units), 2, true),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
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
            nextTide = "Rising until 3:42 PM",
            timeUntilNextTide = "2h 14m",
            nextTideIsHigh = true,
            levelFraction = 0.6f,
            turnRemaining = 0.4f,
            isLoading = false,
            displayDate = LocalDate.now(),
            daily = null,
            tides = emptyList(),
            onDateChange = {},
            onOpenTideList = {}
        )
    }
}
