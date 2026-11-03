package com.kylecorry.trail_sense.tools.tools.ui

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
import androidx.compose.ui.res.painterResource
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSearch
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.sort.CategoricalToolSort

/**
 * The Tools tab: pinned tools as large colored tiles, then every tool by category, each category one card of
 * compact icon tiles so the whole kit fits in a few screens. Searching swaps the grid for rows with each tool's
 * description, which is what you scan when you don't know a tool's name. Long press any tool for its actions.
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

    val isSearching = filter.isNotBlank()
    val results = remember(allTools, filter, toolSearch) {
        if (filter.isBlank()) emptyList() else toolSearch.search(filter)
    }
    val categories = remember(allTools, sorter) { sorter.sort(allTools) }
    val pinned = remember(allTools, pinnedToolIds) {
        pinnedToolIds.mapNotNull { id -> allTools.firstOrNull { it.id == id } }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
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
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalIconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.size(TrailSenseSize.minimumTouchTarget)
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                }

                item("search") {
                    ToolSearchField(
                        value = filter,
                        onValueChange = { filter = it },
                        focusRequester = searchFocusRequester,
                        modifier = Modifier.fillMaxWidth()
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
                                PinnedTools(pinned, onToolClick, onToolLongClick)
                            }
                        }
                    }
                    items(categories, key = { "category_${it.categoryName}" }) { group ->
                        Column {
                            if (group.categoryName != null) TrailSenseGroupHeader(group.categoryName)
                            ToolGrid(group.tools, onToolClick, onToolLongClick)
                        }
                    }
                }
            }
        }
    }
}

/** Pinned tools: two to a row, each in its own container color so the favorites read at a glance. */
@Composable
private fun PinnedTools(tools: List<Tool>, onClick: (Tool) -> Unit, onLongClick: (Tool) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tones = listOf(
        scheme.primaryContainer to scheme.onPrimaryContainer,
        scheme.tertiaryContainer to scheme.onTertiaryContainer,
        scheme.secondaryContainer to scheme.onSecondaryContainer
    )
    Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
        tools.chunked(2).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
                row.forEachIndexed { columnIndex, tool ->
                    val (container, content) = tones[(rowIndex * 2 + columnIndex) % tones.size]
                    PinnedTile(
                        tool = tool,
                        container = container,
                        content = content,
                        onClick = { onClick(tool) },
                        onLongClick = { onLongClick(tool) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PinnedTile(
    tool: Tool,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "pinnedPress"
    )
    Surface(
        modifier = modifier
            .height(100.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(TrailSenseShape.groupOuter),
        color = container,
        contentColor = content
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    interactionSource = interaction,
                    indication = androidx.compose.material3.ripple(),
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onLongClickLabel = tool.name
                )
                .padding(TrailSenseSpacing.lg),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val cookie = MaterialShapes.Cookie6Sided.toShape()
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        shape = cookie
                        clip = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(color = content.copy(alpha = 0.12f), modifier = Modifier.fillMaxSize()) {}
                Icon(painterResource(tool.icon), contentDescription = null, modifier = Modifier.size(22.dp))
            }
            Text(
                text = tool.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * A category: its tools as chips, two to a row on a phone (more on wider screens), each an icon well and a name on
 * one line. A category with a single tool stays a single chip instead of a mostly empty card.
 */
@Composable
private fun ToolGrid(tools: List<Tool>, onClick: (Tool) -> Unit, onLongClick: (Tool) -> Unit) {
    BoxWithConstraints {
        val columns = (maxWidth / 172.dp).toInt().coerceIn(2, 4)
        Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
            tools.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
                    row.forEach { tool ->
                        ToolChip(
                            tool = tool,
                            onClick = { onClick(tool) },
                            onLongClick = { onLongClick(tool) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolChip(
    tool: Tool,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "toolPress"
    )
    Surface(
        modifier = modifier
            .heightIn(min = 64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    interactionSource = interaction,
                    indication = androidx.compose.material3.ripple(),
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onLongClickLabel = tool.name
                )
                .padding(horizontal = TrailSenseSpacing.md, vertical = TrailSenseSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(tool.icon), contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
            Text(
                text = tool.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

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
        icon = painterResource(tool.icon),
        iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
        onClick = onClick,
        onLongClick = onLongClick,
        position = position
    )
}

@Composable
private fun ToolSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        leadingIcon = {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingIcon = {
            AnimatedContent(
                targetState = value.isNotEmpty(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "searchClear"
            ) { shown ->
                if (shown) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.clouds_clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        placeholder = { Text(stringResource(R.string.search_tools), style = MaterialTheme.typography.bodyLarge) },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        modifier = modifier.focusRequester(focusRequester)
    )
}
