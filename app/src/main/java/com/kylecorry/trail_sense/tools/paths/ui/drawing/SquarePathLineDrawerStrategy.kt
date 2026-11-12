package com.kylecorry.trail_sense.tools.paths.ui.drawing

import com.kylecorry.trail_sense.shared.canvas.MapDrawer
import com.kylecorry.trail_sense.shared.canvas.MapPathEffects
class SquarePathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvasDrawer: MapDrawer,
        color: Int,
        strokeScale: Float,
        block: MapDrawer.() -> Unit
    ) {
        val size = 8f / strokeScale
        val dotted = MapPathEffects.square(size, size * 2)
        canvasDrawer.apply {
            pathEffect(dotted)
            noStroke()
            fill(color)
            block()
        }
    }


}
