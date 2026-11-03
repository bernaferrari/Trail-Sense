package com.kylecorry.trail_sense.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.sound.ISoundPlayer
import kotlinx.coroutines.withContext
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.morse.SignalPlayer
import com.kylecorry.trail_sense.shared.morse.Signals
import com.kylecorry.trail_sense.shared.morse.asSignal
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.tools.whistle.infrastructure.Whistle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * Beautiful pure Compose Whistle screen.
 * Replaces the old BoundFragment + XML implementation with modern Material 3 expressive UI.
 */
@Composable
fun WhistleScreen(
    modifier: Modifier = Modifier,
    onAudioUnavailable: () -> Unit = {}
) {
    val context = LocalContext.current

    var isPlaying by remember { mutableStateOf(false) }
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

    // Emergency signals
    val emergencySignal = remember { Signals.help() }
    val sosSignal = remember {
        Signals.sos(Duration.ofMillis(400)) + listOf(
            com.kylecorry.trail_sense.shared.morse.Signal.off(Duration.ofMillis(400 * 7))
        )
    }

    fun stopAll() {
        signalPlayer?.cancel()
        whistlePlayer?.off()
        isPlaying = false
        currentSignal = null
    }

    fun playContinuous() {
        stopAll()
        whistlePlayer?.on()
        isPlaying = true
        currentSignal = "Whistle"
    }

    fun stopContinuous() {
        whistlePlayer?.off()
        isPlaying = false
        currentSignal = null
    }

    fun playSignal(name: String, signal: List<com.kylecorry.trail_sense.shared.morse.Signal>, loop: Boolean) {
        stopAll()
        signalPlayer?.play(signal, loop)
        isPlaying = true
        currentSignal = name
    }

    val whistleActive = isPlaying && currentSignal == "Whistle"
    val anyActive = isPlaying

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = TrailSenseSpacing.screen,
                        vertical = TrailSenseSpacing.xl
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
            Text(
                text = stringResource(R.string.tool_whistle_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(TrailSenseSpacing.sm))

            // Dominant press-and-hold control with a clear tonal active state.
            HoldToBlowButton(
                active = whistleActive,
                onPress = { playContinuous() },
                onRelease = { stopContinuous() }
            )

            Spacer(Modifier.height(TrailSenseSpacing.md))

            // Emergency signal section
            Text(
                text = "Signals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
            ) {
                SignalButton(
                    label = stringResource(R.string.help),
                    selected = currentSignal == context.getString(R.string.help),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        playSignal(
                            context.getString(R.string.help),
                            emergencySignal,
                            loop = true
                        )
                    }
                )

                SignalButton(
                    label = "SOS",
                    selected = currentSignal == "SOS",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        playSignal("SOS", sosSignal, loop = true)
                    }
                )
            }

            // More signals
            var showSignalPicker by remember { mutableStateOf(false) }

            FilledTonalButton(
                onClick = { showSignalPicker = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = "More signals…",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            if (showSignalPicker) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showSignalPicker = false },
                    shape = MaterialTheme.shapes.extraLarge,
                    title = {
                        Text(
                            text = "Whistle Signals",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
                        ) {
                            listOf(
                                "Where are you?" to Signals.comeHere(),
                                "Acknowledged" to Signals.acknowledged(),
                                "Come here" to Signals.comeHere(),
                                "Help (loop)" to Signals.help()
                            ).forEach { (label, signal) ->
                                FilledTonalButton(
                                    onClick = {
                                        playSignal(label, signal, loop = label.contains("loop", ignoreCase = true))
                                        showSignalPicker = false
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSignalPicker = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Active "now playing" banner — bold tonal state.
            AnimatedVisibility(
                visible = anyActive && !whistleActive && currentSignal != null,
                enter = fadeIn(TrailSenseAnimation.mediumFade),
                exit = fadeOut(TrailSenseAnimation.fastFade)
            ) {
                NowPlayingBanner(signal = currentSignal ?: "")
            }

            Spacer(Modifier.height(TrailSenseSpacing.sm))

            Text(
                text = "Tip: Hold the big button for continuous whistle. Tap signals for attention-getting patterns.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = TrailSenseSpacing.md)
            )
            }
        }
    }
}

/**
 * Large central press-and-hold control. Calm tonal surface when idle, bold error
 * tones with a static halo while emitting. Pointer handling preserves the original
 * hold-to-blow behavior exactly.
 */
@Composable
private fun HoldToBlowButton(
    active: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (active)
            MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primaryContainer,
        animationSpec = tween(TrailSenseMotion_medium),
        label = "whistleContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (active)
            MaterialTheme.colorScheme.onError
        else MaterialTheme.colorScheme.onPrimaryContainer,
        animationSpec = tween(TrailSenseMotion_medium),
        label = "whistleContent"
    )

    // Press feedback: a small satisfying spring-driven dip on the whole control.
    val pressScale by animateFloatAsState(
        targetValue = if (active) 0.97f else 1f,
        animationSpec = TrailSenseAnimation.standardSpring,
        label = "whistlePressScale"
    )

    val haloAlpha by animateFloatAsState(
        targetValue = if (active) 0.28f else 0f,
        animationSpec = TrailSenseAnimation.mediumFade,
        label = "whistleHaloAlpha"
    )

    Box(
        modifier = Modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        // Static tonal halo communicates activity without continuous motion.
        if (active) {
            Surface(
                modifier = Modifier
                    .size(240.dp)
                    .alpha(haloAlpha),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error
            ) {}
        }

        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(pressScale)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.pressed }) {
                                onPress()
                            } else {
                                onRelease()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { /* handled by pointer */ },
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (active) 10.dp else 4.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = contentColor
                    )
                    Spacer(Modifier.height(TrailSenseSpacing.md))
                    Text(
                        text = if (active) "BLOWING" else "HOLD TO BLOW",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SignalButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = tween(TrailSenseMotion_medium),
        label = "signalContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSecondaryContainer,
        animationSpec = tween(TrailSenseMotion_medium),
        label = "signalContent"
    )

    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (selected) 4.dp else 1.dp
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun NowPlayingBanner(signal: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(TrailSenseSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Playing: $signal",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

// Local copy of the shared medium motion duration to avoid an extra cross-module
// import while keeping animation timings consistent with the design tokens.
private const val TrailSenseMotion_medium = 250
