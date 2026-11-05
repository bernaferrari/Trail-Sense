package com.kylecorry.trail_sense.shared.views.subscaleview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Draws [state]'s image in tiles with pan, pinch zoom, fling, double tap zoom and double tap and drag zoom.
 * [overlay] draws over the image in view pixels (use the state to convert source coordinates); it is not rotated,
 * so rotate it by [SubsamplingImageState.rotation] about the center if it should turn with the image.
 * [onTap] gets confirmed single taps in view pixels as touched.
 */
@Composable
fun SubsamplingImage(
    state: SubsamplingImageState,
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
    onTap: (Offset) -> Unit = {},
    overlay: DrawScope.() -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    DisposableEffect(state) {
        state.attach(scope)
        onDispose { state.detach() }
    }
    val currentOnTap by rememberUpdatedState(onTap)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { state.onSizeChanged(it.width, it.height) }
            .pointerInput(state) { detectImageGestures(state) { currentOnTap(it) } }
    ) {
        // Read so a moved image or an arrived tile redraws
        state.frame
        if (background != Color.Transparent) {
            drawRect(background)
        }
        drawIntoCanvas { state.draw(it.nativeCanvas) }
        overlay()
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectImageGestures(
    state: SubsamplingImageState,
    onTap: (Offset) -> Unit
) = coroutineScope {
    val touchSlop = viewConfiguration.touchSlop
    val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
    val doubleTapSlop = 100.dp.toPx()
    val quickScaleThreshold = 20.dp.toPx()
    val minFlingDistance = 50f
    val minFlingVelocity = 500f

    var pendingTap: Job? = null
    var lastTapUpTime = 0L
    var lastTapPosition = Offset.Zero

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        state.stopAnimation()

        val isDoubleTap = pendingTap?.isActive == true &&
                down.uptimeMillis - lastTapUpTime < doubleTapTimeout &&
                (down.position - lastTapPosition).getDistance() < doubleTapSlop
        pendingTap?.cancel()
        pendingTap = null

        if (isDoubleTap && state.isZoomEnabled && state.hasLayout) {
            quickScale(state, down, quickScaleThreshold)
            return@awaitEachGesture
        }

        val velocityTracker = VelocityTracker()
        velocityTracker.addPosition(down.uptimeMillis, down.position)
        val start = down.position
        var isPanning = false
        var wasMultiTouch = false
        var previousCentroid: Offset? = null
        var previousSpan = 0f
        var lastUp: PointerInputChange = down

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val pressed = event.changes.filter { it.pressed }
            if (pressed.isEmpty()) {
                lastUp = event.changes.firstOrNull { it.id == down.id } ?: event.changes.first()
                break
            }

            if (pressed.size >= 2) {
                wasMultiTouch = true
                val a = state.unrotate(pressed[0].position.x, pressed[0].position.y)
                val b = state.unrotate(pressed[1].position.x, pressed[1].position.y)
                val centroid = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                val span = hypot(a.x - b.x, a.y - b.y)
                val prev = previousCentroid
                if (prev != null && previousSpan > 0f && state.isZoomEnabled) {
                    val zoom = span / previousSpan
                    state.zoomBy(zoom, prev.x, prev.y, centroid.x - prev.x, centroid.y - prev.y)
                }
                previousCentroid = centroid
                previousSpan = span
                velocityTracker.resetTracking()
                event.changes.forEach { it.consume() }
                continue
            }

            // One finger: it may be what is left of a pinch
            previousCentroid = null
            previousSpan = 0f
            val change = pressed.first()
            velocityTracker.addPosition(change.uptimeMillis, change.position)
            if (!isPanning && (change.position - start).getDistance() > touchSlop) {
                isPanning = true
            }
            if (isPanning || wasMultiTouch) {
                val now = state.unrotate(change.position.x, change.position.y)
                val before = state.unrotate(change.previousPosition.x, change.previousPosition.y)
                state.panBy(now.x - before.x, now.y - before.y)
                change.consume()
            }
        }

        when {
            isPanning && !wasMultiTouch -> {
                val velocity = velocityTracker.calculateVelocity()
                val moved = lastUp.position - start
                if ((abs(moved.x) > minFlingDistance || abs(moved.y) > minFlingDistance) &&
                    (abs(velocity.x) > minFlingVelocity || abs(velocity.y) > minFlingVelocity)
                ) {
                    val v = state.unrotate(velocity.x, velocity.y)
                    val origin = state.unrotate(0f, 0f)
                    state.fling(v.x - origin.x, v.y - origin.y)
                }
            }

            !isPanning && !wasMultiTouch -> {
                // A tap: wait to see whether it's the first of a double tap
                lastTapUpTime = lastUp.uptimeMillis
                lastTapPosition = lastUp.position
                val position = down.position
                pendingTap = launch {
                    delay(doubleTapTimeout)
                    onTap(position)
                }
            }
        }
    }
}

/** Double tap and drag: dragging down zooms in, up zooms out; releasing without dragging zooms in a step. */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.quickScale(
    state: SubsamplingImageState,
    down: PointerInputChange,
    threshold: Float
) {
    val focus = state.unrotate(down.position.x, down.position.y)
    var lastDistance = -1f
    var lastY = down.position.y
    var moved = false
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.first()
        if (!change.pressed) break
        val distance = abs(down.position.y - change.position.y) * 2f + threshold
        if (lastDistance == -1f) {
            lastDistance = distance
        }
        val isDownwards = change.position.y > lastY
        lastY = change.position.y
        val spanDiff = abs(1f - distance / lastDistance) * 0.5f
        if (spanDiff > 0.03f || moved) {
            moved = true
            val multiplier = if (lastDistance > 0f) {
                if (isDownwards) 1f + spanDiff else 1f - spanDiff
            } else {
                1f
            }
            state.zoomBy(multiplier, focus.x, focus.y)
        }
        lastDistance = distance
        change.consume()
    }
    if (!moved) {
        state.doubleTapZoom(focus.x, focus.y)
    }
}
