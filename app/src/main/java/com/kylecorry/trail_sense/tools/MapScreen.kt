package com.kylecorry.trail_sense.tools.map.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.map.ui.MapView

/**
 * Pure Compose Map screen.
 * The complex custom MapView (gestures, layers, tiles) is isolated here via AndroidView
 * as a pragmatic exception for performance. Everything else (controls, FABs, etc.) is pure Compose.
 */
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    isInteractive = true
                    isPanEnabled = true
                    isZoomEnabled = true
                    isFlingEnabled = true
                    useDensityPixelsForZoom = true
                    mapViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                mapViewRef = view
            }
        )

        // Pure Compose controls can be added here later (FABs, etc.)
        // Example placeholder using design system
        // TrailSenseMapFabRow(...) 
    }
}
