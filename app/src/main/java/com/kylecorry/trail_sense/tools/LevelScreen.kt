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
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.sense.level.Level
import com.kylecorry.luna.time.Throttle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Pure Compose Level / Bubble Level screen.
 */
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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(TrailSenseSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl)
        ) {
            // Hero stage hosting the bubble visualization.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                LevelStage(
                    xAngle = xAngle,
                    yAngle = yAngle,
                    isLevel = isLevel
                )
            }

            // Emphasized numeric readouts.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                AngleReadout(
                    label = "X",
                    value = formatService.formatDegrees(abs(xAngle), 1),
                    isLevel = isLevel,
                    modifier = Modifier.weight(1f)
                )
                AngleReadout(
                    label = "Y",
                    value = formatService.formatDegrees(abs(yAngle), 1),
                    isLevel = isLevel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private const val LEVEL_TOLERANCE_DEGREES = 1.0

/**
 * Large tonal stage that frames the animated bubble level. Gives a subtle
 * scale + color lift when the device is in tolerance.
 */
@Composable
private fun LevelStage(
    xAngle: Float,
    yAngle: Float,
    isLevel: Boolean
) {
    val stageColor by animateColorAsState(
        targetValue = if (isLevel) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 250),
        label = "stageColor"
    )
    val stageScale by animateFloatAsState(
        targetValue = if (isLevel) 1.03f else 1f,
        animationSpec = TrailSenseAnimation.standardSpring,
        label = "stageScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(stageScale),
        shape = MaterialTheme.shapes.extraLarge,
        color = stageColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(TrailSenseSpacing.xl),
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

@Composable
private fun BubbleLevelCanvas(
    xAngle: Float,
    yAngle: Float,
    isLevel: Boolean,
    modifier: Modifier = Modifier
) {
    val ring = MaterialTheme.colorScheme.outlineVariant
    val crosshair = MaterialTheme.colorScheme.outline
    val target = MaterialTheme.colorScheme.primary
    val well = MaterialTheme.colorScheme.surfaceContainerLowest

    val activeBubble = if (isLevel) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val bubbleColor by animateColorAsState(
        targetValue = activeBubble,
        animationSpec = tween(durationMillis = 250),
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
        drawCircle(ring, radius, centerPoint, style = Stroke(width = 3.dp.toPx()))

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

@Composable
private fun AngleReadout(
    label: String,
    value: String,
    isLevel: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isLevel) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 250),
        label = "readoutContainer"
    )
    val contentColor = if (isLevel) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TrailSenseSpacing.lg,
                    vertical = TrailSenseSpacing.lg
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
