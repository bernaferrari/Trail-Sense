package com.kylecorry.trail_sense.tools.light

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object LightMeterToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.LIGHT_METER,
            context.getString(R.string.tool_light_meter_title),
            TrailIcons.Flashlight,
            Route.LightMeter,
            ToolCategory.Power,
            context.getString(R.string.guide_light_meter_description),
            guideId = R.raw.guide_tool_light_meter,
            isAvailable = { Sensors.hasSensor(it, Sensor.TYPE_LIGHT) },
            diagnostics = listOf(
                ToolDiagnosticFactory.light(context)
            )
        )
    }
}
