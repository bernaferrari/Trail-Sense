package com.kylecorry.trail_sense.shared.grouping.picker

import android.content.Context
import android.widget.FrameLayout
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.trail_sense.shared.grouping.Groupable
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager

object GroupablePickers {

    fun <T : Groupable> item(
        context: Context,
        title: String?,
        manager: GroupListManager<T>,
        mapper: ListItemMapper<T>,
        titleProvider: (T?) -> String,
        emptyText: String,
        initialGroup: Long? = null,
        searchEnabled: Boolean = true,
        onPick: (item: T?) -> Unit
    ) {
        val view = createAlertContent(context)
        val selector = GroupableSelectView(
            context,
            null,
            manager,
            mapper,
            titleProvider,
            emptyText,
            initialGroup,
            searchEnabled
        )
        view.addView(selector)
        var selected: T? = null
        val alert =
            Alerts.dialog(context, title ?: "", contentView = view, okText = null) {
                onPick.invoke(selected)
            }
        selector.onItemClick = {
            if (!it.isGroup) {
                selected = it
                onPick.invoke(it)
                alert.dismiss()
            }
        }
    }

    fun <T : Groupable> group(
        context: Context,
        title: String?,
        okText: String = context.getString(android.R.string.ok),
        manager: GroupListManager<T>,
        mapper: ListItemMapper<T>,
        titleProvider: (T?) -> String,
        emptyText: String,
        initialGroup: Long? = null,
        searchEnabled: Boolean = true,
        onPick: (cancelled: Boolean, item: T?) -> Unit
    ) {
        val view = createAlertContent(context)
        val selector = GroupableSelectView(
            context,
            null,
            manager,
            mapper,
            titleProvider,
            emptyText,
            initialGroup,
            searchEnabled
        )
        view.addView(selector)
        Alerts.dialog(context, title ?: "", contentView = view, okText = okText) {
            onPick.invoke(it, selector.root)
        }
    }

    private fun createAlertContent(context: Context): FrameLayout {
        val padding = Resources.dp(context, 16f).toInt()
        return FrameLayout(context).apply {
            setPadding(padding, padding, padding, padding)
        }
    }

}
