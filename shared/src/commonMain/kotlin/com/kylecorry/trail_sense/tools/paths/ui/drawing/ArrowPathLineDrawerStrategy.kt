package com.kylecorry.trail_sense.tools.paths.ui.drawing

import com.kylecorry.trail_sense.shared.canvas.MapPathEffects
import com.kylecorry.trail_sense.shared.canvas.MapDrawer
class ArrowPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvasDrawer: MapDrawer,
        color: Int,
        strokeScale: Float,
        block: MapDrawer.() -> Unit
    ) {
        val arrow = MapPathEffects.arrow(6f / strokeScale, 10f / strokeScale)
        canvasDrawer.apply {
            pathEffect(arrow)
            noStroke()
            fill(color)
            block()
        }
    }
}
