package com.kylecorry.trail_sense.tools.paths.infrastructure

import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.from

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.grouping.picker.GroupablePickers
import com.kylecorry.trail_sense.tools.paths.domain.IPath
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup
import com.kylecorry.trail_sense.tools.paths.domain.pathsort.IPathSortStrategy
import com.kylecorry.trail_sense.tools.paths.domain.pathsort.NamePathSortStrategy
import com.kylecorry.trail_sense.tools.paths.ui.PathNameFactory
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object PathPickers {

    suspend fun pickGroup(
        context: Context,
        title: String? = null,
        okText: String = context.getString(android.R.string.ok),
        initialGroup: Long? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        sort: IPathSortStrategy = NamePathSortStrategy(),
        filter: (List<PathGroup>) -> List<PathGroup> = { it }
    ): Pair<Boolean, PathGroup?> = suspendCoroutine { cont ->
        val loader = PathGroupLoader(getAppService<IPathService>())
        val manager = GroupListManager(
            scope,
            loader,
            null,
            augment = { sort.sort(filter(it.filterIsInstance<PathGroup>())) }
        )
        GroupablePickers.group(
            AppHost.from(context),
            title,
            okText,
            manager,
            { getGroupTitle(context, it) },
            { (it as PathGroup).name },
            context.getString(R.string.no_groups),
            initialGroup,
            searchEnabled = false
        ) { cancelled, item ->
            cont.resume(cancelled to item as PathGroup?)
        }
    }

    suspend fun pickPath(
        context: Context,
        title: String? = null,
        initialGroup: Long? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        sort: IPathSortStrategy = NamePathSortStrategy(),
        filter: (List<IPath>) -> List<IPath> = { it }
    ): Path? = suspendCoroutine { cont ->
        val loader = PathGroupLoader(getAppService<IPathService>())
        val manager = GroupListManager(
            scope,
            loader,
            null,
            augment = { sort.sort(filter(it)) }
        )
        GroupablePickers.item(
            AppHost.from(context),
            title,
            manager,
            { getGroupTitle(context, it) },
            {
                when (it) {
                    is PathGroup -> it.name
                    is Path -> PathNameFactory().getName(it)
                    else -> ""
                }
            },
            context.getString(R.string.no_paths),
            initialGroup,
            searchEnabled = false
        ) {
            cont.resume(it as Path?)
        }
    }

    private fun getGroupTitle(context: Context, group: IPath?): String {
        return if (group is PathGroup) {
            group.name
        } else {
            context.getString(R.string.paths)
        }
    }
}
