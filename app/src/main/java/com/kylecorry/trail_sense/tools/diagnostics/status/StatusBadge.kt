package com.kylecorry.trail_sense.tools.diagnostics.status

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.ColorInt

data class StatusBadge(
    val name: String,
    @ColorInt val color: Int,
    val icon: ImageVector,
    val isHealthy: Boolean = false
)
