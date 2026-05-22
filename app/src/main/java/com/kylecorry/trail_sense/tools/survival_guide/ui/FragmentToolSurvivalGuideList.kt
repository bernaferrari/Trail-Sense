package com.kylecorry.trail_sense.tools.survival_guide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.kylecorry.trail_sense.tools.survival_guide.domain.SurvivalGuideRepo

@Composable
fun Fragment.SurvivalGuideListContent(modifier: Modifier = Modifier) {
    val context = requireContext()
    val navController = findNavController()
    val repo = remember { SurvivalGuideRepo.getInstance(context) }

    var chapters by remember { mutableStateOf<List<com.kylecorry.trail_sense.tools.survival_guide.domain.SurvivalGuideSection>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        chapters = repo.getChapters()
    }

    val filtered = chapters.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
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
                            putLong("chapter_id", chapter.id)
                        }
                        navController.navigate(R.id.survivalGuideReaderFragment, bundle)
                    }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(chapter.name, style = MaterialTheme.typography.titleMedium)
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
        }

        useSearch(searchView, setQuery)

        useEffect(searchView, searchArgumentQuery) {
            if (searchArgumentQuery.isNotEmpty()) {
                searchView.query = searchArgumentQuery
                searchView.setCursorPosition(searchArgumentQuery.length)
            }
        }

        listView.emptyView = emptyTextView

        val listItems = useMemo(navController, chapters, query, searchResults, markdown) {
            if (query.isBlank() || searchResults.isEmpty()) {
                chapters.map {
                    ListItem(
                        it.chapter.resource.toLong(),
                        it.chapter.title,
                        it.sections.firstOrNull()?.summary,
                        icon = ResourceListIcon(
                            it.chapter.icon,
                            Resources.androidTextColorSecondary(requireContext())
                        )
                    ) {
                        navController.navigateWithAnimation(
                            R.id.fragmentToolSurvivalGuideReader,
                            Bundle().apply {
                                putInt("chapter_resource_id", it.chapter.resource)
                            }
                        )
                    }
                }
            } else {
                val textColor = Resources.androidTextColorSecondary(requireContext())
                searchResults.map {
                    ListItem(
                        it.chapter.resource.toLong(),
                        it.heading ?: getString(R.string.overview),
                        it.summary,
                        icon = ResourceListIcon(it.chapter.icon, textColor),
                        tags = listOf(
                            ListItemTag(
                                it.chapter.title,
                                ResourceListIcon(it.chapter.icon, size = 12f),
                                textColor
                            )
                        )
                    ) {
                        // TODO: Scroll to the heading
                        navController.navigateWithAnimation(
                            R.id.fragmentToolSurvivalGuideReader,
                            Bundle().apply {
                                putInt("chapter_resource_id", it.chapter.resource)
                                putInt("header_index", it.headingIndex)
                            }
                        )
                    }
                }
            }
        }

        useEffect(listView, listItems) {
            listView.setItems(listItems)
        }

        useEffect(
            summaryHolderView,
            summaryTitleView,
            summaryView,
            summaryChapterBadgeView,
            summary,
            markdown,
            searchResults
        ) {
            summaryHolderView.isVisible = summary.isNotBlank()
            markdown.setMarkdown(summaryView, summary)
            val result = searchResults.firstOrNull()
            if (result != null) {
                summaryTitleView.text = listOfNotNull(
                    result.heading,
                    if (SurvivalGuideSearch.shouldUseSubsection(result)) result.bestSubsection?.heading else null
                ).joinToString(" > ")
                summaryChapterBadgeView.setStatusText(result.chapter.title)
                ResourceListIcon(
                    result.chapter.icon,
                    size = 12f
                ).apply(summaryChapterBadgeView.statusImage)
                val backgroundColor = Resources.androidTextColorSecondary(context)
                summaryChapterBadgeView.setBackgroundTint(backgroundColor)
                summaryChapterBadgeView.setForegroundTint(
                    Colors.mostContrastingColor(
                        Color.WHITE,
                        Color.BLACK,
                        backgroundColor
                    )
                )
                summaryTitleView.setCompoundDrawables(
                    size = Resources.dp(context, 16f).toInt(),
                    right = R.drawable.ic_keyboard_arrow_right
                )
                summaryHolderView.setOnClickListener {
                    navController.navigateWithAnimation(
                        R.id.fragmentToolSurvivalGuideReader,
                        Bundle().apply {
                            putInt("chapter_resource_id", result.chapter.resource)
                            putInt("header_index", result.headingIndex)
                        }
                    )
                }
            }
        }

        useEffect(summaryScrollView, summary) {
            summaryScrollView.scrollTo(0, 0)
        }
    }
}
