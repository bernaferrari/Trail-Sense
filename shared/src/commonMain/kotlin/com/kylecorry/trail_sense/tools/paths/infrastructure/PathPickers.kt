package com.kylecorry.trail_sense.tools.paths.infrastructure

import com.kylecorry.trail_sense.shared.views.compose.AppHost

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.dialogs.DialogLabels
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
        host: AppHost,
        title: String? = null,
        okText: CharSequence = DialogLabels.Ok,
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
            host,
            title,
            okText,
            manager,
            { getGroupTitle(it) },
            { (it as PathGroup).name },
            getAppService<AppStrings>().get(Res.string.no_groups),
            initialGroup,
            searchEnabled = false
        ) { cancelled, item ->
            cont.resume(cancelled to item as PathGroup?)
        }
    }

    suspend fun pickPath(
        host: AppHost,
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
            host,
            title,
            manager,
            { getGroupTitle(it) },
            {
                when (it) {
                    is PathGroup -> it.name
                    is Path -> PathNameFactory().getName(it)
                    else -> ""
                }
            },
            getAppService<AppStrings>().get(Res.string.no_paths),
            initialGroup,
            searchEnabled = false
        ) {
            cont.resume(it as Path?)
        }
    }

    private fun getGroupTitle(group: IPath?): String {
        return if (group is PathGroup) {
            group.name
        } else {
            getAppService<AppStrings>().get(Res.string.paths)
        }
    }
}
