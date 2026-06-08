package com.kylecorry.trail_sense.main

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
    val visible: Boolean = true
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainBottomNavigation(
    state: MainBottomNavigationState,
    onItemClick: (Int) -> Unit,
    onItemLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleItems = state.items.filter { it.visible }
    val height = if (state.useCompactMode) 55.dp else 80.dp
    NavigationBar(
        modifier = modifier.height(height + state.bottomInsetDp.dp),
        containerColor = if (state.useBlackTheme) Color.Black else MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        visibleItems.forEach { item ->
            val selected = item.id == state.selectedItemId
            NavigationBarItem(
                selected = selected,
                enabled = state.isEnabled,
                onClick = {},
                icon = {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = item.title
                    )
                },
                label = if (!state.useCompactMode && (state.showAllLabels || selected)) {
                    { Text(item.title) }
                } else {
                    null
                },
                modifier = Modifier
                    .padding(bottom = state.bottomInsetDp.dp)
                    .combinedClickable(
                        enabled = state.isEnabled,
                        onClick = { onItemClick(item.id) },
                        onLongClick = { onItemLongClick(item.id) }
                    )
            )
        }
    }
}
