package com.kylecorry.trail_sense.tools.paths.ui.drawing

import com.kylecorry.trail_sense.shared.andromeda_temp.canvas.ICanvasDrawer
class SolidPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvasDrawer: ICanvasDrawer,
        color: Int,
        strokeScale: Float,
        block: ICanvasDrawer.() -> Unit
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
