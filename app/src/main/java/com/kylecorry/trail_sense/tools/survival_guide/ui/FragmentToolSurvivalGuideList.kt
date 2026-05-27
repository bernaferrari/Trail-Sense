package com.kylecorry.trail_sense.tools.survival_guide.ui

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
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters

@Composable
fun Fragment.SurvivalGuideListContent(modifier: Modifier = Modifier) {
    val context = requireContext()
    val navController = findNavController()

    val allChapters = remember { Chapters.getChapters(context) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = allChapters.filter {
        searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = modifier.fillMaxSize()) {
        TrailSenseSearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search survival guide..."
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(filtered) { chapter ->
                TrailSenseCard(
                    onClick = {
                        val bundle = Bundle().apply {
                            putInt("chapter_resource_id", chapter.resource)
                        }
                        navController.navigate(R.id.fragmentToolSurvivalGuideReader, bundle)
                    }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(chapter.title, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

class FragmentToolSurvivalGuideList : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        SurvivalGuideListContent()
    }
}
