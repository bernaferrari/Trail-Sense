package com.kylecorry.trail_sense.tools.waterpurification.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.waterpurification.domain.WaterService
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant

class WaterPurificationFragment : Fragment() {
    private val sensorService by lazy { SensorService(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val altimeter by lazy { sensorService.getAltimeter() }
    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }
    private val waterService = WaterService()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    WaterPurificationScreen(
                        cache = cache,
                        formatService = formatService,
                        getSelectedDuration = { selection ->
                            getSelectedDuration(selection)
                        }
                    )
                }
            }
        }
    }

    private suspend fun getSelectedDuration(selection: TimeSelection): Duration = onDefault {
        if (selection == TimeSelection.Auto) {
            if (!altimeter.hasValidReading) {
                withTimeoutOrNull(Duration.ofSeconds(10).toMillis()) {
                    altimeter.read()
                }
            }

            if (altimeter.hasValidReading) {
                return@onDefault waterService.getPurificationTime(
                    Distance.from(altimeter.altitude, DistanceUnits.Meters)
                )
            }
        } else if (selection == TimeSelection.LowAltitude) {
            return@onDefault Duration.ofMinutes(1)
        }

        Duration.ofMinutes(3)
    }

    companion object {
        const val WATER_PURIFICATION_END_TIME_KEY = "water_purification_start_time"
    }
}

@Composable
private fun WaterPurificationScreen(
    cache: IPreferences,
    formatService: FormatService,
    getSelectedDuration: suspend (TimeSelection) -> Duration
) {
    val context = LocalContext.current
    var selectedTime by remember { mutableStateOf(TimeSelection.Auto) }
    var duration by remember { mutableStateOf<Duration?>(null) }
    var remaining by remember { mutableStateOf<Duration?>(null) }
    val isRunning = remaining != null

    LaunchedEffect(selectedTime) {
        duration = null
        duration = getSelectedDuration(selectedTime)
    }

    LaunchedEffect(Unit) {
        while (true) {
            remaining = getRemainingTime(cache)
            delay(250)
        }
    }

    fun stop() {
        cache.remove(WaterPurificationFragment.WATER_PURIFICATION_END_TIME_KEY)
        WaterPurificationTimerService.stop(context)
        remaining = null
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WaterStep(stringResource(R.string.one), stringResource(R.string.water_purification_step_1))
                WaterStep(stringResource(R.string.two), stringResource(R.string.water_purification_step_2))
                WaterStep(stringResource(R.string.three), stringResource(R.string.water_purification_step_3))
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val displayed = remaining ?: duration
                if (displayed == null) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = displayed.seconds.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (isRunning) {
                                stop()
                            } else {
                                val selectedDuration = duration ?: Duration.ofMinutes(3)
                                cache.putInstant(
                                    WaterPurificationFragment.WATER_PURIFICATION_END_TIME_KEY,
                                    Instant.now().plus(selectedDuration)
                                )
                                WaterPurificationTimerService.start(
                                    context,
                                    selectedDuration.seconds
                                )
                            }
                        }
                    ) {
                        Text(stringResource(if (isRunning) android.R.string.cancel else R.string.start))
                    }
                }
            }

            if (!isRunning) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TimeChip(
                        text = stringResource(R.string.auto),
                        selected = selectedTime == TimeSelection.Auto,
                        onClick = { selectedTime = TimeSelection.Auto }
                    )
                    TimeChip(
                        text = formatService.formatDuration(Duration.ofMinutes(1), short = true),
                        selected = selectedTime == TimeSelection.LowAltitude,
                        onClick = { selectedTime = TimeSelection.LowAltitude }
                    )
                    TimeChip(
                        text = formatService.formatDuration(Duration.ofMinutes(3), short = true),
                        selected = selectedTime == TimeSelection.HighAltitude,
                        onClick = { selectedTime = TimeSelection.HighAltitude }
                    )
                }
            }
        }
    }
}

@Composable
private fun WaterStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = number,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun TimeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        modifier = Modifier.padding(8.dp)
    )
}

private fun getRemainingTime(cache: IPreferences): Duration? {
    val lastEndTime =
        cache.getInstant(WaterPurificationFragment.WATER_PURIFICATION_END_TIME_KEY) ?: return null
    val duration = Duration.between(Instant.now(), lastEndTime)
    return if (!duration.isNegative && !duration.isZero) duration else null
}

private enum class TimeSelection {
    Auto,
    LowAltitude,
    HighAltitude
}
