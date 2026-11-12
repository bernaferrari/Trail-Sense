package com.kylecorry.trail_sense.tools.tools.ui.sort

import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDescriptor

interface ToolSort {
    fun <T : ToolDescriptor> sort(tools: List<T>): List<CategorizedTools<T>>
}
