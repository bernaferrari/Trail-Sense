package com.kylecorry.trail_sense.tools.paths.ui.drawing

import com.kylecorry.trail_sense.shared.canvas.MapDrawer
class SolidPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvasDrawer: MapDrawer,
        color: Int,
        strokeScale: Float,
        block: MapDrawer.() -> Unit
    ) {
        canvasDrawer.apply {
            noPathEffect()
            noFill()
            stroke(color)
            strokeWeight(6f / strokeScale)
            block()
        }
    }
}
