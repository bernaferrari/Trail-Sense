@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.turn_back.ui

import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
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
        loadSunset = controller::getNextSunset,
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

    /** Tonight's sunset at the last known location, for the "Return before dark" row (no GPS fix is requested). */
    suspend fun getNextSunset(): Instant? = withContext(Dispatchers.Default) {
        val location = LocationSubsystem.getInstance(host.requireContext()).location
        astronomy.getNextSunset(location, SunTimesMode.Actual)?.atZone(ZoneId.systemDefault())?.toInstant()
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
                    title = stringResource(R.string.places_turn_back_idle_title),
                    description = stringResource(R.string.turn_back_empty_description),
                    modifier = Modifier
                        .fillParentMaxHeight(0.9f)
                        .testTag("instructions"),
                    actionLabel = stringResource(R.string.places_turn_back_set_return_time),
                    onAction = { onPickReturnTime(returnTime) },
                    actionModifier = Modifier.testTag("edittext"),
                    action = {
                        TextButton(
                            onClick = onUseSunset,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .heightIn(min = TrailSenseSize.minimumTouchTarget)
                                .testTag("sunset_button")
                        ) {
                            Text(
                                if (formattedSunset != null) {
                                    stringResource(R.string.places_turn_back_before_dark_at, formattedSunset)
                                } else {
                                    stringResource(R.string.return_before_dark)
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
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
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
            ) {
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
            val height = ButtonDefaults.MediumContainerHeight
            FilledTonalButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = height)
                    .testTag("cancel_button"),
                shapes = ButtonDefaults.shapesFor(height),
                contentPadding = ButtonDefaults.contentPaddingFor(height)
            ) {
                Icon(
                    TrailIcons.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.iconSizeFor(height))
                )
                Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
                Text(stringResource(R.string.turn_back_cancel), style = ButtonDefaults.textStyleFor(height))
            }
        }
    }
}
