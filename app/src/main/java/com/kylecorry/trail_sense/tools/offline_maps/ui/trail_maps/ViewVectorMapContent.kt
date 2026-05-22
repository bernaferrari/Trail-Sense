package com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment

/**
 * Scoped Compose conversion of ViewVectorMapFragment (Ralph Loop iteration 8).
 * The complex Mapsforge-based map view is kept via AndroidView for velocity.
 * This allows immediate XML deletion while the surrounding chrome can be modernized later.
 */
@Composable
fun Fragment.ViewVectorMapContent(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            android.view.LayoutInflater.from(ctx).inflate(R.layout.fragment_offline_map_view, null)
        },
        modifier = modifier.fillMaxSize()
    )
}

class ViewVectorMapFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        ViewVectorMapContent()
    }
}
