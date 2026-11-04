package com.kylecorry.trail_sense.tools.packs

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object PackingListsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.PACKING_LISTS,
            context.getString(R.string.packing_lists),
            TrailIcons.ToolPack,
            R.id.packListFragment,
            ToolCategory.Other,
            guideId = R.raw.guide_tool_packing_lists,
            additionalNavigationIds = listOf(
                R.id.createItemFragment,
                R.id.packItemListFragment
            )
        )
    }
}
