package com.kylecorry.trail_sense.tools.packs

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration

object PackingListsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            ToolIds.PACKING_LISTS,
            context.getString(R.string.packing_lists),
            TrailIcons.ToolPack,
            Route.PackList,
            ToolCategory.Other,
            guideId = RawTexts.guide_tool_packing_lists,
            additionalRoutes = listOf(
                Route.CreatePackItem::class,
                Route.PackItemList::class
            )
        )
    }
}
