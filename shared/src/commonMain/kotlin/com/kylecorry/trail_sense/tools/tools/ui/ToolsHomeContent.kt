package com.kylecorry.trail_sense.tools.tools.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.shared.text.RawTextReader
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDescriptor

/** Shared Tools home state and screen, with platform actions supplied by the host. */
@Composable
fun <T : ToolDescriptor> ToolsHomeContent(
    tools: List<T>,
    pinnedTools: PinnedToolManager,
    keywordReader: RawTextReader,
    quickActions: List<ComposeQuickActionButtonView>,
    onShowLongPressHint: () -> Unit,
    onToolClick: (T) -> Unit,
    onToolLongClick: (T, ToolsHomeViewModel) -> Unit,
    onSettingsClick: () -> Unit,
    onController: (ToolSearchFocusable?) -> Unit
) {
    val model = viewModel { ToolsHomeViewModel(pinnedTools) }
    val searchFocus = remember { ToolsSearchFocus() }
    DisposableEffect(searchFocus) {
        onController(searchFocus)
        onDispose { onController(null) }
    }
    ToolsScreen(
        tools = tools,
        keywordReader = keywordReader,
        onShowLongPressHint = onShowLongPressHint,
        pinnedToolIds = model.pinnedToolIds,
        quickActions = quickActions,
        onToolClick = onToolClick,
        onToolLongClick = { onToolLongClick(it, model) },
        onSettingsClick = onSettingsClick,
        onSearchFocusRequestChanged = { searchFocus.focusSearchRequest = it }
    )
}

private class ToolsSearchFocus : ToolSearchFocusable {
    var focusSearchRequest: (() -> Boolean)? = null
    override fun focusSearch(): Boolean = focusSearchRequest?.invoke() == true
}

class ToolsHomeViewModel(private val pinnedToolManager: PinnedToolManager) : ViewModel() {
    val pinnedToolIds: List<Long> get() = pinnedToolManager.pinnedIds.value
    fun isPinned(toolId: Long): Boolean = pinnedToolManager.isPinned(toolId)
    fun pin(toolId: Long) = pinnedToolManager.pin(toolId)
    fun unpin(toolId: Long) = pinnedToolManager.unpin(toolId)
}
