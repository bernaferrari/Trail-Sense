package com.kylecorry.trail_sense.tools.beacons.infrastructure.commands

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RenameBeaconGroupCommand(
    private val scope: CoroutineScope,
    private val service: IBeaconService,
    private val onRenamed: () -> Unit
) {

    fun execute(group: BeaconGroup) {
        Pickers.text(
            AppHost.current(),
            getAppService<AppStrings>().get(Res.string.group),
            null,
            group.name,
            getAppService<AppStrings>().get(Res.string.name)
        ) {
            if (it != null) {
                scope.launch {
                    onIO {
                        service.add(group.copy(name = it))
                    }
                    onMain {
                        onRenamed()
                    }
                }
            }
        }
    }

}
