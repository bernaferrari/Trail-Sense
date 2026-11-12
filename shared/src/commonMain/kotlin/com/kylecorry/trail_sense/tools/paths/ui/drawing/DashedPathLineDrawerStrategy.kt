package com.kylecorry.trail_sense.tools.paths.ui.drawing

import com.kylecorry.trail_sense.shared.canvas.MapPathEffects
import com.kylecorry.trail_sense.shared.canvas.MapDrawer
class DashedPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvasDrawer: MapDrawer,
        color: Int,
        strokeScale: Float,
        block: MapDrawer.() -> Unit
    ) {
        val dotted = MapPathEffects.dashed(3f / strokeScale, 10f / strokeScale)
        canvasDrawer.apply {
            pathEffect(dotted)
            noFill()
            strokeWeight(4f / strokeScale)
            stroke(color)
            block()
        }
    }


}
