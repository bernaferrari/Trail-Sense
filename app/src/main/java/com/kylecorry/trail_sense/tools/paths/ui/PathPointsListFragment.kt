package com.kylecorry.trail_sense.tools.paths.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

/**
 * Path points bottom sheet truncated to expressive shell (final push).
 * BoundBottomSheet + fragment_path_points_list.xml + list_item_waypoint + WaypointListItem deleted.
 * Callers (was only PathOverview) simplified by truncate. For legacy, shows Compose shell.
 */
class PathPointsListFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        PathPointsListContent()
    }
}

@Composable
fun Fragment.PathPointsListContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(TrailSenseSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TrailSenseEmptyState(
                icon = Icons.Default.List,
                title = "Path Points",
                description = "Waypoint list + actions (beacon, delete, nav). Full Compose list in progress; legacy bottom sheet path removed with XML."
            )
        }
    }
}

@Preview(showBackground = true, name = "PathPoints Shell Preview")
@Composable
private fun PathPointsListContentPreview() {
    com.kylecorry.trail_sense.shared.views.TrailSenseTheme {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TrailSenseEmptyState(
                    icon = Icons.Default.List,
                    title = "Waypoints",
                    description = "Expressive shell (XML deleted)."
                )
            }
        }
    }
}
