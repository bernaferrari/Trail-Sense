package com.kylecorry.trail_sense.shared.views

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.ErrorBannerReason

data class UserError(
    val reason: ErrorBannerReason,
    val title: String,
    val icon: ImageVector,
    val action: String? = null,
    val onAction: () -> Unit = {}
)
