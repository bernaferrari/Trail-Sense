package com.kylecorry.andromeda.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Draw with Compose's multiplatform graphics and coordinate APIs. */
@Composable
fun Drawing(modifier: Modifier, draw: DrawScope.() -> Unit) {
    Canvas(modifier, onDraw = draw)
}
