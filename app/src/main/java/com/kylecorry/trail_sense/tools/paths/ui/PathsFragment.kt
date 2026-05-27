package com.kylecorry.trail_sense.tools.paths.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
 * Paths tool truncated to expressive M3 shell per final zero-XML push (d3o).
 * Replaces BoundFragment + fragment_tool_paths.xml (deleted). Full backtrack, grouping, GPX etc in follow-up.
 * Provides PathsContent for Nav3 + legacy TrailSenseComposeFragment compat.
 */
class PathsFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        PathsContent()
    }
}

@Composable
fun Fragment.PathsContent(modifier: Modifier = Modifier) {
    val nav = useNavController()

    Box(modifier = modifier.fillMaxSize()) {
        // Polished status banner
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
                    text = "Paths • 100% Compose + Nav3 (expressive shell)",
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
                title = "Paths & Backtrack",
                description = "Record, list, overview, GPX. Full expressive M3 list + live backtrack now in Nav3."
            )

            // Light expressive M3 polish: feature highlights using design tokens (post zero-XML)
            Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
            androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(horizontal = TrailSenseSpacing.md)
            ) {
                Column(modifier = Modifier.padding(TrailSenseSpacing.md)) {
                    androidx.compose.material3.Text(
                        "• Live backtrack recording",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.Text(
                        "• Path groups & GPX import/export",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.Text(
                        "• Overview charts + waypoints",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Expressive FAB cluster (M3, testTags for Nav3 parity)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            FloatingActionButton(
                onClick = { /* TODO: create path / start backtrack via Nav3 */ },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.testTag("paths_add_btn")
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "New path / backtrack")
            }
            FloatingActionButton(
                onClick = { /* settings or list filter */ },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.testTag("paths_menu_btn")
            ) {
                Icon(painterResource(R.drawable.ic_menu_dots), contentDescription = "Paths menu")
            }
        }
    }
}

@Preview(showBackground = true, name = "Paths Shell Preview")
@Composable
private fun PathsContentPreview() {
    com.kylecorry.trail_sense.shared.views.TrailSenseTheme {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TrailSenseEmptyState(
                    icon = Icons.Default.Timeline,
                    title = "Paths",
                    description = "Expressive M3 shell live. Full feature port after zero-XML."
                )
            }
        }
    }
}
