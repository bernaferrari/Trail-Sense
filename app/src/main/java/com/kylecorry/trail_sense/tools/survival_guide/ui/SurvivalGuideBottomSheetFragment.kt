package com.kylecorry.trail_sense.tools.survival_guide.ui

import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearch
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearchResult
import kotlinx.coroutines.delay

/**
 * Modern Compose port of the survival guide quick search bottom sheet.
 * Expressive search + summary preview card using design system components.
 * Clicking summary navigates to reader (same behavior).
 */
@Composable
fun Fragment.SurvivalGuideBottomSheetContent(modifier: Modifier = Modifier) {
    val context = requireContext()
    val navController = useNavController()

    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SurvivalGuideSearchResult>>(emptyList()) }
    var summary by remember { mutableStateOf("") }

    val searchService = remember { SurvivalGuideSearch(context) }

    // Debounced search effect (ported from old reactive hook)
    LaunchedEffect(query) {
        delay(200)
        if (query.isBlank()) {
            searchResults = emptyList()
            summary = ""
        } else {
            val results = onIO { searchService.search(query) }
            searchResults = results
            val first = results.firstOrNull()
            summary = if (first != null && first.score >= 0.5f) {
                onIO { searchService.getSummary(query, first) }
            } else {
                ""
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header row with title + action to full list (replaces old toolbar right button)
        TrailSenseSectionHeader(
            title = context.getString(R.string.survival_guide),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            // The right icon action is handled by parent in old; here we embed simple text action or rely on caller
        }

        TrailSenseSearchBar(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search survival guide...",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        val topResult = searchResults.firstOrNull()

        if (summary.isNotBlank() && topResult != null) {
            val displayTitle = listOfNotNull(
                topResult.heading,
                if (SurvivalGuideSearch.shouldUseSubsection(topResult)) topResult.bestSubsection?.heading else null
            ).joinToString(" > ")

            TrailSenseCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .clickable {
                        // navigate to reader and dismiss via activity? (bottom sheet host will handle)
                        navController.navigateWithAnimation(
                            R.id.fragmentToolSurvivalGuideReader,
                            Bundle().apply {
                                putInt("chapter_resource_id", topResult.chapter.resource)
                                putInt("header_index", topResult.headingIndex)
                            }
                        )
                    }
            ) {
                Column(Modifier.padding(12.dp)) {
                    // Chapter badge as simple text chip
                    Text(
                        text = topResult.chapter.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Text(
                text = context.getString(R.string.survival_guide_empty_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Subtle footer action to open full list (expressive touch target)
        Text(
            text = "Open full Survival Guide →",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable {
                    navController.navigateWithAnimation(
                        R.id.fragmentToolSurvivalGuideList,
                        Bundle().apply { putString("search_query", query) }
                    )
                }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SurvivalGuideBottomSheetContentPreview() {
    MaterialTheme {
        // Fragment.SurvivalGuideBottomSheetContent not directly previewable without context; structure demo
        Column(Modifier.padding(8.dp)) {
            TrailSenseSearchBar("fire", {}, "Search survival guide...")
            Spacer(Modifier.height(8.dp))
            TrailSenseCard {
                Column(Modifier.padding(12.dp)) {
                    Text("Chapter • Fire", style = MaterialTheme.typography.labelSmall)
                    Text("Making fire in wet conditions > Flint", style = MaterialTheme.typography.titleMedium)
                    Text("Strike the flint... (preview summary)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

class SurvivalGuideBottomSheetFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        SurvivalGuideBottomSheetContent()
    }
}
