package com.kylecorry.trail_sense.tools.tools.widgets

import android.content.Context
import androidx.lifecycle.Lifecycle
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetView

/** A widget whose content is a chart ([com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent.Chart]). */
abstract class ChartToolWidgetViewBase : ToolWidgetView {

    override fun onInAppEvent(context: Context, event: Lifecycle.Event, triggerUpdate: () -> Unit) {
        // Do nothing
    }
}
