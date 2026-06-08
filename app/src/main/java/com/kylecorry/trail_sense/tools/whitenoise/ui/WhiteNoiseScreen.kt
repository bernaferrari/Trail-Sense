@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.whitenoise.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.SleepSound
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

@Composable
fun WhiteNoiseScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cache = remember { PreferencesSubsystem.getInstance(context).preferences }
    val soundTypes = remember {
        listOf(
            SleepSound.WhiteNoise to context.getString(R.string.sleep_sound_white_noise),
            SleepSound.PinkNoise to context.getString(R.string.sleep_sound_pink_noise),
            SleepSound.BrownNoise to context.getString(R.string.sleep_sound_brown_noise),
            SleepSound.Crickets to context.getString(R.string.sleep_sound_crickets),
            SleepSound.CricketsNoChirp to context.getString(R.string.sleep_sound_crickets_no_chirping),
            SleepSound.OceanWaves to context.getString(R.string.sleep_sound_ocean_waves),
            SleepSound.Fan to context.getString(R.string.sleep_sound_fan)
        )
    }

    var selectedSound by remember {
        mutableStateOf(
            SleepSound.entries.withId(
                cache.getLong(WhiteNoiseService.CACHE_KEY_SLEEP_SOUND_ID)
                    ?: SleepSound.PinkNoise.id
            ) ?: SleepSound.PinkNoise
        )
    }
    var isRunning by remember { mutableStateOf(WhiteNoiseService.isRunning) }
    var showSoundPicker by remember { mutableStateOf(false) }
    var useSleepTimer by remember {
        val stopTime = cache.getInstant(WhiteNoiseService.CACHE_KEY_OFF_TIME)
        mutableStateOf(stopTime != null && stopTime > Instant.now())
    }
    var hours by remember { mutableStateOf("00") }
    var minutes by remember { mutableStateOf("00") }
    var seconds by remember { mutableStateOf("00") }

    fun setDuration(duration: Duration) {
        val clamped = duration.coerceIn(Duration.ZERO, MAX_DURATION)
        hours = clamped.toHours().toString().padStart(2, '0')
        minutes = (clamped.toMinutes() % 60).toString().padStart(2, '0')
        seconds = (clamped.seconds % 60).toString().padStart(2, '0')
    }

    fun currentDuration(): Duration {
        return Duration.ofHours(hours.toLongOrNull() ?: 0)
            .plusMinutes(minutes.toLongOrNull() ?: 0)
            .plusSeconds(seconds.toLongOrNull() ?: 0)
            .coerceIn(Duration.ZERO, MAX_DURATION)
    }

    fun stopWhiteNoise() {
        WhiteNoiseService.stop(context)
        WhiteNoiseService.clearSleepTimer(context)
        isRunning = false
    }

    LaunchedEffect(Unit) {
        setDuration(Duration.ZERO)
        while (true) {
            isRunning = WhiteNoiseService.isRunning
            val stopTime = cache.getInstant(WhiteNoiseService.CACHE_KEY_OFF_TIME)
            if (stopTime != null && stopTime > Instant.now()) {
                useSleepTimer = true
                setDuration(Duration.between(Instant.now(), stopTime))
            }
            delay(250)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    if (isRunning) {
                        stopWhiteNoise()
                    } else {
                        WhiteNoiseService.play(
                            context,
                            null,
                            if (useSleepTimer) currentDuration() else null
                        )
                        isRunning = true
                    }
                },
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = if (isRunning) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_tool_white_noise),
                    contentDescription = stringResource(R.string.tool_white_noise_title),
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = { showSoundPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = soundTypes.first { it.first == selectedSound }.second,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sleep_timer),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Switch(
                    checked = useSleepTimer,
                    onCheckedChange = {
                        useSleepTimer = it
                        stopWhiteNoise()
                    }
                )
            }

            if (useSleepTimer) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DurationField(
                        value = hours,
                        label = stringResource(R.string.hours),
                        modifier = Modifier.weight(1f),
                        onChange = {
                            hours = it.cleanDurationPart(99)
                            stopWhiteNoise()
                        }
                    )
                    DurationField(
                        value = minutes,
                        label = stringResource(R.string.minutes),
                        modifier = Modifier.weight(1f),
                        onChange = {
                            minutes = it.cleanDurationPart(59)
                            stopWhiteNoise()
                        }
                    )
                    DurationField(
                        value = seconds,
                        label = stringResource(R.string.seconds),
                        modifier = Modifier.weight(1f),
                        onChange = {
                            seconds = it.cleanDurationPart(59)
                            stopWhiteNoise()
                        }
                    )
                }
            }
        }
    }

    if (showSoundPicker) {
        AlertDialog(
            onDismissRequest = { showSoundPicker = false },
            title = { Text(stringResource(R.string.tool_white_noise_title)) },
            text = {
                Column {
                    soundTypes.forEach { sound ->
                        TextButton(
                            onClick = {
                                selectedSound = sound.first
                                cache.putLong(WhiteNoiseService.CACHE_KEY_SLEEP_SOUND_ID, sound.first.id)
                                stopWhiteNoise()
                                showSoundPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = sound.second,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSoundPicker = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }
}

@Composable
private fun DurationField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

private fun String.cleanDurationPart(max: Int): String {
    val value = filter { it.isDigit() }
        .take(2)
        .toIntOrNull()
        ?: 0
    return value.coerceIn(0, max).toString().padStart(2, '0')
}

private fun Duration.coerceIn(min: Duration, max: Duration): Duration {
    return when {
        this < min -> min
        this > max -> max
        else -> this
    }
}

private val MAX_DURATION: Duration = Duration.ofHours(99).plusMinutes(59).plusSeconds(59)
