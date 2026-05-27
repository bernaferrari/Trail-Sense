package com.kylecorry.trail_sense.tools.tools.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.CustomUiUtils.oneTimeToast
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.compose.useAndroidContext
import com.kylecorry.trail_sense.shared.extensions.compose.useEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useMemo
import com.kylecorry.trail_sense.shared.extensions.compose.useService
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseElevation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapes
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSearch
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.sort.ToolSortFactory
import com.kylecorry.trail_sense.tools.tools.ui.sort.ToolSortType

@Composable
fun Fragment.ToolsContent(modifier: Modifier = Modifier) {
    val prefs = useService<UserPreferences>()
    val navController = findNavController()
    val context = requireContext()

    // Core data
    var tools by remember { mutableStateOf(emptyList<Tool>()) }
    var filter by remember { mutableStateOf("") }
    var sortType by remember { mutableStateOf(prefs.toolSort) }

    // Pinned state
    val pinnedManager = remember { PinnedToolManager(prefs) }
    var pinnedIds by remember { mutableStateOf(pinnedManager.getPinnedToolIds().toSet()) }

    // Search helper
    val toolSearch = remember { ToolSearch(context) }

    // Load tools once
    useEffect {
        tools = Tools.getTools(context)
    }

    val filteredTools = useMemo(tools, filter) {
        if (filter.isBlank()) tools else toolSearch.search(filter)
    }

    val pinnedTools = useMemo(tools, pinnedIds) {
        tools.filter { pinnedIds.contains(it.id) }
    }

    // Apply sort
    val sortFactory = remember { ToolSortFactory(context) }
    val categorizedTools = useMemo(filteredTools, sortType) {
        sortFactory.getToolSort(sortType).sort(filteredTools)
    }

    // Context menu state
    var contextMenuTool by remember { mutableStateOf<Tool?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = TrailSenseSpacing.lg)
    ) {
        // Search bar + settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TrailSenseSpacing.lg, bottom = TrailSenseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrailSenseSearchBar(
                value = filter,
                onValueChange = { filter = it },
                placeholder = stringResource(R.string.search),
                modifier = Modifier
                    .weight(1f)
                    .testTag("tool_search")
            )

            Spacer(Modifier.width(TrailSenseSpacing.sm))

            IconButton(
                onClick = {
                    navController.navigate(R.id.action_settings)
                },
                modifier = Modifier.testTag("settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(bottom = TrailSenseSpacing.xxxl),
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
            modifier = Modifier.fillMaxSize()
        ) {
            // Pinned section (only when not searching)
            if (filter.isBlank() && pinnedTools.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    TrailSenseSectionHeader(
                        title = stringResource(R.string.pinned),
                        icon = painterResource(R.drawable.ic_edit),
                        onIconClick = {
                            editPinnedTools(tools, pinnedManager, pinnedIds) { newPinned ->
                                pinnedIds = newPinned
                            }
                        }
                    )
                }

                items(
                    items = pinnedTools.sortedBy { it.name },
                    key = { "pinned_${it.id}" }
                ) { tool ->
                    ToolCard(
                        tool = tool,
                        onClick = {
                            navController.navigateWithAnimation(tool.navAction)
                        },
                        onLongClick = {
                            contextMenuTool = tool
                        }
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Tools section — grouped by category when sorted by Category
            for (group in categorizedTools) {
                if (group.categoryName != null) {
                    item(
                        span = { GridItemSpan(maxLineSpan) },
                        key = "category_${group.categoryName}"
                    ) {
                        TrailSenseSectionHeader(title = group.categoryName)
                    }
                } else if (filter.isBlank() || categorizedTools.size > 1) {
                    // Single flat section header for alphabetical sort
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        TrailSenseSectionHeader(
                            title = stringResource(R.string.tools),
                            icon = painterResource(R.drawable.sort_ascending),
                            onIconClick = {
                                changeSortType { newSort ->
                                    sortType = newSort
                                    prefs.toolSort = newSort
                                }
                            }
                        )
                    }
                }

                items(
                    items = group.tools,
                    key = { "tool_${it.id}" }
                ) { tool ->
                    ToolCard(
                        tool = tool,
                        onClick = {
                            navController.navigateWithAnimation(tool.navAction)
                        },
                        onLongClick = {
                            contextMenuTool = tool
                        }
                    )
                }
            }
        }
    }

    // Context menu
    ContextMenu(
        tool = contextMenuTool,
        isPinned = contextMenuTool?.let { pinnedIds.contains(it.id) } ?: false,
        onDismiss = { contextMenuTool = null },
        onPinToggle = { tool ->
            if (pinnedIds.contains(tool.id)) {
                pinnedManager.unpin(tool.id)
                pinnedIds = pinnedIds - tool.id
            } else {
                pinnedManager.pin(tool.id)
                pinnedIds = pinnedIds + tool.id
            }
            contextMenuTool = null
        },
        onGuide = { tool ->
            tool.guideId?.let { UserGuideUtils.showGuide(this, it) }
            contextMenuTool = null
        },
        onSettings = { tool ->
            tool.settingsNavAction?.let { navController.navigateWithAnimation(it) }
            contextMenuTool = null
        },
        onAddToHome = { tool ->
            addToolToHomeScreen(tool)
            contextMenuTool = null
        }
    )

    // One-time toast hint
    val androidContext = useAndroidContext()
    useEffect {
        oneTimeToast(
            androidContext,
            getString(R.string.tool_long_press_hint_toast),
            "tools_long_press_notice_shown",
            short = false
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolCard(
    tool: Tool,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val iconPainter: Painter = painterResource(tool.icon)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("tool_card_${tool.id}"),
        shape = RoundedCornerShape(TrailSenseShapes.medium),
        elevation = CardDefaults.cardElevation(defaultElevation = TrailSenseElevation.low),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TrailSenseSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(TrailSenseSpacing.lg))
            Text(
                text = tool.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ContextMenu(
    tool: Tool?,
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onPinToggle: (Tool) -> Unit,
    onGuide: (Tool) -> Unit,
    onSettings: (Tool) -> Unit,
    onAddToHome: (Tool) -> Unit
) {
    tool ?: return

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        if (tool.isExperimental) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.experimental)) },
                onClick = onDismiss,
                enabled = false
            )
        }

        DropdownMenuItem(
            text = { Text(if (isPinned) stringResource(R.string.unpin) else stringResource(R.string.pin)) },
            onClick = { onPinToggle(tool) }
        )

        if (tool.guideId != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tool_user_guide_title)) },
                onClick = { onGuide(tool) }
            )
        }

        if (tool.settingsNavAction != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings)) },
                onClick = { onSettings(tool) }
            )
        }

        DropdownMenuItem(
            text = { Text(stringResource(R.string.add_to_homescreen)) },
            onClick = { onAddToHome(tool) }
        )

        if (tool.description != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pref_category_about)) },
                onClick = onDismiss
            )
        }
    }
}

private fun Fragment.editPinnedTools(
    allTools: List<Tool>,
    manager: PinnedToolManager,
    currentPinned: Set<Long>,
    onResult: (Set<Long>) -> Unit
) {
    val sorted = allTools.sortedBy { tool ->
        if (currentPinned.contains(tool.id)) "0${tool.name}" else tool.name
    }
    val names = sorted.map { it.name }
    val preselected = sorted.mapIndexedNotNull { idx, t ->
        if (currentPinned.contains(t.id)) idx else null
    }

    com.kylecorry.andromeda.pickers.Pickers.items(
        requireContext(),
        getString(R.string.pinned),
        names,
        preselected
    ) { selectedIndices ->
        if (selectedIndices != null) {
            val newPinned = selectedIndices.map { sorted[it].id }.toSet()
            manager.setPinnedToolIds(newPinned.toList())
            onResult(newPinned)
        }
    }
}

private fun Fragment.changeSortType(onSelected: (ToolSortType) -> Unit) {
    val types = ToolSortType.values()
    val labels = types.map {
        when (it) {
            ToolSortType.Name -> getString(R.string.name)
            ToolSortType.Category -> getString(R.string.category)
        }
    }

    com.kylecorry.andromeda.pickers.Pickers.item(
        requireContext(),
        getString(R.string.sort),
        labels,
        types.indexOf(UserPreferences(requireContext()).toolSort)
    ) { idx ->
        if (idx != null) onSelected(types[idx])
    }
}

private fun Fragment.addToolToHomeScreen(tool: Tool) {
    tryOrNothing {
        val shortcutInfo = androidx.core.content.pm.ShortcutInfoCompat.Builder(
            requireContext(),
            "tool-${tool.id}"
        )
            .setIcon(
                androidx.core.graphics.drawable.IconCompat.createFromIcon(
                    requireContext(),
                    android.graphics.drawable.Icon.createWithResource(requireContext(), tool.icon)
                )
            )
            .setIntent(MainActivity.openToolIntent(requireContext(), tool.id))
            .setShortLabel(tool.name)
            .setLongLabel(tool.name)
            .build()

        val callbackIntent = androidx.core.content.pm.ShortcutManagerCompat
            .createShortcutResultIntent(requireContext(), shortcutInfo)
        val successCallback = android.app.PendingIntent.getBroadcast(
            requireContext(), 0, callbackIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        androidx.core.content.pm.ShortcutManagerCompat.requestPinShortcut(
            requireContext(),
            shortcutInfo,
            successCallback.intentSender
        )
    }
}
