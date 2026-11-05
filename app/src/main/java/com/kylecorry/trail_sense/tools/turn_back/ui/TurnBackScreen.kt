@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.turn_back.ui

import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.shared.views.TurnBackEmptyIllustration
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesKeyValue
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesRow
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseMetricItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseMetricRow
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseReading
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.shared.permissions.IsBackgroundLocationRequired
import com.kylecorry.trail_sense.shared.preferences.PreferencesFlag
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.turn_back.infrastructure.receivers.TurnBackAlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import android.content.Context
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object TurnBackKeys {
    const val PREF_TURN_BACK_TIME = "pref_turn_back_time"
    const val PREF_TURN_BACK_RETURN_TIME = "pref_turn_back_return_time"
}

@Composable
fun TurnBackToolScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel {
        TurnBackViewModel(
            preferences = PreferencesSubsystem.getInstance(appContext).preferences,
            locationSubsystem = LocationSubsystem.getInstance(appContext),
            sensorSubsystem = SensorSubsystem.getInstance(appContext)
        )
    }
    val formatter = remember(appContext) { FormatService.getInstance(appContext) }
    val userPrefs = remember(appContext) { UserPreferences(appContext) }

    LaunchedEffect(viewModel) {
        viewModel.alertSet.collect {
            scope.launch {
                // Exact alarms first; the alarm starts whether or not they were granted, as it always has.
                effects.requestPermission(AppPermission.ExactAlarms)
                TurnBackAlarmReceiver.start(context)
                requestBackgroundLocation(context, viewModel.preferences, effects)
            }
            effects.showMessage(context.getString(R.string.turn_back_alert_set))
        }
    }

    TurnBackScreen(
        formatter = formatter,
        loadTurnBackTime = viewModel::turnBackTime,
        loadReturnTime = viewModel::returnTime,
        onPickReturnTime = { currentReturnTime ->
            CustomUiUtils.pickTime(
                context,
                userPrefs.use24HourTime,
                currentReturnTime?.atZone(ZoneId.systemDefault())?.toLocalTime() ?: LocalTime.now()
            ) {
                if (it != null) {
                    viewModel.setReturnTimeAt(it)
                }
            }
        },
        onUseSunset = {
            val job = viewModel.setReturnTimeToSunset()
            Alerts.withCancelableLoading(
                context,
                context.getString(R.string.loading),
                onCancel = { job.cancel() }
            ) {
                // The sunset lookup runs in the ViewModel; the dialog only offers to cancel it.
            }
        },
        loadSunset = viewModel::getNextSunset,
        onCancel = {
            viewModel.cancelTurnBack()
            TurnBackAlarmReceiver.stop(context)
        }
    )
}

/**
 * Turn back's state: the return and turn-around times in preferences, and tonight's sunset. Plain Kotlin over the
 * preferences and location subsystems; [alertSet] fires once a new return time is stored so the screen can start the
 * alarm and ask for its permissions.
 */
internal class TurnBackViewModel(
    val preferences: IPreferences,
    private val locationSubsystem: LocationSubsystem,
    private val sensorSubsystem: SensorSubsystem
) : ViewModel() {

    private val astronomy = AstronomyService()

    private val _alertSet = Channel<Unit>(Channel.BUFFERED)

    /** One event each time a return time is set. */
    val alertSet: Flow<Unit> = _alertSet.receiveAsFlow()

    fun turnBackTime(): Instant? = preferences.getInstant(TurnBackKeys.PREF_TURN_BACK_TIME)

    fun returnTime(): Instant? = preferences.getInstant(TurnBackKeys.PREF_TURN_BACK_RETURN_TIME)

    /** Return at [time] today, or tomorrow if it has already passed. */
    fun setReturnTimeAt(time: LocalTime) {
        val newReturnTime = if (time < LocalTime.now()) {
            ZonedDateTime.now().plusDays(1).with(time)
        } else {
            ZonedDateTime.now().with(time)
        }
        setReturnTime(newReturnTime)
    }

    /** Tonight's sunset at the last known location, for the "Return before dark" row (no GPS fix is requested). */
    suspend fun getNextSunset(): Instant? = withContext(Dispatchers.Default) {
        val location = locationSubsystem.location
        astronomy.getNextSunset(location, SunTimesMode.Actual)?.atZone(ZoneId.systemDefault())?.toInstant()
    }

    /** Finds tonight's sunset from a fresh location and returns by it; cancel the job to give up. */
    fun setReturnTimeToSunset(): Job {
        return viewModelScope.launch {
            val sunsetTime = withContext(Dispatchers.IO) {
                val location = sensorSubsystem.getLocation()
                astronomy.getNextSunset(location, SunTimesMode.Actual)?.atZone(ZoneId.systemDefault())
            }
            setReturnTime(sunsetTime ?: return@launch)
        }
    }

    private fun setReturnTime(time: ZonedDateTime) {
        val instant = time.toInstant()
        val now = Instant.now()
        preferences.putInstant(TurnBackKeys.PREF_TURN_BACK_TIME, turnBackTimeFor(now, instant))
        preferences.putInstant(TurnBackKeys.PREF_TURN_BACK_RETURN_TIME, instant)
        _alertSet.trySend(Unit)
    }

    fun cancelTurnBack() {
        preferences.remove(TurnBackKeys.PREF_TURN_BACK_TIME)
        preferences.remove(TurnBackKeys.PREF_TURN_BACK_RETURN_TIME)
    }

    companion object {
        private const val TURN_BACK_PERCENT = 0.4f

        /** When to turn around to be back by [returnTime]: 40% of the time left, leaving the rest for the walk back. */
        fun turnBackTimeFor(now: Instant, returnTime: Instant): Instant {
            val remainingMillisToReturn = Duration.between(now, returnTime).toMillis().coerceAtLeast(0)
            return now.plusMillis((remainingMillisToReturn * TURN_BACK_PERCENT).toLong())
        }
    }
}

/** Once, when the alarm needs it, offers background location so the alert can find the sunset while closed. */
private suspend fun requestBackgroundLocation(context: Context, preferences: IPreferences, effects: ScreenEffects) {
    val shownFlag = PreferencesFlag(preferences, "cache_background_location_requested")
    if (!IsBackgroundLocationRequired().isSatisfiedBy(context)) {
        shownFlag.set(false)
        return
    }
    if (shownFlag.get()) {
        return
    }

    shownFlag.set(true)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return
    }

    val learnMore = effects.showSnackbar(
        context.getString(R.string.sunset_alerts_background_location_disclaimer),
        actionLabel = context.getString(R.string.learn_more),
        long = true
    )
    if (!learnMore) {
        return
    }
    val markdown = DependencyRegistry.get<MarkdownService>()
    val contents = markdown.toMarkdown(context.getString(R.string.access_background_location_rationale))
    val cancelled = suspendCancellableCoroutine { continuation ->
        Alerts.dialog(
            context,
            context.getString(R.string.access_background_location),
            contents,
            okText = context.getString(R.string.settings),
            allowLinks = true
        ) { cancelled ->
            if (continuation.isActive) continuation.resume(cancelled)
        }
    }
    if (!cancelled) {
        effects.requestPermission(AppPermission.BackgroundLocation)
    }
}

/**
 * The object: the sun on its arc over a horizon. Its height is the share of the time before you're due back that you
 * can still spend going on, so it touches the horizon exactly when it's time to turn around, and then warms.
 */
@Composable
private fun TurnBackSun(height: Float, isDue: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val h by animateFloatAsState(height, MaterialTheme.motionScheme.slowSpatialSpec(), label = "sunHeight")
    val sunColor by androidx.compose.animation.animateColorAsState(
        if (isDue) scheme.error else scheme.tertiary,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "sunColor"
    )
    val track = scheme.outlineVariant
    Canvas(modifier) {
        val horizon = size.height - 6.dp.toPx()
        val radius = (size.height - 24.dp.toPx()).coerceAtMost(size.width / 2f - 24.dp.toPx())
        val center = Offset(size.width / 2f, horizon)
        // The day's arc, dashed, and the horizon under it.
        drawArc(
            color = track,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(4.dp.toPx(), 8.dp.toPx())
                )
            )
        )
        // From high noon (1) down the right of the arc to the horizon (0).
        val theta = (1f - h) * (Math.PI.toFloat() / 2f)
        val sun = Offset(center.x + radius * kotlin.math.sin(theta), center.y - radius * kotlin.math.cos(theta))
        drawCircle(sunColor.copy(alpha = 0.22f), 26.dp.toPx(), sun)
        drawCircle(sunColor, 16.dp.toPx(), sun)
        // The ground hides the sun's lower half as it sets.
        drawRect(
            scheme.surface,
            topLeft = Offset(0f, horizon),
            size = androidx.compose.ui.geometry.Size(size.width, size.height - horizon)
        )
        drawLine(track, Offset(0f, horizon), Offset(size.width, horizon), 3.dp.toPx(), StrokeCap.Round)
    }
}

/**
 * Turn back. Until there is a return time, the page is the family's empty state: the question, one medium button to
 * pick a time and a quiet "before dark" choice. Once set, the countdown to the turn-around is the display value with
 * when to turn as the verdict; changing either choice and cancelling sit below it.
 */
@Composable
private fun TurnBackScreen(
    formatter: FormatService,
    loadTurnBackTime: () -> Instant?,
    loadReturnTime: () -> Instant?,
    onPickReturnTime: (Instant?) -> Unit,
    onUseSunset: () -> Unit,
    loadSunset: suspend () -> Instant?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // When do I need to turn around? The countdown answers it.
    var turnBackTime by remember { mutableStateOf(loadTurnBackTime()) }
    var returnTime by remember { mutableStateOf(loadReturnTime()) }
    var now by remember { mutableStateOf(Instant.now()) }
    var sunset by remember { mutableStateOf<Instant?>(null) }

    LaunchedEffect(Unit) {
        sunset = loadSunset()
    }

    LaunchedEffect(Unit) {
        while (true) {
            turnBackTime = loadTurnBackTime()
            returnTime = loadReturnTime()
            now = Instant.now()
            delay(1000)
        }
    }

    val formattedTurnBackTime = turnBackTime?.let { formatter.formatTime(it, includeSeconds = false) }
    val formattedReturnTime = returnTime?.let { formatter.formatTime(it, includeSeconds = false) }
    val formattedSunset = sunset?.let { formatter.formatTime(it, includeSeconds = false) }
    var remaining = turnBackTime?.let { Duration.between(now, it) }
    if (remaining?.isNegative == true) {
        remaining = Duration.ZERO
    }
    val formattedRemainingTime = remaining?.let {
        formatter.formatDuration(
            it,
            includeSeconds = it < Duration.ofMinutes(1)
        )
    }
    val isActive = returnTime != null && formattedRemainingTime != null
    // A passed turn-around is the one real danger here, so only then does the countdown take the error colour.
    val isDue = isActive && remaining?.isZero == true

    TrailSensePage(
        title = stringResource(R.string.tool_turn_back),
        onBack = rememberTrailSenseBack(),
        modifier = modifier
    ) {
        if (!isActive) {
            item("empty") {
                TrailSenseEmptyState(
                    icon = TrailIcons.ToolClock,
                    illustration = { TurnBackEmptyIllustration() },
                    title = stringResource(R.string.places_turn_back_idle_title),
                    description = stringResource(R.string.turn_back_empty_description),
                    modifier = Modifier
                        .fillParentMaxHeight(0.9f)
                        .testTag("instructions"),
                    actionLabel = stringResource(R.string.places_turn_back_set_return_time),
                    onAction = { onPickReturnTime(returnTime) },
                    actionModifier = Modifier.testTag("edittext"),
                    action = {
                        // The one shortcut kept beside the action: a quiet text link, not a second button.
                        // How long until sunset, not its clock time: a time alone read like "now" when the next
                        // sunset was tomorrow's, or minutes away.
                        val untilSunset = sunset?.let { Duration.between(now, it) }?.takeIf { !it.isNegative }
                        TrailSenseButton(
                            text = if (untilSunset != null) {
                                stringResource(
                                    R.string.places_turn_back_before_dark_in,
                                    formatter.formatDuration(untilSunset, short = true)
                                )
                            } else {
                                stringResource(R.string.return_before_dark)
                            },
                            onClick = onUseSunset,
                            style = TrailSenseButtonStyle.Text,
                            modifier = Modifier.testTag("sunset_button")
                        )
                    }
                )
            }
            return@TrailSensePage
        }

        item("answer") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("instructions")
                    .semantics(mergeDescendants = true) {}
                    .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
            ) {
                // Time left to go on, out of that plus the walk back: the sun's height above the horizon.
                val walkBack = if (turnBackTime != null && returnTime != null) {
                    Duration.between(turnBackTime, returnTime).toMillis().coerceAtLeast(1)
                } else {
                    1L
                }
                val left = remaining?.toMillis() ?: 0L
                TurnBackSun(
                    height = (left.toFloat() / (left + walkBack)).coerceIn(0f, 1f),
                    isDue = isDue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
                Text(
                    formattedRemainingTime.orEmpty(),
                    style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (isDue || formattedTurnBackTime == null) {
                        stringResource(R.string.places_turn_back_turn_around_now)
                    } else {
                        stringResource(R.string.places_turn_back_turn_around_at, formattedTurnBackTime)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // The two ways to set the return time, each showing the time it stands for; tapping one changes it.
        trailSenseSection(key = "return") {
            row { position ->
                TrailSenseListItem(
                    title = stringResource(R.string.return_time),
                    subtitle = stringResource(R.string.places_turn_back_pick_hint),
                    icon = rememberVectorPainter(TrailIcons.ToolClock),
                    trailingText = formattedReturnTime,
                    position = position,
                    modifier = Modifier.testTag("edittext"),
                    onClick = { onPickReturnTime(returnTime) }
                )
            }
            row { position ->
                TrailSenseListItem(
                    title = stringResource(R.string.return_before_dark),
                    subtitle = stringResource(R.string.places_turn_back_sunset_hint),
                    icon = rememberVectorPainter(TrailIcons.SunsetNotification),
                    trailingText = formattedSunset,
                    position = position,
                    modifier = Modifier.testTag("sunset_button"),
                    onClick = onUseSunset
                )
            }
        }

        item("cancel") {
            TrailSenseButton(
                text = stringResource(R.string.turn_back_cancel),
                onClick = onCancel,
                icon = TrailIcons.Cancel,
                style = TrailSenseButtonStyle.Tonal,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cancel_button")
            )
        }
    }
}
