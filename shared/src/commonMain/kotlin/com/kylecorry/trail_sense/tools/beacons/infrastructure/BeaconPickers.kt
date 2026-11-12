package com.kylecorry.trail_sense.tools.beacons.infrastructure

import com.kylecorry.trail_sense.shared.dialogs.DialogLabels
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.grouping.picker.GroupablePickers
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.loading.BeaconLoader
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.IBeaconSort
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.NameBeaconSort
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object BeaconPickers {

    suspend fun pickBeacon(
        host: AppHost,
        title: String? = null,
        initialGroup: Long? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        sort: IBeaconSort = NameBeaconSort(),
        filter: (List<IBeacon>) -> List<IBeacon> = { it }
    ): Beacon? = suspendCoroutine { cont ->
        val loader = BeaconLoader(getAppService<IBeaconService>(), getAppService<UserPreferences>().beacons)
        val manager = GroupListManager(
            scope,
            loader,
            null,
            augment = { sort.sort(filter(it)) }
        )
        val titleProvider = { beacon: IBeacon? ->
            beacon?.name ?: getAppService<AppStrings>().get(Res.string.beacons)
        }
        GroupablePickers.item(
            host,
            title,
            manager,
            titleProvider,
            { it.name },
            getAppService<AppStrings>().get(Res.string.no_beacons),
            initialGroup,
            searchEnabled = true
        ) {
            cont.resume(it as Beacon?)
        }
    }

    suspend fun pickGroup(
        host: AppHost,
        title: String? = null,
        okText: CharSequence = DialogLabels.Ok,
        initialGroup: Long? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        sort: IBeaconSort = NameBeaconSort(),
        filter: (List<BeaconGroup>) -> List<BeaconGroup> = { it }
    ): Pair<Boolean, BeaconGroup?> = suspendCoroutine { cont ->
        val loader = BeaconLoader(getAppService<IBeaconService>(), getAppService<UserPreferences>().beacons)
        val manager = GroupListManager(
            scope,
            loader,
            null,
            augment = { sort.sort(filter(it.filterIsInstance<BeaconGroup>())) }
        )
        val titleProvider = { beacon: IBeacon? ->
            beacon?.name ?: getAppService<AppStrings>().get(Res.string.beacons)
        }
        GroupablePickers.group(
            host,
            title,
            okText,
            manager,
            titleProvider,
            { it.name },
            getAppService<AppStrings>().get(Res.string.no_groups),
            initialGroup,
            searchEnabled = false
        ) { cancelled, item ->
            cont.resume(cancelled to item as BeaconGroup?)
        }
    }

}
