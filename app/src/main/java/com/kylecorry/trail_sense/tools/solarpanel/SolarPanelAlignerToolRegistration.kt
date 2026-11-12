package com.kylecorry.trail_sense.tools.solarpanel

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object SolarPanelAlignerToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            ToolIds.SOLAR_PANEL_ALIGNER,
            context.getString(R.string.tool_solar_panel_title),
            TrailIcons.ToolSolarPanel,
            Route.SolarPanel,
            ToolCategory.Power,
            context.getString(R.string.tool_solar_panel_summary),
            guideId = RawTexts.guide_tool_solar_panel_aligner,
            isAvailable = { getAppService<SensorService>().hasCompass() },
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.tilt(context),
                *ToolDiagnosticFactory.compass(context)
            ).distinctBy { it.id }
        )
    }
}
