package com.kylecorry.trail_sense.tools.field_guide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChip
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Pure Compose expressive Sighting list screen for real Nav3Host entries (and legacy fragment compat).
 * Promotes the Field Guide family to 100% Compose + design system. Demo data for quick visible win;
 * swap to FieldGuideRepo + real Sighting loading + full actions (map, photo, edit) in next wave.
 * Heavy use of TrailSense* (SearchBar, ListItem, Card, Chip, SectionHeader, EmptyState, Spacing).
 * testTags + preview per SKILL.md.
 */
@Composable
fun SightingListScreen(
    pageId: Long,
    onCreateSighting: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showHarvestedOnly by remember { mutableStateOf(false) }

    // Demo sightings (expressive placeholder — replace with live repo query + Sighting domain in follow-up)
    val demoSightings = remember {
        listOf(
            DemoSighting(1L, "Mossy Oak", Instant.now().minusSeconds(86400 * 2), "47.6 N, 122.3 W", harvested = true, notes = "Great find near stream"),
            DemoSighting(2L, "Chanterelle Cluster", Instant.now().minusSeconds(86400 * 5), "47.8 N, 122.1 W", harvested = false, notes = "Under douglas fir"),
            DemoSighting(3L, "Granite Outcrop", Instant.now().minusSeconds(86400 * 12), "48.1 N, 121.9 W", harvested = false, notes = "Interesting vein"),
        )
    }

    val filtered = remember(searchQuery, showHarvestedOnly, demoSightings) {
        demoSightings.filter { s ->
            val matchesSearch = searchQuery.isBlank() ||
                s.name.contains(searchQuery, ignoreCase = true) ||
                (s.notes?.contains(searchQuery, ignoreCase = true) == true) ||
                s.location.contains(searchQuery, ignoreCase = true)
            val matchesFilter = !showHarvestedOnly || s.harvested
            matchesSearch && matchesFilter
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TrailSenseSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = stringResource(R.string.search),
                modifier = Modifier
                    .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.sm)
                    .testTag("sighting_search")
            )

            // Filter row — expressive chips (harvested toggle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
            ) {
                TrailSenseChip(
                    text = "Harvested only",
                    selected = showHarvestedOnly,
                    onClick = { showHarvestedOnly = !showHarvestedOnly },
                    modifier = Modifier.testTag("filter_harvested")
                )
                if (pageId != 0L) {
                    Text(
                        "Page #$pageId",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            if (filtered.isEmpty()) {
                TrailSenseEmptyState(
                    title = "No sightings",
                    message = if (searchQuery.isNotBlank() || showHarvestedOnly) "Try adjusting your search or filters." else "Record your first sighting for this page using the + button.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(TrailSenseSpacing.lg)
                        .testTag("empty_state")
                )
            } else {
                TrailSenseSectionHeader(
                    title = "Sightings (${filtered.size})",
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.xs)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = TrailSenseSpacing.lg,
                        vertical = TrailSenseSpacing.sm
                    ),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
                ) {
                    items(filtered, key = { it.id }) { sighting ->
                        TrailSenseListItem(
                            title = sighting.name,
                            subtitle = formatSightingMeta(sighting),
                            icon = Icons.Default.LocationOn,
                            modifier = Modifier.testTag("sighting_row_${sighting.id}"),
                            onClick = {
                                // Future: navigate to sighting detail or edit (real Nav3)
                            },
                            trailing = {
                                if (sighting.harvested) {
                                    TrailSenseChip(text = "Harvested", selected = true, onClick = {})
                                }
                            }
                        )
                        if (sighting.notes?.isNotBlank() == true) {
                            TrailSenseCard(
                                modifier = Modifier
                                    .padding(start = 56.dp, end = 0.dp, top = 0.dp, bottom = TrailSenseSpacing.sm)
                                    .testTag("sighting_notes_${sighting.id}")
                            ) {
                                Text(
                                    sighting.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(TrailSenseSpacing.md)
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onCreateSighting,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(TrailSenseSpacing.lg)
                .testTag("add_sighting_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create))
        }
    }
}

@Composable
fun Fragment.SightingListContent(modifier: Modifier = Modifier) {
    val navController = findNavController()
    val pageId = remember { arguments?.getLong("page_id", 0L) ?: 0L }

    SightingListScreen(
        pageId = pageId,
        onCreateSighting = {
            // Legacy nav kept for compatibility during hybrid migration (promote with CreateFieldGuideSighting route)
            val bundle = android.os.Bundle().apply {
                putLong("page_id", pageId)
            }
            navController.navigate(R.id.createFieldGuideSightingFragment, bundle)
        },
        modifier = modifier
    )
}

private data class DemoSighting(
    val id: Long,
    val name: String,
    val time: Instant?,
    val location: String,
    val harvested: Boolean = false,
    val notes: String? = null
)

private fun formatSightingMeta(s: DemoSighting): String {
    val date = s.time?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "Unknown date"
    return "$date • ${s.location}"
}

@Preview(showBackground = true)
@Composable
private fun SightingListScreenPreview() {
    MaterialTheme {
        SightingListScreen(pageId = 42L)
    }
}

class SightingListFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        SightingListContent()
    }
}
