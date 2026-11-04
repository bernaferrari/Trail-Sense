@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.whitenoise.ui

import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.ToolWhiteNoise
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow
import com.kylecorry.trail_sense.shared.views.compose.trailSensePressScale
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.SleepSound
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/**
 * White noise: the play button is the hero, with what is playing written under it; the sound and the sleep timer
 * are one group of rows below, the timer's duration unfolding under its switch.
 */
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

    val soundName = soundTypes.first { it.first == selectedSound }.second

    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.tool_white_noise_title),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            Column(
                modifier = Modifier.padding(vertical = TrailSenseSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                PlayControl(
                    isRunning = isRunning,
                    contentDescription = stringResource(R.string.tool_white_noise_title),
                    stateDescription = stringResource(if (isRunning) R.string.on else R.string.off),
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

                Text(
                    text = if (isRunning) {
                        stringResource(R.string.dot_separated_pair, stringResource(R.string.on), soundName)
                    } else {
                        stringResource(R.string.off)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isRunning) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                TrailSenseListItem(
                    title = stringResource(R.string.tool_sound),
                    subtitle = soundName,
                    icon = rememberVectorPainter(TrailIcons.ToolWhiteNoise),
                    onClick = { showSoundPicker = true },
                    position = TrailSenseGroupPosition(isFirst = true, isLast = false)
                )
                TrailSenseSwitchRow(
                    label = stringResource(R.string.sleep_timer),
                    checked = useSleepTimer,
                    onCheckedChange = {
                        useSleepTimer = it
                        stopWhiteNoise()
                    },
                    position = TrailSenseGroupPosition(isFirst = false, isLast = !useSleepTimer)
                )
                AnimatedVisibility(
                    visible = useSleepTimer,
                    enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(),
                    exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut()
                ) {
                    TrailSenseGroupCard(
                        position = TrailSenseGroupPosition(isFirst = false, isLast = true)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
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
        }
    }

    if (showSoundPicker) {
        AlertDialog(
            onDismissRequest = { showSoundPicker = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.tool_sound)) },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
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
    stateDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val containerColor by animateColorAsState(
        targetValue = if (isRunning) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "playContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isRunning) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "playContentColor"
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (isRunning) 0.3f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "haloAlpha"
    )
    val haloScale by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0.85f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "haloScale"
    )

    // The halo appears only while sound plays, so the control reads as "on" at a glance.
    Box(
        modifier = modifier.size(232.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor.copy(alpha = haloAlpha),
            modifier = Modifier
                .size(232.dp)
                .scale(haloScale)
        ) {}
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .size(184.dp)
                .trailSensePressScale(interactionSource)
                .semantics { this.stateDescription = stateDescription }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.ToolWhiteNoise),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(80.dp)
                )
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TrailSenseSize.minimumTouchTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.size(TrailSenseSpacing.md))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
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
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
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
