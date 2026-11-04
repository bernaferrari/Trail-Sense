@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.level.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.luna.time.Throttle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Bubble level: one round well that works the way the phone is held.
 * - Lying flat, it is a bubble level for the surface under the phone: the bubble rides to the high side and settles
 *   in the centre ring when the surface is level.
 * - Standing on an edge, it is a tube level for that edge: the bubble slides along a vial lying on the edge and
 *   sits between the two marks when the edge is level.
 * The tilt sits under the well with the verdict naming the high side, and one quiet line says which of the two it is
 * measuring. Once level, the well fills with primaryContainer and the phone gives one haptic tick.
 */
@Composable
fun LevelScreen(modifier: Modifier = Modifier) {
    // One question: is it level, and if not, which side is high?
    val context = LocalContext.current
    val view = LocalView.current
    val formatService = remember { FormatService.getInstance(context) }
    val threshold = remember { UserPreferences(context).bubbleLevel.threshold }
    val gravity = remember { SensorService(context).getGravity() }
    val throttle = remember { Throttle(20) }
    // The up vector in the phone's frame (what a gravity sensor reports): +z out of the screen, +x right, +y top.
    var gx by remember { mutableFloatStateOf(0f) }
    var gy by remember { mutableFloatStateOf(0f) }
    var gz by remember { mutableFloatStateOf(1f) }

    DisposableEffect(gravity, view) {
        view.keepScreenOn = true
        val listener = {
            if (!throttle.isThrottled()) {
                val g = gravity.acceleration
                gx = g.x
                gy = g.y
                gz = g.z
            }
            true
        }
        gravity.start(listener)
        onDispose {
            gravity.stop(listener)
            view.keepScreenOn = false
        }
    }

    val magnitude = sqrt(gx * gx + gy * gy + gz * gz).coerceAtLeast(0.001f)
    // How far the screen is from facing straight up: small when the phone lies flat, near 90° when it stands.
    val fromFlat = Math.toDegrees(acos((gz / magnitude).coerceIn(-1f, 1f).toDouble())).toFloat()
    // With a little hysteresis, so the well doesn't flicker between the two while the phone is held at 50°.
    var onEdge by remember { mutableStateOf(false) }
    if (!onEdge && fromFlat > EDGE_ENTER_DEGREES) onEdge = true
    if (onEdge && fromFlat < EDGE_EXIT_DEGREES) onEdge = false

    // Flat: the tilt along each axis, positive when the right side (x) or the top edge (y) is high.
    val xAngle = Math.toDegrees(atan2(gx, gz).toDouble()).toFloat()
    val yAngle = Math.toDegrees(atan2(gy, gz).toDouble()).toFloat()
    // On edge: which of the phone's four edges is down, and how far the phone is turned from standing square on it.
    val roll = Math.toDegrees(atan2(gx, gy).toDouble()).toFloat()
    val edge = ((roll / 90f).roundToInt() % 4 + 4) % 4
    val edgeError = roll - (roll / 90f).roundToInt() * 90f

    // On edge, the bubble rides along the edge toward its high end: the up direction on screen, projected on the edge.
    val axisRadians = Math.toRadians((roll - edgeError).toDouble())
    val planar = hypot(gx, gy).coerceAtLeast(0.001f)
    val along = ((gx * cos(axisRadians) - gy * sin(axisRadians)) / planar).toFloat().coerceIn(-1f, 1f)
    val edgeBubble = Math.toDegrees(asin(along).toDouble()).toFloat() / EDGE_FULL_SCALE

    val tilt = if (onEdge) abs(edgeError) else fromFlat
    val isLevel = tilt <= threshold
    val formattedX = formatService.formatDegrees(abs(if (onEdge) edgeError else xAngle), 1)
    val formattedY = formatService.formatDegrees(abs(if (onEdge) 0f else yAngle), 1)
    val formattedTilt = formatService.formatDegrees(tilt, 1)
    val levelDescription = stringResource(R.string.bubble_level_angles, formattedX, formattedY, formattedTilt)

    val verdict = stringResource(
        when {
            isLevel -> R.string.level_is_level
            onEdge -> edgeHighSide(edge, gx, gy)
            abs(xAngle) >= abs(yAngle) && xAngle > 0 -> R.string.level_right_high
            abs(xAngle) >= abs(yAngle) -> R.string.level_left_high
            yAngle > 0 -> R.string.level_top_high
            else -> R.string.level_bottom_high
        }
    )

    // The lock: one haptic tick each time the phone settles into level.
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
        largeTitle = true,
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
                    bottom = TrailSenseSpacing.xxl
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl, Alignment.CenterVertically)
        ) {
            LevelWell(
                isLevel = isLevel,
                onEdge = onEdge,
                // The bubble's place in the well, -1..1 on each axis (screen y runs down, so a high top is up).
                bubble = Offset(xAngle / FLAT_FULL_SCALE, -yAngle / FLAT_FULL_SCALE),
                edgeBubble = edgeBubble,
                axisDegrees = roll - edgeError,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .testTag("level")
                    .semantics(mergeDescendants = true) { contentDescription = levelDescription }
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
            ) {
                Text(
                    text = formattedTilt,
                    style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                val verdictColor by animateColorAsState(
                    if (isLevel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    MaterialTheme.motionScheme.defaultEffectsSpec(),
                    label = "verdict"
                )
                Text(
                    text = verdict,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = verdictColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(if (onEdge) R.string.level_hint_edge else R.string.level_hint_flat),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = TrailSenseSpacing.xs)
                )
            }
        }
    }
}

/** On edge, the side of the phone (as you look at it standing on [edge]) that is high. */
private fun edgeHighSide(edge: Int, gx: Float, gy: Float): Int {
    // The phone's own right (x) or top (y) points up a little when that side is high.
    val high = when (edge) {
        0 -> gx > 0 // standing on its bottom edge: its right is your right
        2 -> gx < 0 // upside down: its left is your right
        1 -> gy < 0 // on its left edge: its bottom is your right
        else -> gy > 0 // on its right edge: its top is your right
    }
    return if (high) R.string.level_right_high else R.string.level_left_high
}

/**
 * The one live object, a real spirit level. Flat, it is a round bull's-eye vial: a bubble of light floats toward the
 * high side, presses into an oval against the glass when it is far out, and rounds into a circle inside the centre
 * ring when level. On edge, a tube vial lies along the phone's edge and the bubble slides along it between two
 * marks. The bubble moves on a bouncy spring, so it overshoots and settles like air in liquid. Level turns the
 * liquid primaryContainer and the target tertiary.
 */
@Composable
private fun LevelWell(
    isLevel: Boolean,
    onEdge: Boolean,
    bubble: Offset,
    edgeBubble: Float,
    axisDegrees: Float,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val liquid by animateColorAsState(
        if (isLevel) scheme.primaryContainer else scheme.surfaceContainerHigh,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "liquid"
    )
    val target by animateColorAsState(
        if (isLevel) scheme.tertiary else scheme.outline,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "target"
    )
    val glass = scheme.outlineVariant
    val air = scheme.primary
    val shine = scheme.onPrimary
    val wobble = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    val bubbleX by animateFloatAsState(bubble.x, wobble, label = "bubbleX")
    val bubbleY by animateFloatAsState(bubble.y, wobble, label = "bubbleY")
    val tube by animateFloatAsState(edgeBubble, wobble, label = "tube")
    val edgeShown by animateFloatAsState(
        if (onEdge) 1f else 0f,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "mode"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            val thin = 2.dp.toPx()

            // Flat: the bull's-eye vial.
            val flatAlpha = 1f - edgeShown
            if (flatAlpha > 0f) {
                drawCircle(liquid.copy(alpha = flatAlpha), radius, center)
                drawCircle(glass.copy(alpha = flatAlpha), radius - thin, center, style = Stroke(thin))
                val bubbleRadius = radius * 0.2f
                val ringRadius = bubbleRadius + 10.dp.toPx()
                drawCircle(target.copy(alpha = flatAlpha), ringRadius, center, style = Stroke(3.dp.toPx()))
                drawCircle(glass.copy(alpha = flatAlpha * 0.6f), radius * 0.62f, center, style = Stroke(thin))

                // The bubble's travel ends at the glass; its direction is kept when it gets there.
                var offset = Offset(bubbleX, bubbleY)
                val length = hypot(offset.x, offset.y)
                if (length > 1f) offset /= length
                val reach = min(length, 1f)
                val travel = radius - bubbleRadius - 8.dp.toPx()
                val at = center + offset * travel
                // Far out it presses against the glass into an oval; centred it is a perfect circle.
                val along = bubbleRadius * (1f + 0.18f * reach)
                val across = bubbleRadius * (1f - 0.14f * reach)
                val angle = if (length > 0.001f) Math.toDegrees(atan2(offset.y, offset.x).toDouble()).toFloat() else 0f
                rotate(angle, at) {
                    drawOval(
                        air.copy(alpha = flatAlpha),
                        topLeft = Offset(at.x - along, at.y - across),
                        size = Size(along * 2f, across * 2f)
                    )
                }
                drawCircle(
                    shine.copy(alpha = 0.35f * flatAlpha),
                    bubbleRadius * 0.28f,
                    at + Offset(-bubbleRadius * 0.38f, -bubbleRadius * 0.38f)
                )
            }

            // On edge: a tube vial along the edge the phone stands on.
            if (edgeShown > 0f) {
                rotate(axisDegrees, center) {
                    val length = radius * 1.9f
                    val height = radius * 0.42f
                    val left = center.x - length / 2f
                    val top = center.y - height / 2f
                    drawRoundRect(
                        liquid.copy(alpha = edgeShown),
                        topLeft = Offset(left, top),
                        size = Size(length, height),
                        cornerRadius = CornerRadius(height / 2f)
                    )
                    drawRoundRect(
                        glass.copy(alpha = edgeShown),
                        topLeft = Offset(left + thin / 2f, top + thin / 2f),
                        size = Size(length - thin, height - thin),
                        cornerRadius = CornerRadius(height / 2f),
                        style = Stroke(thin)
                    )
                    val bubbleLength = height * 1.5f
                    val bubbleHeight = height - 14.dp.toPx()
                    // The two marks the bubble sits between when the edge is level.
                    val gap = bubbleLength / 2f + 6.dp.toPx()
                    listOf(-gap, gap).forEach { dx ->
                        drawLine(
                            target.copy(alpha = edgeShown),
                            Offset(center.x + dx, top + 4.dp.toPx()),
                            Offset(center.x + dx, top + height - 4.dp.toPx()),
                            3.dp.toPx(),
                            StrokeCap.Round
                        )
                    }
                    val travel = (length - bubbleLength) / 2f - 7.dp.toPx()
                    val bx = center.x + tube.coerceIn(-1f, 1f) * travel
                    drawRoundRect(
                        air.copy(alpha = edgeShown),
                        topLeft = Offset(bx - bubbleLength / 2f, center.y - bubbleHeight / 2f),
                        size = Size(bubbleLength, bubbleHeight),
                        cornerRadius = CornerRadius(bubbleHeight / 2f)
                    )
                    drawRoundRect(
                        shine.copy(alpha = 0.35f * edgeShown),
                        topLeft = Offset(bx - bubbleLength * 0.3f, center.y - bubbleHeight * 0.32f),
                        size = Size(bubbleLength * 0.4f, bubbleHeight * 0.16f),
                        cornerRadius = CornerRadius(bubbleHeight * 0.08f)
                    )
                }
            }
        }
    }
}

/** A bubble reaches the rim at this tilt, so the last few degrees still move it plainly. */
private const val FLAT_FULL_SCALE = 15f
private const val EDGE_FULL_SCALE = 10f
private const val EDGE_ENTER_DEGREES = 55f
private const val EDGE_EXIT_DEGREES = 45f
