package com.kylecorry.trail_sense.tools.offline_maps.ui.trail_maps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment

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
