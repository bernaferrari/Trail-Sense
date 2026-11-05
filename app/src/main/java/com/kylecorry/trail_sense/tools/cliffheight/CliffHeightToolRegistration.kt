package com.kylecorry.trail_sense.tools.cliffheight

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object CliffHeightToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.CLIFF_HEIGHT,
            context.getString(R.string.tool_cliff_height_title),
            TrailIcons.ToolCliffHeight,
            Route.CliffHeight,
            ToolCategory.Distance,
            context.getString(R.string.tool_cliff_height_description),
            isExperimental = true,
            guideId = R.raw.guide_tool_cliff_height,
            isAvailable = { UserPreferences(it).isCliffHeightEnabled },
        )
    }
}
