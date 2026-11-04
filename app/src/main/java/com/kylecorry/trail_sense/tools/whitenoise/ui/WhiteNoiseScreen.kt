@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.whitenoise.ui

import androidx.compose.foundation.layout.navigationBarsPadding
import com.kylecorry.trail_sense.shared.icons.trail.Grass
import com.kylecorry.trail_sense.shared.icons.trail.Beach
import com.kylecorry.trail_sense.shared.icons.trail.Wind
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGridCellShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseValueChip
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import com.kylecorry.trail_sense.shared.icons.trail.BaselineStop24
import com.kylecorry.trail_sense.shared.icons.trail.BaselinePlayArrow24
import com.kylecorry.trail_sense.shared.icons.trail.Check
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
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
import androidx.graphics.shapes.toPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.foundation.layout.height
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
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

    // One question: is the sound playing, and which one?
    TrailSensePage(
        title = stringResource(R.string.tool_white_noise_title),
        onBack = rememberTrailSenseBack(),
        backLabel = stringResource(R.string.back),
        modifier = modifier
    ) {
        item(key = "player") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
            ) {
                PlayControl(
                    isRunning = isRunning,
                    sound = selectedSound,
                    soundName = soundName,
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
                    text = soundName,
                    modifier = Modifier.padding(top = TrailSenseSpacing.sm),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                // Idle, the line invites the one action instead of just saying "Off".
                Text(
                    text = stringResource(
                        if (isRunning) R.string.white_noise_tap_to_stop else R.string.white_noise_tap_to_play
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        item(key = "controls") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                TrailSenseGroupHeader(stringResource(R.string.tool_sound))
                // Every sound one tap away instead of behind a dialog, as one quiet list with the chosen one checked.
                Column(
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs),
                    modifier = Modifier.selectableGroup()
                ) {
                    soundTypes.forEachIndexed { index, sound ->
                        SoundTile(
                            label = sound.second,
                            selected = sound.first == selectedSound,
                            shape = trailSenseGroupShape(TrailSenseGroupPosition.at(index, soundTypes.size)),
                            onClick = {
                                if (sound.first != selectedSound) {
                                    selectedSound = sound.first
                                    cache.putLong(WhiteNoiseService.CACHE_KEY_SLEEP_SOUND_ID, sound.first.id)
                                    stopWhiteNoise()
                                }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(TrailSenseSpacing.lg))
                TrailSenseSwitchRow(
                    label = stringResource(R.string.sleep_timer),
                    checked = useSleepTimer,
                    onCheckedChange = {
                        useSleepTimer = it
                        stopWhiteNoise()
                    },
                    position = TrailSenseGroupPosition(isFirst = true, isLast = !useSleepTimer)
                )
                AnimatedVisibility(
                    visible = useSleepTimer,
                    enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(),
                    exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                    // Quick durations as one tap each; the fields below stay for anything else.
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = trailSenseGroupShape(TrailSenseGroupPosition(isFirst = false, isLast = false)),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        // The family's one selection look; a typed duration that matches none leaves all unchecked.
                        TrailSenseConnectedChoice(
                            options = listOf(15L, 30L, 60L, 120L).map { mins ->
                                TrailSenseChoice(
                                    value = Duration.ofMinutes(mins),
                                    label = if (mins < 60) {
                                        stringResource(R.string.white_noise_minutes_short, mins.toInt())
                                    } else {
                                        stringResource(R.string.white_noise_hours_short, (mins / 60).toInt())
                                    }
                                )
                            },
                            selected = currentDuration(),
                            onSelect = {
                                setDuration(it)
                                stopWhiteNoise()
                            },
                            label = stringResource(R.string.sleep_timer),
                            modifier = Modifier.padding(TrailSenseSpacing.md)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
                    ) {
                        val last = TrailSenseGroupPosition(isFirst = false, isLast = true)
                        DurationField(
                            value = hours,
                            label = stringResource(R.string.hours),
                            modifier = Modifier.weight(1f),
                            position = last,
                            onChange = {
                                hours = it.cleanDurationPart(99)
                                stopWhiteNoise()
                            }
                        )
                        DurationField(
                            value = minutes,
                            label = stringResource(R.string.minutes),
                            modifier = Modifier.weight(1f),
                            position = last,
                            onChange = {
                                minutes = it.cleanDurationPart(59)
                                stopWhiteNoise()
                            }
                        )
                        DurationField(
                            value = seconds,
                            label = stringResource(R.string.seconds),
                            modifier = Modifier.weight(1f),
                            position = last,
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
}

/**
 * The main action: a large primary cookie that plays and stops the sound. It rounds a little while sound plays, so
 * the shape says the state; there is no idle motion.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayControl(
    isRunning: Boolean,
    @Suppress("UNUSED_PARAMETER") sound: SleepSound,
    @Suppress("UNUSED_PARAMETER") soundName: String,
    contentDescription: String,
    stateDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme
    val round by animateFloatAsState(
        if (isRunning) 1f else 0f,
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "round"
    )
    val morph = remember { androidx.graphics.shapes.Morph(MaterialShapes.Cookie12Sided, MaterialShapes.Cookie9Sided) }
    val blobShape = remember(round) { NoiseMorphShape(morph, round) }

    Box(
        modifier = modifier.size(232.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            shape = blobShape,
            color = scheme.primary,
            contentColor = scheme.onPrimary,
            modifier = Modifier
                .size(208.dp)
                .trailSensePressScale(interactionSource)
                .semantics {
                    this.contentDescription = contentDescription
                    this.stateDescription = stateDescription
                }
        ) {}
        Icon(
            painter = rememberVectorPainter(
                if (isRunning) TrailIcons.BaselineStop24 else TrailIcons.BaselinePlayArrow24
            ),
            contentDescription = null,
            tint = scheme.onPrimary,
            modifier = Modifier.size(72.dp)
        )
    }
}

/**
 * A sound as a quiet list row: just its name (the icons said nothing the names don't), the chosen one in the
 * selection fill with a check.
 */
@Composable
private fun SoundTile(
    label: String,
    selected: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = shape,
        color = if (selected) scheme.secondaryContainer else scheme.surfaceContainer,
        contentColor = if (selected) scheme.onSecondaryContainer else scheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    rememberVectorPainter(TrailIcons.Check),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private class NoiseMorphShape(
    private val morph: androidx.graphics.shapes.Morph,
    private val progress: Float
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = morph.toPath(progress)
        path.transform(android.graphics.Matrix().apply { setScale(size.width, size.height) })
        return androidx.compose.ui.graphics.Outline.Generic(path.asComposePath())
    }
}

@Composable
private fun DurationField(
    value: String,
    label: String,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit
) {
    TrailSenseInputField(
        value = value,
        onValueChange = onChange,
        label = label,
        modifier = modifier,
        position = position,
        keyboardType = KeyboardType.Number
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
