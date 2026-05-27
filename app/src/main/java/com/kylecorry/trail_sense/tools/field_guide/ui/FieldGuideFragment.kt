package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
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
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChip
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo

@Composable
fun Fragment.FieldGuideContent(modifier: Modifier = Modifier) {
    val context = requireContext()
    val navController = findNavController()

    val repo = remember { FieldGuideRepo.getInstance(context) }

    var pages by remember { mutableStateOf<List<FieldGuidePage>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        pages = repo.getAllPages().sortedBy { it.name }
    }

    val filteredPages = remember(pages, searchQuery, selectedCategory) {
        pages.filter { page ->
            val matchesSearch = searchQuery.isBlank() ||
                page.name.contains(searchQuery, ignoreCase = true) ||
                (page.notes?.contains(searchQuery, ignoreCase = true) == true)

            val matchesCategory = selectedCategory == null ||
                page.tags.any { tag ->
                    tag.name.equals(selectedCategory, ignoreCase = true) ||
                    tag.name.contains(selectedCategory ?: "", ignoreCase = true)
                }

            matchesSearch && matchesCategory
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TrailSenseSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search field guide...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val categories = listOf("Plant", "Animal", "Rock", "Weather", "Other")
            // Simple category filter row using chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    TrailSenseChip(
                        text = cat,
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = if (selectedCategory == cat) null else cat
                        }
                    )
                }
            }

            if (filteredPages.isEmpty()) {
                Text(
                    "No pages found",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(filteredPages) { page ->
                        TrailSenseCard(
                            onClick = {
                                val bundle = Bundle().apply {
                                    putLong("page_id", page.id)
                                }
                                navController.navigate(R.id.fieldGuidePageFragment, bundle)
                            }
                        ) {
                            Column {
                                Text(page.name, style = MaterialTheme.typography.titleMedium)
                                val preview = page.notes?.take(120)?.let {
                                    if ((page.notes?.length ?: 0) > 120) "$it..." else it
                                } ?: ""
                                if (preview.isNotBlank()) {
                                    Text(
                                        preview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                navController.navigate(R.id.createFieldGuidePageFragment)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add page")
        }
    }
}

class FieldGuideFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        FieldGuideContent()
    }
}

