package com.kylecorry.trail_sense.tools

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGridCellShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseValueChip
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.sound.ISoundPlayer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.icons.trail.Pause
import com.kylecorry.trail_sense.shared.icons.trail.ToolWhistle
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.WhistleSos
import com.kylecorry.trail_sense.shared.morse.Signal
import com.kylecorry.trail_sense.shared.morse.SignalPlayer
import com.kylecorry.trail_sense.shared.morse.Signals
import com.kylecorry.trail_sense.shared.morse.asSignal
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.whistle.infrastructure.Whistle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.graphics.shapes.RoundedPolygon
import com.kylecorry.trail_sense.shared.icons.trail.Alert
import com.kylecorry.trail_sense.shared.icons.trail.BaselineStop24
import com.kylecorry.trail_sense.shared.icons.trail.Check
import com.kylecorry.trail_sense.shared.icons.trail.HelpSimple
import com.kylecorry.trail_sense.shared.icons.trail.Location
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import java.time.Duration

private const val WHISTLE = "whistle"

private data class WhistleSignal(
    val key: String,
    val label: String,
    val signal: List<Signal>,
    val loop: Boolean,
    val icon: ImageVector,
    val shape: RoundedPolygon
)

/**
 * The whistle: one big hold-to-whistle pad is the instrument; the signal patterns sit below as one group, each row
 * starting (or, when it is the one playing, stopping) its pattern. Holding the pad always interrupts a pattern.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WhistleScreen(
    modifier: Modifier = Modifier,
    onAudioUnavailable: () -> Unit = {}
) {
    val context = LocalContext.current

    var currentSignal by remember { mutableStateOf<String?>(null) }

    // Audio engine (lifecycle managed)
    val audioScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    var whistlePlayer by remember { mutableStateOf<ISoundPlayer?>(null) }
    var signalPlayer by remember { mutableStateOf<SignalPlayer?>(null) }

    DisposableEffect(Unit) {
        audioScope.launch {
            try {
                val w = Whistle()
                val s = SignalPlayer(w.asSignal())
                withContext(Dispatchers.Main) {
                    whistlePlayer = w
                    signalPlayer = s
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Alerts.dialog(
                        context,
                        context.getString(
                            R.string.feature_unavailable,
                            context.getString(R.string.tool_whistle_title)
                        ),
                        context.getString(R.string.audio_error_messsage),
                        cancelText = null
                    ) {
                        onAudioUnavailable()
                    }
                }
            }
        }

        onDispose {
            signalPlayer?.cancel()
            whistlePlayer?.off()
            (whistlePlayer as? com.kylecorry.andromeda.sound.SoundPlayer)?.release()
        }
    }

    val signals = remember(context) {
        listOf(
            WhistleSignal("help", context.getString(R.string.help), Signals.help(), loop = true,
                icon = TrailIcons.Alert, shape = MaterialShapes.Burst),
            WhistleSignal(
                "sos",
                context.getString(R.string.sos),
                Signals.sos(Duration.ofMillis(400)) + listOf(Signal.off(Duration.ofMillis(400 * 7))),
                loop = true,
                icon = TrailIcons.ToolWhistle,
                shape = MaterialShapes.SoftBurst
            ),
            WhistleSignal(
                "where_are_you",
                context.getString(R.string.whistle_signal_where_are_you),
                Signals.comeHere(),
                loop = false,
                icon = TrailIcons.HelpSimple,
                shape = MaterialShapes.Clover4Leaf
            ),
            WhistleSignal(
                "acknowledged",
                context.getString(R.string.whistle_signal_acknowledged),
                Signals.acknowledged(),
                loop = false,
                icon = TrailIcons.Check,
                shape = MaterialShapes.Cookie6Sided
            ),
            WhistleSignal(
                "come_here",
                context.getString(R.string.whistle_signal_come_here),
                Signals.comeHere(),
                loop = false,
                icon = TrailIcons.Location,
                shape = MaterialShapes.Flower
            )
        )
    }

    fun stopAll() {
        signalPlayer?.cancel()
        whistlePlayer?.off()
        currentSignal = null
    }

    fun playContinuous() {
        stopAll()
        whistlePlayer?.on()
        currentSignal = WHISTLE
    }

    fun stopContinuous() {
        if (currentSignal == WHISTLE) {
            whistlePlayer?.off()
            currentSignal = null
        }
    }

    fun toggleSignal(signal: WhistleSignal) {
        if (currentSignal == signal.key) {
            stopAll()
            return
        }
        stopAll()
        signalPlayer?.play(signal.signal, signal.loop)
        currentSignal = signal.key
    }

    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.tool_whistle_title),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = TrailSenseSize.maximumContentWidth)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    top = TrailSenseSpacing.lg,
                    bottom = TrailSenseSpacing.xxxl
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl)
        ) {
            HoldToWhistlePad(
                active = currentSignal == WHISTLE,
                onPress = ::playContinuous,
                onRelease = ::stopContinuous
            )

            // Emergency calls first, as a pair of big tiles; then the conversational ones, each drawing its rhythm.
            val (emergency, calls) = signals.partition { it.loop }
            Column(Modifier.fillMaxWidth()) {
                TrailSenseGroupHeader(stringResource(R.string.whistle_emergency))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
                ) {
                    emergency.forEachIndexed { index, signal ->
                        EmergencyTile(
                            signal = signal,
                            playing = currentSignal == signal.key,
                            shape = trailSenseGridCellShape(index, emergency.size, 0, 1),
                            onClick = { toggleSignal(signal) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
            listOf(
                stringResource(R.string.whistle_signals) to calls
            ).forEach { (title, group) ->
                TrailSenseRowGroup(
                    title = title,
                    footer = if (group === calls) stringResource(R.string.whistle_legend) else null
                ) {
                    group.forEach { signal ->
                        row { position ->
                            WhistleSignalRow(
                                signal = signal,
                                playing = currentSignal == signal.key,
                                position = position,
                                onClick = { toggleSignal(signal) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One signal: a shaped badge with its own glyph, the name, and the pattern itself drawn as a strip of blasts (long
 * pills and short dots), so "three long" and "dot dot dot dash dash dash" read without the guide. Playing turns the
 * row primary and the badge into a stop control.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WhistleSignalRow(
    signal: WhistleSignal,
    playing: Boolean,
    position: TrailSenseGroupPosition,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        when {
            playing -> scheme.primaryContainer
            signal.loop -> scheme.errorContainer
            else -> scheme.surfaceContainerLow
        },
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "signalRow"
    )
    val badgeColor = when {
        playing -> scheme.primary
        signal.loop -> scheme.error
        else -> scheme.secondaryContainer
    }
    val badgeContent = when {
        playing -> scheme.onPrimary
        signal.loop -> scheme.onError
        else -> scheme.onSecondaryContainer
    }
    val ink = when {
        playing -> scheme.onPrimaryContainer
        signal.loop -> scheme.onErrorContainer
        else -> scheme.onSurface
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = trailSenseGroupShape(position),
        color = container
    ) {
        Row(
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrailSenseShapeBadge(
                icon = if (playing) TrailIcons.BaselineStop24 else signal.icon,
                shape = signal.shape.toShape(),
                size = 48.dp,
                containerColor = badgeColor,
                contentColor = badgeContent
            )
            Spacer(Modifier.width(TrailSenseSpacing.lg))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)) {
                Text(
                    signal.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ink
                )
                SignalRhythm(
                    signal = signal.signal,
                    color = if (playing) scheme.primary else if (signal.loop) scheme.error else scheme.secondary,
                    playing = playing
                )
            }
            Spacer(Modifier.width(TrailSenseSpacing.sm))
            TrailSenseValueChip(
                text = if (playing) {
                    stringResource(R.string.whistle_playing)
                } else if (signal.loop) {
                    stringResource(R.string.whistle_repeats_short)
                } else {
                    stringResource(R.string.whistle_once)
                },
                tone = when {
                    playing -> TrailSenseTone.Primary
                    signal.loop -> TrailSenseTone.Error
                    else -> TrailSenseTone.Secondary
                },
                strong = playing
            )
        }
    }
}

/** An emergency call as a tall error-toned tile: solid starburst, name, its rhythm, and whether it repeats. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmergencyTile(
    signal: WhistleSignal,
    playing: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (playing) scheme.error else scheme.errorContainer,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "emergencyTile"
    )
    val content = if (playing) scheme.onError else scheme.onErrorContainer
    Surface(onClick = onClick, modifier = modifier, shape = shape, color = container, contentColor = content) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            TrailSenseShapeBadge(
                icon = if (playing) TrailIcons.BaselineStop24 else signal.icon,
                shape = signal.shape.toShape(),
                size = 56.dp,
                containerColor = if (playing) scheme.onError else scheme.error,
                contentColor = if (playing) scheme.error else scheme.onError
            )
            Text(signal.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            SignalRhythm(signal = signal.signal, color = if (playing) scheme.onError else scheme.error, playing = playing)
            Text(
                stringResource(if (playing) R.string.whistle_playing else R.string.whistle_repeats_short),
                style = MaterialTheme.typography.labelLarge,
                color = content.copy(alpha = 0.8f)
            )
        }
    }
}

/** The pattern as blasts: each "on" is a rounded bar as long as it sounds, gaps as long as the silence (capped). */
@Composable
private fun SignalRhythm(signal: List<Signal>, color: androidx.compose.ui.graphics.Color, playing: Boolean) {
    val transition = rememberInfiniteTransition(label = "rhythm")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "pulse"
    )
    val alpha = if (playing) pulse else 1f
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val unit = 6.dp.toPx() / 400f
        val gapCap = 18.dp.toPx()
        var x = 0f
        for (s in signal) {
            val length = s.duration.toMillis() * unit
            if (s.isOn) {
                val w = length.coerceAtLeast(size.height)
                if (x + w > size.width) break
                drawRoundRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(x, 0f),
                    size = Size(w, size.height),
                    cornerRadius = CornerRadius(size.height / 2f)
                )
                x += w
            } else {
                x += length.coerceIn(4.dp.toPx(), gapCap)
            }
        }
    }
}

/**
 * The hero: a large saturated cookie that whistles while held. Primary at rest; while sounding it turns to the
 * error tone, spins slowly and sends shockwave rings out behind it, so the state reads from across a campsite.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HoldToWhistlePad(
    active: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val currentOnPress by rememberUpdatedState(onPress)
    val currentOnRelease by rememberUpdatedState(onRelease)
    val scheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (active) scheme.error else scheme.primary,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "whistleContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (active) scheme.onError else scheme.onPrimary,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "whistleContent"
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 0.92f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "whistleScale"
    )
    val transition = rememberInfiniteTransition(label = "whistle")
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "wave"
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )
    val label = stringResource(R.string.whistle_hold)
    val ring = scheme.error

    Box(
        modifier = Modifier
            .widthIn(max = 260.dp)
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            Canvas(Modifier.fillMaxSize()) {
                val base = size.minDimension * 0.36f
                val reach = size.minDimension * 0.5f
                repeat(3) { i ->
                    val t = (wave + i / 3f) % 1f
                    drawCircle(
                        color = ring.copy(alpha = (1f - t) * 0.45f),
                        radius = base + (reach - base) * t,
                        style = Stroke(width = (10f * (1f - t) + 2f).dp.toPx())
                    )
                }
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxSize(0.82f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = if (active) spin else 0f
                }
                .semantics {
                    contentDescription = label
                    role = Role.Button
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        currentOnPress()
                        waitForUpOrCancellation()
                        currentOnRelease()
                    }
                },
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = containerColor,
            contentColor = contentColor
        ) {}
        // Content sits outside the spinning surface so the icon and label stay upright.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                TrailIcons.ToolWhistle,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(TrailSenseSpacing.md))
            Text(
                text = if (active) stringResource(R.string.whistle_whistling) else label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
