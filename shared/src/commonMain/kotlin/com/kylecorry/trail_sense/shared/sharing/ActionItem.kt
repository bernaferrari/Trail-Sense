package com.kylecorry.trail_sense.shared.sharing

import androidx.compose.ui.graphics.vector.ImageVector
data class ActionItem(
    val name: String,
    val icon: ImageVector,
    val action: () -> Unit
)
