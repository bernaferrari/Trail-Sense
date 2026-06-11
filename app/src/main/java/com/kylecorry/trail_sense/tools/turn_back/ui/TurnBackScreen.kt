@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.turn_back.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
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
            val markdown = AppServiceRegistry.get<MarkdownService>()
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
    val context = LocalContext.current
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
    val instructions = if (formattedRemainingTime != null) {
        context.getString(
            R.string.turn_back_instructions,
            formattedTurnBackTime,
            formattedRemainingTime,
            formattedReturnTime
        )
    } else {
        stringResource(R.string.time_not_set)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = instructions,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("instructions"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = formattedReturnTime.orEmpty(),
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clickable { onPickReturnTime(returnTime) }
                    .testTag("edittext"),
                label = { Text(stringResource(R.string.return_time)) },
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null
                    )
                }
            )
            Button(
                onClick = onUseSunset,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .testTag("sunset_button")
            ) {
                Text(stringResource(R.string.return_before_dark))
            }
            Spacer(modifier = Modifier.weight(1f))
            if (returnTime != null) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .testTag("cancel_button")
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
    }
}
