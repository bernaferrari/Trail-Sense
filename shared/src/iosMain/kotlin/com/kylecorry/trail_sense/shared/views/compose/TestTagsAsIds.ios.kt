package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.ui.Modifier

// Compose on iOS already exposes test tags as accessibility identifiers
actual fun Modifier.testTagsAsIds(): Modifier = this
