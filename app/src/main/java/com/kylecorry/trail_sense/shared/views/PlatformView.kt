package com.kylecorry.trail_sense.shared.views

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun <T : View> PlatformView(
    factory: () -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = {}
) {
    AndroidView(
        factory = { factory() },
        modifier = modifier,
        update = update
    )
}
