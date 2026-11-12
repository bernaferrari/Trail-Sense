package com.kylecorry.trail_sense.tools.tools.widgets

import android.content.Context
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.tools.tools.infrastructure.Action
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class RefreshWidgetAction(private val widgetId: String) : Action {
    override suspend fun onReceive(context: Context, data: EventData) {
        Tools.triggerWidgetUpdate(context, widgetId)
    }
}
