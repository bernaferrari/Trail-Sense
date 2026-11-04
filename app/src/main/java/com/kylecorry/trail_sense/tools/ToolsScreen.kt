@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.tools.ui

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGridCellShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseAccentScheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.toShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAccents
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTile
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTileGrid
import com.kylecorry.trail_sense.shared.views.compose.rememberTileTitleSize
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSearch
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.sort.CategoricalToolSort

/**
 * The Tools tab: pinned tools, then every tool by category, each category one connected grid of quiet tiles in the
 * scheme's colours. Searching swaps the grid for rows with each tool's description, which is what you scan when you
 * don't know a tool's name. Long press any tool for its actions.
 */
@Composable
fun ToolsScreen(
    pinnedToolIds: List<Long>,
    onToolClick: (Tool) -> Unit,
    onToolLongClick: (Tool) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchFocusRequestChanged: (((() -> Boolean)?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTools = remember { Tools.getTools(context) }
    val toolSearch = remember { ToolSearch(context) }
    val sorter = remember { CategoricalToolSort(context) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var filter by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        CustomUiUtils.oneTimeToast(
            context,
            context.getString(R.string.tool_long_press_hint_toast),
            "tools_long_press_notice_shown",
            short = false
        )
    }

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

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            // One title size for every tile on the page, decided once from the longest tool name.
            val gridWidth = minOf(maxWidth, TrailSenseSize.maximumContentWidth) - TrailSenseSpacing.lg * 2
            val titleSize = rememberTileTitleSize(allTools.map { it.name }, gridWidth, if (gridWidth >= 600.dp) 3 else 2)
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
                contentPadding = PaddingValues(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    top = TrailSenseSpacing.sm,
                    bottom = TrailSenseSpacing.xxxl
                ),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                item("header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = TrailSenseSpacing.xs, top = TrailSenseSpacing.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.tools),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.size(TrailSenseSize.minimumTouchTarget)
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                }

                item("search") {
                    com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar(
                        value = filter,
                        onValueChange = { filter = it },
                        placeholder = stringResource(R.string.search_tools),
                        clearContentDescription = stringResource(R.string.clouds_clear),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocusRequester)
                    )
                }

                if (isSearching) {
                    if (results.isEmpty()) {
                        item("no_results") {
                            TrailSenseEmptyState(
                                icon = Icons.Rounded.Search,
                                title = stringResource(R.string.no_results),
                                modifier = Modifier.heightIn(max = 360.dp)
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
                                TrailSenseGroupHeader(stringResource(R.string.pinned))
                                TrailSenseTileGrid(
                                    pinned.map { it.tile(onToolClick, onToolLongClick) },
                                    titleSize = titleSize
                                )
                            }
                        }
                    }
                    items(categories, key = { "category_${it.categoryName}" }) { group ->
                        Column {
                            if (group.categoryName != null) {
                                TrailSenseGroupHeader(group.categoryName)
                            }
                            TrailSenseTileGrid(
                                group.tools.map { it.tile(onToolClick, onToolLongClick) },
                                titleSize = titleSize
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Tool.tile(onClick: (Tool) -> Unit, onLongClick: (Tool) -> Unit) = TrailSenseTile(
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
    tool: Tool,
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

