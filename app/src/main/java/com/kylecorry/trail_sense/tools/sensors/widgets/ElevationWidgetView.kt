package com.kylecorry.trail_sense.tools.sensors.widgets

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.sensors.ILocationSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences

class ElevationWidgetView : SimpleToolWidgetView() {

    override suspend fun getContent(
        context: Context,
        prefs: WidgetPreferences?
    ): ToolWidgetContent {
        val formatter = FormatService.getInstance(context)
        val locationSubsystem = getAppService<ILocationSubsystem>()
        val prefs = UserPreferences(context)

        val elevation = locationSubsystem.elevation

        val convertedElevation = elevation.convertTo(prefs.baseDistanceUnits)

        return ToolWidgetContent.Simple(
            title = context.getString(R.string.elevation),
            subtitle = formatter.formatDistance(
                convertedElevation,
                Units.getDecimalPlaces(convertedElevation.units)
            ),
            icon = TrailIcons.Altitude,
            clickAction = WidgetClickAction.StartActivity(NavigationUtils.toolIntent(context, ToolIds.NAVIGATION))
        )
    }
}
