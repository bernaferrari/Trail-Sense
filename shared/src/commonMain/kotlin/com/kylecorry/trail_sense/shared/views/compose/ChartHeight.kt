package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Windows at least this tall get the taller chart. */
private val TallWindowHeight = 600.dp

/** The height of a full-width chart: 150 dp, or 200 dp once the window is at least 600 dp tall. */
@Composable
fun chartHeight(): Dp {
    val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    return chartHeightFor(windowHeight)
}

internal fun chartHeightFor(windowHeight: Dp): Dp {
    return if (windowHeight >= TallWindowHeight) 200.dp else 150.dp
}
