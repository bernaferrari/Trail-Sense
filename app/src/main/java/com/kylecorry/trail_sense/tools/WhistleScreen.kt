package com.kylecorry.trail_sense.tools

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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import java.time.Duration

private const val WHISTLE = "whistle"

private data class WhistleSignal(
    val key: String,
    val label: String,
    val signal: List<Signal>,
    val loop: Boolean
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
            WhistleSignal("help", context.getString(R.string.help), Signals.help(), loop = true),
            WhistleSignal(
                "sos",
                context.getString(R.string.sos),
                Signals.sos(Duration.ofMillis(400)) + listOf(Signal.off(Duration.ofMillis(400 * 7))),
                loop = true
            ),
            WhistleSignal(
                "where_are_you",
                context.getString(R.string.whistle_signal_where_are_you),
                Signals.comeHere(),
                loop = false
            ),
            WhistleSignal(
                "acknowledged",
                context.getString(R.string.whistle_signal_acknowledged),
                Signals.acknowledged(),
                loop = false
            ),
            WhistleSignal(
                "come_here",
                context.getString(R.string.whistle_signal_come_here),
                Signals.comeHere(),
                loop = false
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

            TrailSenseRowGroup(title = stringResource(R.string.whistle_signals)) {
                signals.forEach { signal ->
                    row { position ->
                        val playing = currentSignal == signal.key
                        TrailSenseListItem(
                            title = signal.label,
                            subtitle = if (playing) {
                                stringResource(R.string.whistle_playing)
                            } else if (signal.loop) {
                                stringResource(R.string.whistle_signal_repeats)
                            } else {
                                null
                            },
                            icon = rememberVectorPainter(
                                if (playing) TrailIcons.Pause else TrailIcons.WhistleSos
                            ),
                            iconTint = if (playing) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            containerColor = if (playing) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            onClick = { toggleSignal(signal) },
                            position = position
                        )
                    }
                }
            }
        }
    }
}

/**
 * The hero: a large cookie-shaped pad that whistles while held. Calm primary container at rest; while sounding it
 * turns to the error tone and dips slightly, so the state reads from across a campsite.
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
    val containerColor by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "whistleContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "whistleContent"
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 0.96f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "whistleScale"
    )
    val label = stringResource(R.string.whistle_hold)

    Surface(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
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
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                TrailIcons.ToolWhistle,
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(TrailSenseSpacing.md))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
