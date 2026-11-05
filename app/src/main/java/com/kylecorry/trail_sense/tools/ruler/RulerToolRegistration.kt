package com.kylecorry.trail_sense.tools.ruler

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.ruler.quickactions.QuickActionRuler
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object RulerToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.RULER,
            context.getString(R.string.tool_ruler_title),
            TrailIcons.Ruler,
            Route.Ruler,
            ToolCategory.Distance,
            guideId = R.raw.guide_tool_ruler,
            settingsRoute = Route.RulerSettings,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_RULER,
                    context.getString(R.string.tool_ruler_title),
                    ::QuickActionRuler
                )
            )
        )
    }
}
