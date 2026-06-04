package com.kylecorry.trail_sense.tools.survival_guide.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.views.badge.Badge
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.GuideDetails
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.GuideLoader
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearch
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FragmentToolSurvivalGuideList : Fragment() {

    private var query by mutableStateOf("")
    private var chapters by mutableStateOf(emptyList<GuideDetails>())
    private var searchResults by mutableStateOf(emptyList<SurvivalGuideSearchResult>())
    private var summary by mutableStateOf("")
    private var searchView: SearchView? = null
    private var searchJob: Job? = null

    private val markdown by lazy { AppServiceRegistry.get<MarkdownService>() }
    private val searchService by lazy { SurvivalGuideSearch(requireContext()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        query = arguments?.getString("search_query").orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    SurvivalGuideListScreen(
                        query = query,
                        listItems = getListItems(),
                        summary = summary,
                        firstResult = searchResults.firstOrNull(),
                        markdown = markdown,
                        onQueryChange = ::updateQuery,
                        onSearchViewCreated = { searchView = it },
                        onSummaryClick = ::openSearchResult
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CustomUiUtils.disclaimer(
            requireContext(),
            getString(R.string.survival_guide),
            getString(R.string.survival_guide_disclaimer),
            "pref_survival_guide_disclaimer_shown",
            cancelText = null
        )
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (query.isNotBlank()) {
                        updateQuery("")
                        searchView?.query = ""
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
        loadChapters()
        if (query.isNotBlank()) {
            search(query)
        }
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        searchView = null
        super.onDestroyView()
    }

    private fun loadChapters() {
        viewLifecycleOwner.lifecycleScope.launch {
            chapters = GuideLoader(requireContext()).load(includeContent = false)
        }
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
            val results = searchService.search(value)
            if (query != value) {
                return@launch
            }
            searchResults = results
            val firstResult = results.firstOrNull()
            summary = if (firstResult != null && firstResult.score >= 0.5f) {
                searchService.getSummary(value, firstResult)
            } else {
                ""
            }
        }
    }

    private fun getListItems(): List<SurvivalGuideListItem> {
        val context = requireContext()
        return if (query.isBlank() || searchResults.isEmpty()) {
            chapters.map {
                SurvivalGuideListItem(
                    id = it.chapter.resource.toLong(),
                    title = it.chapter.title,
                    subtitle = it.sections.firstOrNull()?.summary,
                    icon = it.chapter.icon,
                    tag = null,
                    onClick = { openChapter(it.chapter.resource) }
                )
            }
        } else {
            searchResults.map {
                SurvivalGuideListItem(
                    id = it.chapter.resource.toLong(),
                    title = it.heading ?: getString(R.string.overview),
                    subtitle = it.summary,
                    icon = it.chapter.icon,
                    tag = it.chapter.title,
                    onClick = { openSearchResult(it) }
                )
            }
        }
    }

    private fun openChapter(chapterResource: Int) {
        findNavController().navigateWithAnimation(
            R.id.fragmentToolSurvivalGuideReader,
            Bundle().apply {
                putInt("chapter_resource_id", chapterResource)
            }
        )
    }

    private fun openSearchResult(result: SurvivalGuideSearchResult) {
        findNavController().navigateWithAnimation(
            R.id.fragmentToolSurvivalGuideReader,
            Bundle().apply {
                putInt("chapter_resource_id", result.chapter.resource)
                putInt("header_index", result.headingIndex)
            }
        )
    }
}

private data class SurvivalGuideListItem(
    val id: Long,
    val title: String,
    val subtitle: String?,
    val icon: Int,
    val tag: String?,
    val onClick: () -> Unit
)

@Composable
private fun SurvivalGuideListScreen(
    query: String,
    listItems: List<SurvivalGuideListItem>,
    summary: String,
    firstResult: SurvivalGuideSearchResult?,
    markdown: MarkdownService,
    onQueryChange: (String) -> Unit,
    onSearchViewCreated: (SearchView) -> Unit,
    onSummaryClick: (SurvivalGuideSearchResult) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SurvivalGuideSearchBox(
                query = query,
                onQueryChange = onQueryChange,
                onSearchViewCreated = onSearchViewCreated
            )
            if (summary.isNotBlank()) {
                SurvivalGuideSummary(
                    summary = summary,
                    result = firstResult,
                    markdown = markdown,
                    onSummaryClick = onSummaryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(horizontal = 8.dp)
                        .padding(top = 8.dp)
                )
            }
            SurvivalGuideChapterList(
                listItems = listItems,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (summary.isBlank()) 16.dp else 8.dp)
            )
        }
    }
}

@Composable
private fun SurvivalGuideSearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchViewCreated: (SearchView) -> Unit
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("search"),
        factory = { context ->
            SearchView(context, null).apply {
                id = R.id.search
                setOnSearchListener(onQueryChange)
                onSearchViewCreated(this)
            }
        },
        update = { searchView ->
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
    result: SurvivalGuideSearchResult?,
    markdown: MarkdownService,
    onSummaryClick: (SurvivalGuideSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.testTag("summary_holder"),
        factory = { context -> SurvivalGuideSummaryView(context) },
        update = {
            it.bind(summary, result, markdown, onSummaryClick)
        }
    )
}

@Composable
private fun SurvivalGuideChapterList(
    listItems: List<SurvivalGuideListItem>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.testTag("list")
    ) {
        items(listItems, key = { it.id }) { item ->
            SurvivalGuideChapterRow(item)
        }
    }
}

@Composable
private fun SurvivalGuideChapterRow(item: SurvivalGuideListItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("guide_item")
            .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md)
            .clickable { item.onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = TrailSenseSpacing.md)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                modifier = Modifier.testTag("guide_item_title"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (item.tag != null) {
                Text(
                    text = item.tag,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private class SurvivalGuideSummaryView(context: Context) : MaterialCardView(context) {

    private val badge = Badge(context, null).apply {
        id = R.id.summary_chapter_title
    }
    private val title = TextView(context).apply {
        id = R.id.summary_title
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        compoundDrawablePadding = Resources.dp(context, 8f).toInt()
        setPadding(0, Resources.dp(context, 4f).toInt(), 0, 0)
    }
    private val scroll = ScrollView(context).apply {
        id = R.id.summary_scroll
    }
    private val summary = TextView(context).apply {
        id = R.id.summary
    }

    init {
        id = R.id.summary_holder
        radius = Resources.dp(context, 12f)
        cardElevation = Resources.dp(context, 2f)
        minimumHeight = Resources.dp(context, 150f).toInt()

        val padding = Resources.dp(context, 16f).toInt()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        addView(
            container,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        container.addView(
            badge,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        container.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        scroll.addView(
            summary,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        container.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = Resources.dp(context, 4f).toInt()
            }
        )
    }

    fun bind(
        markdownText: String,
        result: SurvivalGuideSearchResult?,
        markdown: MarkdownService,
        onSummaryClick: (SurvivalGuideSearchResult) -> Unit
    ) {
        markdown.setMarkdown(summary, markdownText)
        if (result == null) {
            setOnClickListener(null)
            return
        }

        title.text = listOfNotNull(
            result.heading,
            if (SurvivalGuideSearch.shouldUseSubsection(result)) {
                result.bestSubsection?.heading
            } else {
                null
            }
        ).joinToString(" > ")
        badge.setStatusText(result.chapter.title)
        badge.statusImage.setImageResource(result.chapter.icon)
        badge.statusImage.imageTintList = ColorStateList.valueOf(
            Resources.androidTextColorSecondary(context)
        )
        val backgroundColor = Resources.androidTextColorSecondary(context)
        badge.setBackgroundTint(backgroundColor)
        badge.setForegroundTint(
            Colors.mostContrastingColor(
                Color.WHITE,
                Color.BLACK,
                backgroundColor
            )
        )
        title.setCompoundDrawables(
            size = Resources.dp(context, 16f).toInt(),
            right = R.drawable.ic_keyboard_arrow_right
        )
        setOnClickListener {
            onSummaryClick(result)
        }
        scroll.post {
            scroll.scrollTo(0, 0)
        }
    }
}
