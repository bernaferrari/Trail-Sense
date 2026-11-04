package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * Pads the bottom by only the part of the open keyboard that overlaps this element. Whatever already sits below it
 * (the bottom navigation bar, the gesture inset) is not counted again, and a closed keyboard adds nothing. Use it
 * instead of `imePadding()` on screens inside the main navigation, where `imePadding()` doubles the gap.
 */
fun Modifier.trailSenseImePadding(): Modifier = composed {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val windowHeight = LocalWindowInfo.current.containerSize.height.toFloat()
    var outerBottom by remember { mutableFloatStateOf(windowHeight) }
    val below = (windowHeight - outerBottom).coerceAtLeast(0f)
    val overlap = with(density) { (imeBottom - below).coerceAtLeast(0f).toDp() }
    this
        .onGloballyPositioned { outerBottom = it.boundsInWindow().bottom }
        .padding(bottom = overlap)
}
