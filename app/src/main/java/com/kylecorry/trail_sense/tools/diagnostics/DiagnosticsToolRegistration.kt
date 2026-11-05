package com.kylecorry.trail_sense.tools.diagnostics

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object DiagnosticsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.DIAGNOSTICS,
            context.getString(R.string.diagnostics),
            TrailIcons.Alert,
            Route.Diagnostics,
            ToolCategory.Other,
            guideId = R.raw.guide_tool_diagnostics
        )
    }
}
