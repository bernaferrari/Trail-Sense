package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.content.Context
import androidx.lifecycle.Lifecycle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences

interface ToolWidgetView {

    fun onInAppEvent(context: Context, event: Lifecycle.Event, triggerUpdate: () -> Unit)

    suspend fun getContent(context: Context, prefs: WidgetPreferences?): ToolWidgetContent

    /**
     * Get the widget's loading state.
     */
    fun getLoadingContent(context: Context, prefs: WidgetPreferences?): ToolWidgetContent {
        return ToolWidgetContent.Simple(
            title = context.getString(R.string.app_name),
            subtitle = "",
            icon = R.drawable.ic_launcher_foreground
        )
    }

}
