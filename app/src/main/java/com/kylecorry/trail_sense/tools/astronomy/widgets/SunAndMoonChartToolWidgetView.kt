package com.kylecorry.trail_sense.tools.astronomy.widgets

import kotlin.time.Clock

import com.kylecorry.trail_sense.tools.astronomy.ui.AstroChartColors

import com.kylecorry.trail_sense.tools.astronomy.ui.drawAstroChart

import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetChart

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.ui.AstroChartState
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
import com.kylecorry.trail_sense.tools.tools.widgets.ChartToolWidgetViewBase
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences

class SunAndMoonChartToolWidgetView : ChartToolWidgetViewBase() {

    override suspend fun getContent(
        context: Context,
        prefs: WidgetPreferences?
    ): ToolWidgetContent {
        val astronomy = AstronomySubsystem.getInstance(context)

        val moon = astronomy.moon

        val instant = Clock.System.now()
        val sunAltitudes = astronomy.getSunAltitudes()
        val moonAltitudes = astronomy.getMoonAltitudes()
        val currentSun = sunAltitudes.minByOrNull {
            (it.time - instant).absoluteValue
        }
        val currentMoon = moonAltitudes.minByOrNull {
            (it.time - instant).absoluteValue
        }

        var content: ToolWidgetContent.Chart? = null
        onMain {
            val userPrefs = UserPreferences(context)
            val size = Resources.dp(context, 24f).toInt()
            val moonImage = MoonPhaseImageMapper().getPhaseImage(
                moon.phaseAngle,
                size,
                size,
                moon.tilt
            )
            val state = AstroChartState(
                startTime = sunAltitudes.firstOrNull()?.time ?: Clock.System.now(),
                sun = sunAltitudes,
                moon = moonAltitudes,
                sunPosition = currentSun,
                moonPosition = currentMoon,
                showBands = userPrefs.astronomy.showAstronomyBands
            )

            content = ToolWidgetContent.Chart(
                drawing = WidgetChart { scheme -> drawAstroChart(state, AstroChartColors.from(scheme), moonImage) },
                clickAction = WidgetClickAction.StartActivity(NavigationUtils.toolIntent(context, ToolIds.ASTRONOMY))
            )
        }

        return content!!
    }
}
