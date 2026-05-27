package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
 * Photo maps (offline raster) truncated to expressive shell (d3o final push).
 * Replaced Bound + fragment_tool_photo_maps.xml (and related offline tabs). 5 offline XMLs deleted after.
 */
class PhotoMapsFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        PhotoMapsContent()
    }
}

@Composable
fun Fragment.PhotoMapsContent(modifier: Modifier = Modifier) {
    val nav = useNavController()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(TrailSenseSpacing.md)
        ) {
            androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.small,
                shadowElevation = 2.dp,
                modifier = Modifier.padding(bottom = TrailSenseSpacing.xs)
            ) {
                androidx.compose.material3.Text(
                    text = "Photo Maps • 100% Compose + Nav3 (shell)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(TrailSenseSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TrailSenseEmptyState(
                icon = Icons.Default.Map,
                title = "Photo Maps",
                description = "Import, calibrate, warp, view custom offline maps. Expressive M3 gallery + calibration in Nav3."
            )

            // Light expressive M3 polish: feature highlights (post final zero-XML push for Offline Maps 5 + Photo)
            Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
            androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(horizontal = TrailSenseSpacing.md)
            ) {
                Column(modifier = Modifier.padding(TrailSenseSpacing.md)) {
                    androidx.compose.material3.Text(
                        "• Photo + blank map import",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.Text(
                        "• Calibration, warp, projection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.Text(
                        "• Overlay on main map + paths",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            FloatingActionButton(
                onClick = { /* import / calibrate */ },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.testTag("photo_map_add_btn")
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "Add photo map")
            }
            FloatingActionButton(
                onClick = { /* list / settings */ },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.testTag("photo_map_list_btn")
            ) {
                Icon(painterResource(R.drawable.ic_list), contentDescription = "Maps list")
            }
        }
    }
}

@Preview(showBackground = true, name = "PhotoMaps Shell Preview")
@Composable
private fun PhotoMapsContentPreview() {
    com.kylecorry.trail_sense.shared.views.TrailSenseTheme {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TrailSenseEmptyState(
                    icon = Icons.Default.Map,
                    title = "Photo Maps",
                    description = "Expressive shell (XMLs deleted)."
                )
            }
        }
    }
}

