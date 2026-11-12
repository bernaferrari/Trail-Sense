package com.kylecorry.trail_sense.tools.survival_guide.domain

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.text.RawText

data class Chapter(
    val title: String,
    val resource: RawText,
    val icon: ImageVector
)
