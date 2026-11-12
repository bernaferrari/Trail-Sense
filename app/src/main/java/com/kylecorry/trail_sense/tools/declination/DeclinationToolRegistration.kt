package com.kylecorry.trail_sense.tools.declination

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

object DeclinationToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            ToolIds.DECLINATION,
            context.getString(R.string.declination),
            TrailIcons.Declination,
            Route.Declination,
            ToolCategory.Location,
            guideId = RawTexts.guide_tool_declination,
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
            )
        )
    }
}
