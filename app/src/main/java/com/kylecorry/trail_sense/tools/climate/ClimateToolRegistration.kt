package com.kylecorry.trail_sense.tools.climate

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.tools.climate.infrastructure.ClimateSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object ClimateToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            ToolIds.CLIMATE,
            context.getString(R.string.tool_climate),
            TrailIcons.TemperatureRange,
            Route.Climate,
            ToolCategory.Weather,
            context.getString(R.string.tool_climate_summary),
            guideId = RawTexts.guide_tool_climate,
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.altimeter(context)
            ).distinctBy { it.id },
            singletons = listOf(
                ClimateSubsystem::getInstance
            )
        )
    }
}
