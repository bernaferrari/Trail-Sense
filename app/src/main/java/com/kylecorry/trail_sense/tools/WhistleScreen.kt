package com.kylecorry.trail_sense.tools

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.sound.ISoundPlayer
import kotlinx.coroutines.withContext
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.morse.SignalPlayer
import com.kylecorry.trail_sense.shared.morse.Signals
import com.kylecorry.trail_sense.shared.morse.asSignal
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
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
fun WhistleScreen(modifier: Modifier = Modifier) {
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
                // Audio unavailable — handled gracefully in UI
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(TrailSenseSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.tool_whistle_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = TrailSenseSpacing.md)
        )

        // Big beautiful hold-to-blow button — M3 Expressive treatment
        val buttonScale by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isPlaying && currentSignal == "Whistle") 0.96f else 1f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            ),
            label = "whistleButtonScale"
        )

        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(buttonScale)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.pressed }) {
                                playContinuous()
                            } else {
                                stopContinuous()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { /* handled by pointer */ },
                modifier = Modifier.fillMaxSize(),
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPlaying && currentSignal == "Whistle")
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (isPlaying && currentSignal == "Whistle") 8.dp else 4.dp
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = if (isPlaying && currentSignal == "Whistle")
                            Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (isPlaying && currentSignal == "Whistle") "BLOWING..." else "HOLD TO BLOW",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(TrailSenseSpacing.xl))

        // Signal buttons
        Text(
            "Signals",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = TrailSenseSpacing.sm)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            FilledTonalButton(
                onClick = {
                    playSignal(
                        context.getString(R.string.help),
                        emergencySignal,
                        loop = true
                    )
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (currentSignal == context.getString(R.string.help))
                        MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(stringResource(R.string.help))
            }

            FilledTonalButton(
                onClick = {
                    playSignal("SOS", sosSignal, loop = true)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("SOS")
            }
        }

        Spacer(Modifier.height(TrailSenseSpacing.md))

        // More signals card - M3 expressive
        var showSignalPicker by remember { mutableStateOf(false) }

        TrailSenseCard(
            onClick = { showSignalPicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "More signals…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (showSignalPicker) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showSignalPicker = false },
                title = { Text("Whistle Signals") },
                text = {
                    Column {
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
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
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

        Spacer(Modifier.height(TrailSenseSpacing.lg))

        if (currentSignal != null) {
            TrailSenseCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Playing: $currentSignal",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            "Tip: Hold the big button for continuous whistle. Tap signals for attention-getting patterns.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
