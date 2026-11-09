package com.kylecorry.trail_sense.tools.paths.ui.drawing

import com.kylecorry.trail_sense.shared.andromeda_temp.canvas.DottedPathEffect
import com.kylecorry.trail_sense.shared.andromeda_temp.canvas.ICanvasDrawer
class DottedPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvasDrawer: ICanvasDrawer,
        color: Int,
        strokeScale: Float,
        block: ICanvasDrawer.() -> Unit
    ) {
        val dotted = DottedPathEffect(3f / strokeScale, 10f / strokeScale)
        canvasDrawer.apply {
            pathEffect(dotted)
            noStroke()
            fill(color)
            block()
        }
    }


}
