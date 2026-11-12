package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.material3.ToggleButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Toggle button colors. Android's Material 3 calls this ToggleButtonDefaults.colors and Compose Multiplatform's
 * calls it ToggleButtonDefaults.toggleButtonColors, so each platform picks its own.
 */
@Composable
expect fun toggleButtonColors(
    containerColor: Color,
    contentColor: Color,
    checkedContainerColor: Color,
    checkedContentColor: Color
): ToggleButtonColors
