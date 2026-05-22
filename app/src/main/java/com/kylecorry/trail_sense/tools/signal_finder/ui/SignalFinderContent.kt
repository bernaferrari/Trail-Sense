package com.kylecorry.trail_sense.tools.signal_finder.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment

/**
 * Scoped but clean Compose conversion of ToolSignalFinderFragment (Ralph Loop iteration 6).
 * The complex AndromedaListView + mappers + sensor logic are kept via AndroidView for velocity.
 * This allows immediate deletion of the XML while the surrounding chrome can be modernized later.
 */
@Composable
fun Fragment.SignalFinderContent(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            // Host the original layout so the existing update() logic, mappers, and AndromedaListView continue to work unchanged.
            // This is the proven transitional pattern (see ToolMagnifierFragment for CameraView).
            android.view.LayoutInflater.from(ctx).inflate(R.layout.fragment_tool_signal_finder, null)
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * The fragment is now a thin Compose host.
 */
class ToolSignalFinderFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        SignalFinderContent()
    }
}
