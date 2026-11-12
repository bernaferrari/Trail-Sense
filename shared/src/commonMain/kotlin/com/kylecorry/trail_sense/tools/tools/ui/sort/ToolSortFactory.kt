package com.kylecorry.trail_sense.tools.tools.ui.sort

class ToolSortFactory {

    fun getToolSort(sort: ToolSortType): ToolSort {
        return when (sort) {
            ToolSortType.Name -> AlphabeticalToolSort()
            ToolSortType.Category -> CategoricalToolSort()
        }
    }

}
