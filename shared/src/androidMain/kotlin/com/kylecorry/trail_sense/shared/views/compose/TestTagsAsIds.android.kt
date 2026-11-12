package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

actual fun Modifier.testTagsAsIds(): Modifier = semantics { testTagsAsResourceId = true }
