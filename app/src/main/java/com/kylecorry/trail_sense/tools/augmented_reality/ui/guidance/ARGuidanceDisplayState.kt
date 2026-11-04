package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import androidx.compose.ui.graphics.vector.ImageVector
import android.graphics.Bitmap
import androidx.annotation.ColorInt

data class ARGuidanceDisplayState(
    val name: String,
    val icon: ImageVector,
    @ColorInt val iconTint: Int? = null,
    @ColorInt val iconBackgroundTint: Int? = null,
    val iconRotation: Float = 0f,
    val iconBitmap: Bitmap? = null
)
