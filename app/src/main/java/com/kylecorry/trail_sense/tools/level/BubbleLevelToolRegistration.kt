package com.kylecorry.trail_sense.tools.level

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

object BubbleLevelToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            ToolIds.BUBBLE_LEVEL,
            context.getString(R.string.tool_bubble_level_title),
            TrailIcons.Level,
            Route.Level,
            ToolCategory.Angles,
            context.getString(R.string.tool_bubble_level_summary),
            guideId = RawTexts.guide_tool_bubble_level,
            settingsRoute = Route.BubbleLevelSettings,
            diagnostics = listOf(
                *ToolDiagnosticFactory.tilt(context)
            ).distinctBy { it.id }
        )
    }
}
