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
import androidx.compose.ui.geometry.Offset
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Bubble level: one round well that works the way the phone is held.
 * - Lying flat, it is a bubble level for the surface under the phone: the bubble rides to the high side and settles
 *   in the centre ring when the surface is level.
 * - Standing on an edge, it is a line level for that edge: a line stays on the true horizon and meets the phone's
 *   own axis when the edge is level.
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
                horizonDegrees = roll,
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
 * The one live object: a round well. Flat, a bubble in primary rides inside it toward the high side, with a ring at
 * the centre for level. On edge, a primary line holds the true horizon across the well and a quiet line marks the
 * phone's own axis; level is when they meet. When level the well fills with primaryContainer.
 */
@Composable
private fun LevelWell(
    isLevel: Boolean,
    onEdge: Boolean,
    bubble: Offset,
    horizonDegrees: Float,
    axisDegrees: Float,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val well by animateColorAsState(
        if (isLevel) scheme.primaryContainer else scheme.surfaceContainerHigh,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "well"
    )
    val mark = if (isLevel) scheme.onPrimaryContainer.copy(alpha = 0.5f) else scheme.outlineVariant
    val live = scheme.primary
    val bubbleX by animateFloatAsState(bubble.x, TrailSenseAnimation.standardSpring, label = "bubbleX")
    val bubbleY by animateFloatAsState(bubble.y, TrailSenseAnimation.standardSpring, label = "bubbleY")
    val horizon by animateFloatAsState(horizonDegrees, TrailSenseAnimation.standardSpring, label = "horizon")
    val edgeShown by animateFloatAsState(
        if (onEdge) 1f else 0f,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "mode"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            drawCircle(well, radius, center)

            val thin = 2.dp.toPx()
            val bubbleRadius = radius * 0.17f
            val ringRadius = bubbleRadius + 8.dp.toPx()

            // Flat: a crosshair, the level ring and the bubble.
            val flatAlpha = 1f - edgeShown
            if (flatAlpha > 0f) {
                drawLine(
                    mark.copy(alpha = mark.alpha * flatAlpha),
                    Offset(center.x - radius, center.y),
                    Offset(center.x + radius, center.y),
                    thin
                )
                drawLine(
                    mark.copy(alpha = mark.alpha * flatAlpha),
                    Offset(center.x, center.y - radius),
                    Offset(center.x, center.y + radius),
                    thin
                )
                // The bubble's travel ends at the rim; its direction is kept when it gets there.
                var offset = Offset(bubbleX, bubbleY)
                val length = hypot(offset.x, offset.y)
                if (length > 1f) offset /= length
                val travel = radius - bubbleRadius - 6.dp.toPx()
                drawCircle(
                    live.copy(alpha = flatAlpha),
                    bubbleRadius,
                    center + offset * travel
                )
                drawCircle(
                    (if (isLevel) scheme.onPrimaryContainer else scheme.outline).copy(alpha = flatAlpha),
                    ringRadius,
                    center,
                    style = Stroke(thin)
                )
            }

            // On edge: the phone's axis and the horizon.
            if (edgeShown > 0f) {
                rotate(axisDegrees, center) {
                    drawLine(
                        mark.copy(alpha = mark.alpha * edgeShown),
                        Offset(center.x - radius, center.y),
                        Offset(center.x + radius, center.y),
                        thin
                    )
                }
                rotate(horizon, center) {
                    drawLine(
                        live.copy(alpha = edgeShown),
                        Offset(center.x - radius * 0.86f, center.y),
                        Offset(center.x + radius * 0.86f, center.y),
                        6.dp.toPx(),
                        StrokeCap.Round
                    )
                }
                drawCircle(live.copy(alpha = edgeShown), 7.dp.toPx(), center)
            }
        }
    }
}

/** A bubble reaches the rim at this tilt, so the last few degrees still move it plainly. */
private const val FLAT_FULL_SCALE = 15f
private const val EDGE_ENTER_DEGREES = 55f
private const val EDGE_EXIT_DEGREES = 45f
