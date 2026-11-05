package com.kylecorry.trail_sense.tools.guide

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.guide.quickactions.QuickActionUserGuide
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object UserGuideToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.USER_GUIDE,
            context.getString(R.string.tool_user_guide_title),
            TrailIcons.UserGuide,
            Route.GuideList,
            ToolCategory.Books,
            context.getString(R.string.tool_user_guide_summary),
            additionalRoutes = listOf(
                Route.Guide::class
            ),
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_USER_GUIDE,
                    context.getString(R.string.tool_user_guide_title),
                    ::QuickActionUserGuide
                )
            )
        )
    }
}
