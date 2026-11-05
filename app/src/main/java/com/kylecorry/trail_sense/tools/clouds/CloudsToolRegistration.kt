package com.kylecorry.trail_sense.tools.clouds

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.clouds.quickactions.QuickActionScanCloud
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object CloudsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.CLOUDS,
            context.getString(R.string.clouds),
            TrailIcons.ToolClouds,
            Route.Clouds(),
            ToolCategory.Weather,
            guideId = R.raw.guide_tool_clouds,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_SCAN_CLOUD,
                    context.getString(R.string.cloud_scanner),
                    ::QuickActionScanCloud
                )
            ),
            additionalRoutes = listOf(
                Route.CloudResults::class
            ),
            diagnostics = listOf(
                ToolDiagnosticFactory.camera(context)
            )
        )
    }
}
