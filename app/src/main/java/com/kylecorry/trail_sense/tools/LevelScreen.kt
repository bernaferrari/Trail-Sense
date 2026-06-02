package com.kylecorry.trail_sense.tools.level.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.sense.level.Level
import com.kylecorry.luna.time.Throttle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlin.math.abs

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(TrailSenseSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
    ) {
        Text(
            text = stringResource(
                R.string.bubble_level_angles,
                formatService.formatDegrees(abs(xAngle), 1),
                formatService.formatDegrees(abs(yAngle), 1)
            ),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .sizeIn(minHeight = 280.dp)
        ) {
            BubbleLevelCanvas(
                xAngle = xAngle,
                yAngle = yAngle,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun BubbleLevelCanvas(
    xAngle: Float,
    yAngle: Float,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainerHighest
    val outline = MaterialTheme.colorScheme.outline
    val center = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.42f
        val centerPoint = Offset(size.width / 2f, size.height / 2f)
        val maxAngle = 45f
        val bubbleOffset = Offset(
            (xAngle / maxAngle).coerceIn(-1f, 1f) * radius,
            (yAngle / maxAngle).coerceIn(-1f, 1f) * radius
        )
        val bubbleCenter = centerPoint + bubbleOffset

        drawCircle(surface, radius, centerPoint)
        drawCircle(outline, radius, centerPoint, style = Stroke(width = 4.dp.toPx()))
        drawLine(
            color = outline,
            start = Offset(centerPoint.x - radius, centerPoint.y),
            end = Offset(centerPoint.x + radius, centerPoint.y),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = outline,
            start = Offset(centerPoint.x, centerPoint.y - radius),
            end = Offset(centerPoint.x, centerPoint.y + radius),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(center, 10.dp.toPx(), centerPoint)
        drawCircle(primary, 30.dp.toPx(), bubbleCenter)
        drawCircle(
            color = primary.copy(alpha = 0.22f),
            radius = 48.dp.toPx(),
            center = bubbleCenter
        )
    }
}
