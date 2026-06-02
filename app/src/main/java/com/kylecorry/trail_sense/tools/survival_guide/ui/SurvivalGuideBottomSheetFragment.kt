package com.kylecorry.trail_sense.tools.survival_guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TextBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearch
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SurvivalGuideBottomSheetFragment : BottomSheetDialogFragment() {

    private var query by mutableStateOf("")
    private var searchResults by mutableStateOf(emptyList<SurvivalGuideSearchResult>())
    private var summary by mutableStateOf("")
    private var searchJob: Job? = null

    private val markdown by lazy { AppServiceRegistry.get<MarkdownService>() }
    private val search by lazy { SurvivalGuideSearch(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    SurvivalGuideSheetContent(
                        query = query,
                        summary = summary,
                        firstResult = searchResults.firstOrNull(),
                        markdown = markdown,
                        onQueryChange = ::updateQuery,
                        onOpenSearchList = ::openSearchList,
                        onSummaryClick = ::openSummary,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        super.onDestroyView()
    }

    private fun updateQuery(value: String) {
        if (query == value) {
            return
        }
        query = value
        search(value)
    }

    private fun search(value: String) {
        searchJob?.cancel()
        if (value.isBlank()) {
            searchResults = emptyList()
            summary = ""
            return
        }

        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(200)
            val (results, newSummary) = onDefault {
                val results = search.search(value)
                val firstResult = results.firstOrNull()
                val summary = if (firstResult != null && firstResult.score >= 0.5f) {
                    search.getSummary(value, firstResult)
                } else {
                    ""
                }
                results to summary
            }

            if (query != value) {
                return@launch
            }

            searchResults = results
            summary = newSummary
        }
    }

    private fun openSearchList() {
        dismiss()
        findNavController().navigateWithAnimation(
            R.id.fragmentToolSurvivalGuideList,
            Bundle().apply {
                putString("search_query", query)
            }
        )
    }

    private fun openSummary(result: SurvivalGuideSearchResult) {
        dismiss()
        findNavController().navigateWithAnimation(
            R.id.fragmentToolSurvivalGuideReader,
            Bundle().apply {
                putInt("chapter_resource_id", result.chapter.resource)
                putInt("header_index", result.headingIndex)
            }
        )
    }
}

@Composable
private fun SurvivalGuideSheetContent(
    query: String,
    summary: String,
    firstResult: SurvivalGuideSearchResult?,
    markdown: MarkdownService,
    onQueryChange: (String) -> Unit,
    onOpenSearchList: () -> Unit,
    onSummaryClick: (SurvivalGuideSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SurvivalGuideToolbar(
                onOpenSearchList = onOpenSearchList,
                modifier = Modifier.fillMaxWidth()
            )
            SurvivalGuideSearchBox(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.lg)
                    .padding(bottom = TrailSenseSpacing.sm)
            )

            if (summary.isBlank()) {
                Text(
                    text = stringResource(R.string.survival_guide_empty_desc),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = TrailSenseSpacing.xl, vertical = TrailSenseSpacing.md)
                        .testTag("empty_view_description"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.weight(1f))
            } else if (firstResult != null) {
                SurvivalGuideSummary(
                    summary = summary,
                    result = firstResult,
                    markdown = markdown,
                    onSummaryClick = onSummaryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(horizontal = TrailSenseSpacing.sm)
                        .padding(top = TrailSenseSpacing.md)
                )
            }

            Spacer(Modifier.height(TrailSenseSpacing.md))
        }
    }
}

@Composable
private fun SurvivalGuideToolbar(
    onOpenSearchList: () -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.testTag("title"),
        factory = { context ->
            Toolbar(context, null).apply {
                id = R.id.title
                title.text = context.getString(R.string.survival_guide)
                subtitle.isVisible = false
                rightButton.setImageResource(R.drawable.survival_guide)
                rightButton.isVisible = true
                rightButton.setOnClickListener { onOpenSearchList() }
            }
        },
        update = {
            it.rightButton.setOnClickListener { onOpenSearchList() }
        }
    )
}

@Composable
private fun SurvivalGuideSearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.testTag("search"),
        factory = { context ->
            SearchView(context, null).apply {
                id = R.id.search
                setOnSearchListener(onQueryChange)
            }
        },
        update = { searchView ->
            searchView.setOnSearchListener(onQueryChange)
            if (searchView.query != query) {
                searchView.query = query
                searchView.setCursorPosition(query.length)
            }
        }
    )
}

@Composable
private fun SurvivalGuideSummary(
    summary: String,
    result: SurvivalGuideSearchResult,
    markdown: MarkdownService,
    onSummaryClick: (SurvivalGuideSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    TrailSenseCard(
        modifier = modifier.testTag("summary_holder"),
        onClick = { onSummaryClick(result) }
    ) {
        TextBadge(
            text = result.chapter.title,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            textStyle = MaterialTheme.typography.labelMedium,
            modifier = Modifier.testTag("summary_chapter_title")
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TrailSenseSpacing.sm, bottom = TrailSenseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getSummaryTitle(result),
                modifier = Modifier
                    .weight(1f)
                    .testTag("summary_title"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .testTag("summary_scroll")
        ) {
            MarkdownSummaryText(
                summary = summary,
                markdown = markdown,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MarkdownSummaryText(
    summary: String,
    markdown: MarkdownService,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.testTag("summary"),
        factory = { context ->
            TextView(context).apply {
                id = R.id.summary
                setLineSpacing(Resources.dp(context, 4f), 1f)
                textSize = 14f
            }
        },
        update = {
            markdown.setMarkdown(it, summary)
        }
    )
}

private fun getSummaryTitle(result: SurvivalGuideSearchResult): String {
    return listOfNotNull(
        result.heading,
        if (SurvivalGuideSearch.shouldUseSubsection(result)) {
            result.bestSubsection?.heading
        } else {
            null
        }
    ).joinToString(" > ")
}

@Preview(showBackground = true)
@Composable
private fun SurvivalGuideSheetContentPreview() {
    val context = LocalContext.current
    TrailSenseTheme {
        SurvivalGuideSheetContent(
            query = "",
            summary = "",
            firstResult = null,
            markdown = MarkdownService(context),
            onQueryChange = {},
            onOpenSearchList = {},
            onSummaryClick = {}
        )
    }
}
