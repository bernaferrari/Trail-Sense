package com.kylecorry.trail_sense.tools.field_guide.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment

/**
 * Scoped Compose conversion of SightingListFragment (Ralph Loop iteration 10).
 * The complex sightings list is kept via AndroidView for velocity.
 * This allows immediate XML deletion while the surrounding chrome can be modernized later.
 */
@Composable
fun Fragment.SightingListContent(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            android.view.LayoutInflater.from(ctx).inflate(R.layout.fragment_sightings_list, null)
        },
        modifier = modifier.fillMaxSize()
    )
}

class SightingListFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        SightingListContent()
    }
}
