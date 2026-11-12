package com.kylecorry.trail_sense.tools.lightning

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration

object LightningStrikeDistanceToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            ToolIds.LIGHTNING_STRIKE_DISTANCE,
            context.getString(R.string.tool_lightning_title),
            TrailIcons.TorchOn,
            Route.Lightning,
            ToolCategory.Weather,
            context.getString(R.string.tool_lightning_description),
            guideId = RawTexts.guide_tool_lightning_strike_distance
        )
    }
}
