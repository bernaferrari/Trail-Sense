@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.tools.ui


import com.kylecorry.trail_sense.shared.text.LocalAppStrings
import com.kylecorry.trail_sense.shared.text.RawTextReader
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.generated.resources.*

import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDescriptor
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTile
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTileGrid
import com.kylecorry.trail_sense.shared.views.compose.rememberTileTitleSize
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolKeywordLoader
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSearch
import com.kylecorry.trail_sense.tools.tools.ui.sort.CategoricalToolSort

/**
 * The Tools tab: pinned tools, then every tool by category, each category one connected grid of quiet tiles in the
 * scheme's colours. Searching swaps the grid for rows with each tool's description, which is what you scan when you
 * don't know a tool's name. Long press any tool for its actions.
 */
@Composable
fun <T : ToolDescriptor> ToolsScreen(
    tools: List<T>,
    keywordReader: RawTextReader,
    onShowLongPressHint: () -> Unit,
    pinnedToolIds: List<Long>,
    quickActions: List<ComposeQuickActionButtonView> = emptyList(),
    onToolClick: (T) -> Unit,
    onToolLongClick: (T) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchFocusRequestChanged: (((() -> Boolean)?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val allTools = tools
    var keywords by remember { mutableStateOf<Map<Long, Set<String>>>(emptyMap()) }
    LaunchedEffect(keywordReader) { keywords = ToolKeywordLoader.load(keywordReader) }
    val toolSearch = remember(allTools, keywords) { ToolSearch(allTools, keywords) }
    val sorter = remember(strings) { CategoricalToolSort(strings) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var filter by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { onShowLongPressHint() }

    DisposableEffect(onSearchFocusRequestChanged, keyboard) {
        onSearchFocusRequestChanged?.invoke {
            searchFocusRequester.requestFocus()
            keyboard?.show()
            true
        }
        onDispose {
            onSearchFocusRequestChanged?.invoke(null)
        }
    }

    // One question: which tool do I need? Search and the grid answer it; nothing on the hub is louder than a tile.
    val isSearching = filter.isNotBlank()
    val results = remember(allTools, filter, toolSearch) {
        if (filter.isBlank()) emptyList() else toolSearch.search(filter)
    }
    val categories = remember(allTools, sorter) { sorter.sort(allTools) }
    val pinned = remember(allTools, pinnedToolIds) {
        pinnedToolIds.mapNotNull { id -> allTools.firstOrNull { it.id == id } }
    }

    // One title size for every tile on the page, decided once from the longest tool name.
    val screenWidth = with(androidx.compose.ui.platform.LocalDensity.current) {
        androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toDp()
    }
    val gridWidth = minOf(screenWidth, TrailSenseSize.maximumContentWidth) - TrailSenseSpacing.lg * 2
    val titleSize = rememberTileTitleSize(allTools.map { it.name }, gridWidth, if (gridWidth >= 600.dp) 3 else 2)

    // The tab's page: the family's large title that collapses as the grid scrolls, settings as its one action.
    com.kylecorry.trail_sense.shared.views.compose.TrailSensePage(
        title = appString(Res.string.tools),
        onBack = null,
        modifier = modifier,
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Rounded.Settings, contentDescription = appString(Res.string.settings))
            }
        }
    ) {
        item("search") {
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar(
                value = filter,
                onValueChange = { filter = it },
                placeholder = appString(Res.string.search_tools),
                clearContentDescription = appString(Res.string.clouds_clear),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester)
            )
        }

        if (!isSearching && quickActions.isNotEmpty()) {
            item("quick_actions") {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tool_quick_actions"),
                    horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
                ) {
                    quickActions.forEach { it.Content() }
                }
            }
        }

        if (isSearching) {
            if (results.isEmpty()) {
                item("no_results") {
                    TrailSenseEmptyState(
                        icon = Icons.Rounded.Search,
                        title = appString(Res.string.no_results),
                        modifier = Modifier.fillParentMaxHeight(0.9f)
                    )
                }
            } else {
                item("results") {
                    Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                        results.forEachIndexed { index, tool ->
                            ToolRow(
                                tool = tool,
                                position = TrailSenseGroupPosition.at(index, results.size),
                                onClick = { onToolClick(tool) },
                                onLongClick = { onToolLongClick(tool) }
                            )
                        }
                    }
                }
            }
        } else {
            if (pinned.isNotEmpty()) {
                item("pinned") {
                    Column {
                        TrailSenseGroupHeader(appString(Res.string.pinned))
                        TrailSenseTileGrid(
                            pinned.map { it.tile(onToolClick, onToolLongClick) },
                            titleSize = titleSize
                        )
                    }
                }
            }
            items(categories, key = { "category_${it.categoryName}" }) { group ->
                Column {
                    group.categoryName?.let { TrailSenseGroupHeader(it) }
                    TrailSenseTileGrid(
                        group.tools.map { it.tile(onToolClick, onToolLongClick) },
                        titleSize = titleSize
                    )
                }
            }
        }
    }
}

private fun <T : ToolDescriptor> T.tile(onClick: (T) -> Unit, onLongClick: (T) -> Unit) = TrailSenseTile(
    key = id,
    title = name,
    // No category hues: every well takes the scheme's own colour, so the hub stays calm.
    accent = Color.Unspecified,
    icon = { Icon(rememberVectorPainter(icon), contentDescription = null, modifier = Modifier.size(22.dp)) },
    onClick = { onClick(this) },
    onLongClick = { onLongClick(this) }
)

@Composable
private fun ToolRow(
    tool: ToolDescriptor,
    position: TrailSenseGroupPosition,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    TrailSenseListItem(
        title = tool.name,
        subtitle = tool.description,
        icon = rememberVectorPainter(tool.icon),
        iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
        onClick = onClick,
        onLongClick = onLongClick,
        position = position
    )
}

