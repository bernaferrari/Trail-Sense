package com.kylecorry.trail_sense.tools.beacons.infrastructure.commands

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DeleteBeaconCommand(
    private val scope: CoroutineScope,
    private val service: IBeaconService,
    private val onDeleted: () -> Unit
) {

    fun execute(beacon: Beacon) {
        Alerts.dialog(
            AppHost.current(),
            getAppService<AppStrings>().get(Res.string.delete),
            beacon.name
        ) { cancelled ->
            if (!cancelled) {
                scope.launch {
                    onIO {
                        service.delete(beacon)
                    }
                    onMain {
                        onDeleted()
                    }
                }
            }
        }
    }

}
