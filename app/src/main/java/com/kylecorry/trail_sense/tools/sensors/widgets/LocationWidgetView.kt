package com.kylecorry.trail_sense.tools.sensors.widgets

import android.content.Context
import androidx.lifecycle.Lifecycle
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetView
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences

class LocationWidgetView : ToolWidgetView {

    override fun onInAppEvent(context: Context, event: Lifecycle.Event, triggerUpdate: () -> Unit) {
        // Do nothing
    }

    override suspend fun getContent(
        context: Context,
        prefs: WidgetPreferences?
    ): ToolWidgetContent {
        val formatter = FormatService.getInstance(context)
        val locationSubsystem = LocationSubsystem.getInstance(context)
        val location = locationSubsystem.location

        return ToolWidgetContent.Title(
            title = formatter.formatLocation(location),
            clickAction = WidgetClickAction.StartActivity(NavigationUtils.toolIntent(context, Tools.NAVIGATION))
        )
    }
}
