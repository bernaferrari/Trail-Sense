package com.kylecorry.trail_sense.shared.grouping.picker

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.grouping.Groupable
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.views.SearchView

@SuppressLint("ViewConstructor")
class GroupableSelectView<T : Groupable>(
    context: Context,
    attrs: AttributeSet?,
    private val manager: GroupListManager<T>,
    private val mapper: ListItemMapper<T>,
    private val titleProvider: (T?) -> String,
    emptyText: String,
    initialGroup: Long? = null,
    searchEnabled: Boolean = true
) : LinearLayout(context, attrs) {

    private val groupTitle: Toolbar
    private val searchbox: SearchView
    private val emptyTextView: TextView
    private val list: AndromedaListView

    var onItemClick: (item: T) -> Unit = {}
    var root: T? = null
        private set

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

        emptyTextView = TextView(context).apply {
            id = R.id.empty_text
            gravity = Gravity.CENTER
            setPadding(0, Resources.dp(context, 32f).toInt(), 0, 0)
            isVisible = false
        }
        addView(
            emptyTextView,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        )

        list = AndromedaListView(context, null).apply {
            id = R.id.list
            setPadding(0, 0, 0, 0)
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
        list.emptyView = emptyTextView
        emptyTextView.text = emptyText
        manager.onChange = { root, items, rootChanged ->
            this.root = root
            groupTitle.leftButton.isVisible = root != null
            groupTitle.title.text = titleProvider(root)
            val mapped = items.map {
                // TODO: Don't override anything, just intercept the click action to open folders (let the caller decide what to do on click)
                mapper.map(it).copy(menu = emptyList(), trailingIcon = null, action = {
                    if (it.isGroup) {
                        manager.open(it.id)
                    }
                    onItemClick(it)
                })
            }
            list.setItems(mapped)
            if (rootChanged) {
                list.scrollToPosition(0, false)
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
