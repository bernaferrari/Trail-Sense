@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.level.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.sense.level.Level
import com.kylecorry.luna.time.Throttle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStatGrid
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Bubble level: a cookie-shaped bezel holding a liquid bubble that stretches toward the tilt, turning green once the
 * phone is level, with the two axes and the overall tilt as tiles under it.
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

    val tilt = hypot(xAngle.toDouble(), yAngle.toDouble()).toFloat()
    val isLevel = tilt <= LEVEL_TOLERANCE_DEGREES
    val formattedX = formatService.formatDegrees(abs(xAngle), 1)
    val formattedY = formatService.formatDegrees(abs(yAngle), 1)
    val formattedTilt = formatService.formatDegrees(tilt, 1)
    val levelDescription = stringResource(R.string.bubble_level_angles, formattedX, formattedY, formattedTilt)

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
            LevelStatus(isLevel = isLevel, tilt = formattedTilt)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("level")
                    .semantics(mergeDescendants = true) { contentDescription = levelDescription },
                contentAlignment = Alignment.Center
            ) {
                LevelBezel(xAngle = xAngle, yAngle = yAngle, isLevel = isLevel)
            }
            val tone = if (isLevel) TrailSenseTone.Good else TrailSenseTone.Neutral
            TrailSenseStatGrid(
                stats = listOf(
                    TrailSenseStat(label = "X", value = formattedX, tone = tone),
                    TrailSenseStat(label = "Y", value = formattedY, tone = tone),
                    TrailSenseStat(
                        label = stringResource(R.string.tilt),
                        value = formattedTilt,
                        tone = if (isLevel) TrailSenseTone.Good else TrailSenseTone.Primary
                    )
                ),
                columns = 3,
                valueStyle = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

private const val LEVEL_TOLERANCE_DEGREES = 1.0f
private const val MAX_ANGLE = 45f

@Composable
private fun LevelStatus(isLevel: Boolean, tilt: String) {
    val status = trailSenseStatusColors()
    val color by animateColorAsState(
        if (isLevel) status.good else MaterialTheme.colorScheme.onSurfaceVariant,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "status"
    )
    Text(
        text = if (isLevel) stringResource(R.string.level_is_level) else stringResource(R.string.level_off_by, tilt),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}

/** The bezel: a twelve-sided cookie in the primary tone (good once level) around a deep round face. */
@Composable
private fun LevelBezel(xAngle: Float, yAngle: Float, isLevel: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val status = trailSenseStatusColors()
    val bezel by animateColorAsState(
        if (isLevel) status.goodContainer else scheme.primaryContainer,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "bezel"
    )
    // The bezel turns a little with the tilt direction, like liquid catching in the ridges.
    val turn by animateFloatAsState(
        targetValue = (xAngle - yAngle) * 0.6f,
        animationSpec = TrailSenseAnimation.standardSpring,
        label = "bezelTurn"
    )
    val scale by animateFloatAsState(
        if (isLevel) 1f else 0.97f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "bezelScale"
    )
    Box(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = turn },
            shape = MaterialShapes.Cookie12Sided.toShape(),
            color = bezel
        ) {}
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            shape = CircleShape,
            color = scheme.surfaceContainerHigh
        ) {
            BubbleFace(xAngle = xAngle, yAngle = yAngle, isLevel = isLevel, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun BubbleFace(xAngle: Float, yAngle: Float, isLevel: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val status = trailSenseStatusColors()
    val bubbleColor by animateColorAsState(
        if (isLevel) status.good else scheme.primary,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "bubble"
    )
    val targetColor by animateColorAsState(
        if (isLevel) status.goodContainer else scheme.primary.copy(alpha = 0.10f),
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "target"
    )
    val tick = scheme.outline.copy(alpha = 0.55f)
    val ring = scheme.outlineVariant.copy(alpha = 0.5f)
    val animatedX by animateFloatAsState(xAngle, TrailSenseAnimation.standardSpring, label = "bubbleX")
    val animatedY by animateFloatAsState(yAngle, TrailSenseAnimation.standardSpring, label = "bubbleY")

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        val bubbleRadius = radius * 0.17f
        val travel = radius - bubbleRadius - 10.dp.toPx()
        val dx = (animatedX / MAX_ANGLE).coerceIn(-1f, 1f)
        val dy = (animatedY / MAX_ANGLE).coerceIn(-1f, 1f)
        val bubbleCenter = center + Offset(dx * travel, dy * travel)

        // The level zone: a soft filled target the bubble settles into.
        drawCircle(targetColor, bubbleRadius + 8.dp.toPx(), center)
        // Quiet rings every 15°.
        listOf(1f / 3f, 2f / 3f).forEach {
            drawCircle(ring, travel * it, center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        }
        // Four ticks at the rim and a small cross at the centre, instead of a full crosshair.
        val tickLength = 14.dp.toPx()
        val stroke = 3.dp.toPx()
        listOf(0f, 90f, 180f, 270f).forEach { angle ->
            rotate(angle, center) {
                drawLine(
                    tick,
                    Offset(center.x, center.y - radius + 6.dp.toPx()),
                    Offset(center.x, center.y - radius + 6.dp.toPx() + tickLength),
                    stroke,
                    StrokeCap.Round
                )
            }
        }
        val cross = 6.dp.toPx()
        drawLine(tick, center - Offset(cross, 0f), center + Offset(cross, 0f), 2.dp.toPx(), StrokeCap.Round)
        drawLine(tick, center - Offset(0f, cross), center + Offset(0f, cross), 2.dp.toPx(), StrokeCap.Round)

        // A liquid bubble: it stretches along the tilt and narrows across it, the further it is off level.
        val pull = hypot(dx, dy).coerceIn(0f, 1f)
        val stretch = 1f + pull * 0.35f
        val squash = 1f - pull * 0.15f
        val direction = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        rotate(direction, bubbleCenter) {
            val w = bubbleRadius * 2 * stretch
            val h = bubbleRadius * 2 * squash
            drawOval(
                bubbleColor.copy(alpha = 0.22f),
                topLeft = bubbleCenter - Offset(w / 2f + 8.dp.toPx(), h / 2f + 8.dp.toPx()),
                size = Size(w + 16.dp.toPx(), h + 16.dp.toPx())
            )
            drawOval(bubbleColor, topLeft = bubbleCenter - Offset(w / 2f, h / 2f), size = Size(w, h))
        }
        // A highlight so it reads as a drop of liquid, not a dot.
        drawCircle(
            Color.White.copy(alpha = 0.35f),
            bubbleRadius * 0.28f,
            bubbleCenter + Offset(-bubbleRadius * 0.35f, -bubbleRadius * 0.35f)
        )
    }
}
