package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.content.Context
import androidx.lifecycle.Lifecycle

abstract class SimpleToolWidgetView : ToolWidgetView {

    override fun onInAppEvent(context: Context, event: Lifecycle.Event, triggerUpdate: () -> Unit) {
        // Do nothing
    }
}
