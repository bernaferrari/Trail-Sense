package com.kylecorry.trail_sense.tools.beacons.infrastructure.commands

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.views.compose.AppHost

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MoveBeaconGroupCommand(
    private val scope: CoroutineScope,
    private val service: IBeaconService,
    private val onMoved: () -> Unit
) {

    fun execute(group: BeaconGroup) {
        scope.launch {
            val result = BeaconPickers.pickGroup(
                AppHost.current(),
                null,
                getAppService<AppStrings>().get(Res.string.move),
                initialGroup = group.parentId,
                filter = { it.filter { it.id != group.id } }
            )
            if (result.first) {
                return@launch
            }
            val groupId = result.second?.id
            val groupName = onIO {
                service.add(group.copy(parentId = groupId))
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
