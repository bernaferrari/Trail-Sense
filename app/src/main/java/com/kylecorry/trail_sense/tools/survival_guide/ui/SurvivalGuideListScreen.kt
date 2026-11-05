@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.survival_guide.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAccents
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGridCellShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseWellColors
import com.kylecorry.trail_sense.tools.weather.ui.SkyFocalShape
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLinkedText
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearch
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearchResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.GuideDetails
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.GuideLoader
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SurvivalGuideListItem(
    val id: Long,
    val title: String,
    val subtitle: String?,
    val icon: ImageVector,
    val tag: String?,
    val onClick: () -> Unit
)

/**
 * Survival guide: a search bar, the best answer to the search as a card when there is a confident one, then the
 * chapters (or the matching sections) as one group of rows.
 */
@Composable
fun SurvivalGuideListScreen(
    query: String,
    listItems: List<SurvivalGuideListItem>,
    summary: String,
    firstResult: SurvivalGuideSearchResult?,
    markdown: MarkdownService,
    onQueryChange: (String) -> Unit,
    onSummaryClick: (SurvivalGuideSearchResult) -> Unit
) {
    // One question: what do I do now? Search answers it (with the best passage up front); the chapters follow.
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    TrailSensePage(
        title = stringResource(R.string.survival_guide),
        onBack = { dispatcher?.onBackPressed() },
        backLabel = stringResource(R.string.back)
    ) {
        item(key = "search") {
            TrailSenseSearchBar(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.search),
                clearContentDescription = stringResource(R.string.survival_guide_clear_search),
                modifier = Modifier.testTag("search")
            )
        }

        if (summary.isNotBlank() && firstResult != null) {
            item(key = "summary") {
                SurvivalGuideSummary(
                    summary = summary,
                    result = firstResult,
                    markdown = markdown,
                    onSummaryClick = onSummaryClick,
                    modifier = Modifier.animateItem()
                )
            }
        }

        // The chapters load in the background; until then nothing is shown rather than an empty list.
        if (listItems.isNotEmpty()) {
            if (query.isBlank()) {
                // Browsing: the overview stands apart as the way in, then the chapters as one list. A chapter's line
                // may wrap to a second line rather than cut off mid-word.
                val overview = listItems.first()
                item(key = "overview") {
                    TrailSenseRowGroup(modifier = Modifier.testTag("overview")) {
                        row { position ->
                            SurvivalGuideRow(
                                item = overview.copy(
                                    subtitle = stringResource(R.string.sky_survival_chapters, listItems.size - 1)
                                ),
                                position = position,
                                index = 0,
                                subtitleMaxLines = 1
                            )
                        }
                    }
                }
                item(key = "list") {
                    TrailSenseRowGroup(modifier = Modifier.testTag("list")) {
                        listItems.drop(1).forEachIndexed { index, item ->
                            row { position -> SurvivalGuideRow(item, position, index + 1, subtitleMaxLines = 2) }
                        }
                    }
                }
            } else {
                item(key = "list") {
                    TrailSenseRowGroup(modifier = Modifier.testTag("list")) {
                        listItems.forEachIndexed { index, item ->
                            row { position -> SurvivalGuideRow(item, position, index) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurvivalGuideSummary(
    summary: String,
    result: SurvivalGuideSearchResult,
    markdown: MarkdownService,
    onSummaryClick: (SurvivalGuideSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = getSummaryTitle(result)
    SurvivalGuidePressableSurface(
        onClick = { onSummaryClick(result) },
        shape = RoundedCornerShape(TrailSenseShape.groupOuter),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.testTag("summary_holder")
    ) {
        Column(
            modifier = Modifier.padding(
                start = TrailSenseSpacing.xl,
                end = TrailSenseSpacing.lg,
                top = TrailSenseSpacing.lg,
                bottom = TrailSenseSpacing.xl
            ),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
                ) {
                    Text(
                        text = result.chapter.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("summary_chapter_title")
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("summary_title")
                    )
                }
                Box(
                    modifier = Modifier.size(TrailSenseSize.minimumTouchTarget),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.KeyboardArrowRight),
                        contentDescription = null
                    )
                }
            }
            // The summary can run long: it scrolls inside the card so the chapters stay close below it.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("summary_scroll")
            ) {
                TrailSenseLinkedText(
                    text = markdown.toMarkdown(summary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("summary"),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SurvivalGuideRow(
    item: SurvivalGuideListItem,
    position: TrailSenseGroupPosition,
    index: Int,
    subtitleMaxLines: Int = 2
) {
    // In search results the chapter leads the line so a heading like "Overview" still says where it lives.
    val subtitle = listOfNotNull(item.tag, item.subtitle?.takeIf { it.isNotBlank() })
        .joinToString(" · ")
        .takeIf { it.isNotEmpty() }
    SurvivalGuideGroupRow(
        title = item.title,
        subtitle = subtitle,
        icon = rememberVectorPainter(item.icon),
        position = position,
        onClick = item.onClick,
        modifier = Modifier.testTag("guide_item"),
        titleModifier = Modifier.testTag("guide_item_title"),
        subtitleMaxLines = subtitleMaxLines,
        trailing = {
            Icon(
                painter = rememberVectorPainter(TrailIcons.KeyboardArrowRight),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    ).joinToString(" › ")
}

/** The survival guide's chapters, or the search results for its query; back clears the query first. */
@Composable
fun SurvivalGuideListToolScreen(route: Route.SurvivalGuideList) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val navigator = LocalNavigator.current
    val viewModel = viewModel {
        SurvivalGuideListViewModel(
            searchQuery = route.searchQuery,
            loader = GuideLoader(appContext),
            searchService = SurvivalGuideSearch(appContext)
        )
    }
    LaunchedEffect(Unit) {
        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.survival_guide),
            context.getString(R.string.survival_guide_disclaimer),
            "pref_survival_guide_disclaimer_shown",
            cancelText = null
        )
    }
    // Back clears the query before it leaves the guide.
    BackHandler(enabled = viewModel.query.isNotBlank()) {
        viewModel.updateQuery("")
    }

    val openSearchResult: (SurvivalGuideSearchResult) -> Unit = { result ->
        navigator.navigate(
            Route.SurvivalGuideReader(
                chapterResourceId = result.chapter.resource,
                headerIndex = result.headingIndex
            )
        )
    }
    val overview = stringResource(R.string.overview)
    val listItems = if (viewModel.query.isBlank() || viewModel.searchResults.isEmpty()) {
        viewModel.chapters.map {
            SurvivalGuideListItem(
                id = it.chapter.resource.toLong(),
                title = it.chapter.title,
                subtitle = it.sections.firstOrNull()?.summary,
                icon = it.chapter.icon,
                tag = null,
                onClick = {
                    navigator.navigate(Route.SurvivalGuideReader(chapterResourceId = it.chapter.resource))
                }
            )
        }
    } else {
        viewModel.searchResults.map {
            SurvivalGuideListItem(
                id = it.chapter.resource.toLong(),
                title = it.heading ?: overview,
                subtitle = it.summary,
                icon = it.chapter.icon,
                tag = it.chapter.title,
                onClick = { openSearchResult(it) }
            )
        }
    }

    SurvivalGuideListScreen(
        query = viewModel.query,
        listItems = listItems,
        summary = viewModel.summary,
        firstResult = viewModel.searchResults.firstOrNull(),
        markdown = DependencyRegistry.get(),
        onQueryChange = viewModel::updateQuery,
        onSummaryClick = openSearchResult
    )
}

/** The guide's chapters and the search over them: results follow the query after a short pause. */
internal class SurvivalGuideListViewModel(
    searchQuery: String,
    private val loader: GuideLoader,
    private val searchService: SurvivalGuideSearch
) : ViewModel() {
    var query by mutableStateOf(searchQuery)
        private set
    var summary by mutableStateOf("")
        private set
    var searchResults by mutableStateOf(emptyList<SurvivalGuideSearchResult>())
        private set
    var chapters by mutableStateOf(emptyList<GuideDetails>())
        private set
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            chapters = loader.load(includeContent = false)
        }
        if (query.isNotBlank()) {
            search(query)
        }
    }

    fun updateQuery(value: String) {
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
        searchJob = viewModelScope.launch {
            delay(200)
            val results = searchService.search(value)
            if (query != value) {
                return@launch
            }
            searchResults = results
            val firstResult = results.firstOrNull()
            summary = if (firstResult != null && firstResult.score >= 0.5f) {
                searchService.getSummary(firstResult)
            } else {
                ""
            }
        }
    }
}
