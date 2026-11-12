package com.kylecorry.trail_sense.tools.triangulate

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

object TriangulateLocationToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            ToolIds.TRIANGULATE_LOCATION,
            context.getString(R.string.tool_triangulate_title),
            TrailIcons.ToolTriangulate,
            Route.Triangulate,
            ToolCategory.Location,
            guideId = RawTexts.guide_tool_triangulate_location,
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.sightingCompass(context)
            ).distinctBy { it.id }
        )
    }
}
