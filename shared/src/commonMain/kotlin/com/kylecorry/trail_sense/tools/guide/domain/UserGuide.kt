package com.kylecorry.trail_sense.tools.guide.domain

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.text.RawText

data class UserGuide(
    val name: String,
    val description: String? = null,
    val contents: RawText,
    val icon: ImageVector? = null
)
