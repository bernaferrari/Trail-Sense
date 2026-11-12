package com.kylecorry.trail_sense.tools.metaldetector

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.shared.volume.SystemVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object MetalDetectorToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            ToolIds.METAL_DETECTOR,
            context.getString(R.string.tool_metal_detector_title),
            TrailIcons.ToolMetalDetector,
            Route.MetalDetector,
            ToolCategory.Other,
            guideId = RawTexts.guide_tool_metal_detector,
            volumeActions = listOf(
                ToolVolumeAction(
                    ToolVolumeActionPriority.Normal,
                    { context, isToolOpen, _ -> isToolOpen && UserPreferences(context).metalDetector.isMetalAudioEnabled },
                    { SystemVolumeAction() }
                )
            ),
            isAvailable = { getAppService<SensorService>().hasCompass() },
            diagnostics = listOf(
                ToolDiagnosticFactory.magnetometer(context)
            )
        )
    }
}
