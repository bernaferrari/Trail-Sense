package com.kylecorry.trail_sense.main

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
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
import androidx.compose.ui.semantics.clearAndSetSemantics
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

@OptIn(
    ExperimentalMaterial3AdaptiveNavigationSuiteApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun MainAdaptiveNavigation(
    state: MainBottomNavigationState,
    onItemClick: (Int) -> Unit,
    onItemLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val visibleItems = state.items.filter { it.visible }
    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            visibleItems.forEach { item ->
                val selected = item.id == state.selectedItemId
                this.item(
                    selected = selected,
                    enabled = state.isEnabled,
                    onClick = { onItemClick(item.id) },
                    modifier = Modifier.semantics {
                        onLongClick(label = item.title) {
                            onItemLongClick(item.id)
                            true
                        }
                    },
                    alwaysShowLabel = state.showAllLabels || !state.useCompactMode,
                    icon = {
                        val vector = if (selected) item.selectedIcon else item.unselectedIcon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .combinedClickable(
                                    onClick = { onItemClick(item.id) },
                                    onLongClick = { onItemLongClick(item.id) },
                                    onLongClickLabel = item.title
                                )
                                .clearAndSetSemantics {},
                            contentAlignment = Alignment.Center
                        ) {
                            if (vector != null) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(item.icon),
                                    contentDescription = item.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    label = {
                        Text(
                            text = item.title,
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        },
        content = content
    )
}
