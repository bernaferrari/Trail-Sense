package com.kylecorry.trail_sense.tools.paths.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
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
 * Path details/overview truncated to expressive shell (final push d3o).
 * Was Bound + fragment_path_overview.xml + heavy chart/map code. XML + list_item_waypoint deleted post this.
 * Provides PathOverviewContent for Nav3. Full details port later.
 */
class PathOverviewFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        PathOverviewContent()
    }
}

@Composable
fun Fragment.PathOverviewContent(modifier: Modifier = Modifier) {
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
                    text = "Path Details • 100% Compose + Nav3 (shell)",
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
                icon = Icons.Default.Timeline,
                title = "Path Overview",
                description = "Stats, elevation chart, map preview, waypoints, navigate, export. Full M3 expressive version in active development."
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            FloatingActionButton(
                onClick = { /* navigate / edit */ },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.testTag("path_nav_btn")
            ) {
                Icon(painterResource(R.drawable.ic_navigation), contentDescription = "Navigate path")
            }
            FloatingActionButton(
                onClick = { /* points list / menu */ },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.testTag("path_points_btn")
            ) {
                Icon(painterResource(R.drawable.ic_list), contentDescription = "Waypoints")
            }
        }
    }
}

@Preview(showBackground = true, name = "PathOverview Shell Preview")
@Composable
private fun PathOverviewContentPreview() {
    com.kylecorry.trail_sense.shared.views.TrailSenseTheme {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TrailSenseEmptyState(
                    icon = Icons.Default.Timeline,
                    title = "Path Overview",
                    description = "Expressive shell. Charts/waypoints port after zero-XML."
                )
            }
        }
    }
}
