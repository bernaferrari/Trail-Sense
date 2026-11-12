package com.kylecorry.trail_sense.tools.guide.infrastructure

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.icons.trail.Apps
import com.kylecorry.trail_sense.shared.icons.trail.Layers
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide
import com.kylecorry.trail_sense.tools.guide.domain.UserGuideCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import com.kylecorry.trail_sense.tools.tools.ui.sort.CategoricalToolSort

object Guides {

    fun guides(
        catalog: ToolCatalog = getAppService(),
        strings: AppStrings = getAppService()
    ): List<UserGuideCategory> {
        val sortedTools = CategoricalToolSort(strings).sort(catalog.getTools())

        val locationCategoryName = strings.get(Res.string.location)
        val toolGuides = sortedTools.mapNotNull { category ->
            val guides = category.tools.mapNotNull { tool ->
                val guide = tool.guideId ?: return@mapNotNull null
                UserGuide(tool.name, tool.description, guide, tool.icon)
            } + listOfNotNull(
                // Add map layers guide to the bottom of the location category
                if (category.categoryName == locationCategoryName) {
                    UserGuide(
                        strings.get(Res.string.guide_map_layers),
                        null,
                        RawTexts.guide_tool_map_layers,
                        TrailIcons.Layers
                    )
                } else null
            )

            if (guides.isEmpty()) {
                return@mapNotNull null
            }

            UserGuideCategory(
                category.categoryName ?: strings.get(Res.string.tools),
                guides
            )
        }
        val general = listOf(
            UserGuideCategory(
                strings.get(Res.string.general),
                listOf(
                    UserGuide(
                        strings.get(Res.string.tools),
                        null,
                        RawTexts.guide_tool_tools,
                        TrailIcons.Apps
                    )
                )
            )
        )

        return general + toolGuides
    }
}
