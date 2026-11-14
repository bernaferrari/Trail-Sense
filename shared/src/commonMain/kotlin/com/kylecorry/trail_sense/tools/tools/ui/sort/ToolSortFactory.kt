package com.kylecorry.trail_sense.tools.tools.ui.sort

import com.kylecorry.trail_sense.shared.text.AppStrings

class ToolSortFactory(private val strings: AppStrings) {

    fun getToolSort(sort: ToolSortType): ToolSort {
        return when (sort) {
            ToolSortType.Name -> AlphabeticalToolSort()
            ToolSortType.Category -> CategoricalToolSort(strings)
        }
    }

}
