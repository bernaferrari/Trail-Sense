package com.kylecorry.trail_sense.tools.astronomy.widgets

import kotlin.time.toJavaInstant

import com.kylecorry.trail_sense.shared.time.interop.*

import com.kylecorry.trail_sense.shared.time.JavaTime

import com.kylecorry.trail_sense.tools.astronomy.ui.AstroChartColors

import com.kylecorry.trail_sense.tools.astronomy.ui.drawAstroChart

import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetChart

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.ui.AstroChart
import com.kylecorry.trail_sense.tools.astronomy.ui.AstroChartState
import androidx.compose.ui.graphics.asImageBitmap
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
import com.kylecorry.trail_sense.tools.tools.widgets.ChartToolWidgetViewBase
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences
import java.time.Duration
import java.time.Instant

class SunAndMoonChartToolWidgetView : ChartToolWidgetViewBase() {

    override suspend fun getContent(
        context: Context,
        prefs: WidgetPreferences?
    ): ToolWidgetContent {
        val astronomy = AstronomySubsystem.getInstance(context)

        val moon = astronomy.moon

        val instant = Instant.now()
        val sunAltitudes = astronomy.getSunAltitudes()
        val moonAltitudes = astronomy.getMoonAltitudes()
        val currentSun = sunAltitudes.minByOrNull {
            JavaTime.between(instant, it.time).abs()
        }
        val currentMoon = moonAltitudes.minByOrNull {
            JavaTime.between(instant, it.time).abs()
        }

        var content: ToolWidgetContent.Chart? = null
        onMain {
            val userPrefs = UserPreferences(context)
            val size = Resources.dp(context, 24f).toInt()
            val moonImage = MoonPhaseImageMapper(context).getPhaseImage(
                moon.phaseAngle,
                size,
                size,
                moon.tilt
            ).asImageBitmap()
            val state = AstroChartState(
                startTime = (sunAltitudes.firstOrNull()?.time ?: kotlin.time.Clock.System.now()).toJavaInstant(),
                sun = sunAltitudes,
                moon = moonAltitudes,
                sunPosition = currentSun,
                moonPosition = currentMoon,
                showBands = userPrefs.astronomy.showAstronomyBands
            )

            content = ToolWidgetContent.Chart(
                drawing = WidgetChart { scheme -> drawAstroChart(state, AstroChartColors.from(scheme), moonImage) },
                clickAction = WidgetClickAction.StartActivity(NavigationUtils.toolIntent(context, Tools.ASTRONOMY))
            )
        }

        return content!!
    }
}
