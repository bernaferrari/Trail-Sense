package com.kylecorry.trail_sense.tools.tools.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
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

    /**
     * A chart for the widget: drawn live wherever the app shows widgets (the quick actions sheet), and as a bitmap for
     * home screen widgets when one can be made. A Compose view can't compose until it is attached to a window, so
     * rendering a detached one used to crash the app; now that case just has no bitmap.
     */
    protected fun chartContent(
        context: Context,
        clickAction: WidgetClickAction?,
        chart: @Composable () -> Unit
    ): ToolWidgetContent.Chart {
        val bitmap = runCatching {
            val width = Resources.dp(context, 400f).toInt()
            val height = Resources.dp(context, 200f).toInt()
            Views.renderViewAsBitmap(
                TrailSenseComposeRootView(context, ViewCompositionStrategy.DisposeOnDetachedFromWindow) { chart() },
                width,
                height
            )
        }.getOrNull()
        return ToolWidgetContent.Chart(bitmap = bitmap, chart = chart, clickAction = clickAction)
    }
}
