package com.kylecorry.trail_sense.tools.convert

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.tools.convert.quickactions.QuickActionConvert
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration

object ConvertToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            ToolIds.CONVERT,
            context.getString(R.string.convert),
            TrailIcons.ToolDistanceConvert,
            Route.Convert,
            ToolCategory.Other,
            guideId = RawTexts.guide_tool_convert,
            quickActions = listOf(
                ToolQuickAction(
                    ToolIds.QUICK_ACTION_CONVERT,
                    context.getString(R.string.convert),
                    ::QuickActionConvert
                )
            )
        )
    }
}
