package com.kylecorry.trail_sense.tools.astronomy.widgets

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences

class MoonToolWidgetView : SimpleToolWidgetView() {

    override suspend fun getContent(
        context: Context,
        prefs: WidgetPreferences?
    ): ToolWidgetContent {
        val astronomy = AstronomySubsystem.getInstance(context)
        val formatter = FormatService.getInstance(context)
        val moon = astronomy.moon
        val size = Resources.dp(context, 32f).toInt()
        val bitmap = MoonPhaseImageMapper(context).getPhaseImage(
            moon.phaseAngle,
            size,
            size,
            moon.tilt
        )

        return ToolWidgetContent.Simple(
            title = context.getString(R.string.moon),
            subtitle = formatter.formatMoonPhase(moon.phase),
            iconBitmap = bitmap,
            clickAction = WidgetClickAction.StartActivity(
                NavigationUtils.toolIntent(context, Tools.ASTRONOMY)
            )
        )
    }

}
