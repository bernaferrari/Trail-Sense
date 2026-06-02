package com.kylecorry.trail_sense.tools.turn_back.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.permissions.PermissionRationale
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.permissions.SpecialPermission
import com.kylecorry.andromeda.pickers.material.MaterialPickers
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils.time
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.shared.permissions.IsBackgroundLocationRequired
import com.kylecorry.trail_sense.shared.permissions.alertExactAlarmsDenied
import com.kylecorry.trail_sense.shared.permissions.requirePermissionRequester
import com.kylecorry.trail_sense.shared.preferences.PreferencesFlag
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
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

class TurnBackFragment : Fragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val sharedPrefs by lazy { PreferencesSubsystem.getInstance(requireContext()) }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val astronomy = AstronomyService()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    TurnBackScreen(
                        formatter = formatter,
                        loadTurnBackTime = {
                            sharedPrefs.preferences.getInstant(PREF_TURN_BACK_TIME)
                        },
                        loadReturnTime = {
                            sharedPrefs.preferences.getInstant(PREF_TURN_BACK_RETURN_TIME)
                        },
                        onPickReturnTime = ::pickReturnTime,
                        onUseSunset = ::setReturnTimeToSunset,
                        onCancel = ::cancelTurnBack
                    )
                }
            }
        }
    }

    private fun pickReturnTime(currentReturnTime: Instant?) {
        MaterialPickers.time(
            requireActivity().supportFragmentManager,
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

    private fun setReturnTimeToSunset() {
        lifecycleScope.launch {
            var wasSuccessful = false
            var sunsetTime: ZonedDateTime? = null
            val job = launch(Dispatchers.IO) {
                val location = SensorSubsystem.getInstance(requireContext()).getLocation()
                sunsetTime = astronomy.getNextSunset(location, SunTimesMode.Actual)
                    ?.atZone(ZoneId.systemDefault())
                wasSuccessful = true
            }

            Alerts.withCancelableLoading(
                requireContext(),
                getString(R.string.loading),
                onCancel = { job.cancel() }
            ) {
                lifecycleScope.launch {
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

        sharedPrefs.preferences.putInstant(PREF_TURN_BACK_TIME, newTurnBackTime)
        sharedPrefs.preferences.putInstant(PREF_TURN_BACK_RETURN_TIME, instant)
        enableTurnBackAlarm()

        toast(getString(R.string.turn_back_alert_set))
    }

    private fun enableTurnBackAlarm() {
        requestExactAlarms {
            TurnBackAlarmReceiver.start(requireContext())
            requestBackgroundLocation()
        }
    }

    private fun requestExactAlarms(action: (hasPermission: Boolean) -> Unit) {
        requirePermissionRequester().requestPermission(
            SpecialPermission.SCHEDULE_EXACT_ALARMS,
            PermissionRationale(
                getString(R.string.allow_schedule_exact_alarms, getString(R.string.app_name)),
                AppServiceRegistry.get<MarkdownService>().toMarkdown(
                    getString(
                        R.string.allow_schedule_exact_alarms_instructions,
                        getString(R.string.app_name),
                        getString(R.string.settings)
                    )
                ),
                ok = getString(R.string.settings),
            )
        ) {
            val isGranted = Permissions.hasPermission(
                requireContext(),
                SpecialPermission.SCHEDULE_EXACT_ALARMS
            )
            if (!isGranted) {
                alertExactAlarmsDenied()
            }
            action(isGranted)
        }
    }

    private fun requestBackgroundLocation() {
        val shownFlag = PreferencesFlag(
            sharedPrefs.preferences,
            "cache_background_location_requested"
        )
        if (!IsBackgroundLocationRequired().isSatisfiedBy(requireContext())) {
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
            this,
            getString(R.string.sunset_alerts_background_location_disclaimer),
            Snackbar.LENGTH_LONG,
            getString(R.string.learn_more)
        ) {
            val markdown = AppServiceRegistry.get<MarkdownService>()
            val contents =
                markdown.toMarkdown(getString(R.string.access_background_location_rationale))

            Alerts.dialog(
                requireContext(),
                getString(R.string.access_background_location),
                contents,
                okText = getString(R.string.settings),
                allowLinks = true
            ) { cancelled ->
                if (!cancelled) {
                    requirePermissionRequester().requestPermissions(
                        listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    ) {
                        // No-op
                    }
                }
            }
        }
    }

    private fun cancelTurnBack() {
        sharedPrefs.preferences.remove(PREF_TURN_BACK_TIME)
        sharedPrefs.preferences.remove(PREF_TURN_BACK_RETURN_TIME)
        TurnBackAlarmReceiver.stop(requireContext())
    }

    companion object {
        const val PREF_TURN_BACK_TIME = "pref_turn_back_time"
        const val PREF_TURN_BACK_RETURN_TIME = "pref_turn_back_return_time"
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
