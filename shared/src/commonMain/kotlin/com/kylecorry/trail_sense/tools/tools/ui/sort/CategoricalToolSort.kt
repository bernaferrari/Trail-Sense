package com.kylecorry.trail_sense.tools.tools.ui.sort

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDescriptor

class CategoricalToolSort(strings: AppStrings) : ToolSort {

    private val groupNameMap = mapOf(
        ToolCategory.Signaling to strings.get(Res.string.tool_category_signaling),
        ToolCategory.Distance to strings.get(Res.string.distance),
        ToolCategory.Location to strings.get(Res.string.location),
        ToolCategory.Angles to strings.get(Res.string.tool_category_angles),
        ToolCategory.Time to strings.get(Res.string.time),
        ToolCategory.Power to strings.get(Res.string.power),
        ToolCategory.Weather to strings.get(Res.string.weather),
        ToolCategory.Communication to strings.get(Res.string.communication),
        ToolCategory.Books to strings.get(Res.string.books),
        ToolCategory.Other to strings.get(Res.string.other)
    )

    override fun <T : ToolDescriptor> sort(tools: List<T>): List<CategorizedTools<T>> {
        return tools.sortedBy { it.category.ordinal }.groupBy { it.category }
            .map { (category, tools) ->
                CategorizedTools(groupNameMap[category], tools.sortedBy { it.name })
            }
    }
}
