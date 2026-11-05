package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn

/** Keeps the screen awake while this is composed and [enabled] (a level, a timer, a light). */
@Composable
fun KeepScreenOn(enabled: Boolean = true) {
    if (enabled) {
        Spacer(Modifier.keepScreenOn())
    }
}
