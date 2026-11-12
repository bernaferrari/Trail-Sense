package com.kylecorry.trail_sense.tools.beacons.infrastructure.commands

import android.content.Context
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.snackbar
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MoveBeaconGroupCommand(
    private val context: Context,
    private val scope: CoroutineScope,
    private val service: IBeaconService,
    private val onMoved: () -> Unit
) {

    fun execute(group: BeaconGroup) {
        scope.launch {
            val result = BeaconPickers.pickGroup(
                context,
                null,
                context.getString(R.string.move),
                initialGroup = group.parentId,
                filter = { it.filter { it.id != group.id } }
            )
            if (result.first) {
                return@launch
            }
            val groupId = result.second?.id
            val groupName = onIO {
                service.add(group.copy(parentId = groupId))
                result.second?.name ?: context.getString(R.string.no_group)
            }

            onMain {
                Alerts.snackbar(
                    context,
                    context.getString(R.string.moved_to, groupName)
                )
                onMoved()
            }

        }
    }

}
