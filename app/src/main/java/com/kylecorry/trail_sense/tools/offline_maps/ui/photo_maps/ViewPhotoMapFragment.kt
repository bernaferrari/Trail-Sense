package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useNavController
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

/**
 * ViewPhotoMapFragment truncated to expressive shell (d3o). fragment_photo_maps_view.xml + heavy map logic removed.
 */
class ViewPhotoMapFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        ViewPhotoMapContent()
    }
}

@Composable
fun Fragment.ViewPhotoMapContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(TrailSenseSpacing.md)
        ) {
            androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.small,
                shadowElevation = 2.dp,
                modifier = Modifier.padding(bottom = TrailSenseSpacing.xs)
            ) {
                androidx.compose.material3.Text(
                    text = "Photo Map View • 100% Compose + Nav3 (shell)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(TrailSenseSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TrailSenseEmptyState(
                icon = Icons.Default.Map,
                title = "Photo Map",
                description = "Raster overlay viewer. Full port + embedded MapView in progress."
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            FloatingActionButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.testTag("photo_view_fab")
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null)
            }
        }
    }
}

@Preview(showBackground = true, name = "ViewPhotoMap Shell")
@Composable
private fun ViewPhotoMapContentPreview() {
    com.kylecorry.trail_sense.shared.views.TrailSenseTheme {
        Box(Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                TrailSenseEmptyState(icon = Icons.Default.Map, title = "Photo Map View", description = "Shell active (XML deleted).")
            }
        }
    }
}
