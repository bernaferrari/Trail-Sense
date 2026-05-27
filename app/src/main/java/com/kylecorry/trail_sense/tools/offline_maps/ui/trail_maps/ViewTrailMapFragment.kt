package com.kylecorry.trail_sense.tools.offline_maps.ui.trail_maps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState

@Composable
fun Fragment.VectorMapContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        TrailSenseEmptyState(
            title = "Vector Map",
            message = "Vector map viewer is being migrated to the new modern Compose + design system experience."
        )
    }
}

class ViewVectorMapFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        VectorMapContent()
    }
}
