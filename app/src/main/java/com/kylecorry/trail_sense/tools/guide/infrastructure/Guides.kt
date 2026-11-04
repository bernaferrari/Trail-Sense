package com.kylecorry.trail_sense.tools.guide.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.Apps
import com.kylecorry.trail_sense.shared.icons.trail.Layers
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide
import com.kylecorry.trail_sense.tools.guide.domain.UserGuideCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.sort.CategoricalToolSort

object Guides {

    fun guides(context: Context): List<UserGuideCategory> {
        val tools = Tools.getTools(context)
        val sortedTools = CategoricalToolSort(context).sort(tools)

        val locationCategoryName = context.getString(R.string.location)
        val toolGuides = sortedTools.mapNotNull { category ->
            val guides = category.tools.mapNotNull { tool ->
                if (tool.guideId == null) {
                    return@mapNotNull null
                }

                UserGuide(
                    tool.name,
                    tool.description,
                    tool.guideId,
                    tool.icon
                )
            } + listOfNotNull(
                // Add map layers guide to the bottom of the location category
                if (category.categoryName == locationCategoryName) {
                    UserGuide(
                        context.getString(R.string.guide_map_layers),
                        null,
                        R.raw.guide_tool_map_layers,
                        TrailIcons.Layers
                    )
                } else null
            )

            if (guides.isEmpty()) {
                return@mapNotNull null
            }

            UserGuideCategory(
                category.categoryName ?: context.getString(R.string.tools),
                guides
            )
        }
        val general = listOf(
            UserGuideCategory(
                context.getString(R.string.general),
                listOf(
                    UserGuide(
                        context.getString(R.string.tools),
                        null,
                        R.raw.guide_tool_tools,
                        TrailIcons.Apps
                    )
                )
            )
        )

        return general + toolGuides
    }
}
