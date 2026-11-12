package com.kylecorry.trail_sense.tools.paths.ui.drawing

import com.kylecorry.trail_sense.shared.canvas.MapDrawer
interface IPathLineDrawerStrategy {
    fun draw(
        canvasDrawer: MapDrawer,
        color: Int,
        strokeScale: Float = 1f,
        block: MapDrawer.() -> Unit
    )
}
