@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.level.ui

import com.kylecorry.trail_sense.shared.views.compose.KeepScreenOn

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialShapes
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Bubble level, flat and confident: one target that works the way the phone is held.
 * - Lying flat, a solid bubble rides to the high side of a crosshair and settles in the centre ring when the surface
 *   is level.
 * - Standing on an edge, a solid capsule slides along a track on that edge and sits between the two marks when the
 *   edge is level.
 * The angle is the display number under the target, with the verdict naming the high side and one quiet line saying
 * which of the two it is measuring. Once level, the whole screen fills with primaryContainer, the bubble morphs into
 * a cookie and the phone gives one haptic tick.
 */
@Composable
fun LevelScreen(modifier: Modifier = Modifier) {
    // One question: is it level, and if not, which side is high?
    val context = LocalContext.current
    KeepScreenOn()
    val formatService = remember { FormatService.getInstance(context) }
    val threshold = remember { UserPreferences(context).bubbleLevel.threshold }
    val gravity = remember { SensorService(context).getGravity() }
    val throttle = remember { Throttle(20) }
    // The up vector in the phone's frame (what a gravity sensor reports): +z out of the screen, +x right, +y top.
    var gx by remember { mutableFloatStateOf(0f) }
    var gy by remember { mutableFloatStateOf(0f) }
    var gz by remember { mutableFloatStateOf(1f) }

    DisposableEffect(gravity) {
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

    // When level the whole screen changes state: it fills with primaryContainer and everything on it follows.
    val scheme = MaterialTheme.colorScheme
    val screenColor by animateColorAsState(
        if (isLevel) scheme.primaryContainer else scheme.surface,
        MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "screen"
    )
    val inkColor by animateColorAsState(
        if (isLevel) scheme.onPrimaryContainer else scheme.onSurface,
        MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "ink"
    )

    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.tool_bubble_level_title),
        largeTitle = true,
        containerColor = screenColor,
        contentColor = inkColor,
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
            LevelTarget(
                isLevel = isLevel,
                onEdge = onEdge,
                ink = inkColor,
                // The bubble's place, -1..1 on each axis (screen y runs down, so a high top is up).
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
                    style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = inkColor,
                    maxLines = 1
                )
                Text(
                    text = verdict,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = inkColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(if (onEdge) R.string.level_hint_edge else R.string.level_hint_flat),
                    style = MaterialTheme.typography.bodyMedium,
                    color = inkColor.copy(alpha = 0.72f),
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
 * The one live object, drawn flat in the spirit of a measuring app's level: no glass, no shading.
 * - Flat, it is two circles on a crosshair: a fixed ring at the centre (the target) and a solid primary bubble that
 *   rides toward the high side. When the bubble sits in the ring the surface is level, and the bubble morphs from a
 *   circle into a scalloped cookie with a quarter turn.
 * - On edge, a tonal track lies along the edge the phone stands on, with two marks at its middle, and a solid primary
 *   capsule slides along it. When level, the capsule stretches to meet both marks.
 * The bubble moves on a calm spring and the mode change crossfades.
 */
@Composable
private fun LevelTarget(
    isLevel: Boolean,
    onEdge: Boolean,
    ink: Color,
    bubble: Offset,
    edgeBubble: Float,
    axisDegrees: Float,
    modifier: Modifier = Modifier
) {
    val bubbleColor = MaterialTheme.colorScheme.primary
    val glide = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
    val bubbleX by animateFloatAsState(bubble.x, glide, label = "bubbleX")
    val bubbleY by animateFloatAsState(bubble.y, glide, label = "bubbleY")
    val tube by animateFloatAsState(edgeBubble, glide, label = "tube")
    val lock by animateFloatAsState(
        if (isLevel) 1f else 0f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "lock"
    )
    val edgeShown by animateFloatAsState(
        if (onEdge) 1f else 0f,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "mode"
    )
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }

    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        val guide = ink.copy(alpha = 0.18f)

        // Flat: the crosshair, the bubble and the target ring.
        val flatAlpha = 1f - edgeShown
        if (flatAlpha > 0f) {
            val hair = 1.5.dp.toPx()
            drawLine(guide, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), hair, alpha = flatAlpha)
            drawLine(guide, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), hair, alpha = flatAlpha)

            val bubbleRadius = radius * 0.3f
            var offset = Offset(bubbleX, bubbleY)
            val length = hypot(offset.x, offset.y)
            if (length > 1f) offset /= length
            val at = center + offset * (radius - bubbleRadius)
            // A little larger once it locks, so the cookie's scallops reach toward the ring.
            val drawn = bubbleRadius * (1f + 0.06f * lock)
            val shape = morph.toPath(lock.coerceIn(0f, 1f)).asComposePath()
            rotate(90f * lock, at) {
                withTransform({
                    translate(at.x - drawn, at.y - drawn)
                    scale(drawn * 2f, drawn * 2f, Offset.Zero)
                }) {
                    drawPath(shape, bubbleColor, alpha = flatAlpha)
                }
            }

            // The target sits over the bubble, so it stays readable wherever the bubble is.
            drawCircle(ink, bubbleRadius + 8.dp.toPx(), center, flatAlpha, style = Stroke(3.dp.toPx()))
        }

        // On edge: the track along the edge, the capsule and its two marks.
        if (edgeShown > 0f) {
            rotate(axisDegrees, center) {
                val trackLength = radius * 1.9f
                val trackHeight = radius * 0.34f
                drawRoundRect(
                    ink.copy(alpha = 0.1f),
                    topLeft = Offset(center.x - trackLength / 2f, center.y - trackHeight / 2f),
                    size = Size(trackLength, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f),
                    alpha = edgeShown
                )
                val inset = 6.dp.toPx()
                val capsuleHeight = trackHeight - inset * 2f
                val capsuleLength = capsuleHeight * 2f
                val markWidth = 3.dp.toPx()
                val gap = capsuleLength / 2f + inset + markWidth / 2f
                val travel = (trackLength - capsuleLength) / 2f - inset
                // Locked, the capsule centres itself and stretches to meet both marks.
                val stretched = capsuleLength + 2f * inset * lock
                val cx = center.x + tube.coerceIn(-1f, 1f) * travel * (1f - lock)
                drawRoundRect(
                    bubbleColor,
                    topLeft = Offset(cx - stretched / 2f, center.y - capsuleHeight / 2f),
                    size = Size(stretched, capsuleHeight),
                    cornerRadius = CornerRadius(capsuleHeight / 2f),
                    alpha = edgeShown
                )
                listOf(-gap, gap).forEach { dx ->
                    drawLine(
                        ink,
                        Offset(center.x + dx, center.y - trackHeight / 2f - inset),
                        Offset(center.x + dx, center.y + trackHeight / 2f + inset),
                        markWidth,
                        StrokeCap.Round,
                        alpha = edgeShown
                    )
                }
            }
        }
    }
}

/** A bubble reaches the edge of the field at this tilt, so the last few degrees still move it plainly. */
private const val FLAT_FULL_SCALE = 15f
private const val EDGE_FULL_SCALE = 10f
private const val EDGE_ENTER_DEGREES = 55f
private const val EDGE_EXIT_DEGREES = 45f
