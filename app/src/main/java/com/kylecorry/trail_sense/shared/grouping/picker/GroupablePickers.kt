package com.kylecorry.trail_sense.shared.grouping.picker

import android.content.Context
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.grouping.Groupable
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem

object GroupablePickers {

    fun <T : Groupable> item(
        context: Context,
        title: String?,
        manager: GroupListManager<T>,
        titleProvider: (T?) -> String,
        itemTitleProvider: (T) -> String,
        emptyText: String,
        initialGroup: Long? = null,
        searchEnabled: Boolean = true,
        onPick: (item: T?) -> Unit
    ) {
        val view = createAlertContent(context)
        var root by mutableStateOf<T?>(null)
        var items by mutableStateOf<List<T>>(emptyList())
        var resetKey by mutableIntStateOf(0)
        var selected: T? = null
        var alert: AlertDialog? = null

        manager.onChange = { newRoot, newItems, rootChanged ->
            root = newRoot
            items = newItems
            if (rootChanged) {
                resetKey++
            }
        }

        view.addView(
            ComposeView(context).apply {
                setContent {
                    TrailSenseTheme {
                        GroupableSelectContent(
                            root = root,
                            items = items,
                            emptyText = emptyText,
                            resetKey = resetKey,
                            titleProvider = titleProvider,
                            itemTitleProvider = itemTitleProvider,
                            searchEnabled = searchEnabled,
                            onNavigateUp = { manager.up() },
                            onSearch = { manager.search(it) },
                            onClick = {
                                if (it.isGroup) {
                                    manager.open(it.id)
                                } else {
                                    selected = it
                                    onPick.invoke(it)
                                    alert?.dismiss()
                                }
                            }
                        )
                    }
                }
            }
        )

        manager.open(initialGroup)
        alert = Alerts.dialog(context, title ?: "", contentView = view, okText = null) {
            onPick.invoke(selected)
        }
    }

    fun <T : Groupable> group(
        context: Context,
        title: String?,
        okText: String = context.getString(android.R.string.ok),
        manager: GroupListManager<T>,
        titleProvider: (T?) -> String,
        itemTitleProvider: (T) -> String,
        emptyText: String,
        initialGroup: Long? = null,
        searchEnabled: Boolean = true,
        onPick: (cancelled: Boolean, item: T?) -> Unit
    ) {
        val view = createAlertContent(context)
        var root by mutableStateOf<T?>(null)
        var items by mutableStateOf<List<T>>(emptyList())
        var resetKey by mutableIntStateOf(0)

        manager.onChange = { newRoot, newItems, rootChanged ->
            root = newRoot
            items = newItems
            if (rootChanged) {
                resetKey++
            }
        }

        view.addView(
            ComposeView(context).apply {
                setContent {
                    TrailSenseTheme {
                        GroupableSelectContent(
                            root = root,
                            items = items,
                            emptyText = emptyText,
                            resetKey = resetKey,
                            titleProvider = titleProvider,
                            itemTitleProvider = itemTitleProvider,
                            searchEnabled = searchEnabled,
                            onNavigateUp = { manager.up() },
                            onSearch = { manager.search(it) },
                            onClick = {
                                if (it.isGroup) {
                                    manager.open(it.id)
                                }
                            }
                        )
                    }
                }
            }
        )

        manager.open(initialGroup)
        Alerts.dialog(context, title ?: "", contentView = view, okText = okText) {
            onPick.invoke(it, root)
        }
    }

    private fun createAlertContent(context: Context): FrameLayout {
        val padding = Resources.dp(context, 16f).toInt()
        return FrameLayout(context).apply {
            setPadding(padding, padding, padding, padding)
        }
    }

}

@Composable
private fun <T : Groupable> GroupableSelectContent(
    root: T?,
    items: List<T>,
    emptyText: String,
    resetKey: Int,
    titleProvider: (T?) -> String,
    itemTitleProvider: (T) -> String,
    searchEnabled: Boolean,
    onNavigateUp: () -> Unit,
    onSearch: (String) -> Unit,
    onClick: (T) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (root != null) {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        painter = painterResource(R.drawable.ic_prev),
                        contentDescription = null
                    )
                }
            }
            Text(
                text = titleProvider(root),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }

        if (searchEnabled) {
            var query by remember { mutableStateOf("") }
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = { Text(stringResource(R.string.search)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null
                    )
                }
            )
        }

        GroupableSelectList(
            items = items,
            emptyText = emptyText,
            resetKey = resetKey,
            itemTitleProvider = itemTitleProvider,
            onClick = onClick,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun <T : Groupable> GroupableSelectList(
    items: List<T>,
    emptyText: String,
    resetKey: Int,
    itemTitleProvider: (T) -> String,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(resetKey) {
        listState.scrollToItem(0)
    }

    if (items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        items(items, key = { if (it.isGroup) -it.id else it.id }) { item ->
            TrailSenseListItem(
                title = itemTitleProvider(item),
                onClick = { onClick(item) },
                animateEntrance = false
            )
            TrailSenseDivider()
        }
    }
}
