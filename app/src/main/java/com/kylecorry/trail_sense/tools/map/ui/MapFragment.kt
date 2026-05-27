package com.kylecorry.trail_sense.tools.map.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar

@Composable
fun Fragment.MapContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        // Placeholder modern UI while full map migration continues
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TrailSenseEmptyState(
                title = "Map",
                message = "Full map view is being migrated to the new modern experience.\n\nCore navigation tools (Whistle, Field Guide, etc.) are available now via the new architecture."
            )
        }

        // Minimal floating controls using design system
        FloatingActionButton(
            onClick = { /* TODO: wire location in full migration */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(painterResource(R.drawable.ic_location), contentDescription = "My location")
        }
    }
}

class MapFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        MapContent()
    }
}

