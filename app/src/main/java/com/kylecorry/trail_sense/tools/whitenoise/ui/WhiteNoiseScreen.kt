@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.whitenoise.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
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
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .padding(
                    horizontal = TrailSenseSpacing.screen,
                    vertical = TrailSenseSpacing.xxl
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PlayControl(
                isRunning = isRunning,
                contentDescription = stringResource(R.string.tool_white_noise_title),
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
                }
            )

            Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))

            Text(
                text = stringResource(
                    if (isRunning) R.string.on else R.string.off
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isRunning) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.height(TrailSenseSpacing.xxl))

            SoundSelector(
                label = soundTypes.first { it.first == selectedSound }.second,
                onClick = { showSoundPicker = true }
            )

            Spacer(modifier = Modifier.height(TrailSenseSpacing.md))

            SleepTimerCard(
                enabled = useSleepTimer,
                onEnabledChange = {
                    useSleepTimer = it
                    stopWhiteNoise()
                },
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                onHoursChange = {
                    hours = it.cleanDurationPart(99)
                    stopWhiteNoise()
                },
                onMinutesChange = {
                    minutes = it.cleanDurationPart(59)
                    stopWhiteNoise()
                },
                onSecondsChange = {
                    seconds = it.cleanDurationPart(59)
                    stopWhiteNoise()
                }
            )
        }
        }
    }

    if (showSoundPicker) {
        AlertDialog(
            onDismissRequest = { showSoundPicker = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.tool_white_noise_title)) },
            text = {
                Column {
                    soundTypes.forEach { sound ->
                        SoundOption(
                            label = sound.second,
                            selected = sound.first == selectedSound,
                            onClick = {
                                selectedSound = sound.first
                                cache.putLong(WhiteNoiseService.CACHE_KEY_SLEEP_SOUND_ID, sound.first.id)
                                stopWhiteNoise()
                                showSoundPicker = false
                            }
                        )
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
private fun PlayControl(
    isRunning: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isRunning) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        label = "playContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isRunning) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        label = "playContentColor"
    )
    val pulse by animateFloatAsState(
        targetValue = if (isRunning) 1.04f else 1f,
        animationSpec = TrailSenseAnimation.standardSpring,
        label = "playPulse"
    )

    // Soft halo behind the control, emphasized while playing.
    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        val haloAlpha by animateFloatAsState(
            targetValue = if (isRunning) 0.35f else 0.18f,
            animationSpec = TrailSenseAnimation.mediumFade,
            label = "haloAlpha"
        )
        Surface(
            shape = CircleShape,
            color = containerColor.copy(alpha = haloAlpha),
            modifier = Modifier
                .size(240.dp)
                .scale(pulse)
        ) {}
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .size(196.dp)
                .scale(pulse)
                .clip(CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_tool_white_noise),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(84.dp)
                )
            }
        }
    }
}

@Composable
private fun SoundSelector(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = TrailSenseSpacing.lg, horizontal = TrailSenseSpacing.lg)
        )
    }
}

@Composable
private fun SleepTimerCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    hours: String,
    minutes: String,
    seconds: String,
    onHoursChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
    onSecondsChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = TrailSenseSpacing.lg,
                vertical = TrailSenseSpacing.sm
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TrailSenseSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sleep_timer),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            AnimatedVisibility(
                visible = enabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TrailSenseSpacing.xs, bottom = TrailSenseSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
                ) {
                    DurationField(
                        value = hours,
                        label = stringResource(R.string.hours),
                        modifier = Modifier.weight(1f),
                        onChange = onHoursChange
                    )
                    DurationField(
                        value = minutes,
                        label = stringResource(R.string.minutes),
                        modifier = Modifier.weight(1f),
                        onChange = onMinutesChange
                    )
                    DurationField(
                        value = seconds,
                        label = stringResource(R.string.seconds),
                        modifier = Modifier.weight(1f),
                        onChange = onSecondsChange
                    )
                }
            }
        }
    }
}

@Composable
private fun SoundOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md)
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
        shape = MaterialTheme.shapes.medium,
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
