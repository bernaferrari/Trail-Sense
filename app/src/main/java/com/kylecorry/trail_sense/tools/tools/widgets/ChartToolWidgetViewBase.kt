package com.kylecorry.trail_sense.tools.tools.widgets

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.Lifecycle
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Views
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetView

abstract class ChartToolWidgetViewBase : ToolWidgetView {

    override fun onInAppEvent(context: Context, event: Lifecycle.Event, triggerUpdate: () -> Unit) {
        // Do nothing
    }

    protected fun renderChart(context: Context, view: View): Bitmap {
        val width = Resources.dp(context, 400f).toInt()
        val height = Resources.dp(context, 200f).toInt()
        return Views.renderViewAsBitmap(view, width, height)
    }
}
