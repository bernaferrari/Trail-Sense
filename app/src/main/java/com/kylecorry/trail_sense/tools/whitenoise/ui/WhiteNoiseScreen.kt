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
                .navigationBarsPadding()
                .padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            Column(
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
                    text = if (isRunning) {
                        stringResource(R.string.dot_separated_pair, stringResource(R.string.on), soundName)
                    } else {
                        stringResource(R.string.dot_separated_pair, stringResource(R.string.off), soundName)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                TrailSenseGroupHeader(stringResource(R.string.tool_sound))
                // Every sound one tap away instead of behind a dialog: one connected grid, the chosen one solid
                // primary, and a lone last sound stretched across the row so nothing orphans.
                Column(
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs),
                    modifier = Modifier.selectableGroup()
                ) {
                    val rows = soundTypes.chunked(2)
                    rows.forEachIndexed { rowIndex, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                            row.forEachIndexed { columnIndex, sound ->
                                SoundTile(
                                    label = sound.second,
                                    icon = soundIcon(sound.first),
                                    selected = sound.first == selectedSound,
                                    shape = trailSenseGridCellShape(columnIndex, row.size, rowIndex, rows.size),
                                    modifier = Modifier.weight(1f),
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
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier.padding(TrailSenseSpacing.md),
                            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
                        ) {
                            listOf(15L, 30L, 60L, 120L).forEach { mins ->
                                val duration = Duration.ofMinutes(mins)
                                SoundChip(
                                    label = if (mins < 60) {
                                        stringResource(R.string.white_noise_minutes_short, mins.toInt())
                                    } else {
                                        stringResource(R.string.white_noise_hours_short, (mins / 60).toInt())
                                    },
                                    selected = currentDuration() == duration,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        setDuration(duration)
                                        stopWhiteNoise()
                                    }
                                )
                            }
                        }
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
 * The control is the hero: a large cookie that plays and stops the sound. At rest it is a calm tonal shape saying
 * "tap to play"; while sound plays it fills primary, breathes, turns slowly, and soft rings swell out behind it.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayControl(
    isRunning: Boolean,
    sound: SleepSound,
    soundName: String,
    contentDescription: String,
    stateDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (isRunning) scheme.tertiary else scheme.tertiaryContainer,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "playContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isRunning) scheme.onTertiary else scheme.onTertiaryContainer,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "playContentColor"
    )
    val transition = rememberInfiniteTransition(label = "breath")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600), RepeatMode.Reverse),
        label = "breathing"
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )
    val active by animateFloatAsState(
        if (isRunning) 1f else 0f,
        MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "active"
    )
    val ring = scheme.tertiary
    // The blob takes the sound's character: spiky for white noise, softer down to a gentle round for brown.
    val target = soundShape(sound)
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val morphProgress = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(target) {
        if (target != to) {
            from = to
            to = target
            morphProgress.snapTo(0f)
            morphProgress.animateTo(1f, androidx.compose.animation.core.tween(600))
        }
    }
    val morph = remember(from, to) { androidx.graphics.shapes.Morph(from, to) }
    val blobShape = remember(morph, morphProgress.value) { NoiseMorphShape(morph, morphProgress.value) }

    Box(
        modifier = modifier.size(264.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (active > 0f) {
                val base = size.minDimension * 0.36f
                listOf(0.16f, 0.1f, 0.06f).forEachIndexed { i, alpha ->
                    drawCircle(
                        ring.copy(alpha = alpha * active),
                        radius = base + (i + 1) * size.minDimension * (0.035f + 0.025f * breath)
                    )
                }
            }
        }
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            shape = blobShape,
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .size(208.dp)
                .graphicsLayer {
                    val s = 1f + 0.035f * breath * active
                    scaleX = s
                    scaleY = s
                    rotationZ = spin * active
                }
                .trailSensePressScale(interactionSource)
                .semantics {
                    this.contentDescription = contentDescription
                    this.stateDescription = stateDescription
                }
        ) {}
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = rememberVectorPainter(
                    if (isRunning) TrailIcons.BaselineStop24 else TrailIcons.BaselinePlayArrow24
                ),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = stringResource(if (isRunning) R.string.white_noise_tap_to_stop else R.string.white_noise_tap_to_play),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 150.dp)
            )
        }
    }
}

/** A sound as an equal tile: icon and name, the chosen one solid primary with a check. */
@Composable
private fun SoundTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) scheme.primary else scheme.secondaryContainer,
        MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "soundTile"
    )
    val content by animateColorAsState(
        if (selected) scheme.onPrimary else scheme.onSecondaryContainer,
        MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "soundTileContent"
    )
    Surface(
        modifier = modifier
            .heightIn(min = 64.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = shape,
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                rememberVectorPainter(if (selected) TrailIcons.Check else icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(TrailSenseSpacing.sm))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 2)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
private fun soundShape(sound: SleepSound): androidx.graphics.shapes.RoundedPolygon = when (sound) {
    SleepSound.WhiteNoise -> MaterialShapes.VerySunny
    SleepSound.PinkNoise -> MaterialShapes.Cookie12Sided
    SleepSound.BrownNoise -> MaterialShapes.Cookie6Sided
    SleepSound.Crickets, SleepSound.CricketsNoChirp -> MaterialShapes.Flower
    SleepSound.OceanWaves -> MaterialShapes.Cookie9Sided
    SleepSound.Fan -> MaterialShapes.Clover4Leaf
    else -> MaterialShapes.Cookie12Sided
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

private fun soundIcon(sound: SleepSound): androidx.compose.ui.graphics.vector.ImageVector = when (sound) {
    SleepSound.Crickets, SleepSound.CricketsNoChirp -> TrailIcons.Grass
    SleepSound.OceanWaves -> TrailIcons.Beach
    SleepSound.Fan -> TrailIcons.Wind
    else -> TrailIcons.ToolWhiteNoise
}

@Composable
private fun SoundChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) scheme.primary else scheme.secondaryContainer,
        MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "soundChip"
    )
    val content by animateColorAsState(
        if (selected) scheme.onPrimary else scheme.onSecondaryContainer,
        MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "soundChipContent"
    )
    Surface(
        modifier = modifier
            .heightIn(min = TrailSenseSize.minimumTouchTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = if (selected) MaterialTheme.shapes.medium else CircleShape,
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.sm, vertical = TrailSenseSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(
                    rememberVectorPainter(TrailIcons.Check),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(TrailSenseSpacing.xs))
            }
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
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
