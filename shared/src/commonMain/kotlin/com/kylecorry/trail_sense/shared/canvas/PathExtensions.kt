package com.kylecorry.trail_sense.shared.canvas

import androidx.compose.ui.graphics.Path

/**
 * Adds line segments to the path. [lines] holds x1, y1, x2, y2 for each segment; a segment that starts where the
 * last one ended continues it, and any other starts a new contour. With [smooth], each segment curves toward the
 * midpoint of the next instead of meeting it at a corner.
 */
fun Path.drawLines(lines: FloatArray, smooth: Boolean = false) {
    var i = 0
    var lastX = 0f
    var lastY = 0f
    while (i + 3 < lines.size) {
        val x1 = lines[i]
        val y1 = lines[i + 1]
        val x2 = lines[i + 2]
        val y2 = lines[i + 3]
        if (x1 != lastX || y1 != lastY) {
            moveTo(x1, y1)
        }
        if (smooth) {
            quadraticTo(x1, y1, (x1 + x2) / 2f, (y1 + y2) / 2f)
        } else {
            lineTo(x2, y2)
        }
        lastX = x2
        lastY = y2
        i += 4
    }

    if (smooth && lines.size >= 4) {
        quadraticTo(lines[lines.size - 4], lines[lines.size - 3], lines[lines.size - 2], lines[lines.size - 1])
    }
}
