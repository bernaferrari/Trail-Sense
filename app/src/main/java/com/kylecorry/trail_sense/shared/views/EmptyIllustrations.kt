@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.shared.views

import android.provider.Settings
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/*
 * Living objects for empty screens (the boiler principle): each is a small scene drawn with M3 shapes in scheme
 * colours that loops calmly. They're decorative (no text, no semantics) and lie still on the resting frame when the
 * system animation scale is 0. Pass one to TrailSenseEmptyState's `illustration`.
 *
 * Every scene is drawn on a 160 x 160 grid ([u] is one grid unit), matching the empty state's 160 dp box.
 */

/** A page with a line being written by a pen. */
@Composable
fun NotesEmptyIllustration() {
    val scheme = MaterialTheme.colorScheme
    val backdrop = MaterialShapes.Cookie12Sided.toShape()
    val pen = MaterialShapes.Pill.toShape()
    val loop = rememberLoop(4800, still = 0.7f)
    val page = scheme.surfaceContainerLowest
    val written = scheme.onSurfaceVariant.copy(alpha = 0.35f)
    val ink = scheme.primary
    val penColor = scheme.tertiary
    IllustrationCanvas {
        val p = loop.value
        val u = gridUnit
        drawBackdrop(backdrop, scheme.primaryContainer)
        val start = 54f
        val end = 106f
        val lines = listOf(52f, 64f, 76f, 88f)
        // Two lines are written in turn, then fade together so the page starts fresh.
        val first = segment(p, 0.06f, 0.36f, LinearEasing)
        val second = segment(p, 0.40f, 0.66f, LinearEasing)
        val fade = 1f - segment(p, 0.84f, 0.96f)
        withTransform({ rotate(-4f, Offset(80 * u, 80 * u)) }) {
            drawRoundRect(page, Offset(42 * u, 32 * u), Size(76 * u, 96 * u), CornerRadius(10 * u))
            // The lines already written.
            lines.take(2).forEachIndexed { i, y ->
                val width = if (i == 1) 0.7f else 1f
                drawLine(
                    written, Offset(start * u, y * u), Offset((start + (end - start) * width) * u, y * u),
                    strokeWidth = 4 * u, cap = StrokeCap.Round
                )
            }
            drawLine(
                written, Offset(start * u, 100 * u), Offset((start + 22) * u, 100 * u),
                strokeWidth = 4 * u, cap = StrokeCap.Round
            )
            val tip1 = drawHandwriting(lines[2], start, end, first, ink.copy(alpha = fade))
            val tip2 = drawHandwriting(lines[3], start, end - 14f, second, ink.copy(alpha = fade))
            // The pen rests at the tip of the line in progress, then glides back to the start.
            val back = segment(p, 0.84f, 1f)
            val tip = when {
                p < 0.36f -> tip1
                p < 0.40f -> lerpOffset(tip1, Offset(start * u, lines[3] * u), segment(p, 0.36f, 0.40f))
                p < 0.84f -> tip2
                else -> lerpOffset(tip2, Offset(start * u, lines[2] * u), back)
            }
            val writing = (p in 0.06f..0.36f) || (p in 0.40f..0.66f)
            val bob = if (writing) 1.2f * u * sin(p * 60f * PI).toFloat() else 0f
            withTransform({
                translate(tip.x, tip.y + bob)
                rotate(38f, Offset.Zero)
            }) {
                // Drawn from the tip up: the pill's bottom end touches the page.
                drawShape(pen, Offset(-4.5f * u, -34 * u), Size(9 * u, 34 * u), penColor)
            }
        }
    }
}

/** A folded map that unfolds, with a "you are here" dot that pulses once it lies open. */
@Composable
fun OfflineMapsEmptyIllustration() {
    val scheme = MaterialTheme.colorScheme
    val backdrop = MaterialShapes.Cookie9Sided.toShape()
    val loop = rememberLoop(6000, still = 0.6f)
    val paper = scheme.surfaceContainerLowest
    val paperShade = scheme.surfaceContainerHigh
    val land = scheme.secondary.copy(alpha = 0.18f)
    val route = scheme.tertiary
    val fold = scheme.onSurface
    val dot = scheme.primary
    val dotRing = scheme.surfaceContainerLowest
    IllustrationCanvas {
        val p = loop.value
        val u = gridUnit
        drawBackdrop(backdrop, scheme.primaryContainer)
        val open = segment(p, 0.06f, 0.36f) * (1f - segment(p, 0.88f, 1f))
        val left = 32f
        val top = 46f
        val panel = 32f
        val height = 68f
        val routePath = Path().apply {
            moveTo(38 * u, 104 * u)
            cubicTo(56 * u, 102 * u, 58 * u, 70 * u, 78 * u, 74 * u)
            cubicTo(98 * u, 78 * u, 104 * u, 56 * u, 122 * u, 54 * u)
        }

        fun DrawScope.panelContent(index: Int) {
            val x = (left + index * panel) * u
            drawRect(if (index == 1) paper else paperShade, Offset(x, top * u), Size(panel * u, height * u))
            drawCircle(land, 16 * u, Offset(48 * u, 62 * u))
            drawCircle(land, 22 * u, Offset(112 * u, 100 * u))
            drawPath(routePath, route, style = Stroke(3 * u, cap = StrokeCap.Round))
        }

        // The middle panel stays put; the outer ones swing open from its edges.
        val outerScale = 0.02f + 0.98f * open
        for (index in listOf(0, 2)) {
            val pivotX = if (index == 0) (left + panel) * u else (left + 2 * panel) * u
            val x = (left + index * panel) * u
            withTransform({ scale(outerScale, 1f, Offset(pivotX, 0f)) }) {
                clipRect(x, top * u, x + panel * u, (top + height) * u) { panelContent(index) }
                // A folded panel is in shadow; it brightens as it opens.
                drawRect(fold.copy(alpha = 0.14f * (1f - open)), Offset(x, top * u), Size(panel * u, height * u))
            }
        }
        val mid = (left + panel) * u
        clipRect(mid, top * u, mid + panel * u, (top + height) * u) { panelContent(1) }
        // The creases.
        val crease = fold.copy(alpha = 0.10f * open)
        drawLine(crease, Offset(mid, top * u), Offset(mid, (top + height) * u), 1.5f * u)
        drawLine(crease, Offset(mid + panel * u, top * u), Offset(mid + panel * u, (top + height) * u), 1.5f * u)

        // You are here.
        val here = Offset(80 * u, 75 * u)
        val pop = popIn(segment(p, 0.36f, 0.48f, LinearEasing)) * (1f - segment(p, 0.84f, 0.9f))
        if (pop > 0f) {
            for (ring in 0..1) {
                val t = ((p - 0.48f - ring * 0.16f) / 0.32f)
                if (t in 0f..1f && p < 0.84f) {
                    drawCircle(dot.copy(alpha = 0.45f * (1f - t)), (7f + 17f * t) * u, here)
                }
            }
            drawCircle(dotRing, 8.5f * u * pop, here)
            drawCircle(dot, 5.5f * u * pop, here)
        }
    }
}

/** A backpack that a checked item drops into. */
@Composable
fun PackEmptyIllustration() {
    val scheme = MaterialTheme.colorScheme
    val backdrop = MaterialShapes.Cookie9Sided.toShape()
    val tag = MaterialShapes.Cookie4Sided.toShape()
    val loop = rememberLoop(4000, still = 0.28f)
    val body = scheme.primary
    val inside = lerp(scheme.primary, scheme.onPrimaryContainer, 0.55f)
    val pocket = lerp(scheme.primary, scheme.primaryContainer, 0.35f)
    val strap = lerp(scheme.primary, scheme.onPrimaryContainer, 0.35f)
    val item = scheme.tertiary
    val check = scheme.onTertiary
    IllustrationCanvas {
        val p = loop.value
        val u = gridUnit
        drawBackdrop(backdrop, scheme.primaryContainer)
        // The item appears, is ticked, then falls into the open pack, which gives a small bounce as it lands.
        val appear = popIn(segment(p, 0.02f, 0.14f, LinearEasing))
        val tick = segment(p, 0.12f, 0.26f)
        val fall = segment(p, 0.32f, 0.5f, FastOutLinearInEasing)
        val landing = segment(p, 0.5f, 0.66f, LinearEasing)
        val squash = sin(landing * PI).toFloat()

        withTransform({ scale(1f + 0.04f * squash, 1f - 0.06f * squash, Offset(80 * u, 132 * u)) }) {
            // Handle and the dark mouth of the pack, behind the item.
            drawArc(
                strap, 180f, 180f, false, Offset(68 * u, 52 * u), Size(24 * u, 20 * u),
                style = Stroke(4 * u, cap = StrokeCap.Round)
            )
            drawRoundRect(inside, Offset(54 * u, 60 * u), Size(52 * u, 16 * u), CornerRadius(8 * u))
        }
        if (p < 0.5f) {
            val y = 30f + 60f * fall
            withTransform({
                translate(80 * u, y * u)
                rotate(-12f + 24f * fall, Offset.Zero)
                scale(appear, appear, Offset.Zero)
            }) {
                drawShape(tag, Offset(-14 * u, -14 * u), Size(28 * u, 28 * u), item)
                drawCheck(tick, check)
            }
        }
        withTransform({ scale(1f + 0.04f * squash, 1f - 0.06f * squash, Offset(80 * u, 132 * u)) }) {
            // The body covers the item once it drops in.
            drawRoundRect(body, Offset(50 * u, 68 * u), Size(60 * u, 64 * u), CornerRadius(18 * u))
            drawRoundRect(pocket, Offset(60 * u, 100 * u), Size(40 * u, 24 * u), CornerRadius(10 * u))
            drawLine(strap, Offset(70 * u, 108 * u), Offset(90 * u, 108 * u), 3 * u, cap = StrokeCap.Round)
            drawLine(strap, Offset(62 * u, 72 * u), Offset(62 * u, 94 * u), 3 * u, cap = StrokeCap.Round)
            drawLine(strap, Offset(98 * u, 72 * u), Offset(98 * u, 94 * u), 3 * u, cap = StrokeCap.Round)
        }
    }
}

/** A dotted trail drawing itself behind a hiker dot, toward a flag. */
@Composable
fun PathsEmptyIllustration() {
    val scheme = MaterialTheme.colorScheme
    val backdrop = MaterialShapes.Cookie12Sided.toShape()
    val loop = rememberLoop(6400, still = 0.8f)
    val trail = scheme.onPrimaryContainer.copy(alpha = 0.55f)
    val hiker = scheme.primary
    val halo = scheme.surfaceContainerLowest
    val flag = scheme.tertiary
    IllustrationCanvas {
        val p = loop.value
        val u = gridUnit
        drawBackdrop(backdrop, scheme.primaryContainer)
        val path = Path().apply {
            moveTo(36 * u, 118 * u)
            cubicTo(72 * u, 126 * u, 40 * u, 78 * u, 80 * u, 78 * u)
            cubicTo(120 * u, 78 * u, 92 * u, 42 * u, 116 * u, 44 * u)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length
        val walk = segment(p, 0.04f, 0.78f, FastOutSlowInEasing)
        val fade = 1f - segment(p, 0.86f, 0.96f)

        // The destination.
        val pole = Offset(116 * u, 44 * u)
        drawLine(flag, pole, pole - Offset(0f, 22 * u), 2.5f * u, cap = StrokeCap.Round)
        drawPath(
            Path().apply {
                moveTo(pole.x, pole.y - 22 * u)
                lineTo(pole.x + 14 * u, pole.y - 17 * u)
                lineTo(pole.x, pole.y - 12 * u)
                close()
            },
            flag
        )
        // The start.
        drawCircle(trail, 4 * u, Offset(36 * u, 118 * u), style = Stroke(2 * u))

        val gap = 9 * u
        var d = gap
        while (d < length * walk - 6 * u) {
            drawCircle(trail.copy(alpha = trail.alpha * fade), 2.6f * u, measure.getPosition(d))
            d += gap
        }
        val (at, alpha) = when {
            p < 0.86f -> measure.getPosition(length * walk) to 1f
            p < 0.92f -> measure.getPosition(length) to 1f - segment(p, 0.86f, 0.92f)
            else -> measure.getPosition(0f) to segment(p, 0.94f, 1f)
        }
        val moving = p in 0.04f..0.78f
        val bob = if (moving) -1.5f * u * abs(sin(p * 44f * PI).toFloat()) else 0f
        val c = at + Offset(0f, bob)
        drawCircle(halo.copy(alpha = alpha), 10 * u, c)
        drawCircle(hiker.copy(alpha = alpha), 7 * u, c)
    }
}

/** A pin dropping onto a spot, with a ripple where it lands. */
@Composable
fun PlacesEmptyIllustration() {
    val scheme = MaterialTheme.colorScheme
    val backdrop = MaterialShapes.Cookie9Sided.toShape()
    val loop = rememberLoop(4400, still = 0.6f)
    val pin = scheme.primary
    val hole = scheme.primaryContainer
    val shadow = scheme.onPrimaryContainer.copy(alpha = 0.18f)
    val ripple = scheme.primary
    IllustrationCanvas {
        val p = loop.value
        val u = gridUnit
        drawBackdrop(backdrop, scheme.primaryContainer)
        val spot = Offset(80 * u, 116 * u)
        val fall = segment(p, 0.06f, 0.30f, FastOutLinearInEasing)
        val bounce = sin(segment(p, 0.30f, 0.42f, LinearEasing) * PI).toFloat()
        val leave = segment(p, 0.86f, 1f)
        val appear = segment(p, 0f, 0.08f)
        val lift = (1f - fall) * 56f + bounce * 6f + leave * 10f
        val alpha = appear * (1f - leave)
        val squash = if (p in 0.30f..0.36f) sin(segment(p, 0.30f, 0.36f, LinearEasing) * PI).toFloat() else 0f

        // The ripples spread from where the pin lands.
        for (ring in 0..1) {
            val t = (p - 0.30f - ring * 0.14f) / 0.42f
            if (t in 0f..1f) {
                val rx = (10f + 34f * LinearOutSlowInEasing.transform(t)) * u
                drawOval(
                    ripple.copy(alpha = 0.5f * (1f - t)),
                    Offset(spot.x - rx, spot.y - rx * 0.32f), Size(rx * 2, rx * 0.64f),
                    style = Stroke(2.5f * u)
                )
            }
        }
        // The spot's shadow grows as the pin nears it.
        val near = 1f - lift / 66f
        val sw = (10f + 12f * near) * u
        drawOval(shadow.copy(alpha = shadow.alpha * alpha), Offset(spot.x - sw, spot.y - sw * 0.3f), Size(sw * 2, sw * 0.6f))

        withTransform({
            translate(spot.x, spot.y - lift * u)
            scale(1f + 0.08f * squash, 1f - 0.1f * squash, Offset.Zero)
        }) {
            drawPin(pin.copy(alpha = alpha), hole.copy(alpha = alpha))
        }
    }
}

/** A sun setting slowly on the horizon, and rising again. */
@Composable
fun TurnBackEmptyIllustration() {
    val scheme = MaterialTheme.colorScheme
    val backdrop = MaterialShapes.Cookie12Sided.toShape()
    val sun = MaterialShapes.Sunny.toShape()
    val loop = rememberLoop(7000, still = 0.6f, reverse = true, easing = FastOutSlowInEasing)
    val sunColor = scheme.tertiary
    val horizon = scheme.onTertiaryContainer
    IllustrationCanvas {
        val p = loop.value
        val u = gridUnit
        drawBackdrop(backdrop, scheme.tertiaryContainer)
        val horizonY = 104f * u
        val centre = Offset(80 * u, (72f + 38f * p) * u)
        clipRect(bottom = horizonY - 3 * u) {
            drawCircle(sunColor.copy(alpha = 0.16f), 36 * u, centre)
            withTransform({ rotate(p * 40f, centre) }) {
                drawShape(sun, centre - Offset(25 * u, 25 * u), Size(50 * u, 50 * u), sunColor)
            }
        }
        // The horizon and its reflections, fading toward the viewer.
        drawLine(horizon, Offset(34 * u, horizonY), Offset(126 * u, horizonY), 4 * u, cap = StrokeCap.Round)
        val glint = 1f - 0.5f * p
        drawLine(
            horizon.copy(alpha = 0.45f), Offset((80f - 26f * glint) * u, horizonY + 10 * u),
            Offset((80f + 26f * glint) * u, horizonY + 10 * u), 4 * u, cap = StrokeCap.Round
        )
        drawLine(
            horizon.copy(alpha = 0.25f), Offset((80f - 12f * glint) * u, horizonY + 20 * u),
            Offset((80f + 12f * glint) * u, horizonY + 20 * u), 4 * u, cap = StrokeCap.Round
        )
    }
}

/** Clouds drifting across the sky, the near ones faster than the far one. */
@Composable
fun CloudsEmptyIllustration() {
    val scheme = MaterialTheme.colorScheme
    val backdrop = MaterialShapes.Cookie12Sided.toShape()
    val loop = rememberLoop(36000, still = 0.15f)
    val far = lerp(scheme.primaryContainer, scheme.primary, 0.35f)
    val near = scheme.primary
    IllustrationCanvas {
        val p = loop.value
        val u = gridUnit
        val sky = backdropPath(backdrop)
        drawPath(sky, scheme.primaryContainer)
        clipPath(sky) {
            // Each cloud crosses a whole number of times per loop, so the loop is seamless.
            drawCloud(drift(p, 0.15f, 1), 56f + 3f * sin(p * 2 * PI).toFloat(), 44f, far)
            drawCloud(drift(p, 0.65f, 2), 84f + 2f * sin(p * 4 * PI + 1).toFloat(), 64f, near)
            drawCloud(drift(p, 0.05f, 2), 112f + 2f * sin(p * 4 * PI + 2).toFloat(), 46f, near)
        }
    }
}

// --- Shared drawing helpers ---

/** One grid unit: the scene is laid out on a 160-unit square. */
private val DrawScope.gridUnit: Float
    get() = size.minDimension / 160f

@Composable
private fun IllustrationCanvas(draw: DrawScope.() -> Unit) {
    Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}, onDraw = draw)
}

/**
 * A 0..1 phase that loops every [periodMillis]. When the system animation scale is 0 (or in previews) it holds at
 * [still], the scene's most telling frame.
 */
@Composable
private fun rememberLoop(
    periodMillis: Int,
    still: Float,
    reverse: Boolean = false,
    easing: Easing = LinearEasing
): State<Float> {
    val context = LocalContext.current
    val inspecting = LocalInspectionMode.current
    val animate = remember(context, inspecting) {
        !inspecting && Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) > 0f
    }
    return if (animate) {
        rememberInfiniteTransition(label = "illustration").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(periodMillis, easing = easing),
                if (reverse) RepeatMode.Reverse else RepeatMode.Restart
            ),
            label = "phase"
        )
    } else {
        remember(still) { mutableFloatStateOf(still) }
    }
}

/** Progress through [start]..[end] of the loop, eased. */
private fun segment(p: Float, start: Float, end: Float, easing: Easing = FastOutSlowInEasing): Float =
    easing.transform(((p - start) / (end - start)).coerceIn(0f, 1f))

/** A small overshoot for things that pop into place. */
private fun popIn(t: Float): Float {
    if (t <= 0f) return 0f
    if (t >= 1f) return 1f
    val c = 1.7f
    val x = t - 1f
    return 1f + (c + 1f) * x * x * x + c * x * x
}

private fun lerpOffset(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

private fun DrawScope.backdropPath(shape: Shape): Path {
    val u = gridUnit
    val side = 144 * u
    val outline = shape.createOutline(Size(side, side), layoutDirection, this)
    return Path().apply {
        addOutline(outline)
        translate(Offset((size.width - side) / 2, (size.height - side) / 2))
    }
}

/** The tonal M3 shape the scene sits on. */
private fun DrawScope.drawBackdrop(shape: Shape, color: Color) {
    drawPath(backdropPath(shape), color)
}

private fun DrawScope.drawShape(shape: Shape, topLeft: Offset, size: Size, color: Color) {
    val outline = shape.createOutline(size, layoutDirection, this)
    withTransform({ translate(topLeft.x, topLeft.y) }) { drawOutline(outline, color) }
}

/** A handwritten line from [start] toward [end] at [y], written up to [progress]; returns the pen's tip. */
private fun DrawScope.drawHandwriting(y: Float, start: Float, end: Float, progress: Float, color: Color): Offset {
    val u = gridUnit
    val x1 = start + (end - start) * progress
    fun at(x: Float) = Offset(x * u, (y + 1.6f * sin((x - start) / 7f * 2 * PI).toFloat()) * u)
    if (progress > 0f) {
        val path = Path().apply {
            val first = at(start)
            moveTo(first.x, first.y)
            var x = start
            while (x < x1) {
                x = (x + 1f).coerceAtMost(x1)
                val o = at(x)
                lineTo(o.x, o.y)
            }
        }
        drawPath(path, color, style = Stroke(3.5f * u, cap = StrokeCap.Round))
    }
    return at(x1)
}

/** A tick drawn into a 28-unit item centred on the origin, up to [progress]. */
private fun DrawScope.drawCheck(progress: Float, color: Color) {
    val u = gridUnit
    if (progress <= 0f) return
    val a = Offset(-6.5f * u, 0.5f * u)
    val b = Offset(-2f * u, 5f * u)
    val c = Offset(7f * u, -5f * u)
    val firstShare = 0.35f
    val path = Path().apply {
        moveTo(a.x, a.y)
        if (progress < firstShare) {
            val t = progress / firstShare
            val e = lerpOffset(a, b, t)
            lineTo(e.x, e.y)
        } else {
            lineTo(b.x, b.y)
            val e = lerpOffset(b, c, (progress - firstShare) / (1f - firstShare))
            lineTo(e.x, e.y)
        }
    }
    drawPath(path, color, style = Stroke(3.5f * u, cap = StrokeCap.Round))
}

/** A map pin whose tip is at the origin. */
private fun DrawScope.drawPin(color: Color, hole: Color) {
    val u = gridUnit
    val r = 17 * u
    val headY = -40 * u
    val path = Path().apply {
        moveTo(0f, 0f)
        cubicTo(-5 * u, -12 * u, -r, headY + 12 * u, -r, headY)
        arcTo(Rect(Offset(0f, headY), r), 180f, 180f, false)
        cubicTo(r, headY + 12 * u, 5 * u, -12 * u, 0f, 0f)
        close()
    }
    drawPath(path, color)
    drawCircle(hole, 6.5f * u, Offset(0f, headY))
}

/** A cloud's x (in grid units), crossing the 160-unit scene [laps] times per loop from [start]. */
private fun drift(p: Float, start: Float, laps: Int): Float {
    val span = 260f
    return ((start + p * laps) % 1f) * span - 50f
}

/** A cloud of [width] units with its centre at ([x], [y]). */
private fun DrawScope.drawCloud(x: Float, y: Float, width: Float, color: Color) {
    val u = gridUnit
    val w = width * u
    val c = Offset(x * u, y * u)
    drawRoundRect(
        color, Offset(c.x - w / 2, c.y - w * 0.04f), Size(w, w * 0.3f), CornerRadius(w * 0.15f)
    )
    drawCircle(color, w * 0.2f, c + Offset(-w * 0.2f, w * 0.04f))
    drawCircle(color, w * 0.27f, c + Offset(w * 0.04f, -w * 0.04f))
    drawCircle(color, w * 0.17f, c + Offset(w * 0.28f, w * 0.08f))
}
