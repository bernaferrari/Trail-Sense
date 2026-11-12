package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.ui.graphics.Path
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

/**
 * The cubics as a closed Compose path, each point scaled by [scaleX] and [scaleY] and then moved by [dx] and [dy].
 */
fun List<Cubic>.toComposePath(
    scaleX: Float = 1f,
    scaleY: Float = scaleX,
    dx: Float = 0f,
    dy: Float = 0f,
    path: Path = Path()
): Path {
    path.rewind()
    if (isEmpty()) {
        return path
    }
    path.moveTo(first().anchor0X * scaleX + dx, first().anchor0Y * scaleY + dy)
    for (cubic in this) {
        path.cubicTo(
            cubic.control0X * scaleX + dx, cubic.control0Y * scaleY + dy,
            cubic.control1X * scaleX + dx, cubic.control1Y * scaleY + dy,
            cubic.anchor1X * scaleX + dx, cubic.anchor1Y * scaleY + dy
        )
    }
    path.close()
    return path
}

/** The polygon as a closed Compose path; see [List.toComposePath] for the scaling. */
fun RoundedPolygon.toComposePath(
    scaleX: Float = 1f,
    scaleY: Float = scaleX,
    dx: Float = 0f,
    dy: Float = 0f,
    path: Path = Path()
): Path = cubics.toComposePath(scaleX, scaleY, dx, dy, path)

/** The morph at [progress] as a closed Compose path; see [List.toComposePath] for the scaling. */
fun Morph.toComposePath(
    progress: Float,
    scaleX: Float = 1f,
    scaleY: Float = scaleX,
    dx: Float = 0f,
    dy: Float = 0f,
    path: Path = Path()
): Path = asCubics(progress).toComposePath(scaleX, scaleY, dx, dy, path)
