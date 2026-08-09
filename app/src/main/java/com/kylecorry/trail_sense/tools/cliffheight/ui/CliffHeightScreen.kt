@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.cliffheight.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.cliffheight.domain.CliffHeightService
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

@Composable
internal fun CliffHeightScreen(
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val service = remember { CliffHeightService() }
    val formatter = remember { FormatService.getInstance(context) }
    val prefs = remember { UserPreferences(context) }
    val gps = remember { SensorService(context).getGPS() }
    val units = remember { prefs.baseDistanceUnits }
    val location = remember { gps.location.takeIf { it != Coordinate.zero } }
    val singlePressDuration = remember { Duration.ofMillis(200) }
    var startTime by remember { mutableStateOf<Instant?>(null) }
    var running by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var elapsed by remember { mutableStateOf("") }

    LaunchedEffect(running, startTime, units, location) {
        while (running) {
            title = formatHeight(service, formatter, startTime, units, location)
            elapsed = formatElapsed(startTime)
            delay(16)
        }
    }

    val hasResult = title.isNotEmpty()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CliffHeightHeader(onOpenGuide = onOpenGuide)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(TrailSenseSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl)
            ) {
                HeroReadout(
                    height = title,
                    elapsed = elapsed,
                    running = running,
                    hasResult = hasResult
                )

                DropTimerButton(
                    running = running,
                    onPress = {
                        if (!running) {
                            startTime = Instant.now()
                            running = true
                            val released = tryAwaitRelease()
                            if (released &&
                                Duration.between(startTime, Instant.now()) > singlePressDuration
                            ) {
                                running = false
                            }
                        } else {
                            running = false
                        }
                    }
                )

                InstructionsCard()
            }
        }
    }
}

@Composable
private fun HeroReadout(
    height: String,
    elapsed: String,
    running: Boolean,
    hasResult: Boolean
) {
    val containerColor by animateColorAsState(
        targetValue = if (running || hasResult) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 250),
        label = "heroContainer"
    )
    val contentColor = if (running || hasResult) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val valueScale by animateFloatAsState(
        targetValue = if (running || hasResult) 1f else 0.96f,
        animationSpec = TrailSenseAnimation.standardSpring,
        label = "heroValueScale"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrailSenseSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
        ) {
            Text(
                text = stringResource(R.string.height).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Text(
                text = height.ifEmpty { "--" },
                modifier = Modifier
                    .scale(valueScale)
                    .testTag("cliff_height_title"),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center
            )
            if (elapsed.isNotEmpty()) {
                Text(
                    text = elapsed,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DropTimerButton(
    running: Boolean,
    onPress: suspend androidx.compose.foundation.gestures.PressGestureScope.(androidx.compose.ui.geometry.Offset) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed || running) 0.96f else 1f,
        animationSpec = TrailSenseAnimation.quickSpring,
        label = "dropButtonPressScale"
    )

    val containerColor by animateColorAsState(
        targetValue = if (running) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 250),
        label = "dropButtonContainer"
    )

    Button(
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .pointerInput(running) {
                detectTapGestures(onPress = onPress)
            }
            .testTag("start_btn"),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor
        ),
        contentPadding = ButtonDefaults.ContentPadding,
        interactionSource = interactionSource
    ) {
        Text(
            text = stringResource(
                if (running) R.string.reset else R.string.start
            ),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun InstructionsCard() {
    TrailSenseCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.instructions),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.tool_cliff_height_disclaimer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TrailSenseSpacing.sm)
                .testTag("tide_disclaimer"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CliffHeightHeader(
    onOpenGuide: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = TrailSenseSpacing.lg,
                top = TrailSenseSpacing.sm,
                end = TrailSenseSpacing.sm,
                bottom = TrailSenseSpacing.sm
            )
    ) {
        Text(
            text = stringResource(R.string.tool_cliff_height_title),
            modifier = Modifier.align(Alignment.CenterStart),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
            onClick = onOpenGuide,
            modifier = Modifier.align(Alignment.CenterEnd),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null
            )
        }
    }
}

private fun formatHeight(
    service: CliffHeightService,
    formatter: FormatService,
    startTime: Instant?,
    units: DistanceUnits,
    location: Coordinate?
): String {
    val start = startTime ?: return ""
    val height = service.getCliffHeight(start, Instant.now(), location)
    val converted = height.convertTo(units)
    return formatter.formatDistance(converted, 2)
}

private fun formatElapsed(startTime: Instant?): String {
    val start = startTime ?: return ""
    val millis = Duration.between(start, Instant.now()).toMillis().coerceAtLeast(0)
    return String.format("%.1fs", millis / 1000f)
}
