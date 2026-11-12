package com.kylecorry.trail_sense.tools.ballistics

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration

object BallisticsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            ToolIds.BALLISTICS,
            context.getString(R.string.ballistics),
            TrailIcons.Ballistics,
            Route.Ballistics,
            ToolCategory.Other,
            guideId = RawTexts.guide_tool_ballistics
        )
    }
}
