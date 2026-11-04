@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.cliffheight.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseReading
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSensePressScale
import com.kylecorry.trail_sense.tools.cliffheight.domain.CliffHeightService
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/**
 * Cliff height: the height is the hero with the fall time under it, one big start/reset button below, and the
 * how-to as a calm group at the bottom. The guide sits in the top bar.
 */
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
    var heightValue by remember { mutableStateOf("--") }
    var elapsed by remember { mutableStateOf("") }

    LaunchedEffect(running, startTime, units, location) {
        while (running) {
            title = formatHeight(service, formatter, startTime, units, location)
            heightValue = formatHeightValue(service, startTime, units, location)
            elapsed = formatElapsed(startTime)
            delay(16)
        }
    }

    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.tool_cliff_height_title),
        modifier = modifier,
        actions = {
            IconButton(onClick = onOpenGuide) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = stringResource(R.string.instructions)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = TrailSenseSpacing.xxxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
                ) {
                    TrailSenseReading(
                        value = heightValue,
                        unit = formatter.getDistanceUnitName(units, true),
                        label = stringResource(R.string.height),
                        color = if (running) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .testTag("cliff_height_title")
                            .semantics { contentDescription = title }
                    )
                    // Holds its line even before the first drop so the button doesn't move.
                    Text(
                        text = elapsed.ifEmpty { " " },
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

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

                TrailSenseRowGroup(title = stringResource(R.string.instructions)) {
                    row { position ->
                        TrailSenseGroupCard(position = position) {
                            Text(
                                text = stringResource(R.string.tool_cliff_height_disclaimer),
                                modifier = Modifier.testTag("tide_disclaimer"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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
    val containerColor by animateColorAsState(
        targetValue = if (running) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "dropButtonContainer"
    )
    val contentColor = if (running) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onPrimary
    }
    val height = ButtonDefaults.LargeContainerHeight

    Button(
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .trailSensePressScale(interactionSource, 0.97f)
            .pointerInput(running) {
                detectTapGestures(onPress = onPress)
            }
            .testTag("start_btn"),
        shapes = ButtonDefaults.shapesFor(height),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = ButtonDefaults.contentPaddingFor(height),
        interactionSource = interactionSource
    ) {
        Text(
            text = stringResource(
                if (running) R.string.reset else R.string.start
            ),
            style = MaterialTheme.typography.headlineSmall
        )
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

private fun formatHeightValue(
    service: CliffHeightService,
    startTime: Instant?,
    units: DistanceUnits,
    location: Coordinate?
): String {
    val start = startTime ?: return "--"
    val height = service.getCliffHeight(start, Instant.now(), location)
    return DecimalFormatter.format(height.convertTo(units).value, 2)
}

private fun formatElapsed(startTime: Instant?): String {
    val start = startTime ?: return ""
    val millis = Duration.between(start, Instant.now()).toMillis().coerceAtLeast(0)
    return String.format("%.1fs", millis / 1000f)
}
