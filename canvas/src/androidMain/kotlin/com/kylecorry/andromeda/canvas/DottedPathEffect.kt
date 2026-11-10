package com.kylecorry.andromeda.canvas

import android.graphics.Path
import android.graphics.PathDashPathEffect

public class DottedPathEffect(size: Float = 3f, advance: Float = 10f, phase: Float = 0f) :
    PathDashPathEffect(getDotPath(size), advance, phase, Style.ROTATE) {
    public companion object {
        private fun getDotPath(size: Float): Path {
            val path = Path()
            path.addCircle(0f, 0f, size, Path.Direction.CW)
            return path
        }
    }
}