package com.kylecorry.trail_sense.tools.guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide
import com.kylecorry.trail_sense.tools.guide.domain.UserGuideCategory
import com.kylecorry.trail_sense.tools.guide.infrastructure.Guides

class GuideListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    GuideListScreen(
                        guides = Guides.guides(requireContext()),
                        onOpenGuide = {
                            tryOrNothing {
                                findNavController().navigate(
                                    R.id.action_guideListFragment_to_guideFragment,
                                    Bundle().apply {
                                        putString("guide_name", it.name)
                                        putInt("guide_contents", it.contents)
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideListScreen(
    guides: List<UserGuideCategory>,
    onOpenGuide: (UserGuide) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(guides, query) {
        filterGuides(guides, query)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search)) }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                filtered.forEach { category ->
                    item(key = "category-${category.name}") {
                        Text(
                            text = category.name,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                top = 16.dp,
                                end = 16.dp,
                                bottom = 8.dp
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(
                        items = category.guides,
                        key = { "${category.name}-${it.name}" }
                    ) { guide ->
                        GuideRow(guide = guide, onClick = { onOpenGuide(guide) })
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun GuideRow(guide: UserGuide, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = guide.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        guide.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider()
}

private fun filterGuides(
    guides: List<UserGuideCategory>,
    query: String
): List<UserGuideCategory> {
    if (query.isBlank()) {
        return guides
    }

    return guides.mapNotNull { category ->
        if (category.name.contains(query, ignoreCase = true)) {
            category
        } else {
            val matchingGuides = category.guides.filter {
                it.name.contains(query, ignoreCase = true)
            }
            if (matchingGuides.isEmpty()) {
                null
            } else {
                UserGuideCategory(category.name, matchingGuides)
            }
        }
    }
}
