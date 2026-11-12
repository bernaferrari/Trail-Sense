package com.kylecorry.trail_sense.tools.diagnostics

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

object DiagnosticsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            ToolIds.DIAGNOSTICS,
            context.getString(R.string.diagnostics),
            TrailIcons.Alert,
            Route.Diagnostics,
            ToolCategory.Other,
            guideId = RawTexts.guide_tool_diagnostics,
            diagnostics = listOf(
                ToolDiagnosticFactory.externalStorage(context)
            )
        )
    }
}
