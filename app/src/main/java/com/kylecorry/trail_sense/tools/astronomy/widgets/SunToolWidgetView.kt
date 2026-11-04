package com.kylecorry.trail_sense.tools.astronomy.widgets

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyTransition
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences

class SunToolWidgetView : SimpleToolWidgetView() {
    override suspend fun getContent(
        context: Context,
        prefs: WidgetPreferences?
    ): ToolWidgetContent {
        val formatService = FormatService.getInstance(context)
        val astronomy = AstronomySubsystem.getInstance(context)
        val sun = astronomy.sun

        val clickAction = WidgetClickAction.StartActivity(
            NavigationUtils.toolIntent(context, Tools.ASTRONOMY)
        )

        return if (sun.nextRise != null && sun.nextTransition == AstronomyTransition.Rise) {
            ToolWidgetContent.Simple(
                title = context.getString(R.string.sunrise),
                subtitle = formatService.formatTime(sun.nextRise.toLocalTime(), includeSeconds = false),
                icon = TrailIcons.SunriseNotification,
                clickAction = clickAction
            )
        } else if (sun.nextSet != null && sun.nextTransition == AstronomyTransition.Set) {
            ToolWidgetContent.Simple(
                title = context.getString(R.string.sunset),
                subtitle = formatService.formatTime(sun.nextSet.toLocalTime(), includeSeconds = false),
                icon = TrailIcons.SunsetNotification,
                clickAction = clickAction
            )
        } else if (sun.isUp) {
            ToolWidgetContent.Simple(
                title = context.getString(R.string.sun_up_no_set),
                subtitle = context.getString(R.string.sun_does_not_set),
                icon = TrailIcons.Sun,
                clickAction = clickAction
            )
        } else {
            ToolWidgetContent.Simple(
                title = context.getString(R.string.sun_down_no_set),
                subtitle = context.getString(R.string.sun_does_not_rise),
                icon = TrailIcons.Sun,
                clickAction = clickAction
            )
        }
    }
}
