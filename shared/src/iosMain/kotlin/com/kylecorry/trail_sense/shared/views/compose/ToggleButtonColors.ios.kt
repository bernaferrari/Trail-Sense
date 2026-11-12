package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun toggleButtonColors(
    containerColor: Color,
    contentColor: Color,
    checkedContainerColor: Color,
    checkedContentColor: Color
): ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(
    containerColor = containerColor,
    contentColor = contentColor,
    checkedContainerColor = checkedContainerColor,
    checkedContentColor = checkedContentColor
)
