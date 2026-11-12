package com.kylecorry.trail_sense.tools.temperature_estimation

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object TemperatureEstimateToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            ToolIds.TEMPERATURE_ESTIMATION,
            context.getString(R.string.tool_temperature_estimation_title),
            TrailIcons.Thermometer,
            Route.TemperatureEstimation,
            ToolCategory.Weather,
            context.getString(R.string.tool_temperature_estimation_description),
            guideId = RawTexts.guide_tool_temperature_estimation,
            diagnostics = listOf(
                *ToolDiagnosticFactory.altimeter(context),
            ).distinctBy { it.id }
        )
    }
}
