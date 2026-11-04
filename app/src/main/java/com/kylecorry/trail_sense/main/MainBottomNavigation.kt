package com.kylecorry.trail_sense.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

const val MAX_BOTTOM_NAVIGATION_ITEMS = 8

class MainBottomNavigationState {
    val items = mutableStateListOf<MainBottomNavigationItem>()
    var selectedItemId by mutableIntStateOf(0)
    var isEnabled by mutableStateOf(true)
    var useCompactMode by mutableStateOf(false)
    var showAllLabels by mutableStateOf(false)
    var useBlackTheme by mutableStateOf(false)
    var bottomInsetDp by mutableIntStateOf(0)

    fun contains(@IdRes itemId: Int): Boolean {
        return items.any { it.id == itemId && it.visible }
    }
}

data class MainBottomNavigationItem(
    @IdRes val id: Int,
    val title: String,
    @DrawableRes val icon: Int,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null,
    val visible: Boolean = true
)

/**
 * The family's navigation, as in WiFi Weakspot and the other reforged apps: Material 3 Expressive's short navigation
 * bar on phones (the pill hugs the icon, the label sits under it) and the wide navigation rail on tablets and
 * foldables. Long press a tab for its quick actions; the press is watched without consuming it, so the tab keeps its
 * own ripple and click.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainAdaptiveNavigation(
    state: MainBottomNavigationState,
    onItemClick: (Int) -> Unit,
    onItemLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showBar: Boolean = true,
    content: @Composable () -> Unit
) {
    val visibleItems = state.items.filter { it.visible }
    val latest by rememberUpdatedState(content)
    val movable = remember { movableContentOf { latest() } }

    @Composable
    fun ItemIcon(item: MainBottomNavigationItem, selected: Boolean) {
        val vector = if (selected) item.selectedIcon else item.unselectedIcon
        if (vector != null) {
            Icon(imageVector = vector, contentDescription = null)
        } else {
            Icon(painter = painterResource(item.icon), contentDescription = null)
        }
    }

    fun Modifier.longPress(item: MainBottomNavigationItem): Modifier = this
        .pointerInput(item.id) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                // Watch on the initial pass, before the tab's own click consumes the release; a release (or a drag
                // away) before the timeout is a tap, not a long press.
                val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.none { it.pressed }) break
                    }
                    true
                }
                if (released == null) onItemLongClick(item.id)
            }
        }
        .semantics {
            onLongClick(label = item.title) {
                onItemLongClick(item.id)
                true
            }
        }

    BoxWithConstraints(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        if (maxWidth < 600.dp) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) { movable() }
                // As in ChangeDetection: screens opened from a tab get the whole canvas; the bar slides away and
                // their back arrow takes over, and it slides back on the tab's first screen.
                AnimatedVisibility(
                    visible = showBar,
                    enter = expandVertically(expandFrom = Alignment.Top),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                ShortNavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    visibleItems.forEach { item ->
                        val selected = item.id == state.selectedItemId
                        ShortNavigationBarItem(
                            selected = selected,
                            enabled = state.isEnabled,
                            onClick = { onItemClick(item.id) },
                            icon = { ItemIcon(item, selected) },
                            label = if (state.showAllLabels || !state.useCompactMode || selected) {
                                { Text(item.title, maxLines = 1) }
                            } else {
                                null
                            },
                            modifier = Modifier.longPress(item)
                        )
                    }
                }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = showBar) {
                WideNavigationRail(modifier = Modifier.fillMaxHeight()) {
                    visibleItems.forEach { item ->
                        val selected = item.id == state.selectedItemId
                        WideNavigationRailItem(
                            selected = selected,
                            enabled = state.isEnabled,
                            onClick = { onItemClick(item.id) },
                            icon = { ItemIcon(item, selected) },
                            label = { Text(item.title, maxLines = 1) },
                            railExpanded = false,
                            modifier = Modifier.longPress(item)
                        )
                    }
                }
                }
                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()) { movable() }
            }
        }
    }
}
