package com.kylecorry.andromeda.list

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun <T> ItemList(
    items: List<T>, modifier: Modifier = Modifier, state: LazyListState = rememberLazyListState(),
    horizontal: Boolean = false, reverseLayout: Boolean = false, scrollEnabled: Boolean = true,
    key: ((T) -> Any)? = null, separator: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T) -> Unit
) {
    val content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {
        itemsIndexed(items, key = key?.let { getKey -> { _, item -> getKey(item) } }) { index, item ->
            itemContent(item)
            if (index < items.lastIndex) separator?.invoke()
        }
    }
    if (horizontal) LazyRow(modifier, state, reverseLayout = reverseLayout, userScrollEnabled = scrollEnabled, content = content)
    else LazyColumn(modifier, state, reverseLayout = reverseLayout, userScrollEnabled = scrollEnabled, content = content)
}

@Composable
fun <T> ItemGrid(
    items: List<T>, columns: Int, modifier: Modifier = Modifier, state: LazyGridState = rememberLazyGridState(),
    horizontal: Boolean = false, reverseLayout: Boolean = false, scrollEnabled: Boolean = true,
    key: ((T) -> Any)? = null, span: (T) -> Int = { 1 }, itemContent: @Composable (T) -> Unit
) {
    require(columns > 0)
    val content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit = {
        itemsIndexed(items, key = key?.let { getKey -> { _, item -> getKey(item) } },
            span = { _, item -> GridItemSpan(span(item).coerceIn(1, maxLineSpan)) }) { _, item -> itemContent(item) }
    }
    if (horizontal) LazyHorizontalGrid(GridCells.Fixed(columns), modifier, state, reverseLayout = reverseLayout,
        userScrollEnabled = scrollEnabled, content = content)
    else LazyVerticalGrid(GridCells.Fixed(columns), modifier, state, reverseLayout = reverseLayout,
        userScrollEnabled = scrollEnabled, content = content)
}
