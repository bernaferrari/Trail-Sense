package com.kylecorry.trail_sense.tools.tools.ui.sort

import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDescriptor

data class CategorizedTools<T : ToolDescriptor>(val categoryName: String?, val tools: List<T>)
