package com.kylecorry.trail_sense.tools.guide.ui

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.tools.guide.domain.UserGuideCategory
import com.kylecorry.trail_sense.tools.guide.infrastructure.Guides

/**
 * Modern Compose port of the Guides list.
 * Inlines search + categorized list (no more child PreferenceFragment or XML container).
 * Expressive cards + search using design system. Navigates using legacy action for compatibility.
 */
@Composable
fun Fragment.GuideListContent(modifier: Modifier = Modifier) {
    val context = requireContext()
    val navController = findNavController()

    val allGuides = remember { Guides.guides(context) }

    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(searchQuery, allGuides) {
        if (searchQuery.isBlank()) {
            allGuides
        } else {
            allGuides.mapNotNull { category ->
                val matching = category.guides.filter { it.name.contains(searchQuery, ignoreCase = true) }
                if (matching.isNotEmpty()) {
                    UserGuideCategory(category.name, matching)
                } else if (category.name.contains(searchQuery, ignoreCase = true)) {
                    category
                } else null
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TrailSenseSearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search guides...",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            filtered.forEach { category ->
                item {
                    TrailSenseSectionHeader(title = category.name)
                }
                items(category.guides) { guide ->
                    TrailSenseCard(
                        onClick = {
                            navController.navigate(
                                R.id.action_guideListFragment_to_guideFragment,
                                Bundle().apply {
                                    putString("guide_name", guide.name)
                                    putInt("guide_contents", guide.contents)
                                }
                            )
                        }
                    ) {
                        Column(Modifier.padding(4.dp)) {
                            Text(guide.name, style = MaterialTheme.typography.titleMedium)
                            if (guide.description != null) {
                                Text(
                                    guide.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        "No guides match your search.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

class GuideListFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        GuideListContent()
    }
}
