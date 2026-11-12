package com.kylecorry.trail_sense.tools.beacons.infrastructure.commands

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.views.compose.AppHost

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// TODO: Consolidate with move beacon group
class MoveBeaconCommand(
    private val scope: CoroutineScope,
    private val service: IBeaconService,
    private val onMoved: () -> Unit
) {

    fun execute(beacon: Beacon) {
        scope.launch {
            val result = BeaconPickers.pickGroup(
                AppHost.current(),
                null,
                getAppService<AppStrings>().get(Res.string.move),
                initialGroup = beacon.parentId
            )
            if (result.first) {
                return@launch
            }
            val groupId = result.second?.id
            val groupName = onIO {
                service.add(beacon.copy(parentId = groupId))
                result.second?.name ?: getAppService<AppStrings>().get(Res.string.no_group)
            }

            onMain {
                Alerts.snackbar(AppHost.current(),
                    getAppService<AppStrings>().get(Res.string.moved_to, groupName)
                )
                onMoved()
            }

        }
    }

}
