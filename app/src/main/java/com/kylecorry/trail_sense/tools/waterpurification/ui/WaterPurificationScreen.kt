@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.waterpurification.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

const val WATER_PURIFICATION_END_TIME_KEY = "water_purification_start_time"

@Composable
internal fun WaterPurificationScreen(
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
        cache.remove(WATER_PURIFICATION_END_TIME_KEY)
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
                                    WATER_PURIFICATION_END_TIME_KEY,
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
        cache.getInstant(WATER_PURIFICATION_END_TIME_KEY) ?: return null
    val duration = Duration.between(Instant.now(), lastEndTime)
    return if (!duration.isNegative && !duration.isZero) duration else null
}

internal enum class TimeSelection {
    Auto,
    LowAltitude,
    HighAltitude
}
