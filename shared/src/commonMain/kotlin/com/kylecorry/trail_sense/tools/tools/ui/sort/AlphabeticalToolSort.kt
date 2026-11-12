package com.kylecorry.trail_sense.tools.tools.ui.sort

import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDescriptor

class AlphabeticalToolSort : ToolSort {
    override fun <T : ToolDescriptor> sort(tools: List<T>): List<CategorizedTools<T>> {
        return listOf(CategorizedTools(null, tools.sortedBy { it.name }))
    }
}
