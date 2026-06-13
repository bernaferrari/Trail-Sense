package com.kylecorry.trail_sense.tools.tides.widgets

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.tides.subsystem.TidesSubsystem
import com.kylecorry.trail_sense.tools.tides.ui.TideFormatter
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences

class TidesToolWidgetView : SimpleToolWidgetView() {

    override suspend fun getContent(
        context: Context,
        prefs: WidgetPreferences?
    ): ToolWidgetContent {
        val formatter = TideFormatter(context)
        val tide = TidesSubsystem.getInstance(context).getNearestTide()
        val tideName = tide?.let { formatter.getTideTypeName(it.now.type) }

        return ToolWidgetContent.Simple(
            title = if (tide == null) context.getString(R.string.no_tides) else tide.table.name,
            subtitle = tideName,
            icon = formatter.getTideTypeImage(tide?.now?.type),
            clickAction = WidgetClickAction.StartActivity(NavigationUtils.toolIntent(context, Tools.TIDES))
        )
    }

}
