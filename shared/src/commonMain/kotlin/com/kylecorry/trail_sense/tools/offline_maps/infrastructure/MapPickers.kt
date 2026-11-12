package com.kylecorry.trail_sense.tools.offline_maps.infrastructure

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.DialogLabels
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.grouping.picker.GroupablePickers
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.groups.MapGroupLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object MapPickers {

    // TODO: Add sort
    suspend fun pickGroup(
        host: AppHost,
        title: String? = null,
        okText: CharSequence = DialogLabels.Ok,
        initialGroup: Long? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        filter: (List<MapGroup>) -> List<MapGroup> = { it }
    ): Pair<Boolean, MapGroup?> = suspendCoroutine { cont ->
        val strings = getAppService<AppStrings>()
        val loader = MapGroupLoader(getAppService<MapService>().loader)
        val manager = GroupListManager(
            scope,
            loader,
            null,
            augment = { filter(it.filterIsInstance<MapGroup>()) }
        )
        GroupablePickers.group(
            host,
            title,
            okText,
            manager,
            { it?.name ?: strings.get(Res.string.photo_maps) },
            { it.name },
            strings.get(Res.string.no_groups),
            initialGroup,
            searchEnabled = false
        ) { cancelled, item ->
            cont.resume(cancelled to item as MapGroup?)
        }
    }

}
