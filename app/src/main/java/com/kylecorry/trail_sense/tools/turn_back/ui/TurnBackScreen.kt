@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.turn_back.ui

import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseMetricItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseMetricRow
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseReading
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.alerts.toast
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
fun TurnBackToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { TurnBackController(host) }

    TurnBackScreen(
        formatter = controller.formatter,
        loadTurnBackTime = {
            controller.sharedPrefs.preferences.getInstant(TurnBackKeys.PREF_TURN_BACK_TIME)
        },
        loadReturnTime = {
            controller.sharedPrefs.preferences.getInstant(TurnBackKeys.PREF_TURN_BACK_RETURN_TIME)
        },
        onPickReturnTime = controller::pickReturnTime,
        onUseSunset = controller::setReturnTimeToSunset,
        onCancel = controller::cancelTurnBack
    )
}

private class TurnBackController(private val host: TrailSenseDestinationHost) {

    private val prefs by lazy { UserPreferences(host.requireContext()) }
    val sharedPrefs by lazy { PreferencesSubsystem.getInstance(host.requireContext()) }
    val formatter by lazy { FormatService.getInstance(host.requireContext()) }
    private val astronomy = AstronomyService()

    fun pickReturnTime(currentReturnTime: Instant?) {
        CustomUiUtils.pickTime(
            host.requireContext(),
            prefs.use24HourTime,
            currentReturnTime?.atZone(ZoneId.systemDefault())?.toLocalTime() ?: LocalTime.now()
        ) {
            if (it != null) {
                val newReturnTime = if (it < LocalTime.now()) {
                    ZonedDateTime.now().plusDays(1).with(it)
                } else {
                    ZonedDateTime.now().with(it)
                }

                setReturnTime(newReturnTime)
            }
        }
    }

    fun setReturnTimeToSunset() {
        host.lifecycleScope.launch {
            var wasSuccessful = false
            var sunsetTime: ZonedDateTime? = null
            val job = launch(Dispatchers.IO) {
                val location = SensorSubsystem.getInstance(host.requireContext()).getLocation()
                sunsetTime = astronomy.getNextSunset(location, SunTimesMode.Actual)
                    ?.atZone(ZoneId.systemDefault())
                wasSuccessful = true
            }

            Alerts.withCancelableLoading(
                host.requireContext(),
                host.getString(R.string.loading),
                onCancel = { job.cancel() }
            ) {
                host.lifecycleScope.launch {
                    job.join()
                    if (wasSuccessful) {
                        setReturnTime(sunsetTime ?: return@launch)
                    }
                }
            }
        }
    }

    private fun setReturnTime(time: ZonedDateTime) {
        val turnBackPercent = 0.4f
        val instant = time.toInstant()
        val remainingMillisToReturn =
            Duration.between(Instant.now(), instant).toMillis().coerceAtLeast(0)
        val newTurnBackTime =
            Instant.now().plusMillis((remainingMillisToReturn * turnBackPercent).toLong())

        sharedPrefs.preferences.putInstant(TurnBackKeys.PREF_TURN_BACK_TIME, newTurnBackTime)
        sharedPrefs.preferences.putInstant(TurnBackKeys.PREF_TURN_BACK_RETURN_TIME, instant)
        enableTurnBackAlarm()

        host.toast(host.getString(R.string.turn_back_alert_set))
    }

    private fun enableTurnBackAlarm() {
        requestExactAlarms {
            TurnBackAlarmReceiver.start(host.requireContext())
            requestBackgroundLocation()
        }
    }

    private fun requestExactAlarms(action: (hasPermission: Boolean) -> Unit) {
        host.requestScheduleExactAlarms(action)
    }

    private fun requestBackgroundLocation() {
        val shownFlag = PreferencesFlag(
            sharedPrefs.preferences,
            "cache_background_location_requested"
        )
        if (!IsBackgroundLocationRequired().isSatisfiedBy(host.requireContext())) {
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

        CustomUiUtils.snackbar(
            host,
            host.getString(R.string.sunset_alerts_background_location_disclaimer),
            Snackbar.LENGTH_LONG,
            host.getString(R.string.learn_more)
        ) {
            val markdown = DependencyRegistry.get<MarkdownService>()
            val contents =
                markdown.toMarkdown(host.getString(R.string.access_background_location_rationale))

            Alerts.dialog(
                host.requireContext(),
                host.getString(R.string.access_background_location),
                contents,
                okText = host.getString(R.string.settings),
                allowLinks = true
            ) { cancelled ->
                if (!cancelled) {
                    host.requestBackgroundLocation {
                        // No-op
                    }
                }
            }
        }
    }

    fun cancelTurnBack() {
        sharedPrefs.preferences.remove(TurnBackKeys.PREF_TURN_BACK_TIME)
        sharedPrefs.preferences.remove(TurnBackKeys.PREF_TURN_BACK_RETURN_TIME)
        TurnBackAlarmReceiver.stop(host.requireContext())
    }
}

/**
 * Turn back: a scrolling page. While an alert is set the countdown to the turn-around point is the hero, with the
 * journey's progress and the two times as a metric strip; otherwise a calm empty state. The return time choices are
 * one group, and cancelling is a quiet button that names what it cancels.
 */
@Composable
private fun TurnBackScreen(
    formatter: FormatService,
    loadTurnBackTime: () -> Instant?,
    loadReturnTime: () -> Instant?,
    onPickReturnTime: (Instant?) -> Unit,
    onUseSunset: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var turnBackTime by remember { mutableStateOf(loadTurnBackTime()) }
    var returnTime by remember { mutableStateOf(loadReturnTime()) }
    var now by remember { mutableStateOf(Instant.now()) }

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
    val isActive = returnTime != null
    val instructionText = if (
        formattedRemainingTime != null &&
        formattedTurnBackTime != null &&
        formattedReturnTime != null
    ) {
        stringResource(
            R.string.turn_back_instructions,
            formattedTurnBackTime,
            formattedRemainingTime,
            formattedReturnTime
        )
    } else {
        stringResource(R.string.time_not_set)
    }

    // Fraction of the run still remaining before the turn-back alert (0f..1f).
    val progressTarget = run {
        val turnBack = turnBackTime
        val ret = returnTime
        if (turnBack == null || ret == null) {
            0f
        } else {
            val total = Duration.between(now, ret).toMillis()
                .coerceAtLeast(Duration.between(turnBack, ret).toMillis())
            val left = remaining?.toMillis() ?: 0L
            if (total <= 0L) 0f else (left.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "turnBackProgress"
    )

    val turnFraction = run {
        val turnBack = turnBackTime
        val ret = returnTime
        if (turnBack == null || ret == null) {
            0.4f
        } else {
            val total = Duration.between(now, ret).toMillis()
            val out = Duration.between(now, turnBack).toMillis().coerceAtLeast(0)
            if (total <= 0L) 1f else (out.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }
    }
    val animatedTurn by animateFloatAsState(
        targetValue = turnFraction,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "turnFraction"
    )
    val isLate = isActive && remaining != null && remaining < Duration.ofMinutes(10)
    val tone = when {
        !isActive -> TrailSenseTone.Secondary
        isLate -> TrailSenseTone.Caution
        else -> TrailSenseTone.Tertiary
    }
    val colors = trailSenseToneColors(tone)

    val returnTitle = stringResource(R.string.return_time)
    TrailSensePage(
        title = stringResource(R.string.tool_turn_back),
        onBack = rememberTrailSenseBack(),
        modifier = modifier
    ) {
        item("hero") {
            TrailSenseSpotlight(
                modifier = Modifier
                    .testTag("instructions")
                    .semantics(mergeDescendants = true) {},
                eyebrow = stringResource(if (isActive) R.string.places_turn_back_in else R.string.tool_turn_back),
                title = if (isActive) formattedRemainingTime ?: "--" else instructionText,
                titleStyle = if (isActive) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineMedium,
                supportingText = if (isActive) instructionText else stringResource(R.string.turn_back_empty_description),
                tone = tone,
                badge = {
                    TrailSenseShapeBadge(
                        icon = if (isActive) TrailIcons.Flag else TrailIcons.ToolClock,
                        shape = if (isActive) MaterialShapes.Sunny.toShape() else MaterialShapes.Cookie9Sided.toShape(),
                        size = 64.dp,
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    )
                }
            ) {
                // The track is always there: a preview of the plan until a time is set, the live journey after.
                JourneyTrack(
                    turnFraction = if (isActive) animatedTurn else 0.4f,
                    color = if (isActive) colors.accent else colors.accent.copy(alpha = 0.45f),
                    trackColor = colors.content.copy(alpha = 0.16f),
                    markerRing = colors.container
                )
                Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                    JourneyTile(
                        label = stringResource(R.string.tool_turn_back),
                        value = formattedTurnBackTime ?: "--:--",
                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                        contentColor = colors.content,
                        modifier = Modifier.weight(1f)
                    )
                    JourneyTile(
                        label = stringResource(R.string.places_turn_back_back_by),
                        value = formattedReturnTime ?: "--:--",
                        shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp),
                        contentColor = colors.content,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        trailSenseSection(key = "return", title = returnTitle) {
            row { position ->
                PlacesRow(
                    title = stringResource(R.string.return_time),
                    subtitle = stringResource(R.string.places_turn_back_pick_hint),
                    position = position,
                    modifier = Modifier.testTag("edittext"),
                    leading = {
                        PlacesBadge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            icon = TrailIcons.ToolClock,
                            shape = MaterialShapes.Cookie9Sided.toShape()
                        )
                    },
                    value = formattedReturnTime ?: stringResource(R.string.places_turn_back_set),
                    valueColor = MaterialTheme.colorScheme.primary,
                    onClick = { onPickReturnTime(returnTime) }
                )
            }
            row { position ->
                PlacesRow(
                    title = stringResource(R.string.return_before_dark),
                    subtitle = stringResource(R.string.places_turn_back_sunset_hint),
                    position = position,
                    modifier = Modifier.testTag("sunset_button"),
                    leading = {
                        PlacesBadge(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            icon = TrailIcons.SunsetNotification,
                            shape = MaterialShapes.Sunny.toShape()
                        )
                    },
                    onClick = onUseSunset,
                    actions = {
                        Icon(
                            TrailIcons.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = TrailSenseSpacing.md)
                        )
                    }
                )
            }
        }

        item("cancel") {
            AnimatedVisibility(
                visible = isActive,
                enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut()
            ) {
                FilledTonalButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("cancel_button"),
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(TrailIcons.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(TrailSenseSpacing.sm))
                    Text(stringResource(R.string.turn_back_cancel))
                }
            }
        }
    }
}

/**
 * The walk as a track: out to the turn-around point in full colour, the way back faded, a flag where you turn and
 * a home dot at the end. It shortens as the turn-around time gets closer.
 */
@Composable
private fun JourneyTrack(turnFraction: Float, color: Color, trackColor: Color, markerRing: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val stroke = 12.dp.toPx()
        val y = size.height / 2
        val start = stroke / 2
        val end = size.width - stroke / 2
        val turnX = start + (end - start) * turnFraction
        drawLine(trackColor, Offset(start, y), Offset(end, y), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(start, y), Offset(turnX, y), strokeWidth = stroke, cap = StrokeCap.Round)
        drawCircle(markerRing, radius = 12.dp.toPx(), center = Offset(turnX, y))
        drawCircle(color, radius = 8.dp.toPx(), center = Offset(turnX, y))
        drawCircle(color.copy(alpha = 0.55f), radius = 5.dp.toPx(), center = Offset(end, y))
    }
}

@Composable
private fun JourneyTile(
    label: String,
    value: String,
    shape: androidx.compose.ui.graphics.Shape,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.72f),
        contentColor = contentColor
    ) {
        Column(Modifier.padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = contentColor.copy(alpha = 0.8f))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

