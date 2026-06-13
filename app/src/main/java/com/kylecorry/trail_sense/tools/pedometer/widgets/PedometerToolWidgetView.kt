package com.kylecorry.trail_sense.tools.pedometer.widgets

import android.content.Context
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences
import kotlin.jvm.optionals.getOrNull

class PedometerToolWidgetView : SimpleToolWidgetView() {

    override suspend fun getContent(
        context: Context,
        prefs: WidgetPreferences?
    ): ToolWidgetContent {
        val subsystem = PedometerSubsystem.getInstance(context)
        val prefs = UserPreferences(context)
        val formatter = FormatService.getInstance(context)
        val steps = subsystem.steps.value.getOrNull() ?: 0
        val distance = subsystem.distance.value.getOrNull() ?: Distance.meters(0f)
        val converted = distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        val formattedDistance =
            formatter.formatDistance(converted, Units.getDecimalPlaces(converted.units))

        return ToolWidgetContent.Simple(
            title = formattedDistance,
            subtitle = context.resources.getQuantityString(
                R.plurals.number_steps,
                steps.toInt(),
                steps.toInt()
            ),
            icon = R.drawable.steps,
            clickAction = WidgetClickAction.StartActivity(NavigationUtils.toolIntent(context, Tools.PEDOMETER))
        )
    }
}
