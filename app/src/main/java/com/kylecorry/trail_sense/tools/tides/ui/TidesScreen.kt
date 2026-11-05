@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.tides.ui

import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

import com.kylecorry.trail_sense.shared.navigation.Route

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
fun TidesToolScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val navigator = LocalNavigator.current
    val viewModel = viewModel {
        TidesViewModel(
            tideService = TideService(appContext),
            loadTable = LoadTideTableCommand(appContext)::execute,
            locationSubsystem = LocationSubsystem.getInstance(appContext)
        )
    }
    val formatService = remember(context) { FormatService.getInstance(context) }
    val units = remember(context) { UserPreferences(context).baseDistanceUnits }
    val tideFormatter = remember(context) { TideFormatter(context) }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(viewModel, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (viewModel.needsTable) {
                        viewModel.resetDisplayDate()
                        ShowTideDisclaimerCommand(context, navigator) {
                            viewModel.loadTideTable()
                        }.execute()
                    } else {
                        viewModel.startCurrentRefresh()
                    }
                }

                Lifecycle.Event.ON_PAUSE -> viewModel.stop()
                Lifecycle.Event.ON_DESTROY -> viewModel.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stop()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.noTides.collect {
            val cancelled = CoroutineAlerts.dialog(
                context,
                context.getString(R.string.no_tides),
                context.getString(R.string.calibrate_new_tide)
            )
            if (!cancelled) {
                navigator.navigate(Route.TideList)
            }
        }
    }

    val current = viewModel.current
    val nextTide = viewModel.nextTide
    val table = viewModel.table

    val title = if (current == null) {
        "--"
    } else {
        val name = tideFormatter.getTideTypeName(current.type)
        val waterLevel = if (current.waterLevel != null) {
            val height = Distance.meters(current.waterLevel).convertTo(units)
            " (${formatService.formatDistance(height, 2, true)})"
        } else {
            ""
        }
        name + waterLevel
    }

    // Where the estimate comes from, said plainly: the built-in model for where you are ("Nearby" in the tide table
    // list), or the table you picked by name. Empty until the tide is known, so loading never shows a bare "Nearby".
    val subtitle = when {
        table == null || current == null -> ""
        table.isAutomaticNearbyTide -> stringResource(R.string.tides_source_nearby)
        else -> stringResource(
            R.string.tides_source_table,
            table.name
                ?: table.location?.let { formatService.formatLocation(it) }
                ?: stringResource(android.R.string.untitled)
        )
    }

    // The verdict: which way the water is going and until when ("Falling until 12:45 PM"). The next turn is implied
    // by the direction, so a "Low" tide that is still falling never reads as "Next: Low".
    val nextTideText = nextTide?.let { tide ->
        val time = formatService.formatTime(tide.time.toLocalTime(), includeSeconds = false)
        when (current?.rising) {
            true -> stringResource(R.string.tides_rising_until, time)
            false -> stringResource(R.string.tides_falling_until, time)
            null -> stringResource(
                R.string.tides_next_tide,
                stringResource(if (tide.isHigh) R.string.high_tide else R.string.low_tide),
                time
            )
        }
    }

    // The time until the water turns, like "2h 14m".
    val timeUntilNextTide = viewModel.timeUntilNextTide?.takeIf { !it.isNegative }?.let {
        formatService.formatDuration(it)
    }

    TidesScreen(
        title = title,
        subtitle = subtitle,
        isRising = current?.rising,
        nextTide = nextTideText,
        timeUntilNextTide = timeUntilNextTide,
        nextTideIsHigh = nextTide?.isHigh,
        levelFraction = viewModel.levelFraction,
        turnRemaining = viewModel.turnRemaining,
        isLoading = viewModel.isLoading,
        displayDate = viewModel.displayDate,
        daily = viewModel.daily,
        tides = viewModel.getDisplayTides(),
        onDateChange = viewModel::onDisplayDateSelected,
        onOpenTideList = {
            viewModel.clearTable()
            navigator.navigate(Route.TideList)
        },
        onBack = { dispatcher?.onBackPressed() }
    )
}

/**
 * The tide estimate's state: plain Kotlin over the tide service, the table loader and the location subsystem. It
 * loads the chosen table, keeps the current tide fresh each minute while shown, and reports [noTides] when there is
 * no table to estimate from.
 */
internal class TidesViewModel(
    private val tideService: TideService,
    private val loadTable: suspend () -> TideTable?,
    private val locationSubsystem: LocationSubsystem
) : ViewModel() {

    private val currentTideCommand = CurrentTideCommand(tideService)
    private val dailyTideCommand = DailyTideCommand(tideService)

    var table by mutableStateOf<TideTable?>(null)
        private set
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
    var timeUntilNextTide by mutableStateOf<Duration?>(null)
        private set
    /** How much of the way from the last turn to the next one is still to go (1 just after a turn, 0 at it). */
    var turnRemaining by mutableStateOf<Float?>(null)
        private set

    private val _noTides = Channel<Unit>(Channel.CONFLATED)
    /** One event each time a load finds no tide table. */
    val noTides: Flow<Unit> = _noTides.receiveAsFlow()

    private var currentTideJob: Job? = null
    private var dailyTideJob: Job? = null
    private var currentRefreshJob: Job? = null

    /** True until a table is loaded, and again after the person leaves to pick another. */
    val needsTable: Boolean
        get() = table == null

    fun resetDisplayDate() {
        displayDate = LocalDate.now()
    }

    fun stop() {
        currentTideJob?.cancel()
        dailyTideJob?.cancel()
        currentRefreshJob?.cancel()
    }

    fun clearTable() {
        table = null
    }

    fun onDisplayDateSelected(date: LocalDate) {
        displayDate = date
        updateDailyTide()
    }

    fun loadTideTable() {
        isLoading = true
        daily = null
        current = null
        nextTide = null
        levelFraction = null
        timeUntilNextTide = null
        turnRemaining = null

        viewModelScope.launch {
            val updatedTable = loadTable()
            if (updatedTable?.isAutomaticNearbyTide == true &&
                locationSubsystem.locationAge > Duration.ofMinutes(30)
            ) {
                locationSubsystem.updateLocation()
            }

            table = updatedTable
            isLoading = false

            if (updatedTable == null) {
                _noTides.send(Unit)
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
        dailyTideJob = viewModelScope.launch {
            daily = dailyTideCommand.execute(currentTable, date)
        }
    }

    private fun updateCurrentTide() {
        val currentTable = table ?: return
        currentTideJob?.cancel()
        currentTideJob = viewModelScope.launch {
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

    fun startCurrentRefresh() {
        currentRefreshJob?.cancel()
        currentRefreshJob = viewModelScope.launch {
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
        // One wavy outline for everything: the water fills it, the track follows it and the turn countdown is a
        // thick stroke along the same line, so the ring hugs the water with no gaps between them.
        val face = scheme.surfaceContainerLow
        val track = scheme.surfaceVariant
        Canvas(Modifier.fillMaxSize()) { drawPath(tideOutline(size.width, stroke / 2f), face) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(TideOutlineShape(stroke)),
            contentAlignment = Alignment.Center
        ) {
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
        // Drawn over the water, so the countdown stroke is whole where they meet.
        Canvas(
            Modifier
                .fillMaxSize()
                .then(if (waiting) Modifier.testTag("loading") else Modifier)
        ) {
            val outline = tideOutline(size.width, stroke / 2f)
            drawPath(outline, track, style = Stroke(width = stroke * 0.5f))
            val measure = androidx.compose.ui.graphics.PathMeasure().apply { setPath(outline, true) }
            val length = measure.length
            val (from, to) = if (waiting) {
                // Waiting: a quarter of the ring travels around it.
                val head = (drift * length)
                head to head + length * 0.25f
            } else {
                0f to length * ring.coerceIn(0f, 1f)
            }
            val active = Path()
            if (to <= length) {
                measure.getSegment(from, to, active, true)
            } else {
                measure.getSegment(from, length, active, true)
                measure.getSegment(0f, to - length, active, true)
            }
            if (to - from > 0.5f) {
                drawPath(active, ringColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
        }
    }
}

/** The day being shown, with its arrows: tap the date to pick one, long-press it for today. */
@OptIn(ExperimentalFoundationApi::class)
/** Waves around the tide well's outline, and how far they reach in and out. */
private const val TIDE_OUTLINE_WAVES = 14
private val TideOutlineAmplitude = 4.dp

/**
 * The tide well's outline: a circle whose edge rises and falls [TIDE_OUTLINE_WAVES] times, starting at the top and
 * running clockwise, inset by [inset] so a stroke along it stays inside the box.
 */
private fun androidx.compose.ui.unit.Density.tideOutline(side: Float, inset: Float): Path {
    val amplitude = TideOutlineAmplitude.toPx()
    val center = side / 2f
    val radius = center - inset - amplitude
    val steps = 720
    return Path().apply {
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val angle = (t * 2 * PI - PI / 2).toFloat()
            val r = radius + amplitude * kotlin.math.cos(t * 2 * PI * TIDE_OUTLINE_WAVES).toFloat()
            val x = center + r * kotlin.math.cos(angle)
            val y = center + r * kotlin.math.sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/** Clips the water to the inside of the tide well's outline. */
private class TideOutlineShape(private val strokePx: Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ) = androidx.compose.ui.graphics.Outline.Generic(density.tideOutline(size.width, strokePx / 2f))
}

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
                        CustomUiUtils.pickDate(context, date) {
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
