package com.kylecorry.trail_sense.tools.guide.domain

import androidx.annotation.RawRes
import androidx.compose.ui.graphics.vector.ImageVector

data class UserGuide(
    val name: String,
    val description: String? = null,
    @RawRes val contents: Int,
    val icon: ImageVector? = null
)
