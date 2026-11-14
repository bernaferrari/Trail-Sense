package com.kylecorry.trail_sense.tools.map.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import androidx.compose.ui.graphics.Canvas as ComposeCanvas

/** Android adapter for context-derived display metrics and widget bitmap rendering. */
class MapView(context: Context) : SharedMapView(
    context.resources.displayMetrics.density,
    context.resources.configuration.fontScale
) {
    var backgroundTintList: ColorStateList? = null
        set(value) {
            field = value
            backgroundColorOverride = value?.defaultColor
        }

    fun draw(canvas: Canvas) = draw(ComposeCanvas(canvas), canvas.width, canvas.height)
}
