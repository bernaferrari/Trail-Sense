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
 * The Tools tab, laid out like Keepout's settings: pinned tools, then every tool by category, each category one
 * connected grid of tiles whose icon wells carry the category's accent. Searching swaps the grid for rows with each tool's
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
                                PinnedToolGrid(
                                    tools = pinned,
                                    onClick = onToolClick,
                                    onLongClick = onToolLongClick
                                )
                            }
                        }
                    }
                    items(categories, key = { "category_${it.categoryName}" }) { group ->
                        Column {
                            if (group.categoryName != null) {
                                ToolCategoryHeader(
                                    group.categoryName,
                                    group.tools.size,
                                    group.tools.firstOrNull()?.category?.accent() ?: TrailSenseAccents.Lavender
                                )
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

/**
 * Five colour families instead of a rainbow, so a hue means a kind of tool: teal for finding your way (location,
 * distance, angles), amber for the sky and time, blue for weather, coral for signalling and talking, lavender for
 * the kit (power, books, everything else). Used in the icon wells and the section titles.
 */
internal fun ToolCategory.accent(): Color = when (this) {
    ToolCategory.Location, ToolCategory.Distance, ToolCategory.Angles -> TrailSenseAccents.Seafoam
    ToolCategory.Time -> TrailSenseAccents.Sun
    ToolCategory.Weather -> TrailSenseAccents.Sky
    ToolCategory.Signaling, ToolCategory.Communication -> TrailSenseAccents.Coral
    ToolCategory.Power, ToolCategory.Books, ToolCategory.Other -> TrailSenseAccents.Lavender
}

/** A section title in its family's colour, with how many tools it holds. */
@Composable
private fun ToolCategoryHeader(title: String, count: Int, accent: Color) {
    val tones = trailSenseAccentScheme(accent)
    Row(
        Modifier.padding(
            start = TrailSenseSpacing.lg,
            end = TrailSenseSpacing.lg,
            top = TrailSenseSpacing.md,
            bottom = TrailSenseSpacing.sm
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(tones.primary)
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(TrailSenseSpacing.sm))
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = tones.primary
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(TrailSenseSpacing.sm))
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun Tool.tile(onClick: (Tool) -> Unit, onLongClick: (Tool) -> Unit) = TrailSenseTile(
    key = id,
    title = name,
    accent = category.accent(),
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
            Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
            focusedContainerColor = androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.primaryContainer, 0.45f),
            unfocusedContainerColor = androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.primaryContainer, 0.45f),
            disabledContainerColor = androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.primaryContainer, 0.45f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        modifier = modifier.focusRequester(focusRequester)
    )
}

/**
 * The tools someone reaches for most, as the hub's hero: big tiles washed in each tool's category colour, the icon in
 * an expressive shape, the name below. Two to a row; a lone last tile spans the row.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PinnedToolGrid(
    tools: List<Tool>,
    onClick: (Tool) -> Unit,
    onLongClick: (Tool) -> Unit
) {
    // The first pinned tool is the door: one wide tile with a bigger badge and what it is for. The rest follow
    // three to a row as compact tiles.
    val lead = tools.firstOrNull() ?: return
    val rest = tools.drop(1)
    val rows = rest.chunked(3)
    val shapes = listOf(
        androidx.compose.material3.MaterialShapes.Cookie9Sided,
        androidx.compose.material3.MaterialShapes.Sunny,
        androidx.compose.material3.MaterialShapes.Cookie6Sided,
        androidx.compose.material3.MaterialShapes.Clover4Leaf
    )
    val leadTones = trailSenseAccentScheme(lead.category.accent())
    val leadShape = androidx.compose.foundation.shape.RoundedCornerShape(
        topStart = TrailSenseShape.groupOuter,
        topEnd = TrailSenseShape.groupOuter,
        bottomStart = if (rows.isEmpty()) TrailSenseShape.groupOuter else TrailSenseShape.groupInner,
        bottomEnd = if (rows.isEmpty()) TrailSenseShape.groupOuter else TrailSenseShape.groupInner
    )
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val turn by androidx.compose.animation.core.animateFloatAsState(
        if (pressed) 20f else 0f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "pinnedTurn"
    )
    Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
        Surface(
            shape = leadShape,
            color = leadTones.primaryContainer,
            contentColor = leadTones.onPrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .clip(leadShape)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = androidx.compose.material3.ripple(),
                    role = androidx.compose.ui.semantics.Role.Button,
                    onClick = { onClick(lead) },
                    onLongClick = { onLongClick(lead) },
                    onLongClickLabel = lead.name
                )
        ) {
            Row(
                Modifier.padding(TrailSenseSpacing.xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrailSenseShapeBadge(
                    icon = lead.icon,
                    shape = androidx.compose.material3.MaterialShapes.Cookie12Sided.toShape(),
                    size = 72.dp,
                    containerColor = leadTones.primary,
                    contentColor = leadTones.onPrimary,
                    modifier = Modifier.graphicsLayer { rotationZ = turn }
                )
                androidx.compose.foundation.layout.Spacer(Modifier.size(TrailSenseSpacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(
                        lead.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    lead.description?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = leadTones.onPrimaryContainer.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                row.forEachIndexed { column, tool ->
                    val tones = trailSenseAccentScheme(tool.category.accent())
                    val shape = trailSenseGridCellShape(column, row.size, rowIndex + 1, rows.size + 1)
                    Surface(
                        shape = shape,
                        color = tones.primaryContainer,
                        contentColor = tones.onPrimaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(shape)
                            .combinedClickable(
                                role = androidx.compose.ui.semantics.Role.Button,
                                onClick = { onClick(tool) },
                                onLongClick = { onLongClick(tool) },
                                onLongClickLabel = tool.name
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(TrailSenseSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
                        ) {
                            TrailSenseShapeBadge(
                                icon = tool.icon,
                                shape = shapes[(rowIndex * 3 + column) % shapes.size].toShape(),
                                size = 44.dp,
                                containerColor = tones.primary,
                                contentColor = tones.onPrimary
                            )
                            Text(
                                tool.name,
                                style = MaterialTheme.typography.titleSmall,

                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
