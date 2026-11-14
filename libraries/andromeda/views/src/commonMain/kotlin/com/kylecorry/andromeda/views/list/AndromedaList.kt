package com.kylecorry.andromeda.views.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.list.ItemList

public typealias ListIcon = @Composable () -> Unit

public data class ListMenuItem(val text: String, val action: () -> Unit)
public data class ListItemCheckbox(val checked: Boolean, val onClick: () -> Unit)
public data class ListItemTag(val text: String, val icon: ListIcon? = null, val color: Color = Color.LightGray)
public data class ListItemData(
    val text: String,
    val icon: ListIcon? = null,
    val grow: Float = 0f,
    val modifier: Modifier = Modifier,
    val alignment: Alignment.Vertical = Alignment.CenterVertically
)
public data class ListItemDataAlignment(
    val horizontal: Arrangement.Horizontal = Arrangement.Start,
    val vertical: Alignment.Vertical = Alignment.CenterVertically
)

public data class ListItem(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
    val titleMaxLines: Int = Int.MAX_VALUE,
    val subtitleMaxLines: Int = Int.MAX_VALUE,
    val icon: ListIcon? = null,
    val checkbox: ListItemCheckbox? = null,
    val tags: List<ListItemTag> = emptyList(),
    val data: List<ListItemData> = emptyList(),
    val dataAlignment: ListItemDataAlignment = ListItemDataAlignment(),
    val trailingText: String? = null,
    val trailingIcon: ListIcon? = null,
    val menu: List<ListMenuItem> = emptyList(),
    val longClickAction: () -> Unit = {},
    val action: () -> Unit = {},
    val titleContent: (@Composable () -> Unit)? = null,
    val subtitleContent: (@Composable () -> Unit)? = null
)

public fun interface ListItemMapper<T> { public fun map(value: T): ListItem }

@Composable
public fun AndromedaList(
    items: List<ListItem>, modifier: Modifier = Modifier, state: LazyListState = rememberLazyListState(),
    menuLabel: String = "More options", emptyContent: (@Composable () -> Unit)? = null
) {
    if (items.isEmpty() && emptyContent != null) {
        Box(modifier, contentAlignment = Alignment.Center) { emptyContent() }
    } else {
        ItemList(items, modifier, state, key = { it.id }, separator = { HorizontalDivider() }) { item ->
            AndromedaListItem(item, menuLabel)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
public fun AndromedaListItem(item: ListItem, menuLabel: String = "More options", modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().combinedClickable(onClick = item.action, onLongClick = item.longClickAction).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        item.checkbox?.let { checkbox -> Checkbox(checkbox.checked, onCheckedChange = { checkbox.onClick() }) }
        item.icon?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (item.titleContent != null) item.titleContent.invoke()
            else Text(item.title, maxLines = item.titleMaxLines, style = MaterialTheme.typography.bodyLarge)
            if (item.subtitleContent != null) item.subtitleContent.invoke()
            else item.subtitle?.let { Text(it, maxLines = item.subtitleMaxLines, style = MaterialTheme.typography.bodyMedium) }
            if (item.tags.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.tags.forEach { tag ->
                    val foreground = if (tag.color.luminance() > 0.5f) Color.Black else Color.White
                    Surface(color = tag.color, contentColor = foreground, shape = MaterialTheme.shapes.small) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            tag.icon?.invoke()
                            Text(tag.text, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            if (item.data.isNotEmpty()) Row(horizontalArrangement = item.dataAlignment.horizontal, verticalAlignment = item.dataAlignment.vertical) {
                item.data.forEach { value ->
                    val width = if (value.grow > 0f) value.modifier.weight(value.grow) else value.modifier
                    Row(width.align(value.alignment), verticalAlignment = Alignment.CenterVertically) {
                        value.icon?.invoke()
                        Text(value.text, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item.trailingText?.let { Text(it) }
        item.trailingIcon?.invoke()
        if (item.menu.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { expanded = true }) { Text(menuLabel) }
                DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                    item.menu.forEach { action ->
                        DropdownMenuItem(text = { Text(action.text) }, onClick = { expanded = false; action.action() })
                    }
                }
            }
        }
    }
}
