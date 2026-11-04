package com.kylecorry.trail_sense.tools.level.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.sense.level.Level
import com.kylecorry.luna.time.Throttle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseReading
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Pure Compose Level / Bubble Level screen.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LevelScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    val formatService = remember { FormatService.getInstance(context) }
    val level = remember { Level(SensorService(context).getOrientation()) }
    val throttle = remember { Throttle(20) }
    var xAngle by remember { mutableFloatStateOf(0f) }
    var yAngle by remember { mutableFloatStateOf(0f) }

    DisposableEffect(level, view) {
        view.keepScreenOn = true
        val listener = {
            if (!throttle.isThrottled()) {
                xAngle = level.x
                yAngle = -level.y
            }
            true
        }
        level.start(listener)
        onDispose {
            level.stop(listener)
            view.keepScreenOn = false
        }
    }

    // "In tolerance" / level state: both axes within a tight band.
    val isLevel = hypot(xAngle.toDouble(), yAngle.toDouble()) <= LEVEL_TOLERANCE_DEGREES
    val formattedX = formatService.formatDegrees(abs(xAngle), 1)
    val formattedY = formatService.formatDegrees(abs(yAngle), 1)
    val levelDescription = stringResource(
        R.string.bubble_level_angles,
        formattedX,
        formattedY,
        // The string gained the overall tilt upstream; passing two arguments crashed the tool on open.
        formatService.formatDegrees(hypot(xAngle.toDouble(), yAngle.toDouble()).toFloat(), 1)
    )

    // The bubble is the instrument; the two angles under it are the hero reading, in the accent tone once level.
    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.tool_bubble_level_title),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = TrailSenseSize.maximumContentWidth)
                .align(Alignment.TopCenter)
                .padding(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    top = TrailSenseSpacing.sm,
                    bottom = TrailSenseSpacing.xl
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = levelDescription
                    },
                contentAlignment = Alignment.Center
            ) {
                LevelStage(
                    xAngle = xAngle,
                    yAngle = yAngle,
                    isLevel = isLevel
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AngleReadout(label = "X", value = formattedX, isLevel = isLevel)
                AngleReadout(label = "Y", value = formattedY, isLevel = isLevel)
            }
        }
    }
}

private const val LEVEL_TOLERANCE_DEGREES = 1.0

/**
 * Large tonal stage that frames the animated bubble level. Gives a subtle
 * scale + color lift when the device is in tolerance.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LevelStage(
    xAngle: Float,
    yAngle: Float,
    isLevel: Boolean
) {
    // A calm circular well; it takes the tertiary tone only while the phone is level.
    val stageColor by animateColorAsState(
        targetValue = if (isLevel) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "stageColor"
    )

    Surface(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = CircleShape,
        color = stageColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(TrailSenseSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            BubbleLevelCanvas(
                xAngle = xAngle,
                yAngle = yAngle,
                isLevel = isLevel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BubbleLevelCanvas(
    xAngle: Float,
    yAngle: Float,
    isLevel: Boolean,
    modifier: Modifier = Modifier
) {
    val crosshair = MaterialTheme.colorScheme.outlineVariant
    val target = MaterialTheme.colorScheme.primary
    val well = MaterialTheme.colorScheme.surfaceContainerLowest

    val activeBubble = if (isLevel) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val bubbleColor by animateColorAsState(
        targetValue = activeBubble,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "bubbleColor"
    )

    // Smoothly chase the sensor angles so the bubble glides rather than jumps.
    val animatedX by animateFloatAsState(
        targetValue = xAngle,
        animationSpec = TrailSenseAnimation.standardSpring,
        label = "bubbleX"
    )
    val animatedY by animateFloatAsState(
        targetValue = yAngle,
        animationSpec = TrailSenseAnimation.standardSpring,
        label = "bubbleY"
    )

    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.46f
        val centerPoint = Offset(size.width / 2f, size.height / 2f)
        val maxAngle = 45f
        val bubbleOffset = Offset(
            (animatedX / maxAngle).coerceIn(-1f, 1f) * radius,
            (animatedY / maxAngle).coerceIn(-1f, 1f) * radius
        )
        val bubbleCenter = centerPoint + bubbleOffset

        // Outer well.
        drawCircle(well, radius, centerPoint)

        // Inner target ring marking the "level" zone.
        val targetRadius = radius * 0.18f
        drawCircle(
            color = target.copy(alpha = if (isLevel) 0.9f else 0.35f),
            radius = targetRadius,
            center = centerPoint,
            style = Stroke(width = 2.dp.toPx())
        )

        // Crosshair.
        drawLine(
            color = crosshair,
            start = Offset(centerPoint.x - radius, centerPoint.y),
            end = Offset(centerPoint.x + radius, centerPoint.y),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = crosshair,
            start = Offset(centerPoint.x, centerPoint.y - radius),
            end = Offset(centerPoint.x, centerPoint.y + radius),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Soft halo + crisp bubble.
        drawCircle(
            color = bubbleColor.copy(alpha = 0.18f),
            radius = 52.dp.toPx(),
            center = bubbleCenter
        )
        drawCircle(bubbleColor, 32.dp.toPx(), bubbleCenter)
        drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = 32.dp.toPx(),
            center = bubbleCenter,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AngleReadout(
    label: String,
    value: String,
    isLevel: Boolean,
    modifier: Modifier = Modifier
) {
    val valueColor by animateColorAsState(
        targetValue = if (isLevel) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "readoutColor"
    )
    TrailSenseReading(
        value = value,
        label = label,
        color = valueColor,
        style = MaterialTheme.typography.displayMedium,
        modifier = modifier
    )
}
