package com.kylecorry.trail_sense.shared

import android.content.Context
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.math.sumOfFloat
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

fun ICanvasDrawer.getBounds(rotation: Float = 0f): Rectangle {
    val rectangle = Rectangle(
        0f,
        canvas.height.toFloat(),
        canvas.width.toFloat(),
        0f,
    )

    if (rotation != 0f) {
        return rectangle.rotate(rotation)
    }

    return rectangle
}


fun ICanvasDrawer.text(str: String, x: Float, y: Float, lineSpacing: Float) {
    val lines = str.split("\n")
    var lastHeight = 0f
    var lastY = y
    lines.forEachIndexed { index, line ->
        val newY = lastY + lastHeight + if (index == 0) 0f else lineSpacing
        lastY = newY
        lastHeight = textHeight(line)
        text(line, x, newY)
    }
}


fun ICanvasDrawer.textDimensions(str: String, lineSpacing: Float): Pair<Float, Float> {
    val lines = str.split("\n")
    val totalTextHeight = lines.sumOfFloat { textHeight(it) }
    val totalSpacing = lineSpacing * (lines.size - 1)
    val maxWidth = lines.maxOfOrNull { textWidth(it) } ?: 0f
    return maxWidth to totalTextHeight + totalSpacing
}


fun TrailSenseNavigator.openTool(context: Context, toolId: Long) {
    val route = Tools.getTool(context, toolId)?.route ?: return
    navigate(route)
}

