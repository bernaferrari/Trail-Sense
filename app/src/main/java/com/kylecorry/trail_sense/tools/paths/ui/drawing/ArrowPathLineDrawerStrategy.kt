package com.kylecorry.trail_sense.tools.paths.ui.drawing

import com.kylecorry.trail_sense.shared.andromeda_temp.canvas.ArrowPathEffect
import com.kylecorry.trail_sense.shared.andromeda_temp.canvas.ICanvasDrawer
class ArrowPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvasDrawer: ICanvasDrawer,
        color: Int,
        strokeScale: Float,
        block: ICanvasDrawer.() -> Unit
    ) {
        val arrow = ArrowPathEffect(6f / strokeScale, 10f / strokeScale)
        canvasDrawer.apply {
            pathEffect(arrow)
            noStroke()
            fill(color)
            block()
        }
    }
}
