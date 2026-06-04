package com.kylecorry.trail_sense.shared.grouping.picker

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.grouping.Groupable
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem

@SuppressLint("ViewConstructor")
class GroupableSelectView<T : Groupable>(
    context: Context,
    attrs: AttributeSet?,
    private val manager: GroupListManager<T>,
    private val titleProvider: (T?) -> String,
    private val itemTitleProvider: (T) -> String,
    emptyText: String,
    initialGroup: Long? = null,
    searchEnabled: Boolean = true
) : LinearLayout(context, attrs) {

    private val groupTitle: Toolbar
    private val searchbox: SearchView
    private val list: ComposeView

    var onItemClick: (item: T) -> Unit = {}
    var root: T? = null
        private set
    private var items by mutableStateOf<List<T>>(emptyList())
    private var resetKey by mutableIntStateOf(0)

    init {
        orientation = VERTICAL

        groupTitle = Toolbar(context, null).apply {
            id = R.id.group_title
            leftButton.setImageResource(R.drawable.ic_prev)
            leftButton.isVisible = false
            subtitle.isVisible = false
        }
        addView(
            groupTitle,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        searchbox = SearchView(context, null).apply {
            id = R.id.searchbox
        }
        addView(
            searchbox,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = Resources.dp(context, 16f).toInt()
                rightMargin = Resources.dp(context, 16f).toInt()
            }
        )

        list = ComposeView(context).apply {
            id = R.id.list
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                TrailSenseTheme {
                    GroupableSelectList(
                        items = items,
                        emptyText = emptyText,
                        resetKey = resetKey,
                        itemTitleProvider = itemTitleProvider,
                        onClick = {
                            if (it.isGroup) {
                                manager.open(it.id)
                            }
                            onItemClick(it)
                        }
                    )
                }
            }
        }
        addView(
            list,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = Resources.dp(context, 16f).toInt()
            }
        )

        // Search
        searchbox.isVisible = searchEnabled
        searchbox.setOnSearchListener {
            manager.search(searchbox.query)
        }

        // List items
        manager.onChange = { root, items, rootChanged ->
            this.root = root
            groupTitle.leftButton.isVisible = root != null
            groupTitle.title.text = titleProvider(root)
            this.items = items
            if (rootChanged) {
                resetKey++
            }
        }

        // Back
        groupTitle.leftButton.isVisible = false
        groupTitle.leftButton.setOnClickListener {
            manager.up()
        }

        manager.open(initialGroup)
    }
}

@Composable
private fun <T : Groupable> GroupableSelectList(
    items: List<T>,
    emptyText: String,
    resetKey: Int,
    itemTitleProvider: (T) -> String,
    onClick: (T) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(resetKey) {
        listState.scrollToItem(0)
    }

    if (items.isEmpty()) {
        Box(
            modifier = Modifier
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

    LazyColumn(state = listState) {
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
