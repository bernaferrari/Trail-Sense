@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.level.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.kylecorry.andromeda.sense.level.Level
import com.kylecorry.luna.time.Throttle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Bubble level: one live object, a scalloped bezel holding a bubble that stretches toward the tilt, with the tilt
 * and the verdict right under it and X and Y as one quiet list. Once the phone is level the bezel smooths into a
 * true circle, turns green and gives one haptic tick.
 */
@Composable
fun LevelScreen(modifier: Modifier = Modifier) {
    // One question: is it level, and if not, which side is high?
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

    // The lock: one haptic tick each time the bubble settles into the level zone.
    val haptics = LocalHapticFeedback.current
    var firstLevelCheck by remember { mutableStateOf(true) }
    LaunchedEffect(isLevel) {
        if (isLevel && !firstLevelCheck) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        }
        firstLevelCheck = false
    }

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
            // The dial and its readings travel together at the optical centre instead of drifting apart.
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl, Alignment.CenterVertically)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .testTag("level")
                    .semantics(mergeDescendants = true) { contentDescription = levelDescription },
                contentAlignment = Alignment.Center
            ) {
                LevelBezel(xAngle = xAngle, yAngle = yAngle, isLevel = isLevel)
            }
            // The reading and its verdict, then the two axes as one quiet list.
            val statusColors = trailSenseStatusColors()
            val starColor by animateColorAsState(
                if (isLevel) statusColors.good else MaterialTheme.colorScheme.onSurface,
                MaterialTheme.motionScheme.defaultEffectsSpec(),
                label = "star"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formattedTilt,
                    style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    // The bubble rides to the high side, so the verdict names that side.
                    text = stringResource(
                        when {
                            isLevel -> R.string.level_is_level
                            abs(xAngle) >= abs(yAngle) && xAngle > 0 -> R.string.level_right_high
                            abs(xAngle) >= abs(yAngle) -> R.string.level_left_high
                            yAngle > 0 -> R.string.level_bottom_high
                            else -> R.string.level_top_high
                        }
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = starColor
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                AxisRow("X", formattedX, TrailSenseGroupPosition(isFirst = true, isLast = false))
                AxisRow("Y", formattedY, TrailSenseGroupPosition(isFirst = false, isLast = true))
            }
        }
    }
}

@Composable
private fun AxisRow(label: String, value: String, position: TrailSenseGroupPosition) {
    TrailSenseListItem(
        title = label,
        position = position,
        trailing = {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold
            )
        }
    )
}

private const val LEVEL_TOLERANCE_DEGREES = 1.0f
private const val MAX_ANGLE = 45f

/** A shape that follows a [Morph] between two of Material's polygons. */
private class LevelMorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        val path = morph.toPath(progress)
        val matrix = android.graphics.Matrix().apply { setScale(size.width, size.height) }
        path.transform(matrix)
        return Outline.Generic(path.asComposePath())
    }
}

/**
 * The bezel: a twelve-sided cookie in the primary tone that smooths into a circle and turns good once level,
 * around a deep round face.
 */
@Composable
private fun LevelBezel(xAngle: Float, yAngle: Float, isLevel: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val statusColors = trailSenseStatusColors()
    val bezel by animateColorAsState(
        if (isLevel) statusColors.good else scheme.primaryContainer,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "bezel"
    )
    val smooth by animateFloatAsState(
        targetValue = if (isLevel) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "bezelSmooth"
    )
    val morph = remember { Morph(MaterialShapes.Cookie12Sided, MaterialShapes.Circle) }
    val shape = remember(smooth) { LevelMorphShape(morph, smooth) }
    Box(
        modifier = Modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            shape = shape,
            color = bezel
        ) {}
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(26.dp),
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
        if (isLevel) status.goodContainer else scheme.primaryContainer.copy(alpha = 0.7f),
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "target"
    )
    val tick = scheme.onSurfaceVariant
    val ring = scheme.outline.copy(alpha = 0.6f)
    val animatedX by animateFloatAsState(xAngle, TrailSenseAnimation.standardSpring, label = "bubbleX")
    val animatedY by animateFloatAsState(yAngle, TrailSenseAnimation.standardSpring, label = "bubbleY")

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        val bubbleRadius = radius * 0.22f
        val travel = radius - bubbleRadius - 10.dp.toPx()
        val dx = (animatedX / MAX_ANGLE).coerceIn(-1f, 1f)
        val dy = (animatedY / MAX_ANGLE).coerceIn(-1f, 1f)
        val bubbleCenter = center + Offset(dx * travel, dy * travel)

        // The level zone: a soft filled target the bubble settles into.
        drawCircle(targetColor, bubbleRadius + 10.dp.toPx(), center)
        drawCircle(
            ring,
            bubbleRadius + 10.dp.toPx(),
            center,
            style = Stroke(2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
        )
        // Rings every 15°.
        listOf(1f / 3f, 2f / 3f).forEach {
            drawCircle(ring, travel * it, center, style = Stroke(1.5.dp.toPx()))
        }
        // Major ticks at the four points of the rim, minor ones between.
        val stroke = 3.dp.toPx()
        for (i in 0 until 24) {
            val angle = i * 15f
            val major = i % 6 == 0
            val length = if (major) 16.dp.toPx() else 7.dp.toPx()
            rotate(angle, center) {
                drawLine(
                    if (major) tick else tick.copy(alpha = 0.45f),
                    Offset(center.x, center.y - radius + 8.dp.toPx()),
                    Offset(center.x, center.y - radius + 8.dp.toPx() + length),
                    if (major) stroke else 2.dp.toPx(),
                    StrokeCap.Round
                )
            }
        }
        val cross = 7.dp.toPx()
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
    }
}
