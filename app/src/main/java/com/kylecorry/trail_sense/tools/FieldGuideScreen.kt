package com.kylecorry.trail_sense.tools.field_guide.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pure Compose Field Guide entry (design system ready).
 * Full implementation in progress as part of 100% Compose migration.
 */
@Composable
fun FieldGuideScreen(modifier: Modifier = Modifier) {
    Text(
        "Field Guide (pure Compose + TrailSense design system)",
        modifier = modifier.padding(16.dp)
    )
}
