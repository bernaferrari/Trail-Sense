package com.kylecorry.trail_sense.tools.sensors.widgets

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
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
        val locationSubsystem = LocationSubsystem.getInstance(context)
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
            clickAction = WidgetClickAction.StartActivity(NavigationUtils.toolIntent(context, Tools.NAVIGATION))
        )
    }
}
