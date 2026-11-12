package com.kylecorry.trail_sense.tools.map.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import com.kylecorry.andromeda.core.units.PixelCoordinate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** What the map does with touches beyond moving its camera. Positions are screen pixels. */
interface MapGestureHandler {
    /** Whether the map responds to touch at all. */
    val isInteractive: Boolean

    fun onTap(position: PixelCoordinate) {}

    /** Called for a long press, unless a pinch is in progress. */
    fun onLongPress(position: PixelCoordinate) {}

    /** Whether a touch starting at [position] grabs something on the map; if so, the touch drags it. */
    fun onDragStart(position: PixelCoordinate): Boolean = false

    fun onDrag(position: PixelCoordinate) {}

    fun onDragEnd(position: PixelCoordinate) {}
}

/**
 * Touch for a map: a tap, a double tap that zooms in on the spot, and a long press; one finger pans, two pinch-zoom
 * around their center, and a quick release flings. A touch that starts on something draggable drags it instead.
 * Moves are consumed only once they pass the touch slop, so a still press stays a tap or a long press.
 */
@Composable
fun Modifier.mapGestures(camera: MapCamera, handler: MapGestureHandler): Modifier {
    val scope = rememberCoroutineScope()
    val gestures = remember(camera, handler) { MapGestureState(camera, handler, scope) }
    // Taps first, then the drag loop inside it: a drag consumes its moves, which cancels a pending tap or long press.
    return this
        .pointerInput(camera, handler) {
            detectTapGestures(
                onDoubleTap = {
                    if (handler.isInteractive) camera.zoomWithFocus(2f, PixelCoordinate(it.x, it.y))
                },
                onTap = { if (handler.isInteractive) handler.onTap(PixelCoordinate(it.x, it.y)) },
                onLongPress = {
                    if (handler.isInteractive && !gestures.isScaling) handler.onLongPress(PixelCoordinate(it.x, it.y))
                }
            )
        }
        .pointerInput(camera, handler) { with(gestures) { detectDrags(viewConfiguration.touchSlop) } }
}

private class MapGestureState(
    private val camera: MapCamera,
    private val handler: MapGestureHandler,
    private val scope: CoroutineScope
) {
    var isScaling = false
        private set
    private var fling: Job? = null

    suspend fun PointerInputScope.detectDrags(touchSlop: Float) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!handler.isInteractive) return@awaitEachGesture
            fling?.cancel()
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            val isDragging = handler.onDragStart(down.position.toPixel())
            var pastSlop = false
            var travelled = Offset.Zero
            var lastPosition = down.position
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) break
                val pan = event.calculatePan()
                val zoom = event.calculateZoom()
                if (!pastSlop) {
                    travelled += pan
                    pastSlop = travelled.getDistance() > touchSlop || (pressed.size > 1 && zoom != 1f)
                }
                lastPosition = pressed.first().position
                if (isDragging) {
                    handler.onDrag(lastPosition.toPixel())
                } else if (pastSlop) {
                    isScaling = pressed.size > 1
                    if (zoom != 1f) {
                        camera.zoomWithFocus(zoom, event.calculateCentroid(useCurrent = false).toPixel())
                    }
                    if (pan != Offset.Zero) {
                        camera.pan(pan.x, pan.y)
                    }
                }
                if (pastSlop) {
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
                if (pressed.size == 1) {
                    tracker.addPosition(pressed.first().uptimeMillis, pressed.first().position)
                } else {
                    tracker.resetTracking()
                }
            }
            if (isDragging) {
                handler.onDragEnd(lastPosition.toPixel())
            } else if (pastSlop && !isScaling) {
                val velocity = tracker.calculateVelocity()
                fling = scope.launch { camera.fling(velocity.x, velocity.y) }
            }
            isScaling = false
        }
    }

    private fun Offset.toPixel() = PixelCoordinate(x, y)
}
