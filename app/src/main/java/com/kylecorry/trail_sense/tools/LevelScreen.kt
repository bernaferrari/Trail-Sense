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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
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
 * The one live object, a real spirit level: a liquid-filled glass vial set in a tonal housing, with the level marks
 * engraved into the glass and a bubble of air floating in the liquid. The bubble is see-through: the liquid shows
 * through it, its edge darkens where the light bends (the refraction rim), a soft highlight sits on its upper side and
 * a faint caustic glows on the lower one.
 * - Flat, it is a round bull's-eye vial: the bubble floats toward the high side, presses into an oval against the
 *   glass when it is far out, and rounds into a circle inside the engraved ring when level.
 * - On edge, a tube vial lies along the phone's edge and the bubble hugs the top of the tube, sliding between two
 *   engraved marks.
 * The bubble moves on a bouncy spring, so it overshoots and settles like air in liquid. Level turns the liquid
 * primaryContainer and fills the engraved marks with primary.
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
        if (isLevel) scheme.primaryContainer else scheme.secondaryContainer,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "liquid"
    )
    val engraving by animateColorAsState(
        if (isLevel) scheme.primary else Color.Black.copy(alpha = 0.32f),
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "engraving"
    )
    val housing = scheme.surfaceContainerHigh
    val glass = scheme.outlineVariant
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
            val thin = 1.5.dp.toPx()

            // Flat: the bull's-eye vial.
            val flatAlpha = 1f - edgeShown
            if (flatAlpha > 0f) {
                drawCircle(housing.copy(alpha = flatAlpha), radius, center)
                val vial = radius - 16.dp.toPx()
                drawGlassLiquid(liquid, flatAlpha) { brush, alpha -> drawCircle(brush, vial, center, alpha) }
                // The glass curves away at the rim, so the liquid darkens toward it.
                drawCircle(
                    Brush.radialGradient(
                        0.78f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.28f),
                        center = center,
                        radius = vial
                    ),
                    vial,
                    center,
                    flatAlpha
                )
                drawCircle(glass.copy(alpha = flatAlpha), vial, center, style = Stroke(thin))

                val bubbleRadius = vial * 0.24f
                // The engraved marks: the ring the bubble sits in when level, a wider guide ring, and four ticks.
                drawEngravedRing(engraving, center, bubbleRadius + 8.dp.toPx(), 2.5.dp.toPx(), flatAlpha)
                drawEngravedRing(engraving.copy(alpha = engraving.alpha * 0.5f), center, vial * 0.66f, thin, flatAlpha)
                for (quarter in 0 until 4) {
                    rotate(quarter * 90f, center) {
                        drawEngravedLine(
                            engraving,
                            Offset(center.x, center.y - vial + 6.dp.toPx()),
                            Offset(center.x, center.y - vial + 18.dp.toPx()),
                            2.dp.toPx(),
                            flatAlpha
                        )
                    }
                }

                // The bubble's travel ends at the glass; its direction is kept when it gets there.
                var offset = Offset(bubbleX, bubbleY)
                val length = hypot(offset.x, offset.y)
                if (length > 1f) offset /= length
                val reach = min(length, 1f)
                val travel = vial - bubbleRadius - 6.dp.toPx()
                val at = center + offset * travel
                // Far out it presses against the glass into an oval; centred it is a perfect circle.
                val along = bubbleRadius * (1f - 0.14f * reach)
                val across = bubbleRadius * (1f + 0.12f * reach)
                val angle = if (length > 0.001f) Math.toDegrees(atan2(offset.y, offset.x).toDouble()).toFloat() else 0f
                rotate(angle, at) {
                    val topLeft = Offset(at.x - along, at.y - across)
                    val ovalSize = Size(along * 2f, across * 2f)
                    drawOval(airBrush(at, maxOf(along, across)), topLeft, ovalSize, flatAlpha)
                    drawOval(Color.Black.copy(alpha = 0.3f * flatAlpha), topLeft, ovalSize, style = Stroke(thin))
                }
                // Light from above: a soft highlight on the upper side, and a faint caustic where it focuses below.
                drawCircle(
                    Brush.radialGradient(
                        0f to Color.White.copy(alpha = 0.55f * flatAlpha),
                        1f to Color.Transparent,
                        center = at + Offset(-bubbleRadius * 0.32f, -bubbleRadius * 0.4f),
                        radius = bubbleRadius * 0.42f
                    ),
                    bubbleRadius * 0.42f,
                    at + Offset(-bubbleRadius * 0.32f, -bubbleRadius * 0.4f)
                )
                drawArc(
                    Color.White.copy(alpha = 0.22f * flatAlpha),
                    startAngle = 30f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = at - Offset(bubbleRadius * 0.72f, bubbleRadius * 0.72f),
                    size = Size(bubbleRadius * 1.44f, bubbleRadius * 1.44f),
                    style = Stroke(bubbleRadius * 0.12f, cap = StrokeCap.Round)
                )
            }

            // On edge: a tube vial along the edge the phone stands on.
            if (edgeShown > 0f) {
                rotate(axisDegrees, center) {
                    val housingLength = radius * 1.96f
                    val housingHeight = radius * 0.62f
                    drawRoundRect(
                        housing.copy(alpha = edgeShown),
                        topLeft = Offset(center.x - housingLength / 2f, center.y - housingHeight / 2f),
                        size = Size(housingLength, housingHeight),
                        cornerRadius = CornerRadius(housingHeight / 2f)
                    )
                    val length = housingLength - 28.dp.toPx()
                    val height = housingHeight - 28.dp.toPx()
                    val left = center.x - length / 2f
                    val top = center.y - height / 2f
                    val corner = CornerRadius(height / 2f)
                    drawGlassLiquid(liquid, edgeShown) { brush, alpha ->
                        drawRoundRect(brush, Offset(left, top), Size(length, height), corner, alpha = alpha)
                    }
                    // A round tube: darker along its upper and lower walls, clearer through the middle.
                    drawRoundRect(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.26f),
                            0.3f to Color.Transparent,
                            0.7f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.3f),
                            startY = top,
                            endY = top + height
                        ),
                        Offset(left, top),
                        Size(length, height),
                        corner,
                        alpha = edgeShown
                    )
                    drawRoundRect(
                        glass.copy(alpha = edgeShown),
                        topLeft = Offset(left, top),
                        size = Size(length, height),
                        cornerRadius = corner,
                        style = Stroke(thin)
                    )

                    val wall = 3.dp.toPx()
                    val bubbleLength = height * 1.7f
                    val bubbleThickness = (height - wall * 2f) * 0.7f
                    // The two marks the bubble sits between when the edge is level, and quieter ones beyond them.
                    val gap = bubbleLength / 2f + 5.dp.toPx()
                    listOf(-gap, gap).forEach { dx ->
                        drawEngravedLine(
                            engraving,
                            Offset(center.x + dx, top + 5.dp.toPx()),
                            Offset(center.x + dx, top + height - 5.dp.toPx()),
                            2.5.dp.toPx(),
                            edgeShown
                        )
                    }
                    listOf(-gap - 14.dp.toPx(), gap + 14.dp.toPx()).forEach { dx ->
                        drawEngravedLine(
                            engraving.copy(alpha = engraving.alpha * 0.5f),
                            Offset(center.x + dx, top + height * 0.3f),
                            Offset(center.x + dx, top + height * 0.7f),
                            thin,
                            edgeShown
                        )
                    }

                    val travel = (length - bubbleLength) / 2f - 10.dp.toPx()
                    val bx = center.x + tube.coerceIn(-1f, 1f) * travel
                    drawTubeBubble(bx, top + wall, bubbleLength, bubbleThickness, thin, edgeShown)
                }
            }
        }
    }
}

/** The liquid: lit a little from above, so it reads as depth rather than a flat fill. */
private fun DrawScope.drawGlassLiquid(
    liquid: Color,
    alpha: Float,
    draw: DrawScope.(Brush, Float) -> Unit
) {
    val brush = Brush.verticalGradient(
        0f to lerp(liquid, Color.White, 0.08f),
        1f to lerp(liquid, Color.Black, 0.12f)
    )
    draw(brush, alpha)
}

/**
 * The see-through body of the bubble: mostly clear, brighter just inside its edge where the light bends, and darker
 * at the very edge (the refraction rim).
 */
private fun airBrush(at: Offset, radius: Float): Brush = Brush.radialGradient(
    0f to Color.White.copy(alpha = 0.06f),
    0.62f to Color.White.copy(alpha = 0.12f),
    0.86f to Color.White.copy(alpha = 0.3f),
    1f to Color.Black.copy(alpha = 0.32f),
    center = at,
    radius = radius
)

/**
 * The bubble in a tube vial: it rises against the top wall, so its top is flat on the glass and its belly curves
 * underneath, with a highlight streak along the top and a faint caustic in the belly.
 */
private fun DrawScope.drawTubeBubble(
    centerX: Float,
    top: Float,
    length: Float,
    thickness: Float,
    rim: Float,
    alpha: Float
) {
    val left = centerX - length / 2f
    val right = centerX + length / 2f
    val bottom = top + thickness
    val shape = Path().apply {
        moveTo(left, top + thickness * 0.4f)
        cubicTo(left, top, left + thickness * 0.3f, top, left + thickness * 0.7f, top)
        lineTo(right - thickness * 0.7f, top)
        cubicTo(right - thickness * 0.3f, top, right, top, right, top + thickness * 0.4f)
        cubicTo(right, top + thickness * 0.85f, right - length * 0.28f, bottom, centerX, bottom)
        cubicTo(left + length * 0.28f, bottom, left, top + thickness * 0.85f, left, top + thickness * 0.4f)
        close()
    }
    drawPath(
        shape,
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.3f),
            0.45f to Color.White.copy(alpha = 0.1f),
            0.82f to Color.White.copy(alpha = 0.16f),
            1f to Color.Black.copy(alpha = 0.3f),
            startY = top,
            endY = bottom
        ),
        alpha = alpha
    )
    drawPath(shape, Color.Black.copy(alpha = 0.3f * alpha), style = Stroke(rim))
    drawLine(
        Color.White.copy(alpha = 0.55f * alpha),
        Offset(left + thickness * 0.75f, top + thickness * 0.2f),
        Offset(centerX - length * 0.08f, top + thickness * 0.2f),
        thickness * 0.14f,
        StrokeCap.Round
    )
    drawLine(
        Color.White.copy(alpha = 0.2f * alpha),
        Offset(centerX - length * 0.16f, bottom - thickness * 0.22f),
        Offset(centerX + length * 0.22f, bottom - thickness * 0.22f),
        thickness * 0.1f,
        StrokeCap.Round
    )
}

/** A line cut into the glass: the groove in [color], with a thin catch of light along its lower lip. */
private fun DrawScope.drawEngravedLine(color: Color, start: Offset, end: Offset, width: Float, alpha: Float) {
    val lip = Offset(0f, width * 0.6f)
    drawLine(Color.White.copy(alpha = 0.16f * alpha), start + lip, end + lip, width * 0.6f, StrokeCap.Round)
    drawLine(color.copy(alpha = color.alpha * alpha), start, end, width, StrokeCap.Round)
}

/** A ring cut into the glass, lit the same way as [drawEngravedLine]. */
private fun DrawScope.drawEngravedRing(color: Color, center: Offset, radius: Float, width: Float, alpha: Float) {
    drawCircle(
        Color.White.copy(alpha = 0.16f * alpha),
        radius,
        center + Offset(0f, width * 0.6f),
        style = Stroke(width * 0.6f)
    )
    drawCircle(color.copy(alpha = color.alpha * alpha), radius, center, style = Stroke(width))
}

/** A bubble reaches the rim at this tilt, so the last few degrees still move it plainly. */
private const val FLAT_FULL_SCALE = 15f
private const val EDGE_FULL_SCALE = 10f
private const val EDGE_ENTER_DEGREES = 55f
private const val EDGE_EXIT_DEGREES = 45f
